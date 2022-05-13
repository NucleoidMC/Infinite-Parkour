package io.github.haykam821.infiniteparkour;

import io.github.haykam821.infiniteparkour.game.InfiniteParkourConfig;
import io.github.haykam821.infiniteparkour.game.InfiniteParkourGame;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.GameType;

public class InfiniteParkour implements ModInitializer {
	private static final String MOD_ID = "infiniteparkour";

	private static final Identifier INFINITE_PARKOUR_ID = new Identifier(MOD_ID, "infinite_parkour");
	public static final GameType<InfiniteParkourConfig> INFINITE_PARKOUR = GameType.register(INFINITE_PARKOUR_ID, InfiniteParkourConfig.CODEC, InfiniteParkourGame::open);

	@Override
	public void onInitialize() {
		return;
	}
}
