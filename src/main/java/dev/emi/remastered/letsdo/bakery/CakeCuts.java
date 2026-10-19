package dev.emi.remastered.letsdo.bakery;

import java.util.List;

/**
 * The Bakery pies and cakes that are cut into slices with a knife (Bakery's PieBlock: four cuts, one slice each).
 * Plain ids so the table needs no Bakery class; entries whose items are missing in the installed version are skipped.
 * Meadow's cheesecake and cheese tart are not part of it: they are not cut with a knife and have a normal crafting
 * recipe that EMI shows anyway.
 */
public final class CakeCuts {
	public static final String NAMESPACE = "bakery";
	public static final int SLICES = 4;

	/** Item paths in the namespace {@value #NAMESPACE}: whole cake, slice. */
	public static final List<Cut> CUTS = List.of(
			new Cut("apple_pie", "apple_pie_slice"),
			new Cut("bundt_cake", "bundt_cake_slice"),
			new Cut("chocolate_cake", "chocolate_cake_slice"),
			new Cut("chocolate_gateau", "chocolate_gateau_slice"),
			new Cut("chocolate_tart", "chocolate_tart_slice"),
			new Cut("glowberry_tart", "glowberry_pie_slice"),
			new Cut("linzer_tart", "linzer_tart_slice"),
			new Cut("pudding", "pudding_slice"),
			new Cut("strawberry_cake", "strawberry_cake_slice"),
			new Cut("sweetberry_cake", "sweetberry_cake_slice"));

	private CakeCuts() {
	}

	public record Cut(String whole, String slice) {
	}
}
