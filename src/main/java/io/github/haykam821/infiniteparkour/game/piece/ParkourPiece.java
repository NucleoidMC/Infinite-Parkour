package io.github.haykam821.infiniteparkour.game.piece;

import java.util.Random;

import io.github.haykam821.infiniteparkour.game.InfiniteParkourConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

public class ParkourPiece {
	public static final BlockState AIR = Blocks.AIR.getDefaultState();

	private final BlockPos pos;
	private final double angle;
	private final Box completionBox;

	private final DyeColor color;
	private final Random random;

	public ParkourPiece(BlockPos pos, double angle, DyeColor color, Random random) {
		this.pos = pos;
		this.angle = angle;
		this.completionBox = new Box(pos.up(2));

		this.color = ParkourBlockColor.getOrPickColor(color, random);
		this.random = random;
	}

	public void placeWool(ServerWorld world) {
		this.place(world, ColoredBlockProvider.WOOL);
	}

	public void placeGlass(ServerWorld world) {
		this.place(world, ColoredBlockProvider.GLASS);
	}

	public void destroy(ServerWorld world) {
		world.setBlockState(this.pos, AIR);
	}

	private void place(ServerWorld world, ColoredBlockProvider provider) {
		BlockState state = provider.forColor(this.color);
		world.setBlockState(this.pos, state);
	}

	public Completion getCompletion(ServerPlayerEntity player, InfiniteParkourConfig config) {
		if (player.getY() < this.pos.getY() - config.failurePadding()) {
			return Completion.FAILED;
		} else if (player.isOnGround() && this.completionBox.intersects(player.getBoundingBox())) {
			return Completion.COMPLETE;
		} else {
			return Completion.INCOMPLETE;
		}
	}

	private boolean isDeltaOutOfWorld(int deltaY, ServerWorld world) {
		if (deltaY < 0 && this.pos.getY() == world.getBottomY()) return true;
		if (deltaY > 0 && this.pos.getY() == world.getTopY()) return true;

		return false;
	}

	public int getDeltaY(ParkourPiece lastPiece) {
		return this.pos.getY() - lastPiece.pos.getY();
	}

	private double getRadius(int deltaY, InfiniteParkourConfig config) {
		return config.pieceOffsetRadius().orElseGet(() -> {
			return MathHelper.nextDouble(this.random, 3, 5 - deltaY);
		});
	}

	public ParkourPiece createNextPiece(ServerWorld world, DyeColor color, InfiniteParkourConfig config) {
		int deltaY = MathHelper.nextInt(this.random, -1, 1);
		if (this.isDeltaOutOfWorld(deltaY, world)) {
			deltaY = 0;
		}

		double radius = this.getRadius(deltaY, config);

		double deltaAngle = MathHelper.nextDouble(random, -config.maxAngleVariance(), config.maxAngleVariance());
		double angle = this.angle + deltaAngle;

		int deltaX = (int) (radius * Math.cos(angle));
		int deltaZ = (int) (radius * Math.sin(angle));

		BlockPos pos = this.pos.add(deltaX, deltaY, deltaZ);
		return new ParkourPiece(pos, angle, color, this.random);
	}
}
