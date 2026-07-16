package io.github.haykam821.infiniteparkour.game.piece;

import io.github.haykam821.infiniteparkour.game.InfiniteParkourConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ParkourPiece {
	public static final BlockState AIR = Blocks.AIR.defaultBlockState();

	private final BlockPos pos;
	private final double angle;
	private final AABB completionBox;

	private final DyeColor color;
	private final RandomSource random;

	public ParkourPiece(BlockPos pos, double angle, DyeColor color, RandomSource random) {
		this.pos = pos;
		this.angle = angle;
		this.completionBox = new AABB(pos.above(2));

		this.color = ParkourBlockColor.getOrPickColor(color, random);
		this.random = random;
	}

	public void placeWool(ServerLevel world) {
		this.place(world, ColoredBlockProvider.WOOL);
	}

	public void placeGlass(ServerLevel world) {
		this.place(world, ColoredBlockProvider.GLASS);
	}

	public void destroy(ServerLevel world) {
		world.setBlockAndUpdate(this.pos, AIR);
	}

	private void place(ServerLevel world, ColoredBlockProvider provider) {
		BlockState state = provider.forColor(this.color);
		world.setBlockAndUpdate(this.pos, state);
	}

	protected BlockPos getPos() {
		return this.pos;
	}

	protected boolean isCompleted(ServerPlayer player, InfiniteParkourConfig config) {
		return player.onGround() && this.completionBox.intersects(player.getBoundingBox());
	}

	private boolean isDeltaOutOfWorld(int deltaY, ServerLevel world) {
		if (deltaY < 0 && this.pos.getY() == world.getMinY()) return true;
		if (deltaY > 0 && this.pos.getY() == world.getMaxY()) return true;

		return false;
	}

	public int getDeltaY(ParkourPiece lastPiece) {
		return this.pos.getY() - lastPiece.pos.getY();
	}

	private double getRadius(int deltaY, InfiniteParkourConfig config) {
		return config.pieceOffsetRadius().orElseGet(() -> {
			return Mth.nextDouble(this.random, 3, 5 - deltaY);
		});
	}

	public ParkourPiece createNextPiece(ServerLevel world, DyeColor color, InfiniteParkourConfig config) {
		int deltaY = Mth.nextInt(this.random, -1, 1);
		if (this.isDeltaOutOfWorld(deltaY, world)) {
			deltaY = 0;
		}

		double radius = this.getRadius(deltaY, config);

		double deltaAngle = Mth.nextDouble(random, -config.maxAngleVariance(), config.maxAngleVariance());
		double angle = this.angle + deltaAngle;

		int deltaX = (int) (radius * Math.cos(angle));
		int deltaZ = (int) (radius * Math.sin(angle));

		BlockPos pos = this.pos.offset(deltaX, deltaY, deltaZ);
		return new ParkourPiece(pos, angle, color, this.random);
	}
}
