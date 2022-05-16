package io.github.haykam821.infiniteparkour.game;

import com.google.common.collect.EvictingQueue;

import io.github.haykam821.infiniteparkour.game.map.InfiniteParkourMap;
import io.github.haykam821.infiniteparkour.game.map.InfiniteParkourMapBuilder;
import io.github.haykam821.infiniteparkour.game.piece.Completion;
import io.github.haykam821.infiniteparkour.game.piece.ParkourPiece;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.game.stats.GameStatisticBundle;
import xyz.nucleoid.plasmid.game.stats.StatisticKeys;
import xyz.nucleoid.plasmid.game.stats.StatisticMap;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class InfiniteParkourGame implements GameActivityEvents.Tick, GamePlayerEvents.Remove, GamePlayerEvents.Offer, PlayerDamageEvent, PlayerDeathEvent {
	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final InfiniteParkourMap map;
	private final InfiniteParkourConfig config;
	private final GameStatisticBundle statistics;
	private final ScoreBar bar;

	private final EvictingQueue<ParkourPiece> pieces;
	private ParkourPiece lastPiece = null;
	private ParkourPiece nextPiece = null;

	private ServerPlayerEntity mainPlayer;
	private DyeColor color = null;
	private int score = 0;

	private int ticksUntilClose = -1;

	public InfiniteParkourGame(GameSpace gameSpace, ServerWorld world, InfiniteParkourMap map, InfiniteParkourConfig config, GlobalWidgets widgets) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;
		this.statistics = config.getStatisticBundle(gameSpace);
		this.bar = new ScoreBar(this, widgets);

		this.pieces = EvictingQueue.create(config.maxPieceHistorySize());

		this.nextPiece = this.map.createStartPiece(this.color, this.world.getRandom());
		this.nextPiece.placeWool(this.world);
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
	}

	public static GameOpenProcedure open(GameOpenContext<InfiniteParkourConfig> context) {
		InfiniteParkourConfig config = context.config();
		InfiniteParkourMap map = new InfiniteParkourMapBuilder(config).create(context.server());

		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
			.setGenerator(map.createGenerator(context.server()));

		return context.openWithWorld(worldConfig, (activity, world) -> {
			GlobalWidgets widgets = GlobalWidgets.addTo(activity);

			InfiniteParkourGame phase = new InfiniteParkourGame(activity.getGameSpace(), world, map, config, widgets);
			InfiniteParkourGame.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.TICK, phase);
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

		Completion completion = this.nextPiece.getCompletion(this.mainPlayer, this.config);
		if (completion == Completion.COMPLETE) {
			this.addNextPiece();
		} else if (completion == Completion.FAILED) {
			this.endGame();
		}
	}

	@Override
	public PlayerOfferResult onOfferPlayer(PlayerOffer offer) {
		if (this.mainPlayer == null) {
			return offer.accept(this.world, this.map.getSpawnPos()).and(() -> {
				this.mainPlayer = offer.player();
				offer.player().setYaw(this.map.getSpawnAngle());

				offer.player().changeGameMode(GameMode.ADVENTURE);
			});
		} else {
			Vec3d pos = this.mainPlayer.getPos().add(this.config.spectatorSpawnOffset());
			return offer.accept(this.world, pos).and(() -> {
				offer.player().setYaw(this.mainPlayer.getYaw());
				offer.player().setPitch(this.mainPlayer.getPitch());

				offer.player().changeGameMode(GameMode.SPECTATOR);
			});
		}
	}

	@Override
	public ActionResult onDamage(ServerPlayerEntity player, DamageSource source, float damage) {
		return ActionResult.FAIL;
	}

	@Override
	public ActionResult onDeath(ServerPlayerEntity player, DamageSource source) {
		if (player == this.mainPlayer) {
			this.endGame();
		}
		return ActionResult.FAIL;
	}

	@Override
	public void onRemovePlayer(ServerPlayerEntity player) {
		if (player == this.mainPlayer) {
			this.gameSpace.close(GameCloseReason.FINISHED);
		}
	}

	// Utilities
	private void sendSound(SoundEvent sound, float pitch) {
		this.gameSpace.getPlayers().playSound(sound, SoundCategory.PLAYERS, this.config.soundConfig().volume(), pitch);
	}

	private void sendSound(SoundEvent sound) {
		this.sendSound(sound, this.config.soundConfig().pitch());
	}

	private void addNextPiece() {
		if (this.statistics != null) {
			StatisticMap map = this.statistics.forPlayer(this.mainPlayer);

			map.increment(StatisticKeys.POINTS, 1);
			if (this.score == 0) {
				map.increment(StatisticKeys.GAMES_PLAYED, 1);
			}
		}

		this.score += 1;
		this.mainPlayer.setExperienceLevel(this.score);
		this.bar.updateTitle();

		int deltaY = this.lastPiece == null ? 0 : this.nextPiece.getDeltaY(this.lastPiece);
		this.sendSound(this.config.soundConfig().nextPiece(), this.config.soundConfig().pitch() + deltaY * this.config.soundConfig().nextPiecePitchVariance());

		if (this.lastPiece == null) {
			this.map.destroy(this.world);
		} else {
			this.lastPiece.destroy(this.world);
			this.pieces.add(this.lastPiece);
		}
		this.lastPiece = this.nextPiece;

		this.nextPiece = this.lastPiece.createNextPiece(this.world, this.color, this.config);
		this.nextPiece.placeWool(this.world);
	}

	private void endGame() {
		this.ticksUntilClose = this.config.ticksUntilClose();
		this.mainPlayer.changeGameMode(GameMode.SPECTATOR);

		for (ParkourPiece piece : this.pieces) {
			piece.placeGlass(this.world);
		}

		Text message = new TranslatableText("text.infiniteparkour.reached_score", this.mainPlayer.getDisplayName(), this.score).formatted(Formatting.GOLD);
		this.gameSpace.getPlayers().sendMessage(message);

		this.sendSound(this.config.soundConfig().gameEnd());
	}

	public int getScore() {
		return this.score;
	}
}