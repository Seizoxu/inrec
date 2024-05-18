package inrec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.api.services.sheets.v4.model.ValueRange;

/*
 * Class created to reorganise what I know now and my old code, for my own sanity.
 */
public class InitialScrape
{
	public static void scrape(OsuApiHandler osuApi, GSheetsApiHandler sheetsApi)
	{
		// -#-#- PART 1 -#-#-
		// get top 500 IN
		
		// get top 100 plays and update DB
		
		
		// -#-#- PART 2 -#-#-
		// check DB for beatmaps.
		
		// if exists, update DB.
		
		// crawl osu for top 50 IN scores on each at 3req/10s
		// >c each user ID and update DB
		// remember to check for failures.
		
	}
	
	
	/**
	 * Returns top x players from a certain country.
	 * @param osuApi 	-	osu! API instance
	 * @param sheetsApi	- Google Sheets API instance
	 * @param country	- ISO 3166-1 country code
	 * @param pages		- int page (50 players per page)
	 * @return			JSONArray of player data.
	 * @throws IOException
	 */
	private static List<List<Object>> getAndUpdateTopPlayers(OsuApiHandler osuApi, GSheetsApiHandler sheetsApi,
			String country, int pages) throws IOException
	{
		// Get top players.
		JSONArray rankings = new JSONArray();
		for (int i = 0; i < pages; i++)
		{
			JSONArray rankingPage = osuApi.getUsersByRanking("IN", i+1).getJSONArray("ranking");
			rankings.putAll(rankingPage);
		}
		System.out.println("[LOG] Retrieved top " + 50*pages + " Indian players.");
		
		
		// Filter top ranks to display relevant info.
		List<List<Object>> rankingsFiltered = new ArrayList<List<Object>>();
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
			
			rankingsFiltered.add(playerData);
		}
		
		sheetsApi.editRanges(List.of(new ValueRange()
				.setRange("api_players!A2:F")
				.setValues(rankingsFiltered)));
		System.out.println("[LOG] Updated api_players.");
		
		return rankingsFiltered;
	}
}
