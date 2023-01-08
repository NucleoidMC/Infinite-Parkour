package io.github.haykam821.infiniteparkour.game.map;

import com.google.common.base.Preconditions;

import io.github.haykam821.infiniteparkour.game.piece.ParkourPiece;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

public class InfiniteParkourMap {
	private static final String SPAWN_MARKER = "spawn";
	private static final String START_MARKER = "start";
	private static final String EXIT_MARKER = "exit";
	
	private static final String FACING_KEY = "Facing";

	private final MapTemplate template;
	private final LongSet initialBlocks = new LongOpenHashSet();

	private final TemplateRegion spawn;
	private final TemplateRegion start;
	private final Box exit;

	public InfiniteParkourMap(MapTemplate template) {
		this.template = template;
		for (BlockPos pos : this.template.getBounds()) {
			if (!template.getBlockState(pos).isAir()) {
				this.initialBlocks.add(pos.asLong());
			}
		}

		this.spawn = template.getMetadata().getFirstRegion(SPAWN_MARKER);
		Preconditions.checkNotNull(this.spawn, "Spawn is not present");

		this.start = template.getMetadata().getFirstRegion(START_MARKER);
		Preconditions.checkNotNull(this.start, "Start is not present");

		this.exit = InfiniteParkourMap.getBox(template, EXIT_MARKER);
	}

	public Vec3d getSpawnPos() {
		return this.spawn.getBounds().centerBottom();
	}

	public float getSpawnAngle() {
		return InfiniteParkourMap.getAngle(this.spawn);
	}

	public boolean isPlayerExiting(ServerPlayerEntity player) {
		return this.exit != null && this.exit.intersects(player.getBoundingBox());
	}

	private BlockPos getStartPos() {
		return this.start.getBounds().min();
	}

	private float getStartAngle() {
		return InfiniteParkourMap.getAngle(this.start);
	}

	public ParkourPiece createStartPiece(DyeColor color, Random random) {
		double angle = this.getStartAngle() + (Math.PI / 2);
		return new ParkourPiece(this.getStartPos(), angle, color, random);
	}

	public void destroy(ServerWorld world) {
		BlockPos.Mutable pos = new BlockPos.Mutable();
		BlockPos startPos = this.start.getBounds().min();

		LongIterator iterator = this.initialBlocks.longIterator();
		while (iterator.hasNext()) {
			pos.set(iterator.nextLong());
			if (!pos.equals(startPos)) {
				world.setBlockState(pos, ParkourPiece.AIR);
			}
		}
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}

	private static Box getBox(MapTemplate template, String marker) {
		BlockBounds bounds = template.getMetadata().getFirstRegionBounds(marker);
		return bounds == null ? null : bounds.asBox();
	}

	private static float getAngle(TemplateRegion region) {
		NbtCompound data = region.getData();
		if (data == null) {
			return 0;
		}

		return data.getFloat(FACING_KEY);
	}
}