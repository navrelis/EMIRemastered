package dev.emi.remastered.info.export;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * The row types of the exports, built from plain strings and numbers (no Minecraft classes, unit tested). The client
 * side ({@code ExportJob}) reads EMI and fills these.
 */
public final class ExportRows {
	/** CSV separator between the entries of a list cell (the alternatives of one ingredient use {@code |}). */
	public static final String LIST_SEPARATOR = ";";

	private ExportRows() {
	}

	/**
	 * One recipe slot.
	 *
	 * @param tag    the tag id when the slot is a tag (the members are in the tags export), else null
	 * @param stacks the alternatives (stack ids) when the slot is not a tag
	 */
	public record IngredientData(@Nullable String tag, List<String> stacks, long amount, float chance) {

		JsonObject toJson() {
			JsonObject json = new JsonObject();
			if (tag != null) {
				json.addProperty("tag", tag);
			} else {
				JsonArray array = new JsonArray();
				stacks.forEach(array::add);
				json.add("stacks", array);
			}
			json.addProperty("amount", amount);
			if (chance != 1.0f) {
				json.addProperty("chance", chance);
			}
			return json;
		}

		/** {@code [AMOUNTx ]id[|id...]} or {@code [AMOUNTx ]#tag}, then {@code @CHANCE} when the chance is not 1. */
		public String token() {
			StringBuilder sb = new StringBuilder();
			if (amount != 1) {
				sb.append(amount).append("x ");
			}
			sb.append(tag != null ? "#" + tag : String.join("|", stacks));
			if (chance != 1.0f) {
				sb.append(" @").append(chance);
			}
			return sb.toString();
		}
	}

	/** Describes an index stack. {@code components} is empty for a plain stack. */
	public record StackData(String id, String components, String name, String mod, String type, boolean hidden) {

		void addTo(JsonObject json) {
			json.addProperty("id", id);
			if (!components.isEmpty()) {
				json.addProperty("components", components);
			}
			json.addProperty("name", name);
			json.addProperty("mod", mod);
			json.addProperty("type", type);
			json.addProperty("hidden", hidden);
		}

		List<String> cells() {
			return List.of(id, components, name, mod, type, String.valueOf(hidden));
		}
	}

	/** A recipe; {@code id} is empty for recipes without id. */
	public record RecipeRow(String id, String category, List<IngredientData> inputs, List<IngredientData> catalysts,
			List<IngredientData> outputs) implements Row {

		@Override
		public JsonObject toJson() {
			JsonObject json = new JsonObject();
			json.addProperty("id", id);
			json.addProperty("category", category);
			json.add("inputs", ingredients(inputs));
			json.add("catalysts", ingredients(catalysts));
			json.add("outputs", ingredients(outputs));
			return json;
		}

		@Override
		public List<String> cells() {
			return List.of(id, category, tokens(inputs), tokens(catalysts), tokens(outputs));
		}

		private static JsonArray ingredients(List<IngredientData> list) {
			JsonArray array = new JsonArray();
			list.forEach(i -> array.add(i.toJson()));
			return array;
		}

		private static String tokens(List<IngredientData> list) {
			return String.join(LIST_SEPARATOR, list.stream().map(IngredientData::token).toList());
		}
	}

	/** A stack of EMI's index. */
	public record ItemRow(StackData stack) implements Row {

		@Override
		public JsonObject toJson() {
			JsonObject json = new JsonObject();
			stack.addTo(json);
			return json;
		}

		@Override
		public List<String> cells() {
			return stack.cells();
		}
	}

	/** A stack without a known source (same columns as the items export). */
	public record NoSourceRow(StackData stack) implements Row {

		@Override
		public JsonObject toJson() {
			return new ItemRow(stack).toJson();
		}

		@Override
		public List<String> cells() {
			return stack.cells();
		}
	}

	/** One "How to Get" section of a stack: section id, provider id, title and whether it counts as a source. */
	public record SourceData(String id, String provider, String title, boolean source) {

		JsonObject toJson() {
			JsonObject json = new JsonObject();
			json.addProperty("id", id);
			json.addProperty("provider", provider);
			json.addProperty("title", title);
			json.addProperty("source", source);
			return json;
		}
	}

	/** A stack with the "How to Get" sections the info providers produce for it. */
	public record SourceRow(StackData stack, boolean hasSource, List<SourceData> sections) implements Row {

		@Override
		public JsonObject toJson() {
			JsonObject json = new JsonObject();
			stack.addTo(json);
			json.addProperty("has_source", hasSource);
			JsonArray array = new JsonArray();
			sections.forEach(s -> array.add(s.toJson()));
			json.add("sources", array);
			return json;
		}

		@Override
		public List<String> cells() {
			return List.of(stack.id(), stack.components(), stack.name(), stack.mod(), stack.type(),
				String.valueOf(stack.hidden()), String.valueOf(hasSource), String.valueOf(sections.size()),
				String.join(LIST_SEPARATOR, sections.stream().map(SourceData::id).toList()),
				String.join(LIST_SEPARATOR, sections.stream().map(SourceData::title).toList()));
		}
	}

	/** A tag EMI knows and the ids of its members. */
	public record TagRow(String registry, String id, List<String> members) implements Row {

		@Override
		public JsonObject toJson() {
			JsonObject json = new JsonObject();
			json.addProperty("registry", registry);
			json.addProperty("id", id);
			json.addProperty("count", members.size());
			JsonArray array = new JsonArray();
			members.forEach(array::add);
			json.add("members", array);
			return json;
		}

		@Override
		public List<String> cells() {
			return List.of(registry, id, String.valueOf(members.size()), String.join(LIST_SEPARATOR, members));
		}
	}
}
