package dev.emi.remastered.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.api.IdentifiedEmiPlugin;

class PluginIdsTest {

	@Test
	void plainPluginUsesModId() {
		assertEquals("farmersdelight", PluginIds.resolve(new Object(), "farmersdelight"));
	}

	@Test
	void identifiedPluginUsesItsOwnId() {
		IdentifiedEmiPlugin plugin = () -> "emi_loot";
		assertEquals("emi_loot", PluginIds.resolve(plugin, "emi"));
		IdentifiedEmiPlugin other = () -> "my.addon-part_2";
		assertEquals("my.addon-part_2", PluginIds.resolve(other, "somemod"));
	}

	@Test
	void invalidIdsFallBackToModId() {
		assertEquals("somemod", PluginIds.resolve((IdentifiedEmiPlugin) () -> "", "somemod"));
		assertEquals("somemod", PluginIds.resolve((IdentifiedEmiPlugin) () -> null, "somemod"));
		assertEquals("somemod", PluginIds.resolve((IdentifiedEmiPlugin) () -> "Has Spaces", "somemod"));
		assertEquals("somemod", PluginIds.resolve((IdentifiedEmiPlugin) () -> {
			throw new IllegalStateException("boom");
		}, "somemod"));
	}

	@Test
	void onlyEmiMayClaimTheCoreId() {
		assertEquals("somemod", PluginIds.resolve((IdentifiedEmiPlugin) () -> "emi", "somemod"));
		assertEquals("emi", PluginIds.resolve((IdentifiedEmiPlugin) () -> "emi", "emi"));
	}
}
