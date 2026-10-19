package dev.emi.remastered.info.api;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.text.Text;

/** A row of an {@link InfoSection}, see {@link InfoSection#entry}. All methods return {@code this}. */
public interface InfoEntry {

	/** Right-aligned detail text, e.g. a chance or "3 recipes". Replaces {@link #count}. */
	InfoEntry detail(@Nullable Text detail);

	/** Right-aligned count, shown as "x12". Replaces {@link #detail}. */
	InfoEntry count(long count);

	/** Example stacks shown in the row (as many as fit, at most the "example stacks" setting). */
	InfoEntry stacks(List<? extends EmiIngredient> stacks);

	/** Opened when the row is clicked outside its slots. */
	InfoEntry link(@Nullable InfoLink link);

	/** Extra tooltip lines of the row; the label is always the first line. */
	InfoEntry tooltip(List<Text> tooltip);
}
