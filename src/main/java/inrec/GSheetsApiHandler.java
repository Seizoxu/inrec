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
	private static Sheets sheetsService;
	private static final String GOOGLE_APPLICATION_NAME = "India PP Records";

	private static Credential authorise() throws IOException, GeneralSecurityException
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

	public static Sheets getSheetsService() throws IOException, GeneralSecurityException
	{
		Credential credential = authorise();
		
		return new Sheets.Builder(
				GoogleNetHttpTransport.newTrustedTransport(),
				JacksonFactory.getDefaultInstance(),
				credential)
				.setApplicationName(GOOGLE_APPLICATION_NAME)
				.build();
	}
	
	public static void readFromSheet() throws IOException, GeneralSecurityException
	{
		sheetsService = getSheetsService();
		String range = "'api'!A1:B2";
		
		ValueRange response =  sheetsService.spreadsheets().values()
				.get(RecordsMain.spreadsheetId, range)
				.execute();
		
		List<List<Object>> values = response.getValues();
		
		if (values == null || values.isEmpty())
		{
			System.out.println("No Data Found.");
		}
		else
		{
			for (List<Object> row : values) {System.out.println(row.get(0) + " || " + row.get(1));}
		}
	}

}
