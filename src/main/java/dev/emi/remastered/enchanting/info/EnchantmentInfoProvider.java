package dev.emi.remastered.enchanting.info;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.enchanting.EnchantingContent;
import dev.emi.remastered.enchanting.EnchantingSettings;
import dev.emi.remastered.enchanting.logic.EnchantmentSources;
import dev.emi.remastered.enchanting.logic.LevelCap;
import dev.emi.remastered.enchanting.page.EnchantmentTexts;
import dev.emi.remastered.enchanting.recipe.EnchantmentIndex;
import dev.emi.remastered.enchanting.recipe.EnchantmentInfo;
import dev.emi.remastered.enchanting.recipe.EnchantmentRecipe;
import dev.emi.remastered.info.api.InfoCollector;
import dev.emi.remastered.info.api.InfoLink;
import dev.emi.remastered.info.api.InfoSection;
import dev.emi.remastered.info.api.ItemInfoProvider;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Item Info sections of the enchanting module. A stack that carries enchantments (an enchanted book, an enchanted
 * tool) gets one DETAILS section per enchantment: level, maximum level, what it cannot be combined with and where it
 * comes from, linked to the page of the enchantment. A plain item that can be enchanted gets one section that lists
 * its enchantments. Reads the {@link EnchantmentIndex} that the module builds on every EMI reload.
 */
public final class EnchantmentInfoProvider implements ItemInfoProvider {
	public static final Identifier ID = Identifier.of(EnchantingContent.NAMESPACE, "enchantments");

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public String getModuleId() {
		return "enchanting";
	}

	@Override
	public boolean appliesTo(EmiStack stack) {
		return EnchantingSettings.showInItemInfo && stack.getKey() instanceof Item && !EnchantmentIndex.get().isEmpty();
	}

	@Override
	public void collect(EmiStack stack, InfoCollector collector) {
		EnchantmentIndex index = EnchantmentIndex.get();
		ItemStack itemStack = stack.getItemStack();
		Map<RegistryEntry<Enchantment>, Integer> carried = new LinkedHashMap<>();
		addAll(carried, itemStack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT));
		addAll(carried, itemStack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT));
		boolean any = false;
		for (Map.Entry<RegistryEntry<Enchantment>, Integer> carriedEntry : carried.entrySet()) {
			EnchantmentInfo info = carriedEntry.getKey().getKey().map(key -> index.byId(key.getValue())).orElse(null);
			if (info != null) {
				any = true;
				addEnchantment(collector, info, carriedEntry.getValue());
			}
		}
		if (!any && EnchantingSettings.showForItems && stack.getKey() instanceof Item item) {
			addApplicable(collector, index.forItem(item));
		}
	}

	private static void addAll(Map<RegistryEntry<Enchantment>, Integer> into, ItemEnchantmentsComponent component) {
		for (RegistryEntry<Enchantment> entry : component.getEnchantments()) {
			into.merge(entry, component.getLevel(entry), Math::max);
		}
	}

	private static void addEnchantment(InfoCollector collector, EnchantmentInfo info, int level) {
		InfoSection section = collector.addSection(ID, InfoSection.Kind.DETAILS, info.entry().value().description())
			.icon(info.books().get(Math.min(Math.max(level, 1), info.books().size()) - 1));
		if (level > info.maxLevel()) {
			section.text(Text.translatable("emi_enchanting.info.level_over", level, info.maxLevel()));
		} else {
			section.text(Text.translatable("emi_enchanting.info.level", level, info.maxLevel()));
		}
		if (LevelCap.isCustom(info.levelCap())) {
			section.text(Text.translatable("emi_enchanting.level_cap", EnchantmentTexts.levelCap(info.levelCap())));
		}
		if (EnchantingSettings.showExclusions) {
			if (info.exclusions().isEmpty()) {
				section.text(Text.translatable("emi_enchanting.info.combines_with_all"));
			} else {
				List<EmiIngredient> books = info.exclusions().stream().<EmiIngredient>map(EnchantmentInfo.Exclusion::books).toList();
				section.stacks(Text.translatable("emi_enchanting.info.incompatible"), books);
			}
		}
		if (EnchantingSettings.showSources) {
			section.text(sources(info));
		}
		section.link(Text.translatable("emi_enchanting.info.open_page"), InfoLink.recipe(EnchantmentRecipe.idOf(info.id())));
	}

	private static void addApplicable(InfoCollector collector, List<EnchantmentInfo> infos) {
		if (infos.isEmpty()) {
			return;
		}
		InfoSection section = collector.addSection(ID, InfoSection.Kind.DETAILS, Text.translatable("emi_enchanting.info.applicable"))
			.icon(infos.get(0).books().get(0));
		for (EnchantmentInfo info : infos) {
			section.entry(info.books().get(0), info.entry().value().description())
				.detail(Text.translatable("emi_enchanting.info.max_level", info.maxLevel()))
				.link(InfoLink.recipe(EnchantmentRecipe.idOf(info.id())));
		}
	}

	private static Text sources(EnchantmentInfo info) {
		if (info.sources().isEmpty()) {
			return Text.translatable("emi_enchanting.info.sources", Text.translatable("emi_enchanting.source.none"));
		}
		Text list = null;
		for (EnchantmentSources.Source source : info.sources()) {
			Text name = Text.translatable("emi_enchanting.source." + source.key());
			list = list == null ? name : Text.translatable("emi_enchanting.info.list", list, name);
		}
		return Text.translatable("emi_enchanting.info.sources", list);
	}
}
