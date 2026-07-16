package io.github.haykam821.infiniteparkour.game.piece;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import io.github.haykam821.infiniteparkour.game.InfiniteParkourConfig;

public class ParkourPieceSet {
	private final int size;
	private final List<ParkourPiece> pieces;

	public ParkourPieceSet(int size) {
		this.size = size;
		this.pieces = new ArrayList<>(size);
	}

	public void placeInitialPieces(ParkourPiece piece, ServerLevel world, DyeColor color, InfiniteParkourConfig config) {
		ParkourPiece lastPiece = piece;

		while (this.pieces.size() < this.size) {
			this.pieces.add(lastPiece);
			lastPiece.placeWool(world);

			lastPiece = lastPiece.createNextPiece(world, color, config);
		}
	}

	public void updateCompletedPieces(ParkourPiece completedPiece, ServerLevel world, DyeColor color, InfiniteParkourConfig config) {
		Iterator<ParkourPiece> iterator = this.pieces.iterator();

		while (iterator.hasNext()) {
			ParkourPiece piece = iterator.next();

			iterator.remove();

			if (piece == completedPiece) {
				break;
			} else {
				piece.destroy(world);
			}
		}

		ParkourPiece lastPiece = this.pieces.isEmpty() ? completedPiece : this.pieces.get(this.pieces.size() - 1);
		boolean placedFirst = false;

		while (this.pieces.size() < this.size) {
			if (placedFirst) {
				this.pieces.add(lastPiece);
				lastPiece.placeWool(world);
			} else {
				placedFirst = true;
			}

			lastPiece = lastPiece.createNextPiece(world, color, config);
		}
	}

	public Completion getCompletion(ServerPlayer player, InfiniteParkourConfig config) {
		int minY = Integer.MAX_VALUE;
		int score = 1;

		for (ParkourPiece piece : this.pieces) {
			if (piece.isCompleted(player, config)) {
				return new Completion.Complete(piece, score);
			}
	
			minY = Math.min(minY, piece.getPos().getY());
			score += 1;
		}

		if (player.getY() < minY - config.failurePadding()) {
			return Completion.Failed.INSTANCE;
		}

		return Completion.Incomplete.INSTANCE;
	}
}