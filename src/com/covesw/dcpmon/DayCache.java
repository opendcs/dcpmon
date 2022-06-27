package com.covesw.dcpmon;

import ilex.util.StringPair;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

import lrgs.common.DcpAddress;
import decodes.dcpmon.XmitMediumType;
import decodes.dcpmon.XmitRecSpec;

public class DayCache
{
	private ArrayList<PlatformStat> platStats = new ArrayList<PlatformStat>();
	private long lastRecId = -1;
	private int dayNum = -1;
	private Calendar cal = Calendar.getInstance();
	private DcpNameDescResolver nameDescRes = null;
	HashMap<PlatStatKey, PlatformStat> platHash = new HashMap<PlatStatKey, PlatformStat>();
	
	class PlatStatKey
	{
		XmitMediumType xmt;
		String mediumId;
		int chan;
		int _hashCode;
		public PlatStatKey(XmitMediumType xmt, String mediumId, int chan)
		{
			super();
			this.xmt = xmt;
			this.mediumId = mediumId;
			this.chan = chan;
			_hashCode = xmt.hashCode() + mediumId.hashCode();
			if (xmt == XmitMediumType.GOES)
				_hashCode += chan;
		}
		
		@Override
		public int hashCode() { return _hashCode; }
		
		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof PlatStatKey))
				return false;
			PlatStatKey rhs = (PlatStatKey)obj;
			return xmt == rhs.xmt 
				&& mediumId.equals(rhs.mediumId)
				&& (xmt != XmitMediumType.GOES || chan == rhs.chan);
		}
	}
	
	public DayCache(int dayNum, DcpNameDescResolver nameDescRes)
	{
		this.dayNum = dayNum;
		cal.setTimeZone(TimeZone.getTimeZone("UTC"));
		this.nameDescRes = nameDescRes;
	}
	
	public synchronized void add(XmitRecSpec xrs)
	{
		XmitMediumType xmitMediumType = 
			xrs.getMediumType() == 'G' ? XmitMediumType.GOES :
			xrs.getMediumType() == 'I' ? XmitMediumType.IRIDIUM : XmitMediumType.LOGGER;
		cal.setTime(xrs.getXmitTime());
		int hr = cal.get(Calendar.HOUR_OF_DAY);
		
		if (lastRecId == -1 || xrs.getRecordId() > lastRecId)
			lastRecId = xrs.getRecordId();
		
		PlatStatKey key = new PlatStatKey(xmitMediumType, xrs.getMediumId(), xrs.getGoesChannel());
		PlatformStat ps = platHash.get(key);
		if (ps != null)
		{
			ps.setCodes(hr, xrs.getFailureCodes());
			if (xmitMediumType != XmitMediumType.GOES)
			{
				int sod = cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE)*60
					+ cal.get(Calendar.SECOND);
				if (sod < ps.getFirstMsgSecOfDay())
					ps.setFirstMsgSecOfDay(sod);
			}
			ps.setNumMessages(ps.getNumMessages()+1);
			return;
		}
		// No match. Create new PlatformStat
		ps = new PlatformStat(DayUtil.msecToDay(xrs.getXmitTime().getTime()));
		String mediumId = xrs.getMediumId();
		ps.setDcpAddress(new DcpAddress(mediumId));
		ps.setCodes(hr, xrs.getFailureCodes());
		ps.setXmitMediumType(xmitMediumType);
		ps.setGoesChannel(xrs.getGoesChannel());
		PlatformNameInfo pni = nameDescRes.getPlatformNameInfo(xmitMediumType, xrs.getMediumId());
		StringPair sp = pni.nameDesc;
		ps.setDcpName(sp.first);
		ps.setDcpDescription(sp.second);
		ps.setNumMessages(1);
		if (xmitMediumType == XmitMediumType.GOES && pni.pdtEntry != null)
		{
			ps.setFirstMsgSecOfDay(pni.pdtEntry.st_first_xmit_sod);
			ps.setStChannel(pni.pdtEntry.st_channel);
			ps.setStIntervalSec(pni.pdtEntry.st_xmit_interval);
			ps.setXmitWindow(pni.pdtEntry.st_xmit_window);
			ps.setAgency(pni.pdtEntry.agency);
			ps.setBaud(pni.pdtEntry.baud);
		}
		
		if (pni.platform != null)
		{
			if (pni.platform.getAgency() != null && pni.platform.getAgency().length() > 0)
				ps.setAgency(pni.platform.getAgency());
			if (pni.platform.getPlatformDesignator() != null && pni.platform.getPlatformDesignator().length() > 0)
				ps.setDesignator(pni.platform.getPlatformDesignator());
			ps.isMine = true;
		}
		else
			ps.isMine = false;
		
		platStats.add(ps);
		platHash.put(key, ps);
	}

	public int getDayNum()
	{
		return dayNum;
	}

	public long getLastRecId()
	{
		return lastRecId;
	}

	public ArrayList<PlatformStat> getPlatStats()
	{
		return platStats;
	}
	
}
