package dev.emi.remastered.info.client.hub;

/** The tabs of the item view, in bar order. */
public enum HubTab {
	/** The Item Info screen. */
	INFO,
	/** EMI's recipe screen with the recipes that make the stack ({@code EmiApi.displayRecipes}). */
	RECIPES,
	/** EMI's recipe screen with the recipes that use the stack ({@code EmiApi.displayUses}). */
	USES
}
