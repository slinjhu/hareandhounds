package com.oose2015.slin52.hareandhounds;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.sql.DataSource;

import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import com.oose2015.slin52.hareandhounds.GameService.GameServiceException;

public class UnitTestGame {
	@Test
	public void testGameConstructor() {
		Game game = new Game();
		assertEquals(game.getBoardStatus(), "HOUND_NULL_NULL_HOUND_NULL_NULL_NULL_HARE_HOUND_NULL_NULL");

	}

	@Test
	public void testGameJoin() {
		Game game = new Game();
		// First player
		game.join("HARE", "player1");
		assertEquals(game.getState(), "WAITING_FOR_SECOND_PLAYER");

		// Second player
		String pieceType = game.join("player2");
		assertEquals(pieceType, "HOUND");
		assertEquals(game.getState(), "TURN_HOUND");

		// Third player
		assertEquals(game.join("player3"), "null");

	}

	@Test
	public void testGameTurn() {
		Game game = new Game();
		game.join("HOUND", "player1");
		game.join("player2"); // HARE

		// player not in turn
		assertEquals(game.turn("player2", 0, 1, 1, 1), "INCORRECT_TURN");

		// Hound player tries to move hare piece
		assertEquals(game.turn("player1", 4, 1, 3, 1), "ILLEGAL_MOVE");

		// Hound player moves his own piece too far away
		assertEquals(game.turn("player1", 0, 1, 2, 1), "ILLEGAL_MOVE");

		// Hound player moves his own piece correctly
		assertEquals(game.turn("player1", 0, 1, 1, 1), "SUCCESS");

		// Check if next player is correct
		assertEquals(game.nextPlayer(), "player2");
		// System.out.println(game.getLogWords());

	}

}