//-------------------------------------------------------------------------------------------------------------//
// Code based on the ToDoApp from OOSE class
// https://github.com/jhu-oose/todo
//-------------------------------------------------------------------------------------------------------------//

package com.oose2015.slin52.hareandhounds;

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.Collections;

import static spark.Spark.*;
import org.json.*;

public class GameController {

	private static final String API_CONTEXT = "hareandhounds/api";

	private final GameService gameService;

	private final Logger logger = LoggerFactory.getLogger(GameController.class);

	public GameController(GameService gameService) {
		this.gameService = gameService;
		setupEndpoints();
	}

	private void setupEndpoints() {
		// Start a game
		post(API_CONTEXT + "/games", "application/json", (request, response) -> {
			try {
				String pieceType = new JSONObject(request.body()).getString("pieceType");
				JSONObject body = gameService.newGame(pieceType);
				if (body != null) {
					response.status(201);
					return body.toString();
				} else {
					// Invalid pieceType
					response.status(404);
					return Collections.EMPTY_MAP;
				}
			} catch (GameService.GameServiceException ex) {
				this.logger.error("Failed to create a new game");
				response.status(500);
				return Collections.EMPTY_MAP;
			}
		});

		// Join a game
		put(API_CONTEXT + "/games/:gameId", "application/json", (request, response) -> {
			int gameId = Integer.parseInt(request.params(":gameId"));
			String playerId = "player2";
			try {
				Game game = gameService.findGame(gameId);
				if (game == null) {
					// Invalid gameId
					response.status(404);
					return "{\"reason\": \"INVALID_GAME_ID\"}";
				} else {
					String pieceType = game.join(playerId);
					if (pieceType.equals("null")) {
						// The second player has already joined.
						response.status(410);
						return Collections.EMPTY_MAP;
					} else {
						gameService.updateGame(game, gameId);
						response.status(200);
						JSONObject obj = new JSONObject();
						obj.put("gameId", gameId);
						obj.put("playerId", playerId);
						obj.put("pieceType", pieceType);
						return obj.toString();
					}
				}
			} catch (GameService.GameServiceException ex) {
				this.logger.error(String.format("Failed to find game with id: %s", request.params(":gameId")));
				response.status(500);
				return Collections.EMPTY_MAP;
			}
		});

		// Make a move (play a game)
		post(API_CONTEXT + "/games/:gameId/turns", "application/json", (request, response) -> {
			try {
				int gameId = Integer.parseInt(request.params(":gameId"));
				Game game = gameService.findGame(gameId);
				if (game == null) {
					response.status(404);
					return "{\"reason\": \"INVALID_GAME_ID\"}";
				} else {
					JSONObject data = new JSONObject(request.body());
					String playerId = data.getString("playerId");
					int fromX = data.getInt("fromX");
					int fromY = data.getInt("fromY");
					int toX = data.getInt("toX");
					int toY = data.getInt("toY");
					String rslt = game.turn(playerId, fromX, fromY, toX, toY);
					switch (rslt) {
					case "INVALID_PLAYER_ID":
						response.status(404);
						return String.format("{\"reason\": \"%s\"}", rslt);
					case "INCORRECT_TURN":
						response.status(422);
						return String.format("{\"reason\": \"%s\"}", rslt);

					case "ILLEGAL_MOVE":
						response.status(422);
						return String.format("{\"reason\": \"%s\"}", rslt);

					case "SUCCESS":
						// check stalling
						if (gameService.isStalling(gameId)) {
							game.setState("WIN_HARE_BY_STALLING");
						}
						playerId = game.nextPlayer();
						gameService.updateGame(game, gameId);
						gameService.insertBoard(gameId, game.getBoardStatus());
						response.status(200);
						return String.format("{\"playerId\": \"%s\"}", playerId);
					default:
						response.status(500);
						return "";
					}

				}
			} catch (GameService.GameServiceException ex) {
				this.logger.error("Failed to play the game");
				response.status(500);
				return Collections.EMPTY_MAP;
			}
		});

		// Describe the game board
		get(API_CONTEXT + "/games/:gameId/board", "application/json", (request, response) -> {
			try {
				int gameId = Integer.parseInt(request.params(":gameId"));
				Game game = gameService.findGame(gameId);
				if (game == null) {
					response.status(404);
					return "{\"reason\": \"INVALID_GAME_ID\"}";
				} else {
					response.status(200);
					return game.getBoardDescrition();
				}
			} catch (GameService.GameServiceException ex) {
				logger.error("Failed to get game board description");
				response.status(500);
				return Collections.EMPTY_MAP;
			}
		});

		// Describe the game state
		get(API_CONTEXT + "/games/:gameId/state", "application/json", (request, response) -> {
			try {
				int gameId = Integer.parseInt(request.params(":gameId"));
				Game game = gameService.findGame(gameId);
				if (game == null) {
					response.status(404);

					return "{\"reason\": \"INVALID_GAME_ID\"}";
				} else {
					response.status(200);
					JSONObject obj = new JSONObject();
					obj.put("state", game.getState());
					return obj.toString();
				}
			} catch (GameService.GameServiceException ex) {
				this.logger.error("Failed to get game state");
				response.status(500);
				return Collections.EMPTY_MAP;
			}
		});

	}

}
