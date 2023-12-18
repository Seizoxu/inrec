package inrec;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONArray;
import org.json.JSONObject;

public class OsuApiHandler
{
	public static String authenticate(String clientID, String clientSecret) throws IOException, InterruptedException
	{
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://osu.ppy.sh/oauth/token"))
				.headers("Accept", "application/json", "Content-Type", "application/json")
				.POST
				(
						HttpRequest.BodyPublishers.ofString
						(
								String.format
								(
										"{\"client_id\": %s, "
										+ "\"client_secret\": \"%s\", "
										+ "\"grant_type\": \"%s\", "
										+ "\"scope\": \"%s\"}",
										clientID, clientSecret, "client_credentials", "public"
								)
						)
				)
				.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		return response.body();
	}
	
	
	
	public static String requestData(String token, String requestStr) throws IOException, InterruptedException
	{
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://osu.ppy.sh/api/v2/" + requestStr))
				.header("Authorization", "Bearer "+token)
				.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		return response.body();
	}
	
	
	/**
	 * STD PP rankings.
	 * @param token
	 * @param country
	 * @return
	 */
	public static JSONObject getUsersByRanking(String token, String country, int cursor)
	{
		try
		{
			String jsonStr = requestData(token,
					"rankings/osu/performance/?"
					+ "country=" + country
					+ "&page=" + cursor);
			
			return new JSONObject(jsonStr);
		}
		catch (Exception e) {return null;}
	}
	
	/**
	 * Returns a list of beatmaps given an int-array of IDs (maximum 50).
	 * @param token
	 * @param ids
	 * @return
	 */
	public static JSONArray getBeatmapsById(String token, int[] ids)
	{
		try
		{
			String idQueries = "?";
			for (int i : ids)
			{
				idQueries += "ids[]=" + i + "&";
			}
			
			String jsonStr = requestData(token,
					"beatmaps" + idQueries);
			
			return new JSONObject(jsonStr).getJSONArray("beatmaps");
		}
		catch (Exception e) {return null;}
	}
	
	/**
	 * Gets all of a player's scores on a beatmap.
	 * @param token
	 * @param beatmapId
	 * @param userId
	 * @return JSONArray
	 */
	public static JSONArray getScoresByBeatmap(String token, int beatmapId, int userId)
	{
		try
		{
			String jsonStr = requestData(token,
					"beatmaps/" + beatmapId + "/scores/users/" + userId + "/all");
			
			return new JSONObject(jsonStr).getJSONArray("scores");
		}
		catch (Exception e) {return null;}
	}
}

