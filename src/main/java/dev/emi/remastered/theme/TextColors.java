package dev.emi.remastered.theme;

import java.io.IOException;
import java.io.Reader;

import dev.emi.remastered.EmiRemastered;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

/**
 * Pack-overridable colour remap for the text EMI draws on its panels (recipe pages, Item Info screen, tag browser,
 * copy notice). Resource packs recolour the panel textures; this lets them recolour the text to match.
 * <p>
 * A pack ships {@code assets/emi_remastered/text_colors.json}:
 * <pre>{"replace": {"404040": "FFFFFF", "#6A6A6A": "9FA5A6"}}</pre>
 * Keys and values are exactly 6 hex digits, optional leading '#', case-insensitive; unknown top-level keys are ignored.
 * The files of all packs are merged, a higher priority pack overrides a key of a lower one. The mod ships an empty
 * table, so without such a pack nothing changes. Both the colour passed to a draw call and colours baked into the
 * {@link Style} of a {@link Text} (Formatting colours) go through the table.
 * <p>
 * Tooltips, item count overlays, config screens and the bill of materials screen are not remapped. See
 * {@code docs/THEMING.md}.
 */
public final class TextColors {
	private static final Identifier FILE = Identifier.of(EmiRemastered.NAMESPACE, "text_colors.json");
	private static volatile TextColorTable table = TextColorTable.EMPTY;

	private TextColors() {
	}

	/** Remaps an ARGB (or RGB) colour; returns {@code color} itself if the table is empty or has no entry. */
	public static int map(int color) {
		return table.remap(color);
	}

	/** Remaps the colours baked into the styles of {@code text}; returns {@code text} itself if the table is empty. */
	public static OrderedText map(OrderedText text) {
		TextColorTable t = table;
		if (t.isEmpty()) {
			return text;
		}
		return visitor -> text.accept((index, style, codePoint) -> visitor.accept(index, remapStyle(t, style), codePoint));
	}

	public static OrderedText map(Text text) {
		return map(text.asOrderedText());
	}

	static Style remapStyle(TextColorTable t, Style style) {
		TextColor color = style.getColor();
		if (color == null) {
			return style;
		}
		int rgb = color.getRgb();
		int mapped = t.remap(rgb);
		return mapped == rgb ? style : style.withColor(TextColor.fromRgb(mapped));
	}

	public static void set(TextColorTable t) {
		table = t;
	}

	/** Registers the reload listener that rebuilds the table from the resource packs. Call once on the client. */
	public static void register() {
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public Identifier getFabricId() {
				return Identifier.of(EmiRemastered.NAMESPACE, "text_colors");
			}

			@Override
			public void reload(ResourceManager manager) {
				TextColorTable.Builder builder = TextColorTable.builder();
				// getAllResources returns the lowest priority pack first and the highest last (NamespaceResourceManager
				// ends with Lists.reverse), so iterating in order lets a higher pack override a lower one
				for (Resource resource : manager.getAllResources(FILE)) {
					String pack = resource.getPackId();
					try (Reader reader = resource.getReader()) {
						builder.parseInto(reader, msg -> EmiRemastered.LOGGER.warn("Ignoring {} from pack {}: {}", FILE, pack, msg));
					} catch (IOException | RuntimeException e) {
						EmiRemastered.LOGGER.warn("Ignoring {} from pack {}: {}", FILE, pack, e.toString());
					}
				}
				TextColorTable built = builder.build();
				set(built);
				if (!built.isEmpty()) {
					EmiRemastered.LOGGER.info("Text colour remap: {} colours mapped", built.size());
				}
			}
		});
	}
}
