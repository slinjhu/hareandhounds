package com.oose2015.slin52.hareandhounds;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URL;
import java.net.HttpURLConnection;
import javax.sql.DataSource;

import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.*;

import org.sqlite.SQLiteDataSource;

import com.google.gson.Gson;

import spark.Spark;
import spark.utils.IOUtils;
import java.util.*;

public class TestGameServer {

	@Before
	public void setup() throws Exception {
		// Delete the database file
		Path dbPath = Paths.get(".", "game.db");
		if (Files.exists(dbPath)) {
			Files.delete(dbPath);
		}

		// Start the main server
		Bootstrap.main(null);
		Spark.awaitInitialization();

	}

	@After
	public void clean() throws Exception {
		// Delete the database file
		Path dbPath = Paths.get(".", "game.db");
		if (Files.exists(dbPath)) {
			Files.delete(dbPath);
		}

		// Stop the spark server
		Spark.stop();
	}

	@Test
	public void testGame() throws Exception {
		// Start a game
		JSONObject content = new JSONObject();
		content.put("pieceType", "HOUND");
		Response resStart = request("POST", "games", content.toString());
		assertEquals(201, resStart.httpStatus);
		int gameId = new JSONObject(resStart.content).getInt("gameId");
		String playerHound = new JSONObject(resStart.content).getString("playerId");

		// verify game state
		assertEquals("WAITING_FOR_SECOND_PLAYER", this.getState(gameId));

		// Join the game
		Response resJoin = request("PUT", "games/" + gameId, null);
		String pieceType = new JSONObject(resJoin.content).getString("pieceType");
		String playerHare = new JSONObject(resJoin.content).getString("playerId");
		assertEquals(200, resJoin.httpStatus);
		assertEquals(pieceType, "HARE");

		// Join again
		Response resJoinAgain = request("PUT", "games/" + gameId, null);
		assertEquals(410, resJoinAgain.httpStatus);

		// Join invalid game
		Response resJoinInvalid = request("PUT", "games/4", null);
		assertEquals(404, resJoinInvalid.httpStatus);

		// verify game state
		assertEquals("TURN_HOUND", this.getState(gameId));

		// Verify game board positions
		Board board = new Board();
		assertEquals(board.toString(), this.getBoard(gameId));

		/**
		 * Test playing a game
		 */

		// Wrong gameId
		assertEquals(404, turn(129, playerHound, 0, 1, 2, 1));

		// Wrong playerId
		assertEquals(404, turn(gameId, "foobar", 0, 1, 2, 1));

		// Hare moves first mistakenly.
		assertEquals(422, turn(gameId, playerHare, 4, 1, 3, 1));

		// Hound moves two steps.
		assertEquals(422, turn(gameId, playerHound, 0, 1, 2, 1));

		// Hound makes a correct move
		assertEquals(200, turn(gameId, playerHound, 0, 1, 1, 1));
		// check game state
		assertEquals("TURN_HARE", this.getState(gameId));
		// check board positions after the correct move
		board.setPieceTypeByXY(0, 1, "NULL");
		board.setPieceTypeByXY(1, 1, "HOUND");
		assertEquals(board.toString(), this.getBoard(gameId));

	}

	private String getState(int gameId) {
		Response res = request("GET", "games/" + gameId + "/state", null);
		return new JSONObject(res.content).getString("state");
	}

	private String getBoard(int gameId) {
		Response res = request("GET", "games/" + gameId + "/board", null);
		return res.content;
	}

	private int turn(int gameId, String playerId, int fromX, int fromY, int toX, int toY) {
		// Generate request body
		JSONObject obj = new JSONObject();
		obj.put("playerId", playerId);
		obj.put("fromX", fromX);
		obj.put("fromY", fromY);
		obj.put("toX", toX);
		obj.put("toY", toY);

		// Send request
		Response res = request("GET", "games/" + gameId + "/turns", obj.toString());
		return res.httpStatus;

	}
	// ------------------------------------------------------------------------//
	// Generic Helper Methods and classes
	// ------------------------------------------------------------------------//

	private Response request(String method, String path, String contentAsJson) {
		try {
			String API_CONTEXT = "/hareandhounds/api/";
			URL url = new URL("http", Bootstrap.IP_ADDRESS, Bootstrap.PORT, API_CONTEXT + path);
			System.out.println(method + " | " + url);
			HttpURLConnection http = (HttpURLConnection) url.openConnection();
			http.setRequestMethod(method);
			http.setDoInput(true);
			if (contentAsJson != null) {
				http.setDoOutput(true);
				http.setRequestProperty("Content-Type", "application/json");
				OutputStreamWriter output = new OutputStreamWriter(http.getOutputStream());
				output.write(contentAsJson);
				output.flush();
				output.close();
			}
			String responseBody;
			try {
				responseBody = IOUtils.toString(http.getInputStream());
			} catch (Exception ex) {
				responseBody = "";
			}
			System.out.println(http.getResponseCode() + " | " + responseBody + "\n");
			return new Response(http.getResponseCode(), responseBody);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Sending request failed: " + e.getMessage());
			return null;
		}
	}

	private static class Response {

		public String content;
		public int httpStatus;

		public Response(int httpStatus, String content) {
			this.content = content;
			this.httpStatus = httpStatus;
		}

		public <T> T getContentAsObject(Type type) {
			return new Gson().fromJson(content, type);
		}
	}

}
