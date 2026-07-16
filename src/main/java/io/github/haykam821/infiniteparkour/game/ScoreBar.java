package io.github.haykam821.infiniteparkour.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.BossBarWidget;

public final class ScoreBar {
	private static final BossEvent.BossBarColor COLOR = BossEvent.BossBarColor.YELLOW;
	private static final BossEvent.BossBarOverlay STYLE = BossEvent.BossBarOverlay.PROGRESS;

	private static final Component NAME = Component.translatable("gameType.infiniteparkour.infinite_parkour");
	private static final ChatFormatting FORMATTING = ChatFormatting.YELLOW;

	private final InfiniteParkourGame game;
	private final BossBarWidget widget;

	public ScoreBar(InfiniteParkourGame game, GlobalWidgets widgets) {
		this.game = game;
		this.widget = widgets.addBossBar(this.getTitle(), COLOR, STYLE);
	}

	public void updateTitle() {
		this.widget.setTitle(this.getTitle());
	}

	private Component getTitle() {
		int score = this.game.getScore();
		return Component.translatable("text.infiniteparkour.bar.title", NAME, score).withStyle(FORMATTING);
	}
}
