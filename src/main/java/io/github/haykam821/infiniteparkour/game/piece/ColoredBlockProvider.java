package io.github.haykam821.infiniteparkour.game.piece;

import java.util.function.Function;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import xyz.nucleoid.plasmid.api.util.ColoredBlocks;

@FunctionalInterface
public interface ColoredBlockProvider {
	public static final ColoredBlockProvider WOOL = ColoredBlockProvider.ofDefaultState(ColoredBlocks::wool);
	public static final ColoredBlockProvider GLASS = ColoredBlockProvider.ofDefaultState(ColoredBlocks::glass);

	public BlockState forColor(DyeColor color);

	public static ColoredBlockProvider ofDefaultState(Function<DyeColor, Block> blockMapper) {
		return color -> {
			return blockMapper.apply(color).defaultBlockState();
		};
	}
}
