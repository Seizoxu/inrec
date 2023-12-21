package inrec;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.Credentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

public class GSheetsApiHandler
{
	private static final String GOOGLE_APPLICATION_NAME = "India PP Records";
	
	private String spreadsheetId;
	private Credential credential;			// Credential    is more specific, but more common.
	private Credentials serviceCredential;	// Credential*s* is more general-purpose.
	private Sheets sheetsService;
	
	
	public GSheetsApiHandler(String spreadsheetId)
	{
		try
		{
			this.spreadsheetId = spreadsheetId;
			this.credential = authoriseCredential();
			this.serviceCredential = authoriseServiceCredentials();
			this.sheetsService = getSheetsService();
		}
		catch (IOException | GeneralSecurityException e)
		{
			e.printStackTrace();
			this.credential = null;
		}
	}
	
	
	/**
	 * Authorises a new Google Sheets Credential (singular).
	 * @return
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	private Credential authoriseCredential() throws IOException, GeneralSecurityException
	{
		final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
		final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		
		InputStream in = RecordsMain.class.getResourceAsStream("/googleSheetsCredentials.json");
		if (in == null) {throw new FileNotFoundException("Resource not found: googleSheetsCredentials.json.json");}
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
				jsonFactory, new InputStreamReader(in));
		List<String> scopes = Arrays.asList(SheetsScopes.SPREADSHEETS);
		
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, jsonFactory, clientSecrets, scopes)
				.setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
				.setAccessType("offline")
				.build();
		
//		Credential credential = new AuthorizationCodeInstalledApp(
//				flow,
//				new LocalServerReceiver())
//				.authorize("user");
//		return credential;
		
		LocalServerReceiver receiver = new LocalServerReceiver.Builder()
				.setPort(8888)
				.build();
	    return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}
	
	
	private Credentials authoriseServiceCredentials() throws IOException, FileNotFoundException
	{
		InputStream in = GSheetsApiHandler.class.getResourceAsStream("/serviceAccountCredentials.json");
		if (in == null) {throw new FileNotFoundException("Resource not found: serviceAccountCredentials.json");}
		
		return GoogleCredentials.fromStream(in).createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
	}
	
	
	/**
	 * Returns a new Sheets Service from an authorised credential<u>s</u> (plural).
	 * @return
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	private Sheets getSheetsService() throws IOException, GeneralSecurityException
	{
		return new Sheets.Builder(
				new NetHttpTransport(),
		        GsonFactory.getDefaultInstance(),
		        new HttpCredentialsAdapter(serviceCredential))
		        .setApplicationName(GOOGLE_APPLICATION_NAME)
		        .build();
	}
	
	
	/* --------------------------------------------------------------------------------------------------
	 * --------------------------------------------------------------------------------------------------
	 * -------------------------------------------------------------------------------------------------- */
	
	
	/**
	 * Returns a ValueRange, given a certain GSheets range.
	 * @param range
	 * @return
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public ValueRange getValuesFromRange(String range) throws IOException, GeneralSecurityException
	{
		ValueRange response = sheetsService.spreadsheets()
				.values()
				.get(spreadsheetId, range)
				.execute();
		
		return response;
	}
	
	
	/**
	 * Edits a specified range.
	 * @param range
	 * @param values
	 * @return BatchUpdateValuesResponse
	 * @throws IOException
	 */
	public BatchUpdateValuesResponse editRange(String range, List<List<Object>> values) throws IOException
	{
		List<ValueRange> data = new ArrayList<>();
		data.add(new ValueRange().setRange(range).setValues(values));

		BatchUpdateValuesResponse result = null;
		try
		{
			// Updates the values in the specified range.
			BatchUpdateValuesRequest body = new BatchUpdateValuesRequest()
					.setValueInputOption("RAW") // or "USER_ENTERED"
					.setData(data);
			
			result = sheetsService
					.spreadsheets()
					.values()
					.batchUpdate(spreadsheetId, body)
					.execute();
		}
		catch (GoogleJsonResponseException e) {e.printStackTrace();}
		
		return result;
	}
}
