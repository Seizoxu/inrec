package inrec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class RecordsMain
{
	private static final String FP_TOP500 = "data/topRankIds.txt";
	
	public static void main(String[] args)
	{
		try (BufferedReader rankIdReader = new BufferedReader(new FileReader(FP_TOP500));)
		{
			OsuApiHandler osuApi = new OsuApiHandler(args[0], args[1]);
			GSheetsApiHandler sheetsApi = new GSheetsApiHandler(args[2]);
			
			
			/*JSONArray ids = new JSONArray();
			if (System.currentTimeMillis() - Long.parseLong(rankIdReader.readLine()) > 86_400_000L)
			{
				ids = getTopRankingIds(osuApi, 10);
				
				new File(FP_TOP500).delete();
				new File(FP_TOP500).createNewFile();
				try (BufferedWriter rankIdWriter = new BufferedWriter(new FileWriter(FP_TOP500, true));)
				{
					rankIdWriter.write(System.currentTimeMillis()+"\n");
					for (int i = 0; i < ids.length(); i++)
					{
						rankIdWriter.append(ids.get(i)+"\n");
					}
				}
			}
			else
			{
				for (int i = 0; i < 500; i++)
				{
					ids.put(Integer.parseInt(rankIdReader.readLine()));
				}
				System.out.println("[LOAD] Read top 500 Indian players.");
			}*/
			
			
			String jsonStr = osuApi.requestData("beatmaps/lookup");
			System.out.println(jsonStr);
			
//			System.out.println(OsuApiHandler.getBeatmapsById(osuAccessToken, new int[] {1804553}));
		}
		catch (IOException | InterruptedException e) {System.out.println("[ERROR] - osu! Authentication Failed.");}
	}
	
	
	/**
	 * Retrieves top 500 Indian IDs. If it has been less than 24 hours since
	 * the last update, the IDs are retrieved from the local file.
	 * @param pages
	 * @return A JSON Array of the top 500 Indian IDs.
	 */
	public static JSONArray getTopRankingIds(OsuApiHandler osuApi, int pages)
	{
		JSONArray rankings = new JSONArray();
		JSONArray ids = new JSONArray();
		for (int i = 0; i < pages; i++)
		{
			JSONArray rankingPage = osuApi.getUsersByRanking("IN", i+1).getJSONArray("ranking");
			rankings.putAll(rankingPage);
		}
		for (int i = 0; i < rankings.length(); i++)
		{
			ids.put(
					rankings
					.getJSONObject(i)
					.getJSONObject("user")
					.get("id"));
		}
		
		System.out.println("[LOAD] Retrieved top " + 50*pages + " Indian players.");
		return ids;
	}
}
