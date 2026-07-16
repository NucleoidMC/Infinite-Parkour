package io.github.haykam821.infiniteparkour.game.map;

import java.io.IOException;

import io.github.haykam821.infiniteparkour.game.InfiniteParkourConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.plasmid.api.game.GameOpenException;

public class InfiniteParkourMapBuilder {
	private final InfiniteParkourConfig config;

	public InfiniteParkourMapBuilder(InfiniteParkourConfig config) {
		this.config = config;
	}

	public InfiniteParkourMap create(MinecraftServer server) {
		try {
			MapTemplate template = MapTemplateSerializer.loadFromResource(server, this.config.map());
			return new InfiniteParkourMap(template);
		} catch (IOException exception) {
			throw new GameOpenException(Component.translatable("text.infiniteparkour.template_load_failed"), exception);
		}
	}
}