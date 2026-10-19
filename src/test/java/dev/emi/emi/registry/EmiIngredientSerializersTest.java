package dev.emi.emi.registry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

/** Entries of a type nobody registered (mod removed, plugin disabled) read as empty instead of throwing. */
class EmiIngredientSerializersTest {

	@Test
	void unknownObjectTypeReadsAsEmpty() {
		JsonObject json = new JsonObject();
		json.addProperty("type", "test_unknown_mod:thing");
		json.addProperty("id", "test_unknown_mod:x");
		assertSame(EmiStack.EMPTY, assertDoesNotThrow(() -> EmiIngredientSerializers.deserialize(json)));
		EmiIngredient second = assertDoesNotThrow(() -> EmiIngredientSerializers.deserialize(json));
		assertSame(EmiStack.EMPTY, second);
	}

	@Test
	void unknownStringTypeReadsAsEmpty() {
		assertSame(EmiStack.EMPTY, assertDoesNotThrow(() -> EmiIngredientSerializers.deserialize(new JsonPrimitive("test_unknown_mod:x"))));
	}

	@Test
	void objectWithoutTypeReadsAsEmpty() {
		JsonObject json = new JsonObject();
		json.addProperty("id", "minecraft:stone");
		assertSame(EmiStack.EMPTY, assertDoesNotThrow(() -> EmiIngredientSerializers.deserialize(json)));
	}
}
