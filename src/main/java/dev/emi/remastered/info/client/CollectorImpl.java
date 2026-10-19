package dev.emi.remastered.info.client;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.info.api.InfoCollector;
import dev.emi.remastered.info.api.InfoSection;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Collects the sections of all providers for one stack. */
final class CollectorImpl implements InfoCollector {
	private final EmiStack stack;
	private final List<SectionImpl> sections = new ArrayList<>();
	private Identifier provider;

	CollectorImpl(EmiStack stack) {
		this.stack = stack;
	}

	void begin(Identifier provider) {
		this.provider = provider;
	}

	@Override
	public EmiStack getStack() {
		return stack;
	}

	@Override
	public InfoSection addSection(Identifier id, InfoSection.Kind kind, Text title) {
		SectionImpl section = new SectionImpl(id, provider, kind, title);
		sections.add(section);
		return section;
	}

	@Override
	public boolean hasSource() {
		for (SectionImpl section : sections) {
			if (section.countsAsSource()) {
				return true;
			}
		}
		return false;
	}

	List<SectionImpl> getSections() {
		return sections.stream().filter(s -> !s.isEmpty()).toList();
	}
}
