package inrec;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class RecordsMain
{
	public static void main(String[] args)
	{
		try
		{
			OsuApiHandler osuApi = new OsuApiHandler(args[0], args[1]);
			GSheetsApiHandler sheetsApi = new GSheetsApiHandler(args[2]);
			
			Crawler.updateSheets(osuApi, sheetsApi);
		}
		catch (IOException e) {System.out.println("[ERROR] - IOException."); e.printStackTrace();}
		catch (GeneralSecurityException e) {System.out.println("[ERROR] - Google Sheets Security Error."); e.printStackTrace();}
		catch (Exception e) {e.printStackTrace();}
	}
}
