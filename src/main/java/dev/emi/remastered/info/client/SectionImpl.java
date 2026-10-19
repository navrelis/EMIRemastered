package dev.emi.remastered.info.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.remastered.info.api.InfoElement;
import dev.emi.remastered.info.api.InfoEntry;
import dev.emi.remastered.info.api.InfoLink;
import dev.emi.remastered.info.api.InfoSection;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** The screen's model of one section, filled by a provider through {@link InfoSection}. */
public final class SectionImpl implements InfoSection {
	public final Identifier id;
	public final Identifier providerId;
	public final Kind kind;
	public final Text title;
	public @Nullable EmiRenderable icon;
	public boolean source = true;
	public final List<InfoElement> elements = new ArrayList<>();

	public SectionImpl(Identifier id, Identifier providerId, Kind kind, Text title) {
		this.id = id;
		this.providerId = providerId;
		this.kind = kind;
		this.title = title;
	}

	public boolean isEmpty() {
		return elements.isEmpty();
	}

	/** Whether this section makes the stack obtainable (kind OBTAIN, not marked otherwise, not empty). */
	public boolean countsAsSource() {
		return kind == Kind.OBTAIN && source && !isEmpty();
	}

	@Override
	public InfoSection icon(EmiRenderable icon) {
		this.icon = icon;
		return this;
	}

	@Override
	public InfoSection source(boolean source) {
		this.source = source;
		return this;
	}

	@Override
	public InfoSection text(Text text) {
		return text(text, List.of());
	}

	@Override
	public InfoSection text(Text text, List<Text> tooltip) {
		elements.add(new Elements.TextElement(text, List.copyOf(tooltip), null));
		return this;
	}

	@Override
	public InfoSection stacks(List<? extends EmiIngredient> stacks) {
		return stacks(null, stacks);
	}

	@Override
	public InfoSection stacks(@Nullable Text label, List<? extends EmiIngredient> stacks) {
		if (!stacks.isEmpty() || label != null) {
			elements.add(new Elements.StacksElement(label, List.copyOf(stacks)));
		}
		return this;
	}

	@Override
	public InfoSection link(Text text, InfoLink link) {
		elements.add(new Elements.TextElement(text, List.of(), link));
		return this;
	}

	@Override
	public InfoSection bar(Text label, float progress, int color, @Nullable Text value) {
		elements.add(new Elements.BarElement(label, progress, color, value));
		return this;
	}

	@Override
	public InfoEntry entry(EmiRenderable icon, Text label) {
		EntryImpl entry = new EntryImpl(icon, label);
		elements.add(entry);
		return entry;
	}

	@Override
	public InfoSection element(InfoElement element) {
		elements.add(element);
		return this;
	}
}
