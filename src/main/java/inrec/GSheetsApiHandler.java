package inrec;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

public class GSheetsApiHandler
{
	private static final String GOOGLE_APPLICATION_NAME = "India PP Records";
	
	private Sheets sheetsService;
	private String spreadsheetId;
	private Credential credential;
	
	
	public GSheetsApiHandler(String spreadsheetId)
	{
		try
		{
			this.spreadsheetId = spreadsheetId;
			this.credential = authorise();
			this.sheetsService = getSheetsService();
		}
		catch (IOException | GeneralSecurityException e)
		{
			e.printStackTrace();
			this.credential = null;
		}
	}
	
	
	/**
	 * Authorises a new Google Sheets Credential.
	 * @return
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	private Credential authorise() throws IOException, GeneralSecurityException
	{
		InputStream in = RecordsMain.class.getResourceAsStream("/credentials.json");
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
				JacksonFactory.getDefaultInstance(), new InputStreamReader(in));
		
		List<String> scopes = Arrays.asList(SheetsScopes.SPREADSHEETS);
		
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				GoogleNetHttpTransport.newTrustedTransport(),
				JacksonFactory.getDefaultInstance(),
				clientSecrets,
				scopes)
				.setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
				.setAccessType("offline")
				.build();
		
		Credential credential = new AuthorizationCodeInstalledApp(
				flow,
				new LocalServerReceiver())
				.authorize("user");
		
		return credential;
	}

	
	/**
	 * Returns a new Sheets Service from an authorised credential.
	 * @return
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public Sheets getSheetsService() throws IOException, GeneralSecurityException
	{
		return new Sheets.Builder(
				GoogleNetHttpTransport.newTrustedTransport(),
				JacksonFactory.getDefaultInstance(),
				credential)
				.setApplicationName(GOOGLE_APPLICATION_NAME)
				.build();
	}
	
	
	public ValueRange getValuesFromRange(String range) throws IOException, GeneralSecurityException
	{
		ValueRange response =  sheetsService.spreadsheets()
				.values()
				.get(spreadsheetId, range)
				.execute();
		
		return response;
	}
}
