package io.github.haykam821.infiniteparkour.game;

import com.google.common.collect.EvictingQueue;

import io.github.haykam821.infiniteparkour.game.map.InfiniteParkourMap;
import io.github.haykam821.infiniteparkour.game.map.InfiniteParkourMapBuilder;
import io.github.haykam821.infiniteparkour.game.piece.Completion;
import io.github.haykam821.infiniteparkour.game.piece.ParkourPiece;
import io.github.haykam821.infiniteparkour.game.piece.ParkourPieceSet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.player.JoinOfferResult;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.game.stats.GameStatisticBundle;
import xyz.nucleoid.plasmid.api.game.stats.StatisticKeys;
import xyz.nucleoid.plasmid.api.game.stats.StatisticMap;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class InfiniteParkourGame implements GameActivityEvents.Tick, GamePlayerEvents.Remove, GamePlayerEvents.Accept, GamePlayerEvents.Offer, PlayerDamageEvent, PlayerDeathEvent {
	private final GameSpace gameSpace;
	private final ServerLevel world;
	private final InfiniteParkourMap map;
	private final InfiniteParkourConfig config;
	private final GameStatisticBundle statistics;
	private final ScoreBar bar;

	private final EvictingQueue<ParkourPiece> pieces;
	private ParkourPiece lastPiece = null;
	private final ParkourPieceSet nextPieces;

	private ServerPlayer mainPlayer;
	private DyeColor color = null;
	private int score = 0;

	private int ticksUntilClose = -1;

	public InfiniteParkourGame(GameSpace gameSpace, ServerLevel world, InfiniteParkourMap map, InfiniteParkourConfig config, GlobalWidgets widgets) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;
		this.statistics = config.getStatisticBundle(gameSpace);
		this.bar = new ScoreBar(this, widgets);

		this.pieces = EvictingQueue.create(config.maxPieceHistorySize());
		this.nextPieces = new ParkourPieceSet(this.config.nextPiecesSize());

		ParkourPiece startPiece = this.map.createStartPiece(this.color, this.world.getRandom());
		this.nextPieces.placeInitialPieces(startPiece, this.world, this.color, this.config);
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.BLOCK_DROPS);
		activity.deny(GameRuleType.BREAK_BLOCKS);
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.DISMOUNT_VEHICLE);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.FIRE_TICK);
		activity.deny(GameRuleType.FLUID_FLOW);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.ICE_MELT);
		activity.deny(GameRuleType.MODIFY_ARMOR);
		activity.deny(GameRuleType.MODIFY_INVENTORY);
		activity.deny(GameRuleType.PICKUP_ITEMS);
		activity.deny(GameRuleType.PLACE_BLOCKS);
		activity.deny(GameRuleType.PLAYER_PROJECTILE_KNOCKBACK);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.PVP);
		activity.deny(GameRuleType.SWAP_OFFHAND);
		activity.deny(GameRuleType.THROW_ITEMS);
		activity.deny(GameRuleType.TRIDENTS_LOYAL_IN_VOID);
		activity.deny(GameRuleType.UNSTABLE_TNT);
		activity.deny(GameRuleType.USE_BLOCKS);
	}

	public static GameOpenProcedure open(GameOpenContext<InfiniteParkourConfig> context) {
		InfiniteParkourConfig config = context.config();
		InfiniteParkourMap map = new InfiniteParkourMapBuilder(config).create(context.server());

		RuntimeLevelConfig worldConfig = new RuntimeLevelConfig()
			.setGenerator(map.createGenerator(context.server()));

		return context.openWithLevel(worldConfig, (activity, world) -> {
			GlobalWidgets widgets = GlobalWidgets.addTo(activity);

			InfiniteParkourGame phase = new InfiniteParkourGame(activity.getGameSpace(), world, map, config, widgets);
			InfiniteParkourGame.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.TICK, phase);
			activity.listen(GamePlayerEvents.ACCEPT, phase);
			activity.listen(GamePlayerEvents.OFFER, phase);
			activity.listen(PlayerDamageEvent.EVENT, phase);
			activity.listen(PlayerDeathEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.REMOVE, phase);
		});
	}

	// Listeners
	@Override
	public void onTick() {
		if (this.mainPlayer == null) return;

		// Decrease ticks until game end to zero
		if (this.ticksUntilClose >= 0) {
			if (this.ticksUntilClose == 0) {
				this.gameSpace.close(GameCloseReason.FINISHED);
			}

			this.ticksUntilClose -= 1;
			return;
		}

		// Kick the main player from the game space if they are exiting
		if (this.map.isPlayerExiting(this.mainPlayer)) {
			this.gameSpace.getPlayers().kick(this.mainPlayer);
			return;
		}

		Completion completion = this.nextPieces.getCompletion(this.mainPlayer, this.config);
		if (completion instanceof Completion.Complete complete) {
			this.addNextPiece(complete.piece(), this.config.trackSkippedScore() ? complete.score() : 1);
		} else if (completion instanceof Completion.Failed) {
			this.endGame();
		}
	}

	@Override
	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		if (this.mainPlayer == null) {
			return acceptor.teleport(this.world, this.map.getSpawnPos()).thenRunForEach(player -> {
				this.mainPlayer = player;
				player.setYRot(this.map.getSpawnAngle());

				player.setGameMode(GameType.ADVENTURE);
			});
		} else {
			Vec3 pos = this.mainPlayer.position().add(this.config.spectatorSpawnOffset());
			return acceptor.teleport(this.world, pos).thenRunForEach(player -> {
				player.setYRot(this.mainPlayer.getYRot());
				player.setXRot(this.mainPlayer.getXRot());

				player.setGameMode(GameType.SPECTATOR);
			});
		}
	}

	@Override
	public JoinOfferResult onOfferPlayers(JoinOffer offer) {
		return this.mainPlayer == null ? offer.acceptParticipants() : offer.acceptSpectators();
	}

	@Override
	public EventResult onDamage(ServerPlayer player, DamageSource source, float damage) {
		return EventResult.DENY;
	}

	@Override
	public EventResult onDeath(ServerPlayer player, DamageSource source) {
		if (player == this.mainPlayer) {
			this.endGame();
		}
		return EventResult.DENY;
	}

	@Override
	public void onRemovePlayer(ServerPlayer player) {
		if (player == this.mainPlayer) {
			this.gameSpace.close(GameCloseReason.FINISHED);
		}
	}

	// Utilities
	private void sendSound(SoundEvent sound, float pitch) {
		this.gameSpace.getPlayers().playSound(sound, SoundSource.PLAYERS, this.config.soundConfig().volume(), pitch);
	}

	private void sendSound(SoundEvent sound) {
		this.sendSound(sound, this.config.soundConfig().pitch());
	}

	private void addNextPiece(ParkourPiece completedPiece, int score) {
		if (this.statistics != null) {
			StatisticMap map = this.statistics.forPlayer(this.mainPlayer);

			map.increment(StatisticKeys.POINTS, score);
			if (this.score == 0) {
				map.increment(StatisticKeys.GAMES_PLAYED, 1);
			}
		}

		this.score += score;
		this.mainPlayer.setExperienceLevels(this.score);
		this.bar.updateTitle();

		int deltaY = this.lastPiece == null ? 0 : completedPiece.getDeltaY(this.lastPiece);
		this.sendSound(this.config.soundConfig().nextPiece(), this.config.soundConfig().pitch() + deltaY * this.config.soundConfig().nextPiecePitchVariance());

		if (this.lastPiece == null) {
			this.map.destroy(this.world);
		} else {
			this.lastPiece.destroy(this.world);
			this.pieces.add(this.lastPiece);
		}
		this.lastPiece = completedPiece;

		this.nextPieces.updateCompletedPieces(completedPiece, this.world, this.color, this.config);
	}

	private void endGame() {
		this.ticksUntilClose = this.config.ticksUntilClose();
		this.mainPlayer.setGameMode(GameType.SPECTATOR);

		for (ParkourPiece piece : this.pieces) {
			piece.placeGlass(this.world);
		}

		Component message = Component.translatable("text.infiniteparkour.reached_score", this.mainPlayer.getDisplayName(), this.score).withStyle(ChatFormatting.GOLD);
		this.gameSpace.getPlayers().sendMessage(message);

		this.sendSound(this.config.soundConfig().gameEnd());
	}

	public int getScore() {
		return this.score;
	}
}