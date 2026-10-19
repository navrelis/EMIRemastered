package dev.emi.remastered.world.emi;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.render.EmiTooltipComponents;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.serializer.EmiStackSerializer;
import dev.emi.remastered.world.client.ClientWorldData;
import dev.emi.remastered.world.data.StructureIcons;
import dev.emi.remastered.world.data.StructureRow;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * A structure as EMI stack. A structure has no icon of its own, so it is drawn as an item picked by
 * {@link StructureIcons} (a bell for villages, a crossbow for the pillager outpost, the structure block when nothing
 * fits); name and tooltip are the structure's. Its own key keeps the structure apart from the item in EMI's recipe
 * lookups: the structure page is found by clicking the structure, not the item.
 */
public class StructureEmiStack extends EmiStack {
	private final Identifier structure;
	private final EmiStack icon;

	private StructureEmiStack(Identifier structure) {
		this.structure = structure;
		Item item = Registries.ITEM.get(Identifier.tryParse(StructureIcons.itemOf(structure.toString())));
		this.icon = EmiStack.of(item == Items.AIR ? Items.STRUCTURE_BLOCK : item);
	}

	public static EmiStack of(Identifier structure) {
		return new StructureEmiStack(structure);
	}

	public static EmiStack of(String structure) {
		Identifier id = Identifier.tryParse(structure);
		return id == null ? EmiStack.EMPTY : new StructureEmiStack(id);
	}

	/** The item used for the icon. */
	public EmiStack icon() {
		return icon;
	}

	private record Key(Identifier structure) {
	}

	@Override
	public EmiStack copy() {
		EmiStack e = new StructureEmiStack(structure);
		e.setAmount(getAmount());
		e.setChance(getChance());
		e.setRemainder(getRemainder().copy());
		e.comparison(comparison);
		return e;
	}

	@Override
	public void render(DrawContext gui, int x, int y, float delta, int flags) {
		if ((flags & RENDER_ICON) != 0) {
			icon.render(gui, x, y, delta, RENDER_ICON);
		}
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public ComponentChanges getComponentChanges() {
		return ComponentChanges.EMPTY;
	}

	@Override
	public Object getKey() {
		return new Key(structure);
	}

	@Override
	public Identifier getId() {
		return structure;
	}

	@Override
	public List<Text> getTooltipText() {
		// Line 0 is the name; the following lines are what EMI searches with the $ prefix, so $nether finds structures of the nether
		List<Text> lines = new ArrayList<>();
		lines.add(getName());
		Text dimensions = dimensionsText();
		if (dimensions != null) {
			lines.add(dimensions);
		}
		return lines;
	}

	private Text dimensionsText() {
		StructureRow row = ClientWorldData.snapshot().structure(structure.toString());
		return row == null || row.dimensions().isEmpty() ? null
			: Text.translatable("emi_world.structure.dimensions", WorldTexts.dimensionNames(row.dimensions()));
	}

	@Override
	public List<TooltipComponent> getTooltip() {
		List<TooltipComponent> list = new ArrayList<>();
		list.add(TooltipComponent.of(getName().asOrderedText()));
		if (MinecraftClient.getInstance().options.advancedItemTooltips) {
			list.add(TooltipComponent.of(Text.literal(structure.toString()).formatted(Formatting.DARK_GRAY).asOrderedText()));
		}
		Text dimensions = dimensionsText();
		if (dimensions != null) {
			list.add(TooltipComponent.of(dimensions.copy().formatted(Formatting.GRAY).asOrderedText()));
		}
		EmiTooltipComponents.appendModName(list, structure.getNamespace());
		list.addAll(super.getTooltip());
		return list;
	}

	@Override
	public Text getName() {
		return WorldTexts.structureName(structure.toString());
	}

	/** Serializer type {@code structure}, written as {@code structure:minecraft:village_plains}. */
	public static class Serializer implements EmiStackSerializer<StructureEmiStack> {
		@Override
		public String getType() {
			return "structure";
		}

		@Override
		public EmiStack create(Identifier id, ComponentChanges componentChanges, long amount) {
			return of(id).setAmount(amount);
		}
	}
}
