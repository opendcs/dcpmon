package com.covesw.dcpmon;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import decodes.sql.DbKey;
import lrgs.common.DcpMsg;

public class DayMessageStore
{
	private SimpleDateFormat dateSDF = new SimpleDateFormat("dd MMMMM yyyy");
	private TimeZone displayTZ = TimeZone.getTimeZone("UTC");

	/** Start of day */
	private int dayNum;

//	/** Messages for this day */
//	private ArrayList<DcpMsg> dayMessages = new ArrayList<DcpMsg>();
	
	/** Oriole report dcpStatRecords */
	private ArrayList<PlatformStat> platformStats = new ArrayList<PlatformStat>();
	
	private DbKey lastRecNum = null;
	
	public DayMessageStore(int dayNum)
	{
		this.dayNum = dayNum;
		dateSDF.setTimeZone(displayTZ);
	}

	public int getDayNum()
	{
		return dayNum;
	}
	
	public String getDate()
	{
		return dateSDF.format(DayUtil.daynumToDate(dayNum));
	}

//	public ArrayList<DcpMsg> getDayMessages()
//	{
//		return dayMessages;
//	}
//
	public ArrayList<PlatformStat> getPlatformStats()
	{
		return platformStats;
	}
	
	public ArrayList<Integer> getDistinctChannels()
	{
		// Take advantage of the fact that platformStats are sorted by chan then platform
		ArrayList<Integer> ret = new ArrayList<Integer>();
		for(PlatformStat ps : platformStats)
			if (ret.size() == 0 || ps.getGoesChannel() != ret.get(ret.size()-1))
				ret.add(ps.getGoesChannel());
		return ret;
	}
	
	public ArrayList<PlatformStat> getPlatformStatsForChan(int chan)
	{
		ArrayList<PlatformStat> ret = new ArrayList<PlatformStat>();
		for(PlatformStat ps : platformStats)
			if (ps.getGoesChannel() == chan)
				ret.add(ps);
		return ret;
	}

	public DbKey getLastRecNum()
	{
		return lastRecNum;
	}

	public void setLastRecNum(DbKey lastRecNum)
	{
		this.lastRecNum = lastRecNum;
	}


}
