package dev.emi.remastered.enchanting.page;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** The pages of an enchantment's info recipe, in order. Add new kinds of info by registering a page here. */
public final class InfoPages {
	private static final List<InfoPage> PAGES = new CopyOnWriteArrayList<>(List.of(
		new OverviewPage(), new SourcesPage(), new DescriptionPage(), new ExclusionsPage()));

	private InfoPages() {
	}

	public static void register(InfoPage page) {
		PAGES.add(page);
	}

	public static List<InfoPage> all() {
		return PAGES;
	}
}
