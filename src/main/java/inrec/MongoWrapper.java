package inrec;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.slf4j.LoggerFactory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;


class CommandMonitor implements CommandListener {
	@Override
	public void commandFailed(final CommandFailedEvent event) {
		System.out.println(String.format("Failed execution of command '%s' cause: %s",
				event.getCommandName(),
				event.getThrowable()));
	}
}


public class MongoWrapper
{
	private static String DB_NAME = "inrec";
	private static MongoClient client;
	private static MongoCollection<Document> players;
	private static MongoCollection<Document> beatmaps;
	private static MongoCollection<Document> scores;
	
	private static Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	private static UpdateOptions upsertTrue = new UpdateOptions().upsert(true);

	/**
	 * Connects to the Mongo database.
	 * @param uri
	 */
	public static void connect(String uri)
	{
		root.setLevel(Level.INFO);

		MongoClientSettings settings = MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(uri))
				.addCommandListener(new CommandMonitor())
				.build();
		
		client = MongoClients.create(settings);

		players = client.getDatabase(DB_NAME).getCollection("players");
		beatmaps = client.getDatabase(DB_NAME).getCollection("beatmaps");
		scores = client.getDatabase(DB_NAME).getCollection("scores");

		scores.createIndex(new Document("score_id", 1), new IndexOptions().unique(true));
	}
	
	
	/** Cleanly closes the client.*/
	public static void shutdown()
	{
		client.close();
	}
	
	
	/**
	 * Updates the top 500 players collection.
	 * @param playerList
	 */
	public static void updatePlayers(List<List<Object>> playerList)
	{
		List<Document> playerDocs = new ArrayList<>();
		for (List<Object> currentPlayer : playerList)
		{
			playerDocs.add(new Document()
					.append("country_rank", (Integer) currentPlayer.get(0))
					.append("id", (Integer) currentPlayer.get(1))
					.append("username", (String) currentPlayer.get(2))
					.append("global_rank", (Integer) currentPlayer.get(3))
					.append("pp", (Double) currentPlayer.get(4))
					.append("hit_accuracy", (Double) currentPlayer.get(5))
					);
		}
		
		// Clear all documents and update.
		players.deleteMany(new Document());
		players.insertMany(playerDocs);
	}
	
	
	/**
	 * Updates the scores collection.
	 * @param playsList
	 */
	public static void updateScores(List<List<Object>> playsList)
	{
		Document currentDoc;
		List<WriteModel<Document>> bulkUpdate = new ArrayList<>();
		for (List<Object> currentPlayerTops : playsList)
		{
			currentDoc = new Document()
					.append("user_id", currentPlayerTops.get(0))
					.append("map_id", currentPlayerTops.get(1))
					.append("score_id", currentPlayerTops.get(2))
					.append("username", currentPlayerTops.get(3))
					.append("title", currentPlayerTops.get(4))
					.append("pp", currentPlayerTops.get(5))
					.append("mods", currentPlayerTops.get(6))
					.append("created_at", currentPlayerTops.get(7));
			
			Object scoreId = currentPlayerTops.get(2);
			bulkUpdate.add(new UpdateOneModel<>(
					Filters.eq("score_id", scoreId),
					new Document("$set", currentDoc),
					upsertTrue
					));
		}
		
		if (bulkUpdate.isEmpty()) {return;}
		scores.bulkWrite(bulkUpdate, new BulkWriteOptions().ordered(false));
	}
}
