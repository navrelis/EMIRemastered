package dev.emi.remastered.info.core;

import java.util.Objects;

/**
 * Turns "what the cached pages depend on" into a generation number for {@link GenerationCache}. The identity stamp is
 * compared with {@code ==} (EMI's recipe manager object, replaced on every EMI reload), the value stamp with
 * {@code equals} (registry version, module switches, settings). Pure Java (unit tested); not thread safe.
 */
public final class ReloadGeneration {
	private Object identity;
	private Object value;
	private long generation = 0;
	private boolean started = false;
	private boolean reloaded = false;

	/**
	 * Returns the current generation, a new one when a stamp differs from the previous call.
	 * {@link #wasReloaded()} then tells whether the identity stamp changed (a real EMI reload).
	 */
	public long update(Object identityStamp, Object valueStamp) {
		reloaded = !started || identityStamp != identity;
		if (reloaded || !Objects.equals(valueStamp, value)) {
			generation++;
			identity = identityStamp;
			value = valueStamp;
			started = true;
		}
		return generation;
	}

	/** Whether the last {@link #update} saw a new identity stamp (also true for the very first call). */
	public boolean wasReloaded() {
		return reloaded;
	}

	public long generation() {
		return generation;
	}
}
