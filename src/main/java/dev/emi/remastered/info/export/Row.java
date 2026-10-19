package dev.emi.remastered.info.export;

import java.util.List;

import com.google.gson.JsonObject;

/** One exported record: a JSON object and the cells of its flat CSV row (in the order of {@link ExportKind#columns}). */
public interface Row {

	JsonObject toJson();

	List<String> cells();
}
