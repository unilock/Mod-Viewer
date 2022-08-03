package io.github.haykam821.modviewer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

public final class MicroIconRenderer {
	private static final int MAX_ICON_SIZE = 32;

	private static final String PIXEL_STRING = "█";
	private static final String TRANSPARENCY_STRING = "▒";
	private static final Text TRANSPARENCY_TEXT = Text.literal(TRANSPARENCY_STRING).formatted(Formatting.BLACK);

	private static final Map<String, Text[]> CACHE = new HashMap<>();

	private MicroIconRenderer() {
		return;
	}

	public static Text[] getIcon(ModContainer mod) {
		return CACHE.computeIfAbsent(mod.getMetadata().getId(), modId -> {
			return MicroIconRenderer.renderIcon(mod);
		});
	}

	private static Text[] renderIcon(ModContainer mod) {
		try {
			Path iconPath = MicroIconRenderer.getIconPath(mod);
			if (iconPath == null) return null;

			try (InputStream stream = Files.newInputStream(iconPath)) {
				BufferedImage image = ImageIO.read(stream);
				if (MicroIconRenderer.isInvalidSize(image)) return null;

				Int2ObjectMap<Text> pixels = new Int2ObjectOpenHashMap<>();

				var stepSize = MathHelper.ceil(image.getWidth() / 32d);
				var width = image.getWidth();
				var height = image.getHeight();


				var rendered = new ArrayList<MutableText>();


				for (int y = 0; y < height; y += stepSize) {
					var text = Text.empty();
					for (int x = 0; x < width; x += stepSize) {
						int argba = image.getRGB(x, y);
						text.append(pixels.computeIfAbsent(argba, MicroIconRenderer::getPixel));
					}
					rendered.add(text);
				}

				return rendered.toArray(new MutableText[0]);
			}
		} catch (IOException exception) {
			ModViewer.LOGGER.warn("Failed to render icon for mod {}", mod.getMetadata().getId(), exception);
			return null;
		}
	}

	private static boolean isInvalidSize(BufferedImage image) {
		int width = image.getWidth();
		if (width <= 0) return true;
		//if (width > MAX_ICON_SIZE) return true;

		int height = image.getHeight();
		if (height <= 0) return true;
		//if (height > MAX_ICON_SIZE) return true;

		return false;
	}

	private static Path getIconPath(ModContainer mod) {
		Optional<String> optionalPathStr = mod.getMetadata().getIconPath(MAX_ICON_SIZE);
		if (optionalPathStr.isEmpty()) return null;

		Optional<Path> optionalPath = mod.findPath(optionalPathStr.get());
		return optionalPath.orElse(null);
	}

	private static Text getPixel(int argba) {
		int alpha = argba >> 24;
		if (alpha == 0x00) return TRANSPARENCY_TEXT;

		TextColor color = TextColor.fromRgb(argba & 0xFFFFFF);
		return Text.literal(alpha >= 0 ? TRANSPARENCY_STRING : PIXEL_STRING).styled(style -> {
			return style.withColor(color);
		});
	}
}
