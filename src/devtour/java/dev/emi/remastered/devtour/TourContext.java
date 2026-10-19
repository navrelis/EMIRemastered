package dev.emi.remastered.devtour;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;

/** Shared state handed to every step handler. */
public final class TourContext {
	public final MinecraftClient client;
	public final TourPlan plan;
	public final Path outDir;

	private volatile long frames = 0;
	private int screenshotCount = 0;
	private String info = null;
	private String lastScreenshot = null;

	public TourContext(MinecraftClient client, TourPlan plan, Path outDir) {
		this.client = client;
		this.plan = plan;
		this.outDir = outDir;
	}

	/** Number of frames rendered so far (counted by a HUD render callback). */
	public long frames() {
		return frames;
	}

	void frameRendered() {
		frames++;
	}

	/** Attaches a free-form note to the current step in summary.json. */
	public void info(String text) {
		info = info == null ? text : info + "; " + text;
	}

	String takeInfo() {
		String i = info;
		info = null;
		return i;
	}

	String takeScreenshotName() {
		String s = lastScreenshot;
		lastScreenshot = null;
		return s;
	}

	/**
	 * Captures the main framebuffer (what was drawn in the last frame) to {@code <NN>-<name>.png} in the tour output
	 * folder. Must be called on the render thread, after the frame of interest was rendered.
	 */
	public Path screenshot(String name) throws IOException {
		String safe = name.replaceAll("[^A-Za-z0-9._-]", "_").toLowerCase(Locale.ROOT);
		screenshotCount++;
		Path file = outDir.resolve(String.format("%02d-%s.png", screenshotCount, safe));
		Files.createDirectories(outDir);
		try (NativeImage image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
			image.writeTo(file);
		}
		lastScreenshot = file.getFileName().toString();
		return file;
	}
}
