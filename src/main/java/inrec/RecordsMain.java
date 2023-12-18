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
	private static String clientID;
	private static String clientSecret;
//	private static String osuTokenType = "Bearer"; // Token Type should always be bearer, anyways.
	private static String osuAccessToken;
	private static long osuTokenExpiryEpoch;
	public static String spreadsheetId;

	private static final String FP_TOP500 = "data/topRankIds.txt";
	private static final String FP_BEATMAP_IDS_RANKED = "data/beatmapIds-ranked.txt";
	private static final String FP_BEATMAP_IDS_LOVED = "data/beatmapIds-loved.txt";
	private static final String FP_BEATMAP_IDS_GRAVEYARDED = "data/beatmapIds-graveyarded.txt";
	private static final String FP_BEATMAP_IDS_OTHER = "data/beatmapIds-other.txt";
	
	public static void main(String[] args)
	{
		try (BufferedReader rankIdReader = new BufferedReader(new FileReader(FP_TOP500));)
		{
			clientID = args[0];
			clientSecret = args[1];
			spreadsheetId = args[2];
			
			updateOsuAuthentication();
			
//			JSONArray scores = OsuApiHandler.getScoresByBeatmap(osuAccessToken, 95382, 15846360);
//			System.out.println(scores.get(1));
//			printScore(95382, 15846360);
			
			/*JSONArray ids = new JSONArray();
			if (System.currentTimeMillis() - Long.parseLong(rankIdReader.readLine()) > 86_400_000L)
			{
				ids = getTopRankingIds(10);
				
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
			
			
			String jsonStr = OsuApiHandler.requestData(osuAccessToken, "beatmaps/lookup");
			System.out.println(jsonStr);
			
//			System.out.println(OsuApiHandler.getBeatmapsById(osuAccessToken, new int[] {1804553}));
		}
		catch (IOException | InterruptedException e) {System.out.println("[ERROR] - osu! Authentication Failed.");}
	}
	
	/**
	 * Refreshes the osu! API token.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void updateOsuAuthentication() throws IOException, InterruptedException
	{
		JSONObject json = new JSONObject(OsuApiHandler.authenticate(clientID, clientSecret));
		osuAccessToken = json.get("access_token").toString();
		osuTokenExpiryEpoch = System.currentTimeMillis()/1000 + Long.parseLong(json.get("expires_in").toString());
	}
	
	/**
	 * Retrieves top 500 Indian IDs. If it has been less than 24 hours since
	 * the last update, the IDs are retrieved from the local file.
	 * @param pages
	 * @return A JSON Array of the top 500 Indian IDs.
	 */
	public static JSONArray getTopRankingIds(int pages)
	{
		JSONArray rankings = new JSONArray();
		JSONArray ids = new JSONArray();
		for (int i = 0; i < pages; i++)
		{
			JSONArray rankingPage = OsuApiHandler.getUsersByRanking(osuAccessToken, "IN", i+1).getJSONArray("ranking");
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
	
	/**
	 * Updates the beatmap ID lists with the latest maps via crawling.
	 */
	public static void getAndSortBeatmaps()
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
	}
	
	public static void printScore(int beatmapID, int userID) throws IOException, InterruptedException
	{
		JSONObject responseJson = new JSONObject(OsuApiHandler.requestData(osuAccessToken, String.format("beatmaps/%d/scores/users/%d", beatmapID, userID)));
		JSONObject score = new JSONObject(responseJson.get("score").toString());
		JSONObject beatmapInfo = new JSONObject(score.get("beatmap").toString());
		JSONObject userInfo = new JSONObject(score.get("user").toString());
		
		int position = Integer.parseInt(responseJson.get("position").toString());
		double accuracy = Double.parseDouble(score.get("accuracy").toString())*100;
		String mods = score.get("mods").toString();
		double pp = Double.parseDouble(score.get("pp").toString());
		String rank = score.get("rank").toString();
		int scoreValue = Integer.parseInt(score.get("score").toString());
		
		double starRating = Double.parseDouble(beatmapInfo.get("difficulty_rating").toString());
		double approachRate = Double.parseDouble(beatmapInfo.get("ar").toString());
		double overallDifficulty = Double.parseDouble(beatmapInfo.get("accuracy").toString());
		double circleSize = Double.parseDouble(beatmapInfo.get("cs").toString());
		double hpDrain = Double.parseDouble(beatmapInfo.get("drain").toString());
		int length = Integer.parseInt(beatmapInfo.get("hit_length").toString());

		String username = userInfo.get("username").toString();
		
		System.out.println(String.format(
				"SCORE:%n"
				+ "Username: %s | Position: #%d%n"
				+ "Rank: %s | Mods: %s | Score: %d%n"
				+ "PP: %.2f | Accuracy: %.2f%%%n%n"
				
				+ "SR: %.2f* | Length: %d%n"
				+ "AR: %.1f // OD: %.1f // CS: %.1f // HP: %.1f",
				username, position, rank, mods, scoreValue, pp, accuracy,
				starRating, length, approachRate, overallDifficulty, circleSize, hpDrain
				));
	}
}
