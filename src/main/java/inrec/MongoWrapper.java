package inrec;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.slf4j.LoggerFactory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
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
	
	private static DateTimeFormatter legacyDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

		players.createIndex(Indexes.ascending("country_rank"));
		beatmaps.createIndex(Indexes.descending("beatmapset_id"));
		beatmaps.createIndex(new Document("beatmap_id", 1), new IndexOptions().unique(true));
		scores.createIndex(new Document("id", 1), new IndexOptions().unique(true));
	}
	
	
	/** Cleanly closes the client.*/
	public static void shutdown()
	{
		client.close();
	}
	
	
	/**
	 * Sorts and retrieves the latest date a beatmap was ranked.
	 * @return
	 */
	public static String getLatestBeatmapDate()
	{
		Document latestBeatmap = beatmaps.aggregate(Arrays.asList(
				Aggregates.sort(Sorts.descending("ranked_date")),
				Aggregates.limit(1)
				)).first();
		
		String latestDate = "2007-01-01";
		if (latestBeatmap != null)
		{
			String rankedDate = latestBeatmap.getString("approved_date");
			if (rankedDate != null && !rankedDate.isEmpty())
			{
				latestDate = LocalDateTime.parse(rankedDate, legacyDateFormatter)
						.toLocalDate()
						.format(dateFormatter);
			}
		}
		
		return latestDate;
	}

	
	/**
	 * Updates the top 500 players collection.
	 * @param playerList
	 */
	public static void updatePlayers(List<Document> playerList)
	{
		players.deleteMany(new Document());
		players.insertMany(playerList);
	}
	
	
	/**
	 * Updates the beatmaps collection.
	 * @param mapsList
	 */
	public static void updateBeatmaps(List<Document> mapsList)
	{
		Object mapId;
		List<WriteModel<Document>> bulkUpdate = new ArrayList<>();
		for (Document currentDoc : mapsList)
		{
			mapId = currentDoc.get("beatmap_id");
			bulkUpdate.add(new UpdateOneModel<>(
					Filters.eq("beatmap_id", mapId),
					new Document("$set", currentDoc),
					upsertTrue
					));
		}

		if (bulkUpdate.isEmpty()) {return;}
		beatmaps.bulkWrite(bulkUpdate, new BulkWriteOptions().ordered(false));
	}
	
	
	/**
	 * Updates the scores collection.
	 * @param playsList
	 */
	public static void updateScores(List<Document> playsList)
	{
		Object scoreId;
		List<WriteModel<Document>> bulkUpdate = new ArrayList<>();
		for (Document currentPlayerTops : playsList)
		{
			scoreId = currentPlayerTops.get("id");
			bulkUpdate.add(new UpdateOneModel<>(
					Filters.eq("score_id", scoreId),
					new Document("$set", currentPlayerTops),
					upsertTrue
					));
		}
		
		if (bulkUpdate.isEmpty()) {return;}
		scores.bulkWrite(bulkUpdate, new BulkWriteOptions().ordered(false));
	}
}
