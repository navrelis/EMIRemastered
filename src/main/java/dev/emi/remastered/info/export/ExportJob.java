package dev.emi.remastered.info.export;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiReloadManager;
import dev.emi.emi.runtime.EmiTagKey;
import dev.emi.remastered.EmiRemastered;
import dev.emi.remastered.info.client.InfoService;
import dev.emi.remastered.info.core.ProviderOrdering;
import dev.emi.remastered.info.export.ExportRows.ItemRow;
import dev.emi.remastered.info.export.ExportRows.NoSourceRow;
import dev.emi.remastered.info.export.ExportRows.RecipeRow;
import dev.emi.remastered.info.export.ExportRows.SourceRow;
import dev.emi.remastered.info.export.ExportRows.StackData;
import dev.emi.remastered.info.export.ExportRows.TagRow;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * One running export. Everything that reads EMI or the info providers runs on the render thread in slices of a few
 * milliseconds per client tick ({@link #tick()}); only writing the files happens on a background thread.
 */
final class ExportJob {
	/** Time spent on collecting per client tick. */
	private static final long BUDGET_NANOS = 10_000_000L;
	private static final int MAX_LOGGED_ERRORS = 5;

	private final MinecraftClient client;
	private final List<ExportKind> kinds;
	private final ExportFormat format;
	private final Path dir;
	private final Object recipeManager;
	private final long startNanos = System.nanoTime();
	private final List<Phase> phases = new ArrayList<>();
	private final long total;

	private final List<RecipeRow> recipeRows = new ArrayList<>();
	private final List<ItemRow> itemRows = new ArrayList<>();
	private final List<SourceRow> sourceRows = new ArrayList<>();
	private final List<NoSourceRow> noSourceRows = new ArrayList<>();
	private final List<TagRow> tagRows = new ArrayList<>();

	private int phase;
	private int index;
	private long done;
	private int lastReported;
	private int errors;
	private boolean writing;
	private boolean finished;

	private record Phase(int size, IntConsumer step) {
	}

	ExportJob(MinecraftClient client, List<ExportKind> kinds, ExportFormat format, Path dir) {
		this.client = client;
		this.kinds = kinds;
		this.format = format;
		this.dir = dir;
		this.recipeManager = EmiApi.getRecipeManager();
		if (kinds.contains(ExportKind.RECIPES)) {
			List<EmiRecipe> recipes = List.copyOf(EmiApi.getRecipeManager().getRecipes());
			phases.add(new Phase(recipes.size(), i -> recipeRows.add(ExportData.recipe(recipes.get(i)))));
		}
		List<EmiStack> stacks = List.copyOf(EmiApi.getIndexStacks());
		if (kinds.contains(ExportKind.ITEMS)) {
			phases.add(new Phase(stacks.size(), i -> itemRows.add(new ItemRow(ExportData.stack(stacks.get(i))))));
		}
		if (kinds.contains(ExportKind.SOURCES) || kinds.contains(ExportKind.NOSOURCE)) {
			if (!stacks.isEmpty()) {
				// makes the info service run onReload() of the providers for this EMI load (caches one page)
				InfoService.getPage(stacks.get(0));
			}
			phases.add(new Phase(stacks.size(), i -> collectSources(stacks.get(i))));
		}
		if (kinds.contains(ExportKind.TAGS)) {
			List<EmiTagKey<?>> tags = ExportData.tags();
			phases.add(new Phase(tags.size(), i -> tagRows.add(ExportData.tag(tags.get(i)))));
		}
		this.total = phases.stream().mapToLong(Phase::size).sum();
	}

	boolean isFinished() {
		return finished;
	}

	/** Collects the "How to Get" sections with the info service directly (no page cache involved). */
	private void collectSources(EmiStack stack) {
		StackData data = ExportData.stack(stack);
		SourceRow row = ExportData.sources(data, InfoService.compute(stack));
		if (kinds.contains(ExportKind.SOURCES)) {
			sourceRows.add(row);
		}
		if (!row.hasSource() && ExportData.canBeWithoutSource(stack)) {
			noSourceRows.add(new NoSourceRow(data));
		}
	}

	/** Runs one slice. Returns true when the job is over (done, failed or aborted) and can be dropped. */
	boolean tick() {
		if (finished) {
			return true;
		}
		if (writing) {
			return false;
		}
		if (client.world == null || !EmiReloadManager.isLoaded() || EmiApi.getRecipeManager() != recipeManager) {
			abort(Text.translatable("emi_info.export.aborted"));
			return true;
		}
		long end = System.nanoTime() + BUDGET_NANOS;
		do {
			while (phase < phases.size() && index >= phases.get(phase).size()) {
				phase++;
				index = 0;
			}
			if (phase >= phases.size()) {
				break;
			}
			int i = index++;
			done++;
			Throwable t = ProviderOrdering.isolate(() -> phases.get(phase).step().accept(i));
			if (t != null) {
				if (++errors <= MAX_LOGGED_ERRORS) {
					EmiRemastered.LOGGER.error("Item info export: entry {} of phase {} failed", i, phase, t);
				}
			}
		} while (System.nanoTime() < end);
		report();
		while (phase < phases.size() && index >= phases.get(phase).size()) {
			phase++;
			index = 0;
		}
		if (phase >= phases.size()) {
			startWriting();
		}
		return false;
	}

	private void report() {
		if (total == 0) {
			return;
		}
		int percent = (int) (done * 100 / total);
		while (percent >= lastReported + 10 && lastReported < 90) {
			lastReported += 10;
			ExportCommand.say(Text.translatable("emi_info.export.progress", lastReported));
		}
	}

	void abort(Text reason) {
		finished = true;
		ExportCommand.say(reason);
	}

	private void startWriting() {
		writing = true;
		List<Table> tables = new ArrayList<>();
		if (kinds.contains(ExportKind.RECIPES)) {
			tables.add(new Table(ExportKind.RECIPES, recipeRows));
		}
		if (kinds.contains(ExportKind.ITEMS)) {
			tables.add(new Table(ExportKind.ITEMS, itemRows));
		}
		if (kinds.contains(ExportKind.SOURCES)) {
			tables.add(new Table(ExportKind.SOURCES, sourceRows));
		}
		if (kinds.contains(ExportKind.NOSOURCE)) {
			tables.add(new Table(ExportKind.NOSOURCE, noSourceRows));
		}
		if (kinds.contains(ExportKind.TAGS)) {
			tables.add(new Table(ExportKind.TAGS, tagRows));
		}
		Thread thread = new Thread(() -> write(tables), "EMI Item Info export");
		thread.setDaemon(true);
		thread.start();
	}

	private void write(List<Table> tables) {
		try {
			Files.createDirectories(dir);
			for (Table table : tables) {
				Files.writeString(dir.resolve(table.fileName(format)), table.render(format), StandardCharsets.UTF_8);
				EmiRemastered.LOGGER.info("Item info export: {} ({} rows)", table.fileName(format), table.rows().size());
			}
			String seconds = ExportData.seconds(System.nanoTime() - startNanos);
			client.execute(() -> {
				finished = true;
				ExportCommand.say(Text.translatable("emi_info.export.done", seconds, tables.size(),
					ExportCommand.pathText(dir)));
				if (errors > 0) {
					ExportCommand.say(Text.translatable("emi_info.export.errors", errors));
				}
			});
			EmiRemastered.LOGGER.info("Item info export finished in {} s: {}", seconds, dir);
		} catch (IOException | RuntimeException e) {
			EmiRemastered.LOGGER.error("Item info export to {} failed", dir, e);
			client.execute(() -> {
				finished = true;
				ExportCommand.say(Text.translatable("emi_info.export.failed", String.valueOf(e.getMessage())));
			});
		}
	}
}
