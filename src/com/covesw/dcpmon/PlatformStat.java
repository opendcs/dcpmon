/*
*  $Id: PlatformStat.java,v 1.4 2017/01/02 21:18:44 mmaloney Exp $
*
*  $Log: PlatformStat.java,v $
*  Revision 1.4  2017/01/02 21:18:44  mmaloney
*  Check in old stuff.
*
*  Revision 1.3  2015/02/06 18:24:31  mmaloney
*  Misc Improvements for RC3
*
*  Revision 1.2  2014/11/19 16:03:46  mmaloney
*  dev
*
*  Revision 1.1  2014/11/12 16:18:12  mmaloney
*  Initial Checkin.
*
*  Revision 1.1  2014/06/02 14:28:50  mmaloney
*  rc5 includes initial refactory for dcpmon
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.4  2008/11/11 00:49:19  mjmaloney
*  Bug fixes.
*
*  Revision 1.3  2008/09/18 00:52:46  mjmaloney
*  dev
*
*  Revision 1.2  2008/09/08 19:14:02  mjmaloney
*  LRGS 7 dev
*
*  Revision 1.1  2008/04/04 18:21:01  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2007/12/04 14:28:35  mmaloney
*  added code to download channels from url
*
*  Revision 1.5  2007/06/26 14:21:34  mmaloney
*  Added a new field Agency, it comes from Pdt entry files
*
*  Revision 1.4  2006/12/23 18:15:51  mmaloney
*  DCP Monitor development
*
*  Revision 1.3  2004/10/21 13:39:20  mjmaloney
*  javadoc improvements.
*
*  Revision 1.2  2004/03/18 16:18:44  mjmaloney
*  Working server version beta 01
*
*  Revision 1.1  2004/02/29 20:48:25  mjmaloney
*  Alpha version of server complete.
*
*/
package com.covesw.dcpmon;

import java.util.Date;

import decodes.dcpmon.XmitMediumType;
import lrgs.common.DcpAddress;

/**
This class holds one line's worth of data on the USACE Message Status
report. This is one days-worth of message specs for a particular DCP.
*/
public class PlatformStat
{
	/** The DCP Address */
	private DcpAddress dcpAddress;

	/** The DCP Name */
	private String dcpName;
	
	/** the DCP Description */
	private String dcpDescription = "";

	/** Agency name from PDT */
	private String agency;
	
	/** The second of the day when the first transmit window starts. */
	private int firstMsgSecOfDay;

	/** GOES channel for this message */
	private int goesChannel;

	/** 24 Strings, containing the codes for each hour. */
	private String fcodes[];

//	/** The basin that this DCP belongs to. */
//	private String basin;
//
	boolean isMine;
	
	boolean isUnexpected = false;
	
	private int daynum = 0;
	
	private String firstXmit = "N/A";
	
	private int stChannel = -1;
	
	private String stInterval = "";
	
	private int xmitWindow = 0;
	
	private int baud = 0;
	private int numMessages = 0;
	
	private String designator = "";
	private XmitMediumType xmitMediumType = XmitMediumType.GOES;
	
	public PlatformStat(int daynum)
	{
		this.daynum = daynum;
		setDcpAddress(null);
		setDcpName("");
		setAgency("");
		setFirstMsgSecOfDay(-1);
		setGoesChannel(-1);
		fcodes = new String[24];
		for(int i=0; i<24; i++)
			fcodes[i] = ".";
//		basin = "-";
		isMine = false;
	}

	/**
	  Sets the failure code for a particular hour.
	  @param hour 0...23
	  @param codes String containing one or more failure codes.
	*/
	public void setCodes(int hour, String codes)
	{
		hour %= 24; // just in case.
		
		// If first call, get rid of the '.' placeholder.
		if (fcodes[hour] == ".")
			fcodes[hour] = "";
		for(char c : codes.toCharArray())
		{
			if (c == 'G')
				c = '_';
			if (fcodes[hour].indexOf(c) == -1)
				fcodes[hour] = fcodes[hour] + c;
		}
	}
	
	public String[] getCodes()
	{
		return fcodes;
	}

	DcpAddress getDcpAddress()
	{
		return dcpAddress;
	}
	
	public String getMediumId()
	{
		return dcpAddress.toString();
	}

	void setDcpAddress(DcpAddress dcpAddress)
	{
		this.dcpAddress = dcpAddress;
	}

	public String getDcpName()
	{
		return dcpName;
	}

	void setDcpName(String dcpName)
	{
		this.dcpName = dcpName;
	}

	public String getAgency()
	{
		return agency;
	}

	void setAgency(String agency)
	{
		this.agency = agency;
	}

	int getFirstMsgSecOfDay()
	{
		return firstMsgSecOfDay;
	}

	void setFirstMsgSecOfDay(int firstMsgSecOfDay)
	{
		this.firstMsgSecOfDay = firstMsgSecOfDay;
		if (firstMsgSecOfDay < 0)
			return;
		firstXmit = sec2HMS(firstMsgSecOfDay);
	}
	
	private static String sec2HMS(int x)
	{
		StringBuilder sb = new StringBuilder();
		sb.append((char)((int)'0' + x/36000));
		x %= 36000;
		sb.append((char)((int)'0' + x/3600));
		x %= 3600;
		sb.append(':');
		sb.append((char)((int)'0' + x/600));
		x %= 600;
		sb.append((char)((int)'0' + x/60));
		sb.append(':');
		x %= 60;
		sb.append((char)((int)'0' + x/10));
		x %= 10;
		sb.append((char)((int)'0' + x));
		return sb.toString();
	}

	int getGoesChannel()
	{
		return goesChannel;
	}

	void setGoesChannel(int goesChannel)
	{
		this.goesChannel = goesChannel;
	}

	int getDaynum()
	{
		return daynum;
	}

	public String getFirstXmit()
	{
		return firstXmit;
	}

	public int getStChannel()
	{
		return stChannel;
	}

	public void setStChannel(int stChannel)
	{
		this.stChannel = stChannel;
	}

	public void setStIntervalSec(int stInterval)
	{
		this.stInterval = sec2HMS(stInterval);
	}

	public String getStInterval()
	{
		return stInterval;
	}

	public int getXmitWindow()
	{
		return xmitWindow;
	}

	public void setXmitWindow(int xmitWindow)
	{
		this.xmitWindow = xmitWindow;
	}

	public int getBaud()
	{
		return baud;
	}

	public void setBaud(int baud)
	{
		this.baud = baud;
	}
	
	public String getDateStr()
	{
		return DayUtil.daynumToDateStr(daynum);
	}

	public String getDcpDescription()
	{
		return dcpDescription;
	}

	public void setDcpDescription(String dcpDescription)
	{
		this.dcpDescription = dcpDescription;
	}

	public int getNumMessages()
	{
		return numMessages;
	}

	public void setNumMessages(int numMessages)
	{
		this.numMessages = numMessages;
	}

	public String getDesignator()
	{
		return designator;
	}

	public void setDesignator(String designator)
	{
		this.designator = designator;
	}

	public XmitMediumType getXmitMediumType()
	{
		return xmitMediumType;
	}

	public void setXmitMediumType(XmitMediumType xmitMediumType)
	{
		this.xmitMediumType = xmitMediumType;
	}

}
