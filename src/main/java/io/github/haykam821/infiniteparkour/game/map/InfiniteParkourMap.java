package io.github.haykam821.infiniteparkour.game.map;

import com.google.common.base.Preconditions;

import io.github.haykam821.infiniteparkour.game.piece.ParkourPiece;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.level.generator.TemplateChunkGenerator;

public class InfiniteParkourMap {
	private static final String SPAWN_MARKER = "spawn";
	private static final String START_MARKER = "start";
	private static final String EXIT_MARKER = "exit";
	
	private static final String FACING_KEY = "Facing";

	private final MapTemplate template;
	private final LongSet initialBlocks = new LongOpenHashSet();

	private final TemplateRegion spawn;
	private final TemplateRegion start;
	private AABB exit;

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

	public Vec3 getSpawnPos() {
		return this.spawn.getBounds().centerBottom();
	}

	public float getSpawnAngle() {
		return InfiniteParkourMap.getAngle(this.spawn);
	}

	public boolean isPlayerExiting(ServerPlayer player) {
		return this.exit != null && this.exit.intersects(player.getBoundingBox());
	}

	private BlockPos getStartPos() {
		return this.start.getBounds().min();
	}

	private float getStartAngle() {
		return InfiniteParkourMap.getAngle(this.start);
	}

	public ParkourPiece createStartPiece(DyeColor color, RandomSource random) {
		double angle = this.getStartAngle() + (Math.PI / 2);
		return new ParkourPiece(this.getStartPos(), angle, color, random);
	}

	public void destroy(ServerLevel world) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockPos startPos = this.start.getBounds().min();

		LongIterator iterator = this.initialBlocks.longIterator();
		while (iterator.hasNext()) {
			pos.set(iterator.nextLong());
			if (!pos.equals(startPos)) {
				world.setBlockAndUpdate(pos, ParkourPiece.AIR);
			}
		}

		this.exit = null;
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}

	private static AABB getBox(MapTemplate template, String marker) {
		BlockBounds bounds = template.getMetadata().getFirstRegionBounds(marker);
		return bounds == null ? null : bounds.asBox();
	}

	private static float getAngle(TemplateRegion region) {
		CompoundTag data = region.getData();
		if (data == null) {
			return 0;
		}

		return data.getFloatOr(FACING_KEY, 0);
	}
}