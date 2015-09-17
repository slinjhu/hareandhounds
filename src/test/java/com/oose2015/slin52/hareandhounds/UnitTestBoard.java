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

public class UnitTestBoard {
	@Test
	public void testConstructor() {
		Board board = new Board();
		// check initialize vertices status
		assertEquals(board.getPieceTypeByXY(0, 1), "HOUND");
		assertEquals(board.getPieceTypeByXY(1, 0), "HOUND");
		assertEquals(board.getPieceTypeByXY(1, 2), "HOUND");
		assertEquals(board.getPieceTypeByXY(4, 1), "HARE");
		assertEquals(board.getPieceTypeByXY(3, 1), "NULL");
	}

	@Test
	public void testSetPieceType() {
		Board board = new Board();
		int x = 2;
		int y = 1;

		// Illegal pieceType
		String oldPieceType = board.getPieceTypeByXY(x, y);
		board.setPieceTypeByXY(x, y, "BALABALA");
		assertEquals(board.getPieceTypeByXY(x, y), oldPieceType);

		// Legal pieceType
		board.setPieceTypeByXY(x, y, "HARE");
		assertEquals(board.getPieceTypeByXY(x, y), "HARE");

	}

	@Test
	public void testAdjacency() {
		Board board = new Board();
		assertEquals(board.isAdjacent(0, 1, 1, 1), true);
		assertEquals(board.isAdjacent(2, 0, 1, 1), false);
		assertEquals(board.isAdjacent(2, 0, 2, 2), false);
	}

	@Test
	public void testSetStatus() {
		Board board1 = new Board();
		Board board2 = new Board();
		board2.setPieceTypeByXY(2, 2, "HARE");
		// Set status
		board1.setStatus(board2.getStatus());
		assertEquals(board1.toString(), board2.toString());
	}

	@Test
	public void testGetVerticesByPieceType() {
		Board board = new Board();
		assertEquals(board.getVerticesByPieceType("HARE").size(), 1);
		assertEquals(board.getVerticesByPieceType("HOUND").size(), 3);
		assertEquals(board.getVerticesByPieceType("NULL").size(), 7);
	}

}