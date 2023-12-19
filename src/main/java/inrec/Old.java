package inrec;

import java.io.IOException;

import org.json.JSONObject;

public class Old
{
	public static void printScore(OsuApiHandler osuApi, String osuAccessToken, int beatmapID, int userID) throws IOException, InterruptedException
	{
		JSONObject responseJson = new JSONObject(osuApi.requestData(
				String.format("beatmaps/%d/scores/users/%d", beatmapID, userID)));
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
	
	// get gsheets thing and do this
	/*List<List<Object>> values = response.getValues();
	
	if (values == null || values.isEmpty())
	{
		System.out.println("No Data Found.");
	}
	else
	{
		for (List<Object> row : values) {System.out.println(row.get(0) + " || " + row.get(1));}
	}*/
}
