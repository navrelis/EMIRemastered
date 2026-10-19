package dev.emi.remastered.info.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/** Ordering of info providers: by order, then by id string. Pure Java (unit tested). */
public final class ProviderOrdering {

	private ProviderOrdering() {
	}

	public static <P> List<P> sorted(List<? extends P> providers, ToIntFunction<? super P> order, Function<? super P, String> id) {
		List<P> list = new ArrayList<>(providers);
		list.sort(Comparator.<P>comparingInt(order).thenComparing(id));
		return list;
	}

	/**
	 * Stable sort of sections by their kind (ordinal): sections of one kind keep the order they were collected in,
	 * which is provider order and then insertion order.
	 */
	public static <S> List<S> byKind(List<? extends S> sections, ToIntFunction<? super S> kind) {
		List<S> list = new ArrayList<>(sections);
		list.sort(Comparator.comparingInt(kind));
		return list;
	}

	/**
	 * Runs {@code body} and returns null, or the throwable it threw (also {@link LinkageError}s, e.g. a provider
	 * that references a class of a mod that is not installed). Used to isolate providers from each other.
	 */
	public static Throwable isolate(Runnable body) {
		try {
			body.run();
			return null;
		} catch (Throwable t) {
			if (t instanceof VirtualMachineError vme && !(t instanceof StackOverflowError)) {
				throw vme;
			}
			return t;
		}
	}
}
