package dev.emi.remastered.info.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.util.Identifier;

class ItemInfoRegistryTest {

	private record Provider(Identifier id, int order) implements ItemInfoProvider {
		@Override
		public Identifier getId() {
			return id;
		}

		@Override
		public int getOrder() {
			return order;
		}

		@Override
		public void collect(EmiStack stack, InfoCollector collector) {
		}
	}

	private static List<Identifier> ids(String namespace) {
		return ItemInfoRegistry.getProviders().stream().map(ItemInfoProvider::getId)
			.filter(id -> id.getNamespace().equals(namespace)).toList();
	}

	@Test
	void registerReplacesByIdAndSorts() {
		Identifier late = Identifier.of("test_registry", "late");
		Identifier b = Identifier.of("test_registry", "b");
		Identifier a = Identifier.of("test_registry", "a");
		int version = ItemInfoRegistry.getVersion();
		ItemInfoRegistry.register(new Provider(late, ItemInfoProvider.ORDER_LAST));
		ItemInfoRegistry.register(new Provider(b, ItemInfoProvider.ORDER_DEFAULT));
		ItemInfoRegistry.register(new Provider(a, ItemInfoProvider.ORDER_DEFAULT));
		assertEquals(List.of(a, b, late), ids("test_registry"));
		assertNotEquals(version, ItemInfoRegistry.getVersion());

		// an EMI plugin registering again on reload replaces the old instance and may change the order
		Provider again = new Provider(late, ItemInfoProvider.ORDER_RECIPES);
		ItemInfoRegistry.register(again);
		assertEquals(List.of(late, a, b), ids("test_registry"));
		assertSame(again, ItemInfoRegistry.getProviders().stream().filter(p -> p.getId().equals(late)).findFirst().orElseThrow());

		assertTrue(ItemInfoRegistry.unregister(b));
		assertFalse(ItemInfoRegistry.unregister(b));
		assertEquals(List.of(late, a), ids("test_registry"));
		ItemInfoRegistry.unregister(a);
		ItemInfoRegistry.unregister(late);
		assertEquals(List.of(), ids("test_registry"));
	}

	@Test
	void defaultsOfTheProviderInterface() {
		Provider p = new Provider(Identifier.of("test_defaults", "p"), 5);
		ItemInfoProvider plain = new ItemInfoProvider() {
			@Override
			public Identifier getId() {
				return p.id();
			}

			@Override
			public void collect(EmiStack stack, InfoCollector collector) {
			}
		};
		assertEquals(ItemInfoProvider.ORDER_DEFAULT, plain.getOrder());
		assertEquals(null, plain.getModuleId());
	}
}
