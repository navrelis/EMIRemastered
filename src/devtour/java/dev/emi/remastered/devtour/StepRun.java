package dev.emi.remastered.devtour;

/**
 * A step that is in progress. {@link #tick()} is called once per client tick after the step started, until it
 * returns true. Throwing marks the step as failed. Steps that finish immediately return {@link #DONE}.
 */
@FunctionalInterface
public interface StepRun {
	/** The step is already finished (it did all its work in {@link StepHandler#begin}). */
	StepRun DONE = () -> true;

	/** @return true when the step is finished */
	boolean tick() throws Exception;
}
