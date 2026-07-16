package io.github.haykam821.infiniteparkour;

import io.github.haykam821.infiniteparkour.game.InfiniteParkourConfig;
import io.github.haykam821.infiniteparkour.game.InfiniteParkourGame;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.GameTypes;

public class InfiniteParkour implements ModInitializer {
	private static final String MOD_ID = "infiniteparkour";

	private static final Identifier INFINITE_PARKOUR_ID = InfiniteParkour.identifier("infinite_parkour");
	public static final GameType<InfiniteParkourConfig> INFINITE_PARKOUR = GameTypes.register(INFINITE_PARKOUR_ID, InfiniteParkourConfig.CODEC, InfiniteParkourGame::open);

	@Override
	public void onInitialize() {
		return;
	}

	public static Identifier identifier(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
