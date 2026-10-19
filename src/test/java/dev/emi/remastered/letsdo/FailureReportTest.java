package dev.emi.remastered.letsdo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FailureReportTest {

	@Test
	void emptyReportHasNothingToSay() {
		FailureReport report = new FailureReport("vinery");
		assertTrue(report.isEmpty());
		assertEquals(0, report.size());
	}

	@Test
	void oneLineForAllFailuresWithoutStackTrace() {
		FailureReport report = new FailureReport("vinery");
		report.add("grape_mashing", new NoSuchFieldError("GRAPEVINE_POT"));
		report.add("cheese", new NoClassDefFoundError("net/satisfy/meadow/Foo"));
		assertFalse(report.isEmpty());
		assertEquals(2, report.size());
		String line = report.format();
		assertEquals("vinery: skipped grape_mashing (NoSuchFieldError: GRAPEVINE_POT); cheese (NoClassDefFoundError: net/satisfy/meadow/Foo)", line);
		assertFalse(line.contains("\n"));
		assertFalse(line.contains("\tat "));
	}

	@Test
	void usesTheRootCauseAndShortensLongMessages() {
		Throwable wrapped = new RuntimeException("outer", new IllegalStateException("x".repeat(500)));
		String text = FailureReport.describe(wrapped);
		assertTrue(text.startsWith("IllegalStateException: xxx"));
		assertEquals("IllegalStateException: ".length() + FailureReport.MAX_MESSAGE, text.length());
		assertTrue(text.endsWith("..."));
	}

	@Test
	void messagesWithoutTextFallBackToTheClassName() {
		assertEquals("ClassCastException", FailureReport.describe(new ClassCastException()));
		assertEquals("IllegalStateException: a b c", FailureReport.describe(new IllegalStateException("a\n  b\tc")));
	}
}
