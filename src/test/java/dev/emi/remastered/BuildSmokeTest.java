package dev.emi.remastered;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class BuildSmokeTest {

	@Test
	void modMetadataIsProcessed() throws IOException {
		try (InputStream in = BuildSmokeTest.class.getClassLoader().getResourceAsStream("fabric.mod.json")) {
			assertNotNull(in, "fabric.mod.json on the test classpath");
			JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			assertEquals("emi", json.get("id").getAsString());
			String version = json.get("version").getAsString();
			assertFalse(version.contains("${"), "version placeholder expanded: " + version);
			assertEquals("emi.accesswidener", json.get("accessWidener").getAsString());
		}
	}

	@Test
	void globalMixinIsGenerated() {
		assertNotNull(BuildSmokeTest.class.getClassLoader().getResource("dev/emi/emi/mixin/GlobalMixin.class"),
			"generateClassLists output on the classpath");
	}
}
