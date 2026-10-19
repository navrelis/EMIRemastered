package dev.emi.remastered.devtour;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
 * Log4j appender added at runtime. Collects WARN/ERROR/FATAL lines that concern EMI (logger name or message
 * mentions emi / remastered, or the throwable has a dev.emi frame) and parses EMI's reload timing info lines.
 */
public final class LogCapture extends AbstractAppender {
	private static final int MAX_ENTRIES = 500;
	private static final Pattern RELOAD_TOTAL = Pattern.compile("Reloaded EMI in (\\d+)ms");
	private static final Pattern RELOAD_PLUGIN = Pattern.compile("Reloaded plugin from (.+) in (\\d+)ms");
	private static final Pattern INIT_PLUGIN = Pattern.compile("Initialized plugin from (.+) in (\\d+)ms");

	private final long t0 = System.currentTimeMillis();
	private final List<Map<String, Object>> entries = new ArrayList<>();
	private int dropped = 0;
	private long reloadMs = -1;
	private final Map<String, Long> pluginReloadMs = new LinkedHashMap<>();
	private final Map<String, Long> pluginInitMs = new LinkedHashMap<>();

	private LogCapture() {
		super("EmiTourCapture", (Filter) null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
	}

	/** Installs the appender on the root logger. Returns null (and the tour continues) if log4j-core is not reachable. */
	public static LogCapture install() {
		try {
			LoggerContext ctx = (LoggerContext) LogManager.getContext(LogCapture.class.getClassLoader(), false);
			LogCapture capture = new LogCapture();
			capture.start();
			ctx.getConfiguration().getRootLogger().addAppender(capture, Level.INFO, null);
			return capture;
		} catch (Throwable t) {
			System.err.println("[emi_devtour] could not install log capture: " + t);
			return null;
		}
	}

	@Override
	public void append(LogEvent event) {
		try {
			String message = event.getMessage() == null ? "" : event.getMessage().getFormattedMessage();
			if (message == null) {
				message = "";
			}
			if (event.getLevel().isMoreSpecificThan(Level.INFO) && event.getLevel().intLevel() > Level.WARN.intLevel()) {
				parseTiming(message);
				return;
			}
			// intLevel: lower = more severe. WARN(300), ERROR(200), FATAL(100)
			if (event.getLevel().intLevel() > Level.WARN.intLevel()) {
				return;
			}
			Throwable thrown = event.getThrown();
			String logger = event.getLoggerName() == null ? "" : event.getLoggerName();
			if (!mentionsEmi(logger, message, thrown)) {
				return;
			}
			synchronized (this) {
				if (entries.size() >= MAX_ENTRIES) {
					dropped++;
					return;
				}
				Map<String, Object> e = new LinkedHashMap<>();
				e.put("tMs", System.currentTimeMillis() - t0);
				e.put("level", event.getLevel().name());
				e.put("logger", logger);
				e.put("thread", event.getThreadName());
				e.put("message", message);
				if (thrown != null) {
					e.put("thrown", shorten(thrown));
				}
				entries.add(e);
			}
		} catch (Throwable ignored) {
			// never let the capture break logging
		}
	}

	private void parseTiming(String message) {
		Matcher m = RELOAD_TOTAL.matcher(message);
		if (m.find()) {
			synchronized (this) {
				reloadMs = Long.parseLong(m.group(1));
			}
			return;
		}
		m = RELOAD_PLUGIN.matcher(message);
		if (m.find()) {
			synchronized (this) {
				pluginReloadMs.put(m.group(1), Long.parseLong(m.group(2)));
			}
			return;
		}
		m = INIT_PLUGIN.matcher(message);
		if (m.find()) {
			synchronized (this) {
				pluginInitMs.put(m.group(1), Long.parseLong(m.group(2)));
			}
		}
	}

	private static boolean mentionsEmi(String logger, String message, Throwable thrown) {
		if (hit(logger) || hit(message)) {
			return true;
		}
		for (Throwable t = thrown; t != null; t = t.getCause()) {
			if (hit(t.getMessage())) {
				return true;
			}
			for (StackTraceElement el : t.getStackTrace()) {
				if (el.getClassName().startsWith("dev.emi.")) {
					return true;
				}
			}
			if (t.getCause() == t) {
				break;
			}
		}
		return false;
	}

	private static boolean hit(String s) {
		if (s == null) {
			return false;
		}
		return EMI_WORD.matcher(s).find();
	}

	// "emi" as a word (EMI, [EMI], emi_loot, JEMI, EMIffect), but not inside e.g. "emissive" or "semi"
	private static final Pattern EMI_WORD = Pattern.compile(
		"(?<![a-z])j?emi(?:ffect|professions|(?![a-z]))|remastered", Pattern.CASE_INSENSITIVE);

	private static String shorten(Throwable t) {
		StringBuilder sb = new StringBuilder();
		int depth = 0;
		for (Throwable c = t; c != null && depth < 4; c = c.getCause(), depth++) {
			if (depth > 0) {
				sb.append(" <- caused by ");
			}
			sb.append(c.getClass().getName()).append(": ").append(c.getMessage());
			StackTraceElement[] st = c.getStackTrace();
			for (int i = 0; i < st.length && i < 3; i++) {
				sb.append(" @ ").append(st[i]);
			}
			if (c.getCause() == c) {
				break;
			}
		}
		return sb.toString();
	}

	public synchronized List<Map<String, Object>> entries() {
		return new ArrayList<>(entries);
	}

	public synchronized int dropped() {
		return dropped;
	}

	public synchronized long reloadMs() {
		return reloadMs;
	}

	public synchronized Map<String, Long> pluginReloadMs() {
		return new LinkedHashMap<>(pluginReloadMs);
	}

	public synchronized Map<String, Long> pluginInitMs() {
		return new LinkedHashMap<>(pluginInitMs);
	}
}
