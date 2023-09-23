package io.github.haykam821.infiniteparkour.game.piece;

public sealed interface Completion {
	public record Incomplete() implements Completion {
		public static final Incomplete INSTANCE = new Incomplete();

		@Override
		public String toString() {
			return "Incomplete";
		}
	}

	public record Complete(ParkourPiece piece, int score) implements Completion {
		@Override
		public String toString() {
			return "Completed " + this.piece + " (" + this.score + ")";
		}
	}

	public record Failed() implements Completion {
		public static final Failed INSTANCE = new Failed();

		@Override
		public String toString() {
			return "Failed";
		}
	}
}
