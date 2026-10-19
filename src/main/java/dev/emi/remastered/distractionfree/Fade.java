package dev.emi.remastered.distractionfree;

/** Animation maths of the sidebar fade. Pure logic, no Minecraft classes. */
public final class Fade {
	/** Longest accepted duration of the setting, in milliseconds. */
	public static final int MAX_DURATION_MS = 2000;

	private Fade() {
	}

	public static int clampDuration(int durationMs) {
		return Math.max(0, Math.min(MAX_DURATION_MS, durationMs));
	}

	/**
	 * Moves {@code progress} (0 = hidden, 1 = shown) towards the target in a time of {@code durationMs} for the whole
	 * way; 0 ms jumps right away.
	 */
	public static float step(float progress, boolean visible, long elapsedNanos, int durationMs) {
		int duration = clampDuration(durationMs);
		if (duration == 0) {
			return visible ? 1f : 0f;
		}
		float delta = elapsedNanos / (duration * 1_000_000f);
		float next = visible ? progress + delta : progress - delta;
		return Math.max(0f, Math.min(1f, next));
	}

	/** Smoothstep: the visual value of a linear progress. */
	public static float ease(float progress) {
		float p = Math.max(0f, Math.min(1f, progress));
		return p * p * (3f - 2f * p);
	}
}
