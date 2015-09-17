//-------------------------------------------------------------------------------------------------------------//
// Code based on the ToDoApp from OOSE class
// https://github.com/jhu-oose/todo
//-------------------------------------------------------------------------------------------------------------//

package com.oose2015.slin52.hareandhounds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

import static spark.Spark.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Bootstrap {

	public static final String IP_ADDRESS = "localhost";
	public static final int PORT = 8080;

	public static void main(String[] args) throws Exception {

		ipAddress(IP_ADDRESS);
		port(PORT);
		staticFileLocation("/public");

		try {
			GameService model = new GameService();
			new GameController(model);
		} catch (GameService.GameServiceException ex) {
			System.err.println("Failed to create a GameService instance. Aborting");
		}
	}
}
