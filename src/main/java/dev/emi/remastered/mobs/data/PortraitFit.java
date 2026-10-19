package dev.emi.remastered.mobs.data;

/**
 * Scale and position of a mob in its portrait box, so that every mob fills the box whatever its size and the size of
 * its model. Pure logic, no Minecraft classes.
 *
 * @param scale   pixels per block
 * @param offsetX pixels to move the origin of the entity to the right of the middle of the box
 * @param offsetY pixels to move the origin of the entity down from the middle of the box
 */
public record PortraitFit(double scale, double offsetX, double offsetY) {
	/** Scale limit, for a model that renders (almost) nothing. */
	public static final double MAX_SCALE = 96.0;

	/**
	 * @param minX the extent of the rendered mob in blocks (x to the right, y down, relative to its origin)
	 * @param box  the side of the square the mob has to fit into, in pixels
	 * @return null when the extent is empty or not a number
	 */
	public static PortraitFit of(double minX, double minY, double maxX, double maxY, double box) {
		double width = maxX - minX;
		double height = maxY - minY;
		if (Double.isNaN(width) || Double.isNaN(height) || Double.isInfinite(width) || Double.isInfinite(height) || (width <= 0 && height <= 0)) {
			return null;
		}
		double scale = Math.min(MAX_SCALE, box / Math.max(width, height));
		return new PortraitFit(scale, -scale * (minX + maxX) / 2.0, -scale * (minY + maxY) / 2.0);
	}
}
