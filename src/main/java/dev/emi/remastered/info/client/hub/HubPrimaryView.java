package dev.emi.remastered.info.client.hub;

import dev.emi.emi.EmiPort;
import dev.emi.emi.config.ConfigEnum;
import net.minecraft.text.Text;

/** Values of the {@code info.primary-view} setting: the tab EMI's "view recipes" bind (R, left click) opens. */
public enum HubPrimaryView implements ConfigEnum {
	RECIPES("recipes", HubTab.RECIPES),
	INFO("info", HubTab.INFO);

	private final String name;
	private final HubTab tab;

	HubPrimaryView(String name, HubTab tab) {
		this.name = name;
		this.tab = tab;
	}

	public HubTab tab() {
		return tab;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Text getText() {
		return EmiPort.translatable("emi_info.primary_view." + name.replace("-", "_"));
	}
}
