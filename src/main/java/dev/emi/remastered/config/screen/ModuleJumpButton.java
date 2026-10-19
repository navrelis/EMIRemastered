package dev.emi.remastered.config.screen;

import java.util.List;

import dev.emi.emi.EmiPort;
import dev.emi.emi.screen.widget.config.ConfigJumpButton;
import dev.emi.remastered.EmiRemastered;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Jump button of EMI's config screen for the module groups. Same look as EMI's own jump buttons; icons come from
 * {@code assets/emi_remastered/textures/gui/config_jump.png} (row 0: modules, then one 16x16 icon per module in
 * registry order).
 */
public class ModuleJumpButton extends ConfigJumpButton {
	public static final Identifier TEXTURE = EmiPort.id(EmiRemastered.NAMESPACE, "textures/gui/config_jump.png");

	public ModuleJumpButton(int x, int y, int icon, PressAction action, List<Text> text) {
		super(x, y, icon * 16, 0, action, text);
		this.texture = TEXTURE;
	}
}
