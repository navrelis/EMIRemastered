package dev.emi.remastered.professions;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Page of a single profession: a header (name, villager, workstations) followed by a body area that is empty for now.
 * Extra content for the profession, such as trade rows, goes into {@link #bodyHeight()} and
 * {@link #addBody(WidgetHolder, int)}.
 */
public class ProfessionRecipe implements EmiRecipe {
	protected static final int WIDTH = 124;
	protected static final int HEADER_HEIGHT = 62;
	private final ProfessionEntry entry;

	public ProfessionRecipe(ProfessionEntry entry) {
		this.entry = entry;
	}

	public ProfessionEntry getEntry() {
		return entry;
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return ProfessionsCategories.PROFESSION;
	}

	@Override
	@Nullable
	public Identifier getId() {
		return entry.id();
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return List.copyOf(entry.workstations());
	}

	@Override
	public List<EmiStack> getOutputs() {
		return List.of();
	}

	@Override
	public int getDisplayWidth() {
		return WIDTH;
	}

	@Override
	public int getDisplayHeight() {
		return HEADER_HEIGHT + bodyHeight();
	}

	/**
	 * Height of the area below the header that {@link #addBody(WidgetHolder, int)} fills.
	 */
	protected int bodyHeight() {
		return 0;
	}

	/**
	 * Adds the widgets below the header.
	 *
	 * @param top y position at which the body starts
	 */
	protected void addBody(WidgetHolder widgets, int top) {
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		widgets.addSlot(EmiIngredient.of(entry.workstations()), 92, 22);
		widgets.addDrawable(22, 0, 72, HEADER_HEIGHT, (context, mouseX, mouseY, delta) ->
			VillagerPreview.render(context, 22, 62, 25.0, 38 - mouseX, 15 - mouseY, entry.profession()));
		widgets.addDrawable(0, 0, WIDTH, HEADER_HEIGHT, (context, mouseX, mouseY, delta) -> {
			String text = Screen.hasShiftDown() ? entry.id().toString() : entry.displayName().getString();
			TextRenderer font = MinecraftClient.getInstance().textRenderer;
			MatrixStack matrices = context.getMatrices();
			matrices.push();
			matrices.translate(1, 0, 0);
			int width = font.getWidth(text);
			if (width > WIDTH - 2) {
				float scale = (WIDTH - 2) / (float) width;
				matrices.scale(scale, scale, 1.0F);
			}
			context.drawText(font, text, 0, 0, 8, false);
			matrices.pop();
		});
		addBody(widgets, HEADER_HEIGHT);
	}
}
