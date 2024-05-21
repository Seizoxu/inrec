package inrec;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class RateLimiter
{
	private final int maxRequestsPerInterval;
	private final int intervalSeconds;
	
	private final ScheduledExecutorService scheduler;
	private int requestCount;
	
	public RateLimiter(int maxRequestsPerInterval, int intervalSeconds)
	{
		this.maxRequestsPerInterval = maxRequestsPerInterval;
		this.intervalSeconds = intervalSeconds;
		
		this.scheduler = Executors.newScheduledThreadPool(1);
		this.requestCount = 0;
		scheduleReset();
	}
	

	private void scheduleReset()
	{
		scheduler.scheduleAtFixedRate(
				() -> {synchronized(this) {requestCount = 0;}},
				intervalSeconds,
				intervalSeconds,
				TimeUnit.SECONDS);
	}
	

	// This method is called to both check and increment the request counter.
	public synchronized boolean allowRequest()
	{
		if (requestCount < maxRequestsPerInterval)
		{
			requestCount++;
			return true;
		}
		
		return false;
	}
	
	
	public void shutdown()
	{
		scheduler.shutdown();
	}
}	