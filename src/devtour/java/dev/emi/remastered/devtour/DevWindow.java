package dev.emi.remastered.devtour;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.MinecraftClient;

/**
 * Window mode of dev clients. By default the game window is created hidden (GLFW_VISIBLE false): it never appears on
 * the screen, never gets a taskbar button and can never take the keyboard focus, while Minecraft keeps rendering into
 * its own framebuffer (screenshots work). {@code -Demi.devtour.visible=true} (gradle {@code -PdevtourVisible=true})
 * shows the window, still created without focus and without activation.
 */
public final class DevWindow {
	private static final Logger LOG = LoggerFactory.getLogger("EmiDevTour");
	public static final boolean VISIBLE = Boolean.getBoolean("emi.devtour.visible");
	private static boolean warned = false;

	private DevWindow() {
	}

	public static String mode() {
		return VISIBLE ? "visible" : "hidden";
	}

	/** Called once right after glfwCreateWindow. */
	public static void created(long handle) {
		LOG.info("dev client window {} ({}), focused={} (set -PdevtourVisible=true to show it)", mode(),
			describe(handle), attrib(handle, GLFW.GLFW_FOCUSED));
	}

	/** Safety net, every client tick: a hidden dev window that became visible (any caller) is hidden again. */
	public static void tick(MinecraftClient client) {
		if (VISIBLE || client.getWindow() == null) {
			return;
		}
		long handle = client.getWindow().getHandle();
		if (attrib(handle, GLFW.GLFW_VISIBLE)) {
			GLFW.glfwHideWindow(handle);
			if (!warned) {
				warned = true;
				LOG.warn("dev client window was shown by someone, hidden again ({})", describe(handle));
			}
		}
	}

	public static boolean attrib(long handle, int attrib) {
		return GLFW.glfwGetWindowAttrib(handle, attrib) == GLFW.GLFW_TRUE;
	}

	public static String describe(long handle) {
		int[] x = new int[1];
		int[] y = new int[1];
		GLFW.glfwGetWindowPos(handle, x, y);
		int[] w = new int[1];
		int[] h = new int[1];
		GLFW.glfwGetWindowSize(handle, w, h);
		int[] fw = new int[1];
		int[] fh = new int[1];
		GLFW.glfwGetFramebufferSize(handle, fw, fh);
		return "visible=" + attrib(handle, GLFW.GLFW_VISIBLE) + " pos=" + x[0] + "," + y[0] + " size=" + w[0] + "x" + h[0]
			+ " framebuffer=" + fw[0] + "x" + fh[0];
	}
}
