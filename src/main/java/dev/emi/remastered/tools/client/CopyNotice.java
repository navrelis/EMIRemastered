package dev.emi.remastered.tools.client;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.runtime.EmiDrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/**
 * Feedback of "copy recipe". EMI cancels the toast drawing while its overlay is shown on top of a container screen (and
 * the recipe screen counts as one), so a toast would never be seen where the recipe is copied. In that case a small
 * notice is drawn in the top right corner by EMI's screen manager instead; everywhere else a normal toast is used.
 */
public final class CopyNotice {
	private static final long DURATION_MS = 4000;
	private static final int PAD = 8;
	private static final int MARGIN = 4;

	private static @Nullable Text title;
	private static @Nullable Text text;
	private static long until;

	private CopyNotice() {
	}

	/** Same condition as EMI's toast manager mixin, which hides every toast while it is true. */
	public static boolean toastsHidden() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.currentScreen != null && EmiConfig.enabled && EmiApi.getHandledScreen() != null;
	}

	/** Shows a message with the title in a toast, or in the notice when toasts are hidden. */
	public static void show(Text title, Text text) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (toastsHidden()) {
			CopyNotice.title = title;
			CopyNotice.text = text;
			until = Util.getMeasuringTimeMs() + DURATION_MS;
		} else {
			SystemToast.add(client.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION, title, text);
		}
	}

	/** Draws the notice, called at the end of EMI's screen manager render. */
	public static void render(EmiDrawContext context) {
		Text title = CopyNotice.title;
		Text text = CopyNotice.text;
		if (title == null || text == null) {
			return;
		}
		if (Util.getMeasuringTimeMs() >= until) {
			CopyNotice.title = null;
			CopyNotice.text = null;
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		TextRenderer font = client.textRenderer;
		int screenWidth = client.getWindow().getScaledWidth();
		int width = Math.min(screenWidth - MARGIN * 2, Math.max(160, PAD * 2 + Math.max(font.getWidth(title), font.getWidth(text))));
		int height = 30;
		int x = screenWidth - width - MARGIN;
		int y = MARGIN;
		context.raw().getMatrices().push();
		context.raw().getMatrices().translate(0, 0, 500);
		context.resetColor();
		EmiRenderHelper.drawNinePatch(context, EmiRenderHelper.BACKGROUND, x, y, width, height, 0, 0, 4, 1);
		context.drawText(title, x + PAD, y + 6, 0x404040);
		context.drawText(EmiPort.literal(font.trimToWidth(text.getString(), width - PAD * 2)), x + PAD, y + 17, 0x6C6C6C);
		context.raw().getMatrices().pop();
	}
}
