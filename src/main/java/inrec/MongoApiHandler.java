package inrec;

import org.bson.Document;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;


class CommandMonitor implements CommandListener {
	@Override
	public void commandFailed(final CommandFailedEvent event) {
		System.out.println(String.format("Failed execution of command '%s' cause: %s",
				event.getCommandName(),
				event.getThrowable()));
	}
}


public class MongoApiHandler
{
	private static String DB_NAME;
	private static MongoClient client;
	private static MongoCollection<Document> users;
	private static MongoCollection<Document> beatmaps;
	private static MongoCollection<Document> scores;

	public static void connect(String uri)
	{
//		DB_NAME = ENVIRONMENT.equals("development") ? "ZyenyoStaging" : "MyDatabase";

		MongoClientSettings settings = MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(uri))
				.addCommandListener(new CommandMonitor()).build();

		client = MongoClients.create(settings);

		users = client.getDatabase(DB_NAME).getCollection("users");
		beatmaps = client.getDatabase(DB_NAME).getCollection("beatmaps");
		scores = client.getDatabase(DB_NAME).getCollection("scores");

//		scores.createIndex(Indexes.descending("SOMETHING?"));
	}
}
