//-------------------------------------------------------------------------------------------------------------//
// Code based on the ToDoApp from OOSE class
// https://github.com/jhu-oose/todo
//-------------------------------------------------------------------------------------------------------------//

package com.oose2015.slin52.hareandhounds;

import java.util.*;
import com.google.gson.Gson;
import org.json.*;
import java.lang.Math;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The game class of hare and hounds
 * 
 * @author Sen Lin
 *
 */
public class Game {
	private final Logger logger = LoggerFactory.getLogger(Game.class);
	private String state;
	private String boardStatus;
	private String playerHare;
	private String playerHound;

	/**
	 * State of the game; could be one of WAITING_FOR_SECOND_PLAYER, TURN_HARE,
	 * TURN_HOUND, WIN_HARE_BY_ESCAPE, WIN_HARE_BY_STALLING, WIN_HOUND.
	 * 
	 * @return String of the state
	 */
	public String getState() {
		return this.state;
	}

	/**
	 * A string containing status of all vertices, delimited by "_"
	 * 
	 */
	public String getBoardStatus() {
		return this.boardStatus;
	}

	/**
	 * ID of the player who plays hare
	 * 
	 */
	public String getPlayerHare() {
		return this.playerHare;
	}

	/**
	 * ID of the player who plays hound
	 * 
	 */
	public String getPlayerHound() {
		return this.playerHound;
	}

	public void setState(String state) {
		this.state = state;
	}

	public void setBoardStatus(String boardStatus) {
		this.boardStatus = boardStatus;
	}

	public void setPlayerHare(String playerHare) {
		this.playerHare = playerHare;
	}

	public void setPlayerHound(String playerHound) {
		this.playerHound = playerHound;
	}

	Game() {
		this.state = "null";
		this.playerHare = "null";
		this.playerHound = "null";
		Board board = new Board();
		this.boardStatus = board.getStatus();
	}

	/**
	 * Convert boardStatus (String representation of the board positions) to
	 * JSON object of four pieces.
	 * 
	 * @return JSON string of the four pieces.
	 */
	public String getBoardDescrition() {
		Board board = new Board();
		board.setStatus(this.boardStatus);
		return board.toString();
	}

	/**
	 * Check if a player can make a move.
	 * 
	 * @param playerId
	 *            ID of the player who requests to make a move
	 * @param fromX
	 *            X coordinate of the origin position
	 * @param fromY
	 *            Y coordinate of the origin position
	 * @param toX
	 *            X coordinate of the destination position
	 * @param toY
	 *            Y coordinate of the destination position
	 * @return status of this move; could be INVALID_PLAYER_ID, INCORRECT_TURN,
	 *         ILLEGAL_MOVE or SUCCESS
	 */
	public String turn(String playerId, int fromX, int fromY, int toX, int toY) {
		Board board = new Board();
		board.setStatus(this.boardStatus);

		String pieceTypePlayer = this.getPlayerPieceType(playerId);
		if (!pieceTypePlayer.equals("HARE") && !pieceTypePlayer.equals("HOUND")) {
			return "INVALID_PLAYER_ID";
		} else if (!this.state.equals("TURN_" + pieceTypePlayer)) {
			// This is not the player's turn yet
			return "INCORRECT_TURN";
		} else {
			String pieceTypeFrom = board.getPieceTypeByXY(fromX, fromY);
			String pieceTypeTo = board.getPieceTypeByXY(toX, toY);
			if (!pieceTypeFrom.equals(pieceTypePlayer)) {
				// the player cannot move this piece
				return "ILLEGAL_MOVE";
			} else if (!pieceTypeTo.equals("NULL")) {
				// the destination is already occupied
				return "ILLEGAL_MOVE";
			} else if (!board.isAdjacent(fromX, fromY, toX, toY)) {
				// the move is not adjacent (connect by one and only one edge)
				return "ILLEGAL_MOVE";
			} else if (pieceTypeFrom.equals("HOUND") && toX < fromX) {
				// HOUND cannot go backward
				return "ILLEGAL_MOVE";
			} else {
				// legal move
				board.setPieceTypeByXY(fromX, fromY, "NULL");
				board.setPieceTypeByXY(toX, toY, pieceTypeFrom);
				this.boardStatus = board.getStatus();
				this.checkGameOver();
				return "SUCCESS";
			}
		}
	}

	/**
	 * Check if the game is over (not considering stalling). If over, change
	 * game.state
	 */
	public void checkGameOver() {
		Board board = new Board();
		board.setStatus(this.boardStatus);

		String hareState = board.checkHareState();
		if (hareState != null) {
			this.state = hareState;
		} else {

		}

	}

	/**
	 * @return ID of the player who are supposed to move next.
	 */
	public String nextPlayer() {
		switch (this.state) {
		case "TURN_HARE":
			this.state = "TURN_HOUND";
			return this.playerHound;
		case "TURN_HOUND":
			this.state = "TURN_HARE";
			return this.playerHare;
		default:
			return null;
		}
	}

	/**
	 * Let the second player join the game, and then change the game state.
	 * 
	 * @param playerId
	 *            ID of the second player
	 * @return pieceType of the second player assigned by the game; "null" if
	 *         the game is not expecting another player.
	 */
	public String join(String playerId) {
		// The second player cannot choose pieceType
		if (this.playerHound.equals("null") && !this.playerHare.equals("null")) {
			this.playerHound = playerId;
			this.state = "TURN_HOUND";
			return "HOUND";
		} else if (!this.playerHound.equals("null") && this.playerHare.equals("null")) {
			this.playerHare = playerId;
			this.state = "TURN_HOUND";
			return "HARE";
		} else {
			return "null";
		}
	}

	/**
	 * Let the first player join the game with pieceType chosen by the player,
	 * and then change game state.
	 * 
	 * @param pieceType
	 *            pieceType that the first player wants to be
	 * @param playerId
	 *            ID of the first player
	 * @return true if successfully joined, false otherwise.
	 * 
	 */
	public boolean join(String pieceType, String playerId) {

		if (this.playerHound.equals("null") && this.playerHare.equals("null")) {
			switch (pieceType) {
			case "HARE":
				this.playerHare = playerId;
				this.state = "WAITING_FOR_SECOND_PLAYER";
				return true;
			case "HOUND":
				this.playerHound = playerId;
				this.state = "WAITING_FOR_SECOND_PLAYER";
				return true;
			default:
				return false;
			}
		} else {
			return false;
		}

	}

	/**
	 * Get pieceType of a player given by ID.
	 * 
	 * @param playerId
	 *            ID of the player that you'd like to know pieceType
	 * @return pieceType of the player with given ID.
	 */
	public String getPlayerPieceType(String playerId) {
		String pieceType = "";
		if (this.playerHare.equals(playerId)) {
			pieceType += "HARE";
		}
		if (this.playerHound.equals(playerId)) {
			pieceType += "HOUND";
		}
		return pieceType;
	}

	/**
	 * Override the default toString method
	 */
	@Override
	public String toString() {
		return String.format("state: %s, boardStatus: %s, playerHare: %s, playerHound: %s", this.state,
				this.boardStatus, this.playerHare, this.playerHound);
	}
}

class Board {
	private class Vertex {
		public final int x;
		public final int y;
		private String pieceType = "NULL";

		public Vertex(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public JSONObject toJson() {
			JSONObject obj = new JSONObject();
			obj.put("x", this.x);
			obj.put("y", this.y);
			obj.put("pieceType", this.pieceType);
			return obj;
		}
	}

	private List<Vertex> allVertices;

	public Board() {
		this.initAllVertices();
		// Initialize hare and hounds
		this.setPieceTypeByXY(0, 1, "HOUND");
		this.setPieceTypeByXY(1, 0, "HOUND");
		this.setPieceTypeByXY(1, 2, "HOUND");
		this.setPieceTypeByXY(4, 1, "HARE");
	}

	/**
	 * Initialize all vertices in the board and save them to board.allVertices
	 */
	private void initAllVertices() {
		// Initialize all positions
		int[][] allCoords = { { 1, 0 }, { 2, 0 }, { 3, 0 }, { 0, 1 }, { 1, 1 }, { 2, 1 }, { 3, 1 }, { 4, 1 }, { 1, 2 },
				{ 2, 2 }, { 3, 2 } };
		this.allVertices = new ArrayList<Vertex>();
		for (int[] coord : allCoords) {
			this.allVertices.add(new Vertex(coord[0], coord[1]));
		}
	}

	/**
	 * Fetch a list of pieces with given pieceType
	 * 
	 * @param pieceType
	 *            could be "HARE" or "HOUND"
	 * @return a list of vertices of given pieceType
	 */
	public List<Vertex> getVerticesByPieceType(String pieceType) {
		List<Vertex> pieces = new ArrayList<Vertex>();
		for (Vertex vtx : this.allVertices) {
			if (vtx.pieceType.equals(pieceType)) {
				pieces.add(vtx);
			}
		}
		return pieces;
	}

	/**
	 * Fetch neighbor vertices of a given vertex. Two vertices are neighbors if
	 * and only if they are connected and their distance is one.
	 * 
	 * @param me
	 *            the vertex that you want to find neighbors.
	 * @return a list of vertices that are neighbors of me.
	 */
	public List<Vertex> getVertexNeighbors(Vertex me) {
		List<Vertex> pieces = new ArrayList<Vertex>();
		for (Vertex vtx : this.allVertices) {
			if (this.isAdjacent(me.x, me.y, vtx.x, vtx.y)) {
				pieces.add(vtx);
			}
		}
		return pieces;
	}

	/**
	 * Check if two given vertices are adjacent (connected and distance one)
	 * 
	 * @param fromX
	 *            X coordinate of the first vertex.
	 * @param fromY
	 *            Y coordinate of the first vertex.
	 * @param toX
	 *            X coordinate of the second vertex.
	 * @param toY
	 *            Y coordinate of the second vertex.
	 * @return true if adjacent, false otherwise.
	 * 
	 */
	public boolean isAdjacent(int fromX, int fromY, int toX, int toY) {
		int distance = Math.max(Math.abs(fromX - toX), Math.abs(fromY - toY));
		if (distance != 1) {
			return false;
		} else {
			Set<String> isolatedVertices = new HashSet<String>(Arrays.asList("11_22_31_20".split("_")));
			if (isolatedVertices.contains("" + fromX + fromY) && isolatedVertices.contains("" + toX + toY)) {
				return false;
			} else {
				return true;
			}
		}
	}

	/**
	 * Use a string to represent status of all vertices. Status of a vertex is
	 * "HARE" if a hare stands on it, "HOUND" if a hound stands on it, and
	 * "NULL" if nothing stands on it.
	 * 
	 * @return a string of concatenated vertex statuses delimited by "_".
	 */
	public String getStatus() {
		List<String> status = new ArrayList<String>();
		for (Vertex vtx : this.allVertices) {
			status.add(vtx.pieceType);
		}
		return String.join("_", status);
	}

	/**
	 * Set pieceType of a vertex given by coordinates
	 * 
	 * @param x
	 *            X coordinate of the vertex to set
	 * @param y
	 *            Y coordinate of the vertex to set
	 * @param pieceType
	 *            the new pieceType
	 */
	public void setPieceTypeByXY(int x, int y, String pieceType) {
		if (pieceType.equals("HARE") || pieceType.equals("HOUND") || pieceType.equals("NULL")) {
			for (Vertex vtx : this.allVertices) {
				if (vtx.x == x && vtx.y == y) {
					vtx.pieceType = pieceType;
				}
			}
		} else {
			System.err.println("Wrong pieceType: " + pieceType);
		}
	}

	/**
	 * Get pieceType of vertex given by coordinates.
	 * 
	 * @param x
	 *            X coordinate of the vertex to get pieceType
	 * @param y
	 *            Y coordinate of the vertex to get pieceType
	 * @return pieceType of the vertex
	 */
	public String getPieceTypeByXY(int x, int y) {
		for (Vertex vtx : this.allVertices) {
			if (vtx.x == x && vtx.y == y) {
				return vtx.pieceType;
			}
		}
		return null;
	}

	/**
	 * Retrieve board positions from a status string.
	 * 
	 * @param statusWords
	 *            a string of all vertex statuses delimited by "_"
	 */
	public void setStatus(String statusWords) {
		String[] allStatus = statusWords.split("_");
		for (int index = 0; index < allStatus.length; index++) {
			this.allVertices.get(index).pieceType = allStatus[index];
		}
	}

	/**
	 * Check if the hare has escaped or been trapped by hounds
	 * 
	 * @return "WIN_HARE_BY_ESCAPE" if escaped, "WIN_HOUND" if trapped, and null
	 *         if otherwise.
	 */
	public String checkHareState() {
		Vertex hare = this.getVerticesByPieceType("HARE").get(0);
		// check if the hare escaped
		List<Vertex> hounds = this.getVerticesByPieceType("HOUND");
		if (hounds.get(0).x >= hare.x && hounds.get(1).x >= hare.x && hounds.get(2).x >= hare.x) {
			return "WIN_HARE_BY_ESCAPE";
		}

		// check if the hare is trapped
		boolean flag = true;
		for (Vertex nei : this.getVertexNeighbors(hare)) {
			flag &= !nei.pieceType.equals("NULL");
		}
		if (flag) {
			return "WIN_HOUND";
		}
		return null;
	}

	/**
	 * Override the default toString method.
	 * 
	 * @return a JSON string of four pieces.
	 */
	@Override
	public String toString() {
		JSONArray list = new JSONArray();
		for (Vertex vtx : this.allVertices) {
			if (!vtx.pieceType.equals("NULL")) {
				list.put(vtx.toJson());
			}
		}
		return list.toString();
	}

}
