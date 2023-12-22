package inrec;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class Crawler
{
	private static final List<String> ALLOWED_STATUSES = List.of("ranked", "approved");
	private static final String[] MOD_ORDER = {
			"EZ", "NF", "HT",
			"HR", "SD", "PF", "DT", "NC", "HD", "FL",
			"SO", "TD"
	};
	public static final Map<String, String> MOD_REMAP = Map.of(
			"NF","",	"SO","",
			"SD","",	"PF","",
			"TD","",	"NC","DT"
	);
	private static final List<String> MOD_COMBINATIONS = List.of(
			"NM", "EZ", "HT", "HR", "DT", "HD", "FL", "EZHT", "EZDT", "EZHD", "EZFL", "HTHR", "HTHD", "HTFL", "HRDT",
			"HRHD", "HRFL", "DTHD", "DTFL", "HDFL", "EZHTHD", "EZHTFL", "EZDTHD", "EZDTFL", "EZHDFL", "HTHRHD", "HTHRFL",
			"HTHDFL", "HRDTHD", "HRDTFL", "HRHDFL", "DTHDFL", "EZHTHDFL", "EZDTHDFL", "HTHRHDFL", "HRDTHDFL"
	);
		
	
	/**
	 * Crawls for user scores updates, and updates scores.
	 * @throws InterruptedException 
	 */
	public static void updateSheets(OsuApiHandler osuApi, GSheetsApiHandler sheetsApi)
			throws IOException, GeneralSecurityException, InterruptedException
	{
		// Update top 500 players.
		List<List<Object>> topPlayers = getAndUpdateTopPlayers(osuApi, sheetsApi, "IN", 1);
		

		// Retrieve scores from top 500.
		List<List<Object>> plays = crawlScores(
				osuApi,
				topPlayers.stream().map(x -> (Integer) x.get(1)).toList());
		
		
		// Update sheets from crawled plays.
		updateModSheets(sheetsApi, plays);
	}
	
	
	//TODO: Optimise
	private static void updateModSheets(GSheetsApiHandler sheetsApi, List<List<Object>> plays)
			throws IOException, GeneralSecurityException
	{
		// Convert extra mods (MOD_REMAP) to normal.
		List<Object> remoddedMods = plays.stream()
				.map(row -> {
			if (row.size() >= 7)
				{return Crawler.remapValue(row.get(6));}
			else {return null;}
			}).collect(Collectors.toList());
		
		
		// Retrieve the score IDs that are already there.
		
		
		// Put 2D lists into a hashmap, key being all mod combos, value being 2d list of cells.
		Map<String, List<List<Object>>> sortedPlays = new LinkedHashMap<>();
		for (int i = 0; i < MOD_COMBINATIONS.size(); i++)
		{
			sortedPlays.put(MOD_COMBINATIONS.get(i), new ArrayList<List<Object>>(100));
		}
		sortedPlays.put("EXC", new ArrayList<List<Object>>(100));
		
		String mod;
		for (int i = 0; i < plays.size(); i++)
		{
			//TODO: check if scoreid exists in sheet.
			
			mod = remoddedMods.get(i).toString();
			if (MOD_COMBINATIONS.contains(mod))
			{
				sortedPlays.get(mod).add(plays.get(i));
			}
			else if (mod.equals(""))
			{
				sortedPlays.get("NM").add(plays.get(i));
			}
			else
			{
				sortedPlays.get("EXC").add(plays.get(i));
			}
		}
		

		// Send hashmap to gsheets tabs. Remember to check lengths.
		List<List<Object>> playCounts = sheetsApi.getValuesFromRange("api!B11:C47").getValues();
		Map<String, Integer> playCountsMap = new HashMap<>();
		for (List<Object> row : playCounts)
		{
			playCountsMap.put(
					row.get(0).toString(),
					Integer.parseInt(row.get(1).toString()));
		}
		
        for (Map.Entry<String, List<List<Object>>> entry : sortedPlays.entrySet())
		{
			sheetsApi.editRange(
					entry.getKey()+"!B"+(2+playCountsMap.get(entry.getKey()))+":I",
					entry.getValue());
		}
	}
	
	
	/**
	 * Remaps an extraneous mod to normal mods (NC to DT, NFSO to NM, etc).
	 * @param originalValue
	 * @return
	 */
	private static Object remapValue(Object originalValue)
	{
		String modString = originalValue.toString();
		List<String> modlist = new ArrayList<>(4);
		for (int i = 0; i < modString.length(); i += 2)
		{
			int endIndex = Math.min(i+2, modString.length());
			modlist.add(modString.substring(i, endIndex));
		}
		
		String remodString = "";
		for (String mod : modlist)
		{
			remodString += Crawler.MOD_REMAP.getOrDefault(mod, mod.toString());
		}
		
		return remodString;
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
		
		sheetsApi.editRange("api_players!A2:F", rankingsFiltered);
		System.out.println("[LOG] Updated api_players.");
		
		return rankingsFiltered;
	}
	
	
	/**
	 * Crawls scores from user tops and recents.
	 * @return Number of scores crawled.
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	private static List<List<Object>> crawlScores(OsuApiHandler osuApi, List<Integer> ids)
			throws IOException, InterruptedException
	{
		List<List<Object>> values = new ArrayList<List<Object>>(ids.size()*150);

		for (Integer userId : ids)
		{
			// API ratelimit (shouldn't matter too much, 500 ids = 9 mins.
			Thread.sleep(1000);
			
			List<JSONObject> plays = osuApi.getPlaysById(userId, "best", 5)
					.toList().stream()
					.map(x -> new JSONObject((Map<?, ?>) x))
					.toList();
			
			for (JSONObject play : plays)
			{
				if (!ALLOWED_STATUSES.contains(play.getJSONObject("beatmap").getString("status"))) {continue;}

				String mods = "";
				for (String mod : MOD_ORDER)
				{
					if (play.getJSONArray("mods").toList().contains(mod)) {mods += mod;}
				}
				
				String artistTitleDiff = String.format("%s - %s [%s]",
						play.getJSONObject("beatmapset").get("artist"),
						play.getJSONObject("beatmapset").get("title"),
						play.getJSONObject("beatmap").get("version"));
				
				values.add(List.of(
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
		
		System.out.println("[LOG] Crawled all scores.");
		return values;
	}
}
