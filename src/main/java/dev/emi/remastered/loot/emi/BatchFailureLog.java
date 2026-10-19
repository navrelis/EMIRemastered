package dev.emi.remastered.loot.emi;

import org.slf4j.Logger;

/**
 * Logs the first few failures of a recipe batch with their stack trace and only counts the rest, so a batch that fails
 * for every entry (for example because the world disappeared) cannot flood the log.
 */
public class BatchFailureLog {
    public static final int MAX_STACK_TRACES = 5;

    private final Logger logger;
    private final String batch;
    private int failures;

    public BatchFailureLog(Logger logger, String batch) {
        this.logger = logger;
        this.batch = batch;
    }

    /**
     * @return true when this failure was logged with its stack trace
     */
    public boolean fail(String what, Throwable error) {
        failures++;
        if (failures > MAX_STACK_TRACES) {
            return false;
        }
        logger.error("Critical error encountered while {}; entry skipped", what, error);
        return true;
    }

    public int failures() {
        return failures;
    }

    /**
     * Logs one summary line when more failures happened than were logged in full.
     */
    public void summarize() {
        if (failures > MAX_STACK_TRACES) {
            logger.error("{}: {} entries failed in total, only the first {} were logged with a stack trace", batch, failures, MAX_STACK_TRACES);
        }
    }
}
