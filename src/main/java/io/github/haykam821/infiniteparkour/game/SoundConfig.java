package io.github.haykam821.infiniteparkour.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public record SoundConfig(
	float volume,
	float pitch,
	SoundEvent nextPiece,
	float nextPiecePitchVariance,
	SoundEvent gameEnd
) {
	public static final Codec<SoundConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Codec.FLOAT.fieldOf("volume").forGetter(SoundConfig::volume),
			Codec.FLOAT.fieldOf("pitch").forGetter(SoundConfig::pitch),
			SoundEvent.CODEC.fieldOf("next_piece").forGetter(SoundConfig::nextPiece),
			Codec.FLOAT.fieldOf("next_piece_pitch_variance").forGetter(SoundConfig::nextPiecePitchVariance),
			SoundEvent.CODEC.fieldOf("game_end").forGetter(SoundConfig::gameEnd)
		).apply(instance, SoundConfig::new);
	});

	public static final SoundConfig DEFAULT = new SoundConfig(0.5f, 1f, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, SoundEvents.ENTITY_CREEPER_DEATH);
}
