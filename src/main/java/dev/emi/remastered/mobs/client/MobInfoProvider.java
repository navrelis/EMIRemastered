package dev.emi.remastered.mobs.client;

import java.util.Set;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.info.api.InfoCollector;
import dev.emi.remastered.info.api.InfoLink;
import dev.emi.remastered.info.api.InfoSection;
import dev.emi.remastered.info.api.ItemInfoProvider;
import dev.emi.remastered.mobs.Mobs;
import dev.emi.remastered.mobs.data.MobStats;
import dev.emi.remastered.mobs.data.MobStats.Stat;
import dev.emi.remastered.mobs.emi.MobInfoRecipe;
import net.minecraft.entity.EntityType;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Item Info provider of the mobs module: a spawn egg gets a Details section with the key stats of its mob and a link
 * to the mob page. Drops need no section here, the loot module's providers already list them.
 */
public final class MobInfoProvider implements ItemInfoProvider {
	public static final Identifier ID = Mobs.id("mob");
	private static final Set<Stat> SHOWN = Set.of(Stat.HEALTH, Stat.ARMOR, Stat.ATTACK, Stat.SPEED);

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public String getModuleId() {
		return "mobs";
	}

	@Override
	public boolean appliesTo(EmiStack stack) {
		return stack.getKey() instanceof SpawnEggItem;
	}

	@Override
	public void collect(EmiStack stack, InfoCollector collector) {
		EntityType<?> type = ((SpawnEggItem) stack.getKey()).getEntityType(stack.getItemStack());
		EmiRecipe page = EmiApi.getRecipeManager().getRecipe(MobInfoRecipe.recipeId(Registries.ENTITY_TYPE.getId(type)));
		if (page == null) {
			return;
		}
		MobStats stats = MobInfoRecipe.readStats(type);
		InfoSection section = collector.addSection(ID, InfoSection.Kind.DETAILS, Text.translatable("emi_mobs.info.title"))
			.icon(stack)
			.text(Text.translatable("emi_mobs.info.mob", type.getName()));
		for (Stat stat : stats.visible()) {
			if (SHOWN.contains(stat)) {
				section.text(Text.translatable("emi_mobs.info.stat", Text.translatable("emi_mobs.stat." + stat.key()),
					(stat == Stat.SPEED ? "≈" : "") + stats.value(stat, false)));
			}
		}
		section.link(Text.translatable("emi_mobs.info.open", type.getName()), InfoLink.recipe(page));
	}
}
