package dev.emi.remastered.letsdo;

/**
 * Support for one Let's Do mod. Implementations reference classes of that mod and are therefore only ever loaded by
 * name from {@link LetsDoContent#register} after the mod was found loaded; never reference them statically.
 * <p>
 * Everything an implementation needs from its mod must be read inside {@link #register}. The recipes it registers
 * (see {@link LetsDoRecipe}) only hold vanilla and EMI objects, so drawing them later can never fail on a mod class.
 */
public interface LetsDoSubModule {

	/**
	 * Registers the categories and recipes. May throw {@link LinkageError}s (a mod class or field that is missing in
	 * the installed version); the caller reports them as one warning line.
	 */
	void register(LetsDoContext context);
}
