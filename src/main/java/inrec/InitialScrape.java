package inrec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
	private static final String[] MOD_ORDER = {
			"EZ", "NF", "HT",
			"HR", "SD", "PF", "DT", "NC", "HD", "FL",
			"SO", "TD"
	};

	private static RateLimiter batchLimit = new RateLimiter(3, 10);
	private static RateLimiter unitLimit = new RateLimiter(60, 60);
	
	private static OsuApiHandler osuApi;
	private static GSheetsApiHandler sheetsApi;

	private static List<List<Object>> topPlayers;
	private static List<List<Object>> topPlays;

	public static void scrape(OsuApiHandler osuApiHandler, GSheetsApiHandler sheetsApiHandler)
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
		
		// if exists, update DB.
		
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
		JSONArray rankings = new JSONArray();
		while (currentPage <= NUM_PAGES)
		{
			if (batchLimit.allowRequest())
			{
				JSONArray rankingPage = osuApi.getUsersByRanking(COUNTRY, currentPage).getJSONArray("ranking");
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
		topPlayers = new ArrayList<List<Object>>(rankings.length());
		for (int i = 0; i < rankings.length(); i++)
		{
			JSONObject currentPlayer = rankings.getJSONObject(i);

			// For future reference, JSONObjects from org.json do not preserve order.
			List<Object> playerData = new ArrayList<>();
			playerData.addAll(List.of(
					i+1,														// [0] country_rank
					currentPlayer.getJSONObject("user").getInt("id"),			// [1] user_id
					currentPlayer.getJSONObject("user").getString("username"),	// [2] username
					currentPlayer.getInt("global_rank"),						// [3] global_rank
					currentPlayer.getDouble("pp"),								// [4] pp
					currentPlayer.getDouble("hit_accuracy")						// [5] hit_accuracy
					));
			
			topPlayers.add(playerData);
		}
	};
	
	
	private static Runnable requestTopPlays = () ->
	{
		List<Integer> playerIds = topPlayers.stream().map(x -> (Integer) x.get(1)).toList();
		topPlays = new ArrayList<List<Object>>(playerIds.size()*150);
		
		List<JSONObject> currentPlayerTopPlays;
		for (Integer userId : playerIds)
		{
			for (;;)
			{
				if (unitLimit.allowRequest())
				{
					currentPlayerTopPlays = osuApi.getPlaysById(userId, "best", 100)
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

				// Order mods
				String mods = "";
				for (String mod : MOD_ORDER)
				{
					if (play.getJSONArray("mods").toList().contains(mod)) {mods += mod;}
				}
				
				String artistTitleDiff = String.format("%s - %s [%s]",
						play.getJSONObject("beatmapset").get("artist"),
						play.getJSONObject("beatmapset").get("title"),
						play.getJSONObject("beatmap").get("version"));
				
				topPlays.add(List.of(
						userId,										// [0] User ID
						play.getJSONObject("beatmap").get("id"),	// [1] Map ID
						play.get("id"),								// [2] Score ID
						play.getJSONObject("user").get("username"),	// [3] Username
						artistTitleDiff,							// [4] Artist, Title, Diff
						play.get("pp"),								// [5] PP
						mods,										// [6] Mods
						play.get("created_at")						// [7] Timestamp
						));
			}
		}
	};
}
