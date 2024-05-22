package inrec;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class RecordsMain
{
	public static void main(String[] args)
	{
		final String OSU_CLIENT_ID = args[0];
		final String OSU_CLIENT_SECRET = args[1];
		final String OSU_LEGACY_TOKEN = args[2];
		final String GSHEET_ID = args[3];
		final String MONGO_URI = args[4];
		
		try
		{
			OsuWrapper osuApi = new OsuWrapper(OSU_CLIENT_ID, OSU_CLIENT_SECRET, OSU_LEGACY_TOKEN);
			GSheetsWrapper sheetsApi = new GSheetsWrapper(GSHEET_ID);
			MongoWrapper.connect(MONGO_URI);
			
//			Crawler.updateSheets(osuApi, sheetsApi);
			InitialScrape.scrape(osuApi, sheetsApi);
		}
		catch (IOException e) {System.out.println("[ERROR] - IOException."); e.printStackTrace();}
//		catch (GeneralSecurityException e) {System.out.println("[ERROR] - Google Sheets Security Error."); e.printStackTrace();}
		catch (Exception e) {e.printStackTrace();}
	}
}
