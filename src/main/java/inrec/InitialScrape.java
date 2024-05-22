package inrec;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

/*
 * Class created to reorganise what I know now and my old code, for my own sanity.
 */
public class InitialScrape
{
	private static final int NUM_PAGES = 10;
	private static final String COUNTRY = "IN";

	private static final List<String> PP_STATUSES = List.of("ranked", "approved");

	private static RateLimiter batchLimit = new RateLimiter(3, 10);
	private static RateLimiter unitLimit = new RateLimiter(60, 60);
	
	private static OsuWrapper osuApi;
	private static GSheetsWrapper sheetsApi;

	private static List<Document> topPlayers;
	private static List<Document> topPlays;
	private static List<Document> newBeatmaps;
	
	private static DateTimeFormatter legacyDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public static void scrape(OsuWrapper osuApiHandler, GSheetsWrapper sheetsApiHandler)
			throws IOException
	{
		osuApi = osuApiHandler;
		sheetsApi = sheetsApiHandler;

		// -#-#- PART 1 -#-#-
		// get top 500 IN
		Thread threadPlayers = new Thread(requestTopPlayers);
		threadPlayers.start();
		
		try {threadPlayers.join();}
		catch (InterruptedException e) {e.printStackTrace();}
		
		MongoWrapper.updatePlayers(topPlayers);
		Loggerer.logSuccess("INFO", "Updated player collection.");
		
		// get top 100 plays and update DB
		Thread threadTopPlays = new Thread(requestTopPlays);
		threadTopPlays.start();

		try {threadTopPlays.join();}
		catch (InterruptedException e) {e.printStackTrace();}
		
		MongoWrapper.updateScores(topPlays);
		Loggerer.logSuccess("INFO", "Updated score collection with player tops.");
		

		// -#-#- PART 2 -#-#-
		// check DB for beatmaps.
		Thread threadBeatmaps = new Thread(requestRankedBeatmaps);
		threadBeatmaps.start();

		try {threadBeatmaps.join();}
		catch (InterruptedException e) {e.printStackTrace();}
		
		MongoWrapper.updateBeatmaps(newBeatmaps);
		Loggerer.logSuccess("INFO", "Updated beatmap collection with latest beatmaps.");
		
		// crawl osu for top 50 IN scores on each at 3req/10s
		// >c each user ID and update DB
		// remember to check for failures.
		
		
		// Shut down
		batchLimit.shutdown();
		unitLimit.shutdown();
		MongoWrapper.shutdown();
	}
	
	
	private static Runnable requestTopPlayers = () ->
	{
		int currentPage = 1;
		JSONArray rankingPage;
		JSONArray rankings = new JSONArray();
		while (currentPage <= NUM_PAGES)
		{
			if (batchLimit.allowRequest())
			{
				rankingPage = osuApi.getUsersByRanking(COUNTRY, currentPage).getJSONArray("ranking");
				rankings.putAll(rankingPage);

				Loggerer.logInfo("REQUEST", String.format("Page (%d/%d) retrieved.", currentPage, NUM_PAGES));
				currentPage++;
			}
			else // Wait 200ms before retrying.
			{
				try {Thread.sleep(200);}
				catch (InterruptedException e) {e.printStackTrace();}
			}
		}
		Loggerer.logSuccess("INFO", String.format("Retrieved top %d players.", NUM_PAGES*50));

		// Filter top ranks to display relevant info.
		JSONObject currentPlayer;
		topPlayers = new ArrayList<Document>(rankings.length());
		for (int i = 0; i < rankings.length(); i++)
		{
			currentPlayer = rankings.getJSONObject(i);

			topPlayers.add(Document.parse(currentPlayer.toString()));
		}
	};
	
	
	private static Runnable requestTopPlays = () ->
	{
		List<Integer> playerIds = topPlayers.stream()
				.map(doc -> (doc.getEmbedded(List.of("user", "id"), Integer.class)))
				.toList();
		topPlays = new ArrayList<>(playerIds.size()*100);
		
		List<JSONObject> currentPlayerTopPlays;
		for (Integer userId : playerIds)
		{
			for (;;)
			{
				if (unitLimit.allowRequest())
				{
					currentPlayerTopPlays = osuApi.getTopPlaysByUserId(userId, "best", 100)
							.toList().stream()
							.map(x -> new JSONObject((Map<?, ?>) x))
							.toList();
	
					Loggerer.logInfo("REQUEST", String.format("Player %d retrieved.", userId));
					break;
				}
				else // Wait 200ms before retrying.
				{
					try {Thread.sleep(200);}
					catch (InterruptedException e) {e.printStackTrace();}
				}
			}

			for (JSONObject play : currentPlayerTopPlays)
			{
				if (!PP_STATUSES.contains(play.getJSONObject("beatmap").getString("status"))) {continue;}

				topPlays.add(Document.parse(play.toString()));
			}
		}
	};
	
	
	private static Runnable requestRankedBeatmaps = () ->
	{
		newBeatmaps = new ArrayList<>();
		String latestRankedMapDate = MongoWrapper.getLatestBeatmapDate();
		
		JSONArray newBeatmapsChunk = new JSONArray();
		JSONObject currentBeatmap;
		int mapStatus;
		Set<String> seenBeatmapIds = new HashSet<>();
		while(!latestRankedMapDate.isEmpty())
		{
			// Request
			if (batchLimit.allowRequest())
			{
				newBeatmapsChunk = osuApi.getBeatmapsSinceDate(latestRankedMapDate);
				if (newBeatmapsChunk == null) {break;}

				Loggerer.logInfo("REQUEST", String.format(
						"Retrieved %d beatmaps since %s.",
						newBeatmapsChunk.length(), latestRankedMapDate));
			}
			else // Wait 200ms before retrying.
			{
				try {Thread.sleep(200);}
				catch (InterruptedException e) {e.printStackTrace();}
				
				continue;
			}
			
			// Add to list
			String beatmapId;
			for (int i = 0; i < newBeatmapsChunk.length(); i++)
			{
				// If map is not ranked or approved, skip.
				currentBeatmap = newBeatmapsChunk.getJSONObject(i);
				mapStatus = currentBeatmap.getInt("approved");
				if (mapStatus != 1 && mapStatus != 2) {continue;}

				beatmapId = currentBeatmap.getString("beatmap_id");
				if (!seenBeatmapIds.contains(beatmapId))
				{
					seenBeatmapIds.add(beatmapId);
					newBeatmaps.add(Document.parse(currentBeatmap.toString()));
				}
			}
			
			// Update and check for termination
			String newLatestBeatmapDate = LocalDateTime.parse(
					newBeatmapsChunk.getJSONObject(newBeatmapsChunk.length() - 1).getString("approved_date"),
					legacyDateFormatter)
			.toLocalDate()
			.format(dateFormatter);
			
			if (latestRankedMapDate.equals(newLatestBeatmapDate)) {break;}
			latestRankedMapDate = newLatestBeatmapDate;
		}
		
	};
}
