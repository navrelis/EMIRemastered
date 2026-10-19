package dev.emi.remastered.letsdo;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects what a Let's Do sub-module could not register and turns it into ONE log line (no stack traces).
 * Pure logic, no Minecraft classes.
 */
public final class FailureReport {
	static final int MAX_MESSAGE = 120;

	private final String subModule;
	private final List<String> entries = new ArrayList<>();

	public FailureReport(String subModule) {
		this.subModule = subModule;
	}

	public void add(String what, Throwable t) {
		entries.add(what + " (" + describe(t) + ")");
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public int size() {
		return entries.size();
	}

	/** {@code "vinery: skipped grape_mashing (NoSuchFieldError: FOO); fondue (...)"}. */
	public String format() {
		return subModule + ": skipped " + String.join("; ", entries);
	}

	static String describe(Throwable t) {
		Throwable root = t;
		while (root.getCause() != null && root.getCause() != root) {
			root = root.getCause();
		}
		String message = root.getMessage();
		if (message == null || message.isBlank()) {
			return root.getClass().getSimpleName();
		}
		message = message.replaceAll("\\s+", " ").trim();
		if (message.length() > MAX_MESSAGE) {
			message = message.substring(0, MAX_MESSAGE - 3) + "...";
		}
		return root.getClass().getSimpleName() + ": " + message;
	}
}
