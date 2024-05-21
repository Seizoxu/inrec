package inrec;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * "Loggerer" because "Logger" is already taken up by something else in this project. :-/
 */
public class Loggerer
{
	private static final String ANSI_RESET = "\u001B[0m";
	private static final String ANSI_RED = "\u001B[31m";
	private static final String ANSI_GREEN = "\u001B[32m";
	
	private static DateTimeFormatter timestampFormat = DateTimeFormatter.ofPattern("[HH:mm:ss]");
	

	/**
	 * Regular log.
	 * @param logTitle
	 * @param logBody
	 */
	public static void logInfo(String logTitle, String logBody)
	{
		System.out.printf("[%s] %s | %s%n",
				getCurrentTime(),
				logTitle,
				logBody);
	}
	
	
	/**
	 * Typically used at the end of a section, when a task terminated successfully.
	 * @param logTitle
	 * @param logBody
	 */
	public static void logSuccess(String logTitle, String logBody)
	{
		System.out.printf("%s[%s] %s | %s%s%n",
				ANSI_GREEN,
				getCurrentTime(),
				logTitle,
				logBody,
				ANSI_RESET);
	}
	
	
	private static String getCurrentTime()
	{
		return timestampFormat.format(LocalTime.now());
	}
}
