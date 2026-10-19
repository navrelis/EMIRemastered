package dev.emi.remastered.info.export;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

/** All rows of one {@link ExportKind}, rendered as pretty printed JSON (an array of objects) or as CSV. */
public record Table(ExportKind kind, List<? extends Row> rows) {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	public String render(ExportFormat format) {
		return format == ExportFormat.CSV ? toCsv() : toJson();
	}

	public String toJson() {
		JsonArray array = new JsonArray();
		for (Row row : rows) {
			array.add(row.toJson());
		}
		return GSON.toJson(array) + "\n";
	}

	public String toCsv() {
		return Csv.table(kind.columns, rows.stream().map(Row::cells).toList());
	}

	public String fileName(ExportFormat format) {
		return kind.fileName + "." + format.extension;
	}
}
