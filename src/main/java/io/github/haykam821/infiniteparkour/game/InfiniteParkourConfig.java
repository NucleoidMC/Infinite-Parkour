package io.github.haykam821.infiniteparkour.game;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.stats.GameStatisticBundle;

public record InfiniteParkourConfig(
	Identifier map,
	SoundConfig soundConfig,
	Vec3d spectatorSpawnOffset,
	int ticksUntilClose,
	int maxPieceHistorySize,
	int failurePadding,
	double maxAngleVariance,
	Optional<Double> pieceOffsetRadius,
	Optional<String> statisticBundleNamespace
) {
	public static final Codec<InfiniteParkourConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Identifier.CODEC.fieldOf("map").forGetter(InfiniteParkourConfig::map),
			SoundConfig.CODEC.optionalFieldOf("sounds", SoundConfig.DEFAULT).forGetter(InfiniteParkourConfig::soundConfig),
			Vec3f.CODEC.xmap(Vec3d::new, Vec3f::new).optionalFieldOf("spectator_spawn_offset", new Vec3d(0, 2, 0)).forGetter(InfiniteParkourConfig::spectatorSpawnOffset),
			Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("ticks_until_close", 20 * 10).forGetter(InfiniteParkourConfig::ticksUntilClose),
			Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("max_piece_history_size", 500).forGetter(InfiniteParkourConfig::maxPieceHistorySize),
			Codec.intRange(Integer.MIN_VALUE, 0).optionalFieldOf("failure_padding", 3).forGetter(InfiniteParkourConfig::failurePadding),
			Codec.doubleRange(0, Math.PI).optionalFieldOf("max_angle_variance", 30d * MathHelper.RADIANS_PER_DEGREE).forGetter(InfiniteParkourConfig::maxAngleVariance),
			Codec.doubleRange(0, Integer.MAX_VALUE).optionalFieldOf("piece_offset_radius").forGetter(InfiniteParkourConfig::pieceOffsetRadius),
			Codec.STRING.optionalFieldOf("statistic_bundle_namespace").forGetter(InfiniteParkourConfig::statisticBundleNamespace)
		).apply(instance, InfiniteParkourConfig::new);
	});

	public GameStatisticBundle getStatisticBundle(GameSpace gameSpace) {
		if (this.statisticBundleNamespace.isEmpty()) {
			return null;
		}
		return gameSpace.getStatistics().bundle(this.statisticBundleNamespace.get());
	}
}
