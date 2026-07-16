package io.github.haykam821.infiniteparkour.game.piece;

import net.minecraft.util.Util;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;

public final class ParkourBlockColor {
	private static final DyeColor[] COLORS = DyeColor.values();

	private ParkourBlockColor() {
		return;
	}

	protected static DyeColor getOrPickColor(DyeColor color, RandomSource random) {
		if (color == null) {
			// Pick a random color
			return Util.getRandom(COLORS, random);
		} else {
			// Color already determined
			return color;
		}
	}
}
