package dev.emi.remastered.devtour;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Dev-only reload phase timer for the EMI reload benchmark. EMI logs a line at every phase boundary of
 * {@code EmiReloadManager.ReloadWorker.run} ("Processing tags", "Constructing index", "Loading plugin from X", ...);
 * this Log4j appender records the timestamp of each such line, so the phases can be timed without touching EMI's
 * code. {@link #drain()} returns the lines recorded since the last call; {@link #phases(List)} turns the lines of
 * one complete reload into durations.
 */
public final class PhaseTimer extends AbstractAppender {
	private static final Pattern PLUGIN_LOAD = Pattern.compile("^\\[EMI\\] Loading plugin from (.+)$");
	private static final Pattern PLUGIN_DONE = Pattern.compile("^\\[EMI\\] Reloaded plugin from (.+) in (\\d+)ms$");
	private static final Pattern BAKED = Pattern.compile("^\\[EMI\\] Baked (\\d+) recipes in (\\d+)ms$");
	private static final Pattern BAKED_ASYNC = Pattern.compile("^\\[EMI\\] Baked recipes after reload in (\\d+)ms$");
	private static final Pattern TOTAL = Pattern.compile("^\\[EMI\\] Reloaded EMI in (\\d+)ms$");
	private static final Pattern GROUPS = Pattern.compile("^\\[EMI\\] Reloading item groups on client thread took (\\d+)ms$");

	private static PhaseTimer instance;

	public record Line(long nanos, String thread, String message, String thrown) {
	}

	private final List<Line> lines = new ArrayList<>();

	private PhaseTimer() {
		super("EmiBenchPhaseTimer", (Filter) null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
	}

	public static synchronized PhaseTimer install() {
		if (instance == null) {
			try {
				LoggerContext ctx = (LoggerContext) LogManager.getContext(PhaseTimer.class.getClassLoader(), false);
				PhaseTimer timer = new PhaseTimer();
				timer.start();
				ctx.getConfiguration().getRootLogger().addAppender(timer, Level.INFO, null);
				instance = timer;
			} catch (Throwable t) {
				System.err.println("[emi_devtour] could not install the phase timer: " + t);
			}
		}
		return instance;
	}

	public static PhaseTimer get() {
		return instance;
	}

	@Override
	public void append(LogEvent event) {
		if (!"EMI".equals(event.getLoggerName())) {
			return;
		}
		String message = event.getMessage() == null ? "" : event.getMessage().getFormattedMessage();
		long nanos = event.getInstant().getEpochSecond() * 1_000_000_000L + event.getInstant().getNanoOfSecond();
		String thrown = event.getThrown() == null ? null : event.getThrown().getClass().getName();
		synchronized (this) {
			lines.add(new Line(nanos, event.getThreadName(), message, thrown));
		}
	}

	public synchronized List<Line> drain() {
		List<Line> copy = new ArrayList<>(lines);
		lines.clear();
		return copy;
	}

	/**
	 * Durations in ms of the last complete reload in {@code lines} (from "Starting EMI reload..." to "Reloaded EMI
	 * in"), keyed by phase; plugin register times are keyed {@code plugin <id>}. Also counts the repair exceptions
	 * and recipes. Returns an empty map if there is no complete reload.
	 */
	public static Map<String, Object> phases(List<Line> lines) {
		int end = -1;
		for (int i = lines.size() - 1; i >= 0; i--) {
			if (TOTAL.matcher(lines.get(i).message()).matches()) {
				end = i;
				break;
			}
		}
		Map<String, Object> out = new LinkedHashMap<>();
		if (end < 0) {
			return out;
		}
		String thread = lines.get(end).thread();
		int start = -1;
		for (int i = end; i >= 0; i--) {
			Line l = lines.get(i);
			if (l.thread().equals(thread) && l.message().equals("[EMI] Starting EMI reload...")) {
				start = i;
				break;
			}
		}
		if (start < 0) {
			return out;
		}
		Map<String, Long> marks = new LinkedHashMap<>();
		Map<String, Double> plugins = new LinkedHashMap<>();
		String loadingPlugin = null;
		long loadingSince = 0;
		int repairErrors = 0;
		int errors = 0;
		for (int i = start; i <= end; i++) {
			Line l = lines.get(i);
			if (!l.thread().equals(thread)) {
				continue;
			}
			String m = l.message();
			if (l.thrown() != null) {
				errors++;
				if (m.equals("Exception thrown registering repair recipes")) {
					repairErrors++;
				}
			}
			Matcher pm;
			if ((pm = PLUGIN_LOAD.matcher(m)).matches()) {
				marks.putIfAbsent("plugins", l.nanos());
				loadingPlugin = pm.group(1);
				loadingSince = l.nanos();
			} else if ((pm = PLUGIN_DONE.matcher(m)).matches()) {
				if (pm.group(1).equals(loadingPlugin)) {
					plugins.put("plugin " + loadingPlugin, ms(l.nanos() - loadingSince));
				}
				loadingPlugin = null;
			} else if (m.startsWith("[EMI] Initializing plugin from ")) {
				marks.putIfAbsent("init", l.nanos());
			} else if ((pm = BAKED.matcher(m)).matches()) {
				marks.put("bom+persistent", l.nanos());
				out.put("recipes", Integer.parseInt(pm.group(1)));
			} else if ((pm = GROUPS.matcher(m)).matches()) {
				out.put("item groups (render thread, ms)", Integer.parseInt(pm.group(1)));
			} else {
				switch (m) {
					case "[EMI] Starting EMI reload..." -> marks.put("clear", l.nanos());
					case "[EMI] Processing tags" -> marks.put("tags", l.nanos());
					case "[EMI] Constructing index" -> marks.put("stack list", l.nanos());
					case "[EMI] Baking index" -> marks.put("stack bake", l.nanos());
					case "[EMI] Registering late recipes" -> marks.put("late recipes", l.nanos());
					case "[EMI] Baking recipes" -> marks.put("recipe bake", l.nanos());
					case "[EMI] Baking search" -> marks.put("search bake", l.nanos());
					case "[EMI] Finishing up" -> marks.put("finishing", l.nanos());
					default -> { }
				}
			}
		}
		marks.put("end", lines.get(end).nanos());
		List<Map.Entry<String, Long>> ordered = new ArrayList<>(marks.entrySet());
		ordered.sort(Map.Entry.comparingByValue());
		for (int i = 0; i + 1 < ordered.size(); i++) {
			out.put(ordered.get(i).getKey(), ms(ordered.get(i + 1).getValue() - ordered.get(i).getValue()));
		}
		out.put("total", ms(lines.get(end).nanos() - lines.get(start).nanos()));
		Matcher tm = TOTAL.matcher(lines.get(end).message());
		if (tm.matches()) {
			out.put("total (EMI log, ms)", Integer.parseInt(tm.group(1)));
		}
		out.putAll(plugins);
		for (int i = start; i < lines.size(); i++) {
			Matcher am = BAKED_ASYNC.matcher(lines.get(i).message());
			if (am.matches()) {
				out.put("async sorted manager (ms)", Integer.parseInt(am.group(1)));
			}
		}
		out.put("repair exceptions", repairErrors);
		out.put("exceptions logged", errors);
		return out;
	}

	private static double ms(long nanos) {
		return Math.round(nanos / 10_000.0) / 100.0;
	}
}
