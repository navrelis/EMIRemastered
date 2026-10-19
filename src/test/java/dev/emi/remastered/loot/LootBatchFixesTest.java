package dev.emi.remastered.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.emi.remastered.loot.emi.BatchFailureLog;
import dev.emi.remastered.loot.parser.function.CopyNameFunctionParser;

class LootBatchFixesTest {
	private static JsonObject lang(String name) throws IOException {
		try (Reader reader = new InputStreamReader(LootBatchFixesTest.class.getResourceAsStream("/assets/emi_loot/lang/" + name + ".json"), StandardCharsets.UTF_8)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}

	@Test
	void copyNameKeysAreLowerCaseAndExistInTheLangFiles() throws IOException {
		assertEquals("emi_loot.function.copy_name.block_entity", CopyNameFunctionParser.sourceKey("BLOCK_ENTITY"));
		for (String lang : List.of("en_us", "de_de")) {
			JsonObject json = lang(lang);
			for (String source : List.of("THIS", "KILLER", "KILLER_DIRECT", "BLOCK_ENTITY")) {
				assertTrue(json.has(CopyNameFunctionParser.sourceKey(source)), lang + " lacks the key of " + source);
			}
		}
		assertEquals("the block entity of this block", lang("en_us").get("emi_loot.function.copy_name.block_entity").getAsString());
	}

	@Test
	void onlyTheFirstFailuresKeepTheirStackTrace() {
		AtomicInteger errors = new AtomicInteger();
		AtomicInteger withThrowable = new AtomicInteger();
		Logger logger = (Logger) Proxy.newProxyInstance(Logger.class.getClassLoader(), new Class<?>[] {Logger.class}, (proxy, method, args) -> {
			if (method.getName().equals("error")) {
				errors.incrementAndGet();
				Object last = args[args.length - 1];
				if (last instanceof Throwable) {
					withThrowable.incrementAndGet();
				}
			}
			return null;
		});
		BatchFailureLog log = new BatchFailureLog(logger, "Block loot recipes");
		int logged = 0;
		for (int i = 0; i < 13790; i++) {
			if (log.fail("building a Block loot recipe", new IllegalStateException("no world"))) {
				logged++;
			}
		}
		assertEquals(BatchFailureLog.MAX_STACK_TRACES, logged);
		assertEquals(13790, log.failures());
		assertEquals(5, errors.get());
		log.summarize();
		assertEquals(6, errors.get());
		assertEquals(5, withThrowable.get());
	}

	@Test
	void noSummaryWithoutOverflow() {
		AtomicInteger errors = new AtomicInteger();
		Logger logger = (Logger) Proxy.newProxyInstance(Logger.class.getClassLoader(), new Class<?>[] {Logger.class}, (proxy, method, args) -> {
			if (method.getName().equals("error")) {
				errors.incrementAndGet();
			}
			return null;
		});
		BatchFailureLog log = new BatchFailureLog(logger, "Chest");
		assertEquals(0, log.failures());
		log.fail("x", new RuntimeException());
		log.summarize();
		assertEquals(1, errors.get());
	}
}
