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
	private String clientId;
	private String clientSecret;
//	private String osuTokenType = "Bearer"; // Token Type should always be bearer, anyways.
	private String osuAccessToken;
	private long osuTokenExpiryEpoch;
	
	public OsuApiHandler(String clientId, String clientSecret)
	{
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		
		try {updateOsuAuthentication();}
		catch (IOException | InterruptedException e) {e.printStackTrace();}
	}
	
	
	/**
	 * Refreshes the osu! API token.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void updateOsuAuthentication() throws IOException, InterruptedException
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
										clientId, clientSecret, "client_credentials", "public"
								)
						)
				)
				.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		
		JSONObject json = new JSONObject(response.body());
		osuAccessToken = json.get("access_token").toString();
		osuTokenExpiryEpoch = System.currentTimeMillis()/1000 + Long.parseLong(json.get("expires_in").toString());
	}
	
	
	public String requestData(String requestStr) throws IOException, InterruptedException
	{
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://osu.ppy.sh/api/v2/" + requestStr))
				.header("Authorization", "Bearer " + getAccessToken())
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
	public JSONObject getUsersByRanking(String country, int cursor)
	{
		try
		{
			String jsonStr = requestData(
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
	public JSONArray getBeatmapsById(int[] ids)
	{
		try
		{
			String idQueries = "?";
			for (int i : ids)
			{
				idQueries += "ids[]=" + i + "&";
			}
			
			String jsonStr = requestData("beatmaps" + idQueries);
			
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
	public JSONArray getScoresByBeatmap(int beatmapId, int userId)
	{
		try
		{
			String jsonStr = requestData("beatmaps/" + beatmapId + "/scores/users/" + userId + "/all");
			
			return new JSONObject(jsonStr).getJSONArray("scores");
		}
		catch (Exception e) {return null;}
	}
	
	
	/**
	 * osuAccessToken get method.
	 * @return An always-valid access token, at least for the
	 * next 30 minutes.
	 */
	public String getAccessToken()
	{
		try
		{
			// 1800 seconds = 30 minutes
			if (osuTokenExpiryEpoch > 1800) {return osuAccessToken;}
			
			updateOsuAuthentication();
			return osuAccessToken;
		}
		catch (IOException | InterruptedException e)
		{
			e.printStackTrace();
			return null;
		}
	}
}

