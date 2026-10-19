package dev.emi.remastered.devtour;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.emi.emi.runtime.EmiReloadLog;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.client.MinecraftClient;

/**
 * The tour state machine, driven by the client tick: wait for the world, wait for EMI, warm up, run the steps one
 * after another, write summary.json, stop the client. A watchdog thread guarantees the game never hangs forever.
 */
public final class TourRunner {
	private static final Logger LOG = LoggerFactory.getLogger("EmiDevTour");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private enum State { WAIT_WORLD, WAIT_EMI, WARMUP, RUN, FINISHED }

	private final MinecraftClient client;
	private final LogCapture logs;
	private final String tourArg;
	private final Path gameDir;
	private final long startMs = System.currentTimeMillis();

	private TourPlan plan;
	private TourContext ctx;
	private Path outDir;
	private String planError;

	private State state = State.WAIT_WORLD;
	private long stateSinceMs = startMs;
	private int warmupLeft;
	private boolean emiWasLoaded = false;

	private int nextStep = 0;
	private StepRun current;
	private JsonObject currentSpec;
	private long currentStartMs;
	private final List<JsonObject> results = new ArrayList<>();
	private volatile boolean finished = false;
	private String resultStatus = "running";
	private String resultReason = null;

	public TourRunner(MinecraftClient client, LogCapture logs, String tourArg, Path gameDir) {
		this.client = client;
		this.logs = logs;
		this.tourArg = tourArg;
		this.gameDir = gameDir;
		try {
			plan = TourPlan.load(tourArg, gameDir);
			plan.systemProperties.forEach((key, value) -> {
				System.setProperty(key, value);
				LOG.info("tour sets system property {}={}", key, value);
			});
			outDir = gameDir.resolve("tour-out").resolve(plan.name);
			cleanDir(outDir);
			ctx = new TourContext(client, plan, outDir);
			warmupLeft = plan.warmupTicks;
			LOG.info("tour '{}' loaded from {} ({} steps), output {}", plan.name, plan.source, plan.steps.size(), outDir);
		} catch (Throwable t) {
			planError = "cannot load tour '" + tourArg + "': " + t;
			outDir = gameDir.resolve("tour-out").resolve(tourArg.replaceAll("[^A-Za-z0-9._-]", "_"));
			LOG.error(planError);
		}
		long timeout = (plan != null ? plan.timeoutSeconds : 60) * 1000L;
		Thread watchdog = new Thread(() -> watch(startMs + timeout), "emi-devtour-watchdog");
		watchdog.setDaemon(true);
		watchdog.start();
	}

	TourContext context() {
		return ctx;
	}

	// ------------------------------------------------------------------ driving

	public void onTick(MinecraftClient mc) {
		if (finished) {
			return;
		}
		try {
			if (planError != null) {
				finish("failed", planError);
				return;
			}
			switch (state) {
				case WAIT_WORLD -> waitWorld(mc);
				case WAIT_EMI -> waitEmi();
				case WARMUP -> {
					if (--warmupLeft <= 0) {
						setState(State.RUN);
						LOG.info("warmup done, running {} steps", plan.steps.size());
					}
				}
				case RUN -> runSteps();
				case FINISHED -> { }
			}
		} catch (Throwable t) {
			LOG.error("tour runner crashed", t);
			finish("failed", "tour runner crashed: " + t);
		}
	}

	private void setState(State s) {
		state = s;
		stateSinceMs = System.currentTimeMillis();
	}

	private long inState() {
		return System.currentTimeMillis() - stateSinceMs;
	}

	private void waitWorld(MinecraftClient mc) {
		if (mc.world != null && mc.player != null && mc.currentScreen == null) {
			// a lost window focus must not pop up the pause menu in the middle of the tour
			mc.options.pauseOnLostFocus = false;
			LOG.info("world joined after {} ms, waiting for EMI", System.currentTimeMillis() - startMs);
			setState(State.WAIT_EMI);
		} else if (inState() > plan.worldJoinTimeoutSeconds * 1000L) {
			String screen = mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getSimpleName();
			finish("failed", "world not joined within " + plan.worldJoinTimeoutSeconds + " s (world=" + (mc.world != null)
				+ ", screen=" + screen + "); does run/saves/devworld exist? (gradlew runServer once)");
		}
	}

	private void waitEmi() {
		if (EmiReloadManager.isLoaded()) {
			emiWasLoaded = true;
			LOG.info("EMI loaded (status {}), warming up {} ticks", EmiReloadManager.getStatus(), warmupLeft);
			setState(State.WARMUP);
		} else if (inState() > plan.worldJoinTimeoutSeconds * 1000L) {
			LOG.warn("EMI not loaded after {} s (status {}); running the steps anyway", plan.worldJoinTimeoutSeconds,
				EmiReloadManager.getStatus());
			setState(State.RUN);
		}
	}

	private void runSteps() {
		if (current == null) {
			if (nextStep >= plan.steps.size()) {
				boolean anyFailed = results.stream().anyMatch(r -> !"ok".equals(r.get("status").getAsString()));
				finish(anyFailed ? "failed" : "ok", anyFailed ? "one or more steps failed" : null);
				return;
			}
			currentSpec = plan.steps.get(nextStep++);
			currentStartMs = System.currentTimeMillis();
			String type = currentSpec.has("type") ? currentSpec.get("type").getAsString() : "";
			StepHandler handler = Steps.get(type);
			if (handler == null) {
				completeStep(false, "unknown step type '" + type + "'; known: " + String.join(", ", Steps.types()));
				return;
			}
			try {
				StepRun run = handler.begin(new Args(currentSpec), ctx);
				if (run == null || run == StepRun.DONE) {
					completeStep(true, null);
				} else {
					current = run;
				}
			} catch (Throwable t) {
				completeStep(false, describe(t));
			}
			return;
		}
		try {
			if (current.tick()) {
				completeStep(true, null);
			} else if (System.currentTimeMillis() - currentStartMs > plan.stepTimeoutSeconds * 1000L) {
				completeStep(false, "step timed out after " + plan.stepTimeoutSeconds + " s");
			}
		} catch (Throwable t) {
			completeStep(false, describe(t));
		}
	}

	private static String describe(Throwable t) {
		String m = t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : "");
		if (!(t instanceof IllegalStateException) && !(t instanceof IllegalArgumentException)) {
			StackTraceElement[] st = t.getStackTrace();
			if (st.length > 0) {
				m += " @ " + st[0];
			}
		}
		return m;
	}

	private void completeStep(boolean ok, String error) {
		JsonObject r = new JsonObject();
		r.addProperty("index", nextStep);
		r.add("spec", currentSpec);
		r.addProperty("type", currentSpec.has("type") ? currentSpec.get("type").getAsString() : "");
		r.addProperty("status", ok ? "ok" : "failed");
		if (error != null) {
			r.addProperty("error", error);
		}
		r.addProperty("durationMs", System.currentTimeMillis() - currentStartMs);
		String info = ctx.takeInfo();
		if (info != null) {
			r.addProperty("info", info);
		}
		String shot = ctx.takeScreenshotName();
		if (shot != null) {
			r.addProperty("screenshot", shot);
		}
		r.addProperty("screen", client.currentScreen == null ? "none" : client.currentScreen.getClass().getSimpleName());
		results.add(r);
		LOG.info("step {}/{} {} -> {}{}", nextStep, plan.steps.size(), r.get("type").getAsString(), ok ? "ok" : "FAILED",
			ok ? "" : " (" + error + ")");
		current = null;
		currentSpec = null;
		writeSummary();
	}

	// ------------------------------------------------------------------ finishing

	private synchronized void finish(String status, String reason) {
		if (finished) {
			return;
		}
		finished = true;
		state = State.FINISHED;
		// a step that was running when we were cut off is reported as failed
		if (current != null && currentSpec != null) {
			current = null;
			JsonObject r = new JsonObject();
			r.addProperty("index", nextStep);
			r.add("spec", currentSpec);
			r.addProperty("type", currentSpec.has("type") ? currentSpec.get("type").getAsString() : "");
			r.addProperty("status", "failed");
			r.addProperty("error", "interrupted: " + reason);
			results.add(r);
		}
		resultStatus = status;
		resultReason = reason;
		writeSummary();
		LOG.info("tour finished: {}{}; summary: {}", status, reason == null ? "" : " (" + reason + ")", outDir.resolve("summary.json"));
		client.scheduleStop();
		Thread guard = new Thread(() -> {
			try {
				Thread.sleep(45_000);
			} catch (InterruptedException ignored) {
				return;
			}
			System.err.println("[emi_devtour] client did not stop within 45 s, halting the JVM");
			Runtime.getRuntime().halt(3);
		}, "emi-devtour-exit-guard");
		guard.setDaemon(true);
		guard.start();
	}

	private void watch(long deadlineMs) {
		while (!finished) {
			if (System.currentTimeMillis() > deadlineMs) {
				LOG.error("global tour timeout reached, stopping the client");
				finish("timeout", "global timeout of " + (plan != null ? plan.timeoutSeconds : 60) + " s reached in state " + state);
				return;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				return;
			}
		}
	}

	// ------------------------------------------------------------------ summary

	private synchronized void writeSummary() {
		try {
			JsonObject root = new JsonObject();
			root.addProperty("tour", plan != null ? plan.name : tourArg);
			root.addProperty("source", plan != null ? plan.source : "n/a");
			root.addProperty("status", resultStatus);
			if (resultReason != null) {
				root.addProperty("reason", resultReason);
			}
			root.addProperty("state", state.name());
			root.addProperty("durationMs", System.currentTimeMillis() - startMs);
			root.addProperty("outputDir", outDir.toString());

			JsonObject emi = new JsonObject();
			emi.addProperty("loaded", emiWasLoaded || EmiReloadManager.isLoaded());
			emi.addProperty("status", EmiReloadManager.getStatus());
			emi.addProperty("reloadMs", logs != null ? logs.reloadMs() : -1);
			if (logs != null) {
				emi.add("pluginInitMs", GSON.toJsonTree(logs.pluginInitMs()));
				emi.add("pluginReloadMs", GSON.toJsonTree(logs.pluginReloadMs()));
			}
			emi.addProperty("reloadWarningCount", EmiReloadLog.warningCount);
			emi.add("reloadLog", GSON.toJsonTree(EmiReloadLog.warnings));
			root.add("emi", emi);

			int ok = 0, failed = 0;
			JsonArray steps = new JsonArray();
			for (JsonObject r : results) {
				steps.add(r);
				if ("ok".equals(r.get("status").getAsString())) {
					ok++;
				} else {
					failed++;
				}
			}
			root.addProperty("stepsTotal", plan != null ? plan.steps.size() : 0);
			root.addProperty("stepsOk", ok);
			root.addProperty("stepsFailed", failed);
			root.add("steps", steps);

			List<Map<String, Object>> entries = logs != null ? logs.entries() : List.of();
			root.addProperty("logWarningCount", entries.size());
			root.addProperty("logWarningsDropped", logs != null ? logs.dropped() : 0);
			root.add("logWarnings", GSON.toJsonTree(entries));

			Files.createDirectories(outDir);
			Files.writeString(outDir.resolve("summary.json"), GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (Throwable t) {
			LOG.error("could not write summary.json", t);
		}
	}

	private static void cleanDir(Path dir) throws IOException {
		if (Files.isDirectory(dir)) {
			try (Stream<Path> walk = Files.walk(dir)) {
				for (Path p : (Iterable<Path>) walk.sorted(Comparator.reverseOrder())::iterator) {
					if (!p.equals(dir)) {
						Files.delete(p);
					}
				}
			}
		}
		Files.createDirectories(dir);
	}
}
