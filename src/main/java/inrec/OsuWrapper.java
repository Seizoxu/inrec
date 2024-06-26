package inrec;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OsuWrapper
{
	private String clientId;
	private String clientSecret;
//	private String osuTokenType = "Bearer"; // Token Type should always be bearer, anyways.
	private String legacyToken;
	private String token;
	private long tokenExpiryEpoch;
	
	public OsuWrapper(String clientId, String clientSecret, String legacyToken)
	{
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.legacyToken = legacyToken;
		
		try {updateOsuAuthentication();}
		catch (IOException | InterruptedException e) {e.printStackTrace();}
	}
	
	
	/**
	 * Refreshes the osu! API token.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void updateOsuAuthentication() throws IOException, InterruptedException
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
		token = json.get("access_token").toString();
		tokenExpiryEpoch = System.currentTimeMillis()/1000 + Long.parseLong(json.get("expires_in").toString());
	}

	
	/**
	 * osuAccessToken get method.
	 * @return An always-valid access token, at least for the
	 * next 30 minutes.
	 */
	private String getAccessToken()
	{
		try
		{
			// 1800 seconds = 30 minutes
			if (tokenExpiryEpoch > 1800) {return token;}
			
			updateOsuAuthentication();
			return token;
		}
		catch (IOException | InterruptedException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * Generic method for requesting data from osu API v2.
	 * @param requestStr
	 * @return String formatted in JSON.
	 * @throws IOException
	 * @throws InterruptedException
	 */
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
	 * Generic method for requesting data from osu API v1.
	 * @param requestStr
	 * @return String formatted in JSON.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public String requestDataLegacy(String requestStr) throws IOException, InterruptedException
	{
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(String.format("https://osu.ppy.sh/api/%s&k=%s", requestStr, legacyToken)))
				.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		return response.body();
	}
	

	/* --------------------------------------------------------------------------------------------------
	 * --------------------------------------------------------------------------------------------------
	 * -------------------------------------------------------------------------------------------------- */
	

	/**
	 * STD PP rankings.
	 * @param country
	 * @return JSONObject of 50 users' data.
	 */
	public JSONObject getUsersByRanking(String country, int cursor)
	{
		try
		{
			String jsonStr = requestData(
					"rankings/osu/performance?mode=osu"
					+ "&country=" + country
					+ "&page=" + cursor);
			
			return new JSONObject(jsonStr);
		}
		catch (JSONException e) {return null;}
		catch (IOException | InterruptedException e) {e.printStackTrace(); return null;}
	}
	

	/**
	 * Gets all ranked/loved STD beatmaps since a certain date.
	 * @param sinceDate: A MySQL-formatted DATE String.
	 * @return A JSONArray of up to 500 JSON Beatmap Objects.
	 */
	public JSONArray getBeatmapsSinceDate(String sinceDate)
	{
		try
		{
			String jsonStr = requestDataLegacy(String.format(
					"get_beatmaps?since=%s&m=0",
					sinceDate
					));
			
			return new JSONArray(jsonStr);
		}
		catch (JSONException e) {return null;}
		catch (IOException | InterruptedException e) {e.printStackTrace(); return null;}
	}
	
	
	/**
	 * Returns a list of beatmaps given an int-array of IDs (maximum 50).
	 * @param ids
	 * @return A JSONArray of Beatmap Data.
	 */
	public JSONArray getBeatmapsByIds(int[] ids)
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
		catch (JSONException e) {return null;}
		catch (IOException | InterruptedException e) {e.printStackTrace(); return null;}
	}
	

	/**
	 * Gets all of a user's scores on a beatmap.
	 * @param beatmapId
	 * @param userId
	 * @return JSONArray
	 */
	public JSONArray getBeatmapScoresByUserId(int beatmapId, int userId)
	{
		try
		{
			String jsonStr = requestData("beatmaps/" + beatmapId + "/scores/users/" + userId + "/all");
			
			return new JSONObject(jsonStr).getJSONArray("scores");
		}
		catch (JSONException e) {return null;}
		catch (IOException | InterruptedException e) {e.printStackTrace(); return null;}
	}
	
	
	/**
	 * Returns 100 top plays of a specified user.
	 * @param userId
	 * @param scoreType: can be "best" "firsts" or "recent"
	 * @return JSONArray of top plays.
	 */
	public JSONArray getTopPlaysByUserId(int userId, String scoreType, int limit)
	{
		try
		{
			String jsonStr = requestData(String.format(
					"users/%s/scores/%s?mode=osu&limit=%d",
					userId, scoreType, limit));
			
			return new JSONArray(jsonStr);
		}
		catch (JSONException e) {return null;}
		catch (IOException | InterruptedException e) {e.printStackTrace(); return null;}
	}
}

