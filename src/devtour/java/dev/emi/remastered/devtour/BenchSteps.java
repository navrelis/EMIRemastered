package dev.emi.remastered.devtour;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.emi.emi.registry.EmiRecipes;
import dev.emi.emi.runtime.EmiReloadManager;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;

/**
 * Dev-only steps for the EMI reload benchmark (scripts/perf/run_bench.py, tours {@code perf-bench} and
 * {@code perf-profile}), registered by {@link BenchMod}. Timings come from {@link PhaseTimer}; each measured reload is
 * appended as one JSON line to {@code bench.jsonl} in the tour output folder, with heap figures: heap used after a
 * full GC before and after the reload (retained state) and the bytes allocated by all threads during the reload
 * (includes the async recipe sorting, which the step waits for).
 */
final class BenchSteps {
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
	private static final double MB = 1024.0 * 1024.0;
	private static Recording recording;

	private BenchSteps() {
	}

	private static boolean idle() {
		return EmiReloadManager.isLoaded() && EmiRecipes.activeWorker == null;
	}

	private static long heapUsedAfterGc() {
		System.gc();
		MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
		return memory.getHeapMemoryUsage().getUsed();
	}

	private static long allocatedBytes() {
		if (ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean threads
				&& threads.isThreadAllocatedMemorySupported() && threads.isThreadAllocatedMemoryEnabled()) {
			return threads.getTotalThreadAllocatedBytes();
		}
		return -1;
	}

	private static double mb(long bytes) {
		return Math.round(bytes / MB * 10) / 10.0;
	}

	private static void record(TourContext ctx, String label, Map<String, Object> extra) throws Exception {
		PhaseTimer timer = PhaseTimer.get();
		if (timer == null) {
			throw new IllegalStateException("phase timer not installed");
		}
		Map<String, Object> phases = PhaseTimer.phases(timer.drain());
		if (phases.isEmpty()) {
			throw new IllegalStateException("no complete EMI reload in the log since the last measurement");
		}
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("label", label);
		line.putAll(phases);
		line.putAll(extra);
		line.put("heap after reload (MB, after GC)", mb(heapUsedAfterGc()));
		Files.createDirectories(ctx.outDir);
		Files.writeString(ctx.outDir.resolve("bench.jsonl"), GSON.toJson(line) + "\n", StandardCharsets.UTF_8,
			StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		ctx.info(label + ": total " + phases.get("total") + " ms, recipes " + phases.get("recipes"));
	}

	static void register() {
		// {"type":"bench_record","label":"join"}  records the last complete EMI reload (e.g. the one on world join)
		Steps.register("bench_record", (args, ctx) -> () -> {
			if (!idle()) {
				return false;
			}
			record(ctx, args.str("label", "join"), Map.of());
			return true;
		});

		// {"type":"emi_reload","label":"warm-1","settleTicks":40}  full GC, waits settleTicks, triggers an EMI reload
		// (EmiReloadManager.reload(), the same as the reload after F3+T), waits until EMI and its async recipe
		// sorting are done and records the phase timings and heap figures
		Steps.register("emi_reload", (args, ctx) -> {
			String label = args.str("label", "reload");
			int settle = args.intVal("settleTicks", 40);
			int[] ticks = {0};
			long[] before = {0, 0};
			boolean[] started = {false};
			return () -> {
				if (!started[0]) {
					if (!idle()) {
						return false;
					}
					if (++ticks[0] < settle) {
						return false;
					}
					PhaseTimer.get().drain();
					before[0] = heapUsedAfterGc();
					before[1] = allocatedBytes();
					EmiReloadManager.reload();
					started[0] = true;
					return false;
				}
				if (!idle()) {
					return false;
				}
				long allocated = allocatedBytes();
				Map<String, Object> extra = new LinkedHashMap<>();
				extra.put("heap before reload (MB, after GC)", mb(before[0]));
				if (allocated >= 0 && before[1] >= 0) {
					extra.put("allocated during reload (MB, all threads)", mb(allocated - before[1]));
				}
				record(ctx, label, extra);
				return true;
			};
		});

		// {"type":"emi_dump","name":"final"}  writes EMI's baked state to <out>/dump-<name>/ (see StateDump)
		Steps.register("emi_dump", (args, ctx) -> () -> {
			if (!idle()) {
				return false;
			}
			Path dir = ctx.outDir.resolve("dump-" + args.str("name", "state"));
			Map<String, String> hashes = StateDump.write(dir);
			ctx.info("dump " + dir.getFileName() + ": " + hashes);
			return true;
		});

		// {"type":"jfr_start"}  starts a JFR recording with the JDK's "profile" settings
		Steps.register("jfr_start", (args, ctx) -> {
			if (recording != null) {
				throw new IllegalStateException("a JFR recording is already running");
			}
			recording = new Recording(Configuration.getConfiguration("profile"));
			recording.setName("emi-bench");
			recording.start();
			return StepRun.DONE;
		});

		// {"type":"jfr_stop","name":"reload"}  stops the recording and writes <out>/<name>.jfr
		Steps.register("jfr_stop", (args, ctx) -> {
			if (recording == null) {
				throw new IllegalStateException("no JFR recording running");
			}
			Path file = ctx.outDir.resolve(args.str("name", "reload") + ".jfr");
			Files.createDirectories(ctx.outDir);
			recording.stop();
			recording.dump(file);
			recording.close();
			recording = null;
			ctx.info("wrote " + file.getFileName());
			return StepRun.DONE;
		});
	}
}
