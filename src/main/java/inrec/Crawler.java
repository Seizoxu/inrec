package inrec;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.json.JSONArray;
import org.json.JSONObject;

public class Crawler
{
	/*private static final String FP_BEATMAP_IDS_RANKED = "data/beatmapIds-ranked.txt";
	private static final String FP_BEATMAP_IDS_LOVED = "data/beatmapIds-loved.txt";
	private static final String FP_BEATMAP_IDS_GRAVEYARDED = "data/beatmapIds-graveyarded.txt";
	private static final String FP_BEATMAP_IDS_OTHER = "data/beatmapIds-other.txt";*/
	
	
	/**
	 * Updates the beatmap ID lists with the latest maps via crawling.
	 */
	/*public static void getAndSortBeatmaps()
	{
		List<String> filePaths = Arrays.asList(
                FP_BEATMAP_IDS_RANKED,
                FP_BEATMAP_IDS_LOVED,
                FP_BEATMAP_IDS_GRAVEYARDED,
                FP_BEATMAP_IDS_OTHER
        );

		// Find max ID out of all files.
        int nextId = 1;
        for (String filePath : filePaths)
        {
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    try
                    {
                        int id = Integer.parseInt(line.trim());
                        nextId = Math.max(nextId, id);
                    }
                    catch (NumberFormatException e) {System.err.println("Skipping invalid line in file: " + filePath);}
                }
            }
            catch (IOException e) {e.printStackTrace();}
        }
		
		try (
				BufferedWriter rankedWriter= new BufferedWriter(new FileWriter(FP_BEATMAP_IDS_RANKED, true));
				BufferedWriter lovedWriter = new BufferedWriter(new FileWriter(FP_BEATMAP_IDS_LOVED, true));
				BufferedWriter graveyardedWriter = new BufferedWriter(new FileWriter(FP_BEATMAP_IDS_GRAVEYARDED, true));
				BufferedWriter otherWriter = new BufferedWriter(new FileWriter(FP_BEATMAP_IDS_OTHER, true));
			)
		{
			///////////////////
		}
		catch (IOException e) {e.printStackTrace();}
	}*/
	
	
	/**
	 * Crawls for user scores updates, and updates scores.
	 */
	public static void updateSheets(OsuApiHandler osuApi, GSheetsApiHandler sheetsApi) throws IOException, GeneralSecurityException
	{
		// Make sure api_updates sheet is empty.
		int currentUpdateCount = Integer.parseInt(sheetsApi.getValuesFromRange("api!C5").getValues().get(0).get(0).toString());
		if (currentUpdateCount > 0)
		{
			updateModSheets(sheetsApi);
		}
		

		// Update top 500 players.
		JSONArray topPlayers = getAndUpdateTopPlayers(osuApi, sheetsApi, "IN", 1);

		

		// Retrieve scores from top 500.
		
		
		// Crawl for user updates.
		
		
		// Update sheets from api_updates
		
	}
	
	
	private static void updateModSheets(GSheetsApiHandler sheetsApi)
	{
		
	}
	
	
	/**
	 * Returns top x players from a certain country.
	 * @param osuApi
	 * @param country
	 * @param pages (50 players per page)
	 * @return JSONArray of player data.
	 * @throws IOException
	 */
	private static JSONArray getAndUpdateTopPlayers(OsuApiHandler osuApi, GSheetsApiHandler sheetsApi, String country, int pages)
	{
		// Get top players.
		JSONArray rankings = new JSONArray();
		for (int i = 0; i < pages; i++)
		{
			JSONArray rankingPage = osuApi.getUsersByRanking("IN", i+1).getJSONArray("ranking");
			rankings.putAll(rankingPage);
		}
		System.out.println("[LOAD] Retrieved top " + 50*pages + " Indian players.");
		
		// Filter top ranks to display relevant info.
		JSONArray rankingsFiltered = new JSONArray();
		for (int i = 0; i < rankings.length(); i++)
		{
			JSONObject currentPlayer = rankings.getJSONObject(i);

			rankingsFiltered.put(
					new JSONObject()
					.put("username", currentPlayer.getJSONObject("user").getString("username"))
					.put("id", currentPlayer.getJSONObject("user").getInt("id"))
					.put("global_rank", currentPlayer.getInt("global_rank"))
					.put("pp", currentPlayer.getDouble("pp"))
					.put("country_rank", i+1)
					.put("hit_accuracy", currentPlayer.getDouble("hit_accuracy"))
					);
		}
		
		// Update api_players.
		
		
		return rankingsFiltered;
	}
	
	
	/**
	 * Crawls scores from user tops and recents.
	 * @return Number of scores crawled.
	 */
	private static int crawlScores()
	{
		return 0;
	}
}
