package com.covesw.dcpmon;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DayUtil
{
	public static final long MSEC_PER_DAY = (24L * 60L * 60L * 1000L);
	
	private static SimpleDateFormat dateSdf = new SimpleDateFormat("MM/dd/yyyy");
	static { dateSdf.setTimeZone(TimeZone.getTimeZone("UTC")); }
	

	public static int getCurrentDay()
	{
		return msecToDay(System.currentTimeMillis());
	}

	/** 
	  Convenience method to convert msec time value to day number. 
	  @param msec the msec value
	  @return day number (0 = Jan 1, 1970)
	*/
	public static int msecToDay(long msec)
	{
		return (int)(msec / MSEC_PER_DAY);
	}

	/** 
	  Convenience method to conver msec time value to second of day.
	  @param msec the Java time value.
	  @return second-of-day
	*/
	public static int msecToSecondOfDay(long msec)
	{
		return (int)((msec % MSEC_PER_DAY)/1000L);
	}
	
	public static Date daynumToDate(int daynum)
	{
		return new Date(daynum * MSEC_PER_DAY);
	}
	
	public static synchronized String daynumToDateStr(int daynum)
	{
		return dateSdf.format(daynumToDate(daynum));
	}

}
