package dev.emi.remastered.tools.tags;

/**
 * Which parts of a tag row fit. The tag id is the identity of a tag, so it is kept whole as long as possible: the
 * registry label is dropped first, then the member count, and only when the id alone is wider than the row it is cut
 * (with an ellipsis, by the caller). Pure logic.
 *
 * @param showSize     draw the member count
 * @param showRegistry draw the registry label
 * @param idWidth      width the id may use; smaller than its text width means it has to be cut
 */
public record RowLayout(boolean showSize, boolean showRegistry, int idWidth) {

	/**
	 * @param available  width of the row
	 * @param idText     width of the whole id
	 * @param sizeText   width of the member count
	 * @param registryText width of the registry label, 0 when there is none
	 * @param gap        space between two parts
	 */
	public static RowLayout of(int available, int idText, int sizeText, int registryText, int gap) {
		if (idText > available) {
			return new RowLayout(false, false, Math.max(0, available));
		}
		int free = available - idText;
		if (sizeText + gap > free) {
			return new RowLayout(false, false, idText);
		}
		free -= sizeText + gap;
		boolean registry = registryText > 0 && registryText + gap <= free;
		return new RowLayout(true, registry, idText);
	}
}
