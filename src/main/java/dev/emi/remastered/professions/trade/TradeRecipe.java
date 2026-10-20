package dev.emi.remastered.professions.trade;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.remastered.info.search.SourceLabels;
import dev.emi.remastered.professions.ProfessionsCategories;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * A single villager or wandering trader trade: one or two items bought, one item sold, the workstation as catalyst.
 */
public class TradeRecipe implements EmiRecipe, SourceLabels {
	private static final int WIDTH = 124;
	private final TradeEntry entry;

	public TradeRecipe(TradeEntry entry) {
		this.entry = entry;
	}

	public TradeEntry getEntry() {
		return entry;
	}

	/** Source search ({@code ~}): the profession (or the wandering trader) and the level or pool of the trade. */
	@Override
	public List<String> sourceLabels() {
		return List.of(entry.owner().getString(), entry.levelText().getString());
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return entry.wanderer() ? ProfessionsCategories.WANDERING_TRADER : ProfessionsCategories.VILLAGER_TRADES;
	}

	@Override
	public Identifier getId() {
		return entry.id();
	}

	@Override
	public List<EmiIngredient> getInputs() {
		List<EmiIngredient> inputs = new ArrayList<>(2);
		inputs.add(entry.first());
		if (!entry.second().isEmpty()) {
			inputs.add(entry.second());
		}
		return inputs;
	}

	@Override
	public List<EmiIngredient> getCatalysts() {
		if (entry.workstations().isEmpty()) {
			return List.of();
		}
		return List.of(EmiIngredient.of(entry.workstations()));
	}

	@Override
	public List<EmiStack> getOutputs() {
		return List.of(entry.output());
	}

	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public int getDisplayWidth() {
		return WIDTH;
	}

	@Override
	public int getDisplayHeight() {
		return entry.mapPlaceholder() ? 44 : 32;
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		Text title = entry.wanderer() ? entry.levelText() : Text.translatable("emiprofessions.trade.title", entry.owner(), entry.levelText());
		widgets.addDrawable(0, 0, WIDTH, 10, (context, mouseX, mouseY, delta) -> {
			TextRenderer font = MinecraftClient.getInstance().textRenderer;
			MatrixStack matrices = context.getMatrices();
			matrices.push();
			int width = font.getWidth(title);
			if (width > WIDTH) {
				float scale = WIDTH / (float) width;
				matrices.scale(scale, scale, 1.0F);
			}
			context.drawText(font, title, 0, 0, 8, false);
			matrices.pop();
		});

		widgets.addSlot(entry.first(), 0, 12);
		widgets.addSlot(entry.second(), 20, 12);
		widgets.addTexture(EmiTexture.EMPTY_ARROW, 46, 13);
		widgets.addTooltipText(detailLines(), 46, 12, 24, 18);
		widgets.addSlot(entry.output(), 76, 12).recipeContext(this)
			.appendTooltip(Text.translatable("emiprofessions.trade.max_uses", entry.maxUses()).formatted(Formatting.GRAY))
			.appendTooltip(Text.translatable("emiprofessions.trade.experience", entry.experience()).formatted(Formatting.GRAY));
		if (!entry.workstations().isEmpty()) {
			widgets.addSlot(EmiIngredient.of(entry.workstations()), 104, 12).catalyst(true);
		}
		if (entry.mapPlaceholder()) {
			widgets.addText(Text.translatable("emiprofessions.trade.map_placeholder").formatted(Formatting.DARK_GRAY), 0, 34, 0x555555, false);
		}
	}

	private List<Text> detailLines() {
		List<Text> lines = new ArrayList<>();
		lines.add(Text.translatable("emiprofessions.trade.max_uses", entry.maxUses()));
		lines.add(Text.translatable("emiprofessions.trade.experience", entry.experience()));
		if (!entry.villagerTypes().isEmpty()) {
			lines.add(Text.translatable("emiprofessions.trade.types", String.join(", ", entry.villagerTypes())).formatted(Formatting.GRAY));
		}
		if (entry.mapPlaceholder()) {
			lines.add(Text.translatable("emiprofessions.trade.map_note", entry.mapStructure()).formatted(Formatting.GRAY));
		}
		if (entry.villagerApi()) {
			lines.add(Text.translatable("emiprofessions.trade.villager_api").formatted(Formatting.GRAY));
		}
		return lines;
	}
}
