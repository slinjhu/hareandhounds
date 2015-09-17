//-------------------------------------------------------------------------------------------------------------//
// Code based on the ToDoApp from OOSE class
// https://github.com/jhu-oose/todo
//-------------------------------------------------------------------------------------------------------------//

package com.oose2015.slin52.hareandhounds;

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.json.*;

public class GameService {

	private Sql2o db;
	private final Logger logger = LoggerFactory.getLogger(GameService.class);

	/**
	 * Check if the database file exists in the current directory. If it does
	 * create a DataSource instance for the file and return it. If it does not,
	 * create a new database file
	 * 
	 * @return javax.sql.DataSource corresponding to the game database
	 */
	private static DataSource configureDataSource() {
		Path dbPath = Paths.get(".", "game.db");
		if (!(Files.exists(dbPath))) {
			try {
				Files.createFile(dbPath);
				System.out.println(">> created the database file");
			} catch (java.io.IOException ex) {
				System.err.println(">> Failed to create the database file");
			}
		}
		SQLiteDataSource dataSource = new SQLiteDataSource();
		dataSource.setUrl("jdbc:sqlite:game.db");
		return dataSource;
	}

	/**
	 * Construct the service model. The current implementation also ensures that
	 * the DB schema is created if necessary.
	 *
	 */
	public GameService() throws GameServiceException {
		DataSource dataSource = configureDataSource();
		this.db = new Sql2o(dataSource);
		// Create schema
		try (Connection conn = db.open()) {
			String sqlGame = "CREATE TABLE IF NOT EXISTS game (gameId INTEGER PRIMARY KEY autoincrement,"
					+ "state TEXT, boardStatus TEXT, playerHare TEXT, playerHound TEXT)";
			String sqlBoard = "CREATE TABLE IF NOT EXISTS board (id INTEGER PRIMARY KEY autoincrement,"
					+ "gameId INTEGER,	boardStatus TEXT)";
			conn.createQuery(sqlGame).executeUpdate();
			conn.createQuery(sqlBoard).executeUpdate();
		} catch (Sql2oException ex) {
			String errMsg = "Failed to create schema at startup";
			this.logger.error(errMsg);
			throw new GameServiceException(errMsg, ex);
		}
	}

	/**
	 * Update game information in the database.
	 * 
	 * @param game
	 *            the game object to be updated.
	 * @param gameId
	 *            id of the game object.
	 * @throws GameServiceException
	 */
	public void updateGame(Game game, int gameId) throws GameServiceException {
		try (Connection conn = db.open()) {
			String sql = "UPDATE game SET state = :state, " + "boardStatus = :boardStatus, playerHare = :playerHare, "
					+ "playerHound = :playerHound WHERE gameId = :gameId";
			conn.createQuery(sql).addParameter("gameId", gameId).bind(game).executeUpdate();
		} catch (Sql2oException ex) {
			String errMsg = "GameService.updateGame: Failed to update game";
			this.logger.error(errMsg);
			throw new GameServiceException(errMsg, ex);
		}
	}

	/**
	 * Check if the players are stalling
	 * 
	 * @param gameId
	 *            id of the game to be checked.
	 * @return true if stalling and false otherwise.
	 * @throws GameServiceException
	 */
	public boolean isStalling(int gameId) throws GameServiceException {
		try (Connection conn = db.open()) {
			String sql = "select count(boardStatus) from board where gameId = :gameId "
					+ "group by boardStatus order by count(boardStatus) desc limit 1";
			int maxRepetition = conn.createQuery(sql).addParameter("gameId", gameId).executeScalar(Integer.class);
			if (maxRepetition >= 3) {
				return true;
			} else {
				return false;
			}

		} catch (Sql2oException ex) {
			String errMsg = "GameService.isStalling: Failed to check stalling";
			this.logger.error(errMsg);
			throw new GameServiceException(errMsg, ex);
		}
	}

	/**
	 * Create a new game and the first player, let the first player join the
	 * game, and save to database.
	 * 
	 * @param pieceType
	 *            pieceType of the first player in the game to be created.
	 * @return A JSON object containing information about the newly created game
	 * @throws GameServiceException
	 */
	public JSONObject newGame(String pieceType) throws GameServiceException {
		Game game = new Game();
		// Create the first player and join game
		String playerId = "player1";
		if (game.join(pieceType, playerId)) {
			try (Connection conn = db.open()) {
				String sql = "INSERT INTO game (state, boardStatus, playerHare, playerHound) "
						+ "VALUES (:state, :boardStatus, :playerHare, :playerHound)";
				int gameId = conn.createQuery(sql, true).bind(game).executeUpdate().getKey(Integer.class);
				this.insertBoard(gameId, game.getBoardStatus());
				// Generate response
				JSONObject obj = new JSONObject();
				obj.put("gameId", gameId);
				obj.put("playerId", playerId);
				obj.put("pieceType", pieceType);
				return obj;

			} catch (Sql2oException ex) {
				String errMsg = "GameService.newGame: Failed to create a new game";
				this.logger.error(errMsg);
				throw new GameServiceException(errMsg, ex);
			}
		} else {
			return null;
		}

	}

	/**
	 * Insert the board status string and gameId into database table "board".
	 * 
	 * @param gameId
	 *            The id for the game.
	 * @param boardStatus
	 *            A string representation of the board screenshot.
	 */
	public void insertBoard(int gameId, String boardStatus) throws GameServiceException {
		String sql = "INSERT INTO board (gameId, boardStatus) VALUES (:gameId, :boardStatus)";
		try (Connection conn = db.open()) {
			conn.createQuery(sql).addParameter("gameId", gameId).addParameter("boardStatus", boardStatus)
					.executeUpdate();

		} catch (Sql2oException ex) {
			String errMsg = "GameService.insertBoard: Failed to insert game board to database";
			this.logger.error(errMsg);
			throw new GameServiceException(errMsg, ex);
		}

	}

	/**
	 * Find a game given an Id.
	 * 
	 * @param gameId
	 *            The id for the game.
	 * @return The game object corresponding to the id if one is found,
	 *         otherwise null
	 */
	public Game findGame(int gameId) throws GameServiceException {
		String sql = "SELECT state, boardStatus, playerHare, playerHound FROM game WHERE gameId = :gameId ";
		try (Connection conn = db.open()) {
			Game game = conn.createQuery(sql).addParameter("gameId", gameId).executeAndFetchFirst(Game.class);
			return game;
		} catch (Sql2oException ex) {
			String errMsg = "GameService.findGame: Failed to find the game with given gameId in the database";
			this.logger.error(errMsg);
			throw new GameServiceException(errMsg, ex);
		}

	}

	public static class GameServiceException extends Exception {
		public GameServiceException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
