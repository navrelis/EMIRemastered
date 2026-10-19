package dev.emi.remastered.devtour;

/** Implements one step type. Runs on the client (render) thread. */
@FunctionalInterface
public interface StepHandler {
	/**
	 * Starts the step. Do instant work here and return {@link StepRun#DONE}, or return a {@link StepRun} that is
	 * ticked until it finishes. Throw to fail the step; the message ends up in summary.json.
	 */
	StepRun begin(Args args, TourContext ctx) throws Exception;
}
