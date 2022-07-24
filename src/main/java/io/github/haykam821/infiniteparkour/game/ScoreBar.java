package io.github.haykam821.infiniteparkour.game;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.widget.BossBarWidget;

public final class ScoreBar {
	private static final BossBar.Color COLOR = BossBar.Color.YELLOW;
	private static final BossBar.Style STYLE = BossBar.Style.PROGRESS;

	private static final Text NAME = Text.translatable("gameType.infiniteparkour.infinite_parkour");
	private static final Formatting FORMATTING = Formatting.YELLOW;

	private final InfiniteParkourGame game;
	private final BossBarWidget widget;

	public ScoreBar(InfiniteParkourGame game, GlobalWidgets widgets) {
		this.game = game;
		this.widget = widgets.addBossBar(this.getTitle(), COLOR, STYLE);
	}

	public void updateTitle() {
		this.widget.setTitle(this.getTitle());
	}

	private Text getTitle() {
		int score = this.game.getScore();
		return Text.translatable("text.infiniteparkour.bar.title", NAME, score).formatted(FORMATTING);
	}
}
