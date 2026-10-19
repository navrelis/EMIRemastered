package dev.emi.remastered.info.export;

import java.util.List;
import java.util.Locale;

/** One kind of export: file name and the flat columns of its CSV table (the JSON objects use the same names). */
public enum ExportKind {
	RECIPES("recipes", List.of("id", "category", "inputs", "catalysts", "outputs")),
	ITEMS("items", List.of("id", "components", "name", "mod", "type", "hidden")),
	SOURCES("sources", List.of("id", "components", "name", "mod", "type", "hidden", "has_source", "source_count",
		"source_ids", "source_titles")),
	NOSOURCE("nosource", List.of("id", "components", "name", "mod", "type", "hidden")),
	TAGS("tags", List.of("registry", "id", "count", "members"));

	/** The argument of the command that selects every kind. */
	public static final String ALL = "all";

	public final String fileName;
	public final List<String> columns;

	ExportKind(String fileName, List<String> columns) {
		this.fileName = fileName;
		this.columns = columns;
	}

	/** The kinds a command argument stands for ({@code all} or one file name), empty when unknown. */
	public static List<ExportKind> parse(String what) {
		String text = what.toLowerCase(Locale.ROOT);
		if (text.equals(ALL)) {
			return List.of(values());
		}
		for (ExportKind kind : values()) {
			if (kind.fileName.equals(text)) {
				return List.of(kind);
			}
		}
		return List.of();
	}
}
