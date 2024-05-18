package inrec;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class RecordsMain
{
	public static void main(String[] args)
	{
		final String OSU_CLIENT_ID = args[0];
		final String OSU_CLIENT_SECRET = args[1];
		final String GSHEET_ID = args[2];
		final String MONGO_URI = args[3];
		
		try
		{
			OsuApiHandler osuApi = new OsuApiHandler(OSU_CLIENT_ID, OSU_CLIENT_SECRET);
			GSheetsApiHandler sheetsApi = new GSheetsApiHandler(GSHEET_ID);
			MongoApiHandler.connect(MONGO_URI);
			
//			Crawler.updateSheets(osuApi, sheetsApi);
			InitialScrape.scrape(osuApi, sheetsApi);
		}
//		catch (IOException e) {System.out.println("[ERROR] - IOException."); e.printStackTrace();}
//		catch (GeneralSecurityException e) {System.out.println("[ERROR] - Google Sheets Security Error."); e.printStackTrace();}
		catch (Exception e) {e.printStackTrace();}
	}
}
