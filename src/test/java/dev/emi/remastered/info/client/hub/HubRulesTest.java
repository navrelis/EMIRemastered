package dev.emi.remastered.info.client.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.info.client.hub.HubRules.Bind;
import dev.emi.remastered.info.client.hub.HubRules.Fit;
import dev.emi.remastered.info.client.hub.HubRules.View;

class HubRulesTest {

	@Test
	void bindsFollowThePrimaryView() {
		assertEquals(HubTab.RECIPES, HubRules.forBind(Bind.VIEW_RECIPES, HubTab.RECIPES));
		assertEquals(HubTab.INFO, HubRules.forBind(Bind.VIEW_RECIPES, HubTab.INFO));
		assertEquals(HubTab.USES, HubRules.forBind(Bind.VIEW_USES, HubTab.RECIPES));
		assertEquals(HubTab.USES, HubRules.forBind(Bind.VIEW_USES, HubTab.INFO));
		assertEquals(HubTab.INFO, HubRules.forBind(Bind.OPEN_INFO, HubTab.RECIPES));
		assertEquals(HubTab.INFO, HubRules.forBind(Bind.OPEN_INFO, HubTab.INFO));
		// "view recipes" never opens Uses, whatever the setting holds
		assertEquals(HubTab.RECIPES, HubRules.forBind(Bind.VIEW_RECIPES, HubTab.USES));
	}

	@Test
	void emptyTabsFallBackToInfo() {
		assertEquals(HubTab.RECIPES, HubRules.resolveOpen(HubTab.RECIPES, 3, 0));
		assertEquals(HubTab.INFO, HubRules.resolveOpen(HubTab.RECIPES, 0, 5));
		assertEquals(HubTab.USES, HubRules.resolveOpen(HubTab.USES, 0, 1));
		assertEquals(HubTab.INFO, HubRules.resolveOpen(HubTab.USES, 7, 0));
		assertEquals(HubTab.INFO, HubRules.resolveOpen(HubTab.INFO, 0, 0));
		assertTrue(HubRules.enabled(HubTab.INFO, 0, 0));
		assertFalse(HubRules.enabled(HubTab.RECIPES, 0, 4));
		assertFalse(HubRules.enabled(HubTab.USES, 4, 0));
	}

	@Test
	void onlyViewsOfOneStackGetTheBar() {
		assertTrue(HubRules.showsBar(true, true, View.STACK_RECIPES));
		assertTrue(HubRules.showsBar(true, true, View.STACK_USES));
		assertTrue(HubRules.showsBar(true, true, View.STACK_INFO));
		for (View view : List.of(View.TAG, View.MULTI, View.CATEGORY, View.ALL, View.OTHER, View.RESOLVE)) {
			assertFalse(HubRules.showsBar(true, true, view), view.name());
			assertNull(HubRules.tabOf(view), view.name());
		}
		assertFalse(HubRules.showsBar(false, true, View.STACK_RECIPES), "module off");
		assertFalse(HubRules.showsBar(true, false, View.STACK_RECIPES), "setting off");
		assertEquals(HubTab.RECIPES, HubRules.tabOf(View.STACK_RECIPES));
		assertEquals(HubTab.USES, HubRules.tabOf(View.STACK_USES));
		assertEquals(HubTab.INFO, HubRules.tabOf(View.STACK_INFO));
	}

	@Test
	void viewOfDisplayCalls() {
		assertEquals(View.STACK_RECIPES, HubRules.viewOf(false, false, 1));
		assertEquals(View.STACK_USES, HubRules.viewOf(true, false, 1));
		assertEquals(View.TAG, HubRules.viewOf(false, true, 5));
		assertEquals(View.TAG, HubRules.viewOf(false, true, 1));
		assertEquals(View.MULTI, HubRules.viewOf(true, true, 5));
		assertEquals(View.STACK_USES, HubRules.viewOf(true, true, 1));
		assertEquals(View.MULTI, HubRules.viewOf(false, false, 3));
	}

	@Test
	void switchingReplacesTheHistoryEntry() {
		List<String> history = new ArrayList<>(List.of("inventory"));
		int before = history.size();
		history.add("recipes of iron");
		HubRules.replaceTop(history, before);
		assertEquals(List.of("inventory"), history);
		// nothing pushed: unchanged
		HubRules.replaceTop(history, 1);
		assertEquals(List.of("inventory"), history);
		history.add("a");
		history.add("b");
		HubRules.replaceTop(history, 1);
		assertEquals(List.of("inventory"), history);
		HubRules.replaceTop(history, -3);
		assertTrue(history.isEmpty());
	}

	@Test
	void panelGivesUpHeightOnlyWhenNeeded() {
		// 1280x720 at GUI scale 2: 640x360, EMI defaults (max height 256, vertical margin 20)
		int height = HubRules.emiPanelHeight(360, 256, 20);
		int top = HubRules.emiPanelTop(360, height);
		assertEquals(256, height);
		assertEquals(53, top);
		assertEquals(Math.max(0, HubRules.BAND - 53), HubRules.extraTop(top));
		// large screen: no change
		assertEquals(0, HubRules.extraTop(200));
		// 854x480 at GUI scale 2: 427x240
		int small = HubRules.emiPanelHeight(240, 256, 20);
		int smallTop = HubRules.emiPanelTop(240, small);
		assertEquals(168, small);
		assertEquals(HubRules.BAND, smallTop + HubRules.extraTop(smallTop));
		assertEquals(smallTop + HubRules.extraTop(smallTop) - HubRules.PLATE_HEIGHT, HubRules.plateTop(240, 256, 20));
		// the selected tab never leaves the screen
		for (int h = 100; h < 1000; h += 7) {
			assertTrue(HubRules.plateTop(h, 256, 20) - HubRules.TAB_HEIGHT - HubRules.SELECTED_RAISE >= HubRules.TOP_MARGIN);
		}
	}

	@Test
	void labelsCollapseToFit() {
		int[] labels = { 60, 56 };
		int[] counts = { 72, 62 };
		int[] shorts = { 50, 40 };
		assertEquals(Fit.FULL, HubRules.fit(200, 24, 50, labels, counts, shorts));
		assertEquals(Fit.ICON_INFO, HubRules.fit(168, 24, 50, labels, counts, shorts));
		assertEquals(Fit.NO_COUNTS, HubRules.fit(150, 24, 50, labels, counts, shorts));
		assertEquals(Fit.SHORT, HubRules.fit(120, 24, 50, labels, counts, shorts));
		assertEquals(Fit.TRIMMED, HubRules.fit(100, 24, 50, labels, counts, shorts));
	}
}
