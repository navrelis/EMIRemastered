package dev.emi.remastered.info.export;

import java.util.Locale;
import java.util.Optional;

/** File format of an export. */
public enum ExportFormat {
	JSON("json"),
	CSV("csv");

	public final String extension;

	ExportFormat(String extension) {
		this.extension = extension;
	}

	public static Optional<ExportFormat> parse(String text) {
		String lower = text.toLowerCase(Locale.ROOT);
		for (ExportFormat format : values()) {
			if (format.extension.equals(lower)) {
				return Optional.of(format);
			}
		}
		return Optional.empty();
	}
}
