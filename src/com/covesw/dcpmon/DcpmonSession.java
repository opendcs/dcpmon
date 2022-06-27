package com.covesw.dcpmon;

import ilex.util.Logger;
import ilex.var.TimedVariable;
import ilex.xml.XmlOutputStream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;

import lrgs.archive.XmitWindow;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import decodes.db.Constants;
import decodes.db.NetworkList;
import decodes.db.Platform;
import decodes.db.TransportMedium;
import decodes.dcpmon.DcpMonitorConfig;
import decodes.dcpmon.XmitMediumType;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;
import decodes.tsdb.BadConnectException;
import decodes.util.ResourceFactory;
import decodes.xml.PlatformParser;
import decodes.xml.XmlDbTags;

@ManagedBean
@SessionScoped
public class DcpmonSession
	implements Serializable
{
	private static final long serialVersionUID = 1L;
	private String module = "DcpmonSession";
	private String msgType = "GOESList";
	private String goesList = "";
	private String polledDcpList = "";
	private String iridiumList = "";
	private int goesChannel = 0;
	private int backlog = 0;
	private SimpleDateFormat fullDateTimeSDF = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
	private SimpleDateFormat dateSDF = new SimpleDateFormat("dd MMMMM yyyy");
	private SimpleDateFormat sampleTimeSDF = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	private TimeZone displayTZ = TimeZone.getTimeZone("UTC");
	private DayMessageStore daysData[] = null;
//	private DcpNameDescResolver dcpNameDescResolver = null;
	private String mediumId = null;
	private int dayNum = 0;
	private ArrayList<DcpMsg> dcpMessages = null;
	private PlatformStat platformStat = null;
	private DcpMsg selectedMsg = null;
	private DcpMsgDecoder dcpMsgDecoder = null;
	/** Each element holds a row of columns */
	private ArrayList<DataRow> decDataRow = new ArrayList<DataRow>();
	private DecodedMessage decMsg = null;
	private String mediumType = XmitMediumType.GOES.toString();
	
	private static final long MS_PER_DAY = 3600L * 24L * 1000L;
	private String dayStr = "";


	@ManagedProperty(value="#{dcpmonDBI}")
	private DcpmonDBI dcpmonDBI;

	public DcpmonSession()
	{
		System.out.println(module + " created");
		fullDateTimeSDF.setTimeZone(displayTZ);
		dateSDF.setTimeZone(displayTZ);
		sampleTimeSDF.setTimeZone(displayTZ);
		ResourceFactory.instance();
	}
	
	// getMessage and setMessage
	public String navigateToOriole()
		throws BadConnectException
	{
		if (msgType.equals("GOESList"))
			prepareForGoesListOriole();
		else if (msgType.equals("GOESChannel"))
			prepareforGoesChannelOriole();
		else
			prepareForNonGoesOriole();
		
		String ret = msgType + "-oriole";
		System.out.println("navigateToOriole returning '" + ret + "'");
		return ret;
	}
	
	/**
	 * Called from wazzup page to get the list of DCP messages for the selected platform.
	 * @return
	 */
	public ArrayList<DcpMsg> getDcpMessages() { return dcpMessages; }
	
	/**
	 * Called from wazzup page to get the stat object for the selected platform.
	 * @return
	 */
	public PlatformStat getPlatformStat() { return platformStat; }
	
	public void selectChanLink(ActionEvent evt)
	{
		goesChannel = (Integer)evt.getComponent().getAttributes().get("chan");
	}
	
	public String navigateList2Chan() 
		throws BadConnectException
	{
		msgType = "GOESChannel";
		return navigateToOriole();
	}
	
	
	private void prepareForGoesListOriole()
	{
System.out.println("prepareForGoesListOriole backlog=" + backlog);
		daysData = new DayMessageStore[backlog + 1];
		int daynum = DayUtil.getCurrentDay();
		NetworkList netlist = null;
		try { netlist = getSelectedNetworkList(); }
		catch(BadConnectException ex)
		{
			System.out.println("ERROR: Cannot get selected netlist: " + ex);
			ex.printStackTrace();
			netlist = null;
		}
		//TODO In the following I am iterating through the day caches.
		// I should move this to dcpmonDBI and make it synchronized to prevent
		// concurrent modifications
		// write new method dcpmonDBI.getOrioleDataByNetlist
		for(int idx = 0; idx < backlog + 1; idx++, daynum--)
		{
			daysData[idx] = new DayMessageStore(daynum);
			DayCache dayCache = dcpmonDBI.getDayCache(daynum);
			if (dayCache != null)
			{
				for(PlatformStat ps : dayCache.getPlatStats())
				{
					TransportMedium tm = new TransportMedium(null,
						ps.getXmitMediumType() == XmitMediumType.GOES ? Constants.medium_Goes :
						ps.getXmitMediumType() == XmitMediumType.IRIDIUM ? Constants.medium_IRIDIUM :
						Constants.medium_EDL, ps.getMediumId());
					if (netlist == null || netlist.contains(tm))
						daysData[idx].getPlatformStats().add(ps);
				}
			}
		}

		// First sort each day by GOES Channel, then DCP Address, then Time.
		// Then build the MsgStatRecord array for displaying the Oriole report.
		for(DayMessageStore dms : daysData)
			// Now sort the Platform Status array by channel/1stXmit/dcpAddr
			Collections.sort(dms.getPlatformStats(), 
				new Comparator<PlatformStat>()
				{
					public int compare(PlatformStat o1, PlatformStat o2)
					{
						int result = o1.getGoesChannel() - o2.getGoesChannel();
						if (result != 0)
							return result;
						result = o1.getFirstMsgSecOfDay() - o2.getFirstMsgSecOfDay();
						if (result != 0)
							return result;
						result = o1.getDcpAddress().toString().compareTo(
							o2.getDcpAddress().toString());
						if (result != 0)
							return result;
						return o1.getGoesChannel() - o2.getGoesChannel();
					}
				});
	}
	
	private void prepareforGoesChannelOriole()
	{
		System.out.println("prepareforGoesChannelOriole backlog=" + backlog + ", chan=" + goesChannel);
		daysData = new DayMessageStore[backlog + 1];
		int daynum = DayUtil.getCurrentDay();
		
		//TODO Likewise new method dcpmonDBI.getOrioleDataByChannel
		for(int idx = 0; idx < backlog + 1; idx++, daynum--)
		{
			daysData[idx] = new DayMessageStore(daynum);
			DayCache dayCache = dcpmonDBI.getDayCache(daynum);
			if (dayCache != null)
				for(PlatformStat ps : dayCache.getPlatStats())
					if (ps.getXmitMediumType() == XmitMediumType.GOES
					 && ps.getGoesChannel() == goesChannel)
						daysData[idx].getPlatformStats().add(ps);
		}
		
		// Plat Stats will be for a single channel. Sort by 1st xmit of day and then dcp addr.
		for(DayMessageStore dms : daysData)
			// Now sort the Platform Status array by channel/1stXmit/dcpAddr
			Collections.sort(dms.getPlatformStats(), 
				new Comparator<PlatformStat>()
				{
					public int compare(PlatformStat o1, PlatformStat o2)
					{
						int result = o1.getFirstMsgSecOfDay() - o2.getFirstMsgSecOfDay();
						if (result != 0)
							return result;
						return o1.getDcpAddress().toString().compareTo(
							o2.getDcpAddress().toString());
					}
				});
	}

	private void prepareForNonGoesOriole()
	{
		System.out.println("prepareForNonGoesOriole backlog=" + backlog);

		daysData = new DayMessageStore[backlog + 1];
		int daynum = DayUtil.getCurrentDay();
		//TODO likewise new method dcpmonDBI.getOrioleDataNonGoes
		for(int idx = 0; idx < backlog + 1; idx++, daynum--)
		{
			daysData[idx] = new DayMessageStore(daynum);
			DayCache dayCache = dcpmonDBI.getDayCache(daynum);
			if (dayCache != null)
				for(PlatformStat ps : dayCache.getPlatStats())
					if (ps.getXmitMediumType() == XmitMediumType.LOGGER)
						daysData[idx].getPlatformStats().add(ps);
		}

		// sort by 1st msg of day & medium ID.
		for(DayMessageStore dms : daysData)
		{
			Collections.sort(dms.getPlatformStats(), 
				new Comparator<PlatformStat>()
				{
					public int compare(PlatformStat o1, PlatformStat o2)
					{
						int result = o1.getFirstMsgSecOfDay() - o2.getFirstMsgSecOfDay();
						if (result != 0)
							return result;
						return o1.getDcpAddress().toString().compareTo(
							o2.getDcpAddress().toString());
					}
				});
		}
	}

	/**
	 * Creates the PlatformStat bean with the given parameters.
	 * @param mediumType
	 * @param mediumId
	 * @param dayNum
	 */
	private void makePlatformStat(XmitMediumType mediumType, DcpAddress addr, int dayNum, int chan)
	{
		platformStat = new PlatformStat(dayNum);
		platformStat.setGoesChannel(chan);
		platformStat.setDcpAddress(addr);

		PlatformNameInfo pni = dcpmonDBI.getNameResolver().getPlatformNameInfo(mediumType, addr.toString());

		platformStat.setDcpName(pni.nameDesc.first);
		platformStat.setDcpDescription(pni.nameDesc.second);

		if (pni.platform != null)
		{
			platformStat.setAgency(pni.platform.agency);
			platformStat.setDesignator(pni.platform.getPlatformDesignator());
		}
		
		if (pni.pdtEntry != null)
		{
			platformStat.setFirstMsgSecOfDay(pni.pdtEntry.st_first_xmit_sod);
			if ((platformStat.getAgency() == null
			 || platformStat.getAgency().trim().length() == 0)
			 && pni.pdtEntry.agency != null)
				platformStat.setAgency(pni.pdtEntry.agency);
			platformStat.setStChannel(pni.pdtEntry.st_channel);
			platformStat.setStIntervalSec(pni.pdtEntry.st_xmit_interval);
			platformStat.setBaud(pni.pdtEntry.baud);
			platformStat.setXmitWindow(pni.pdtEntry.st_xmit_window);
		}
	}

	
	private NetworkList getSelectedNetworkList() 
		throws BadConnectException
	{
		ArrayList<ListSpec> lists = 
			msgType.equals("GOESList") ? getGoesLists() :
			msgType.equals("PolledDCP") ? getPolledDcpLists() :
			msgType.equals("Iridium") ? getIridiumLists() : null;
		if (lists == null)
		{
			System.out.println("getSelectedNetworkList - no lists for msgType='" + msgType + "'");
			return null;
		}
		String listName = 
			msgType.equals("GOESList") ? goesList :
			msgType.equals("PolledDCP") ? polledDcpList :
			msgType.equals("Iridium") ? iridiumList : null;
		if (listName == null || listName.trim().length() == 0)
		{
			System.out.println("getSelectedNetworkList - no list name selected.");
			return null;
		}
			
		for(ListSpec listSpec : lists)
			if (listSpec.getListName().equals(listName))
				return listSpec.getNetworkList();
		System.out.println("getSelectedNetworkList - no list matching name '" + listName + "' "
			+ "msgType='" + msgType + "'");
		return null;
	}
	
	public String getCurrentDateTime()
	{
		return displayTZ.getID() + ": " + fullDateTimeSDF.format(new Date());
	}

	/** Return the currently selected message type */
	public String getMsgType()
	{
		System.out.println("getMsgType returning '" + msgType + "'");
		return msgType;
	}
	
	public void setMsgType(String msgType)
	{
		System.out.println("setMsgType setting to '" + msgType + "'");
		this.msgType = msgType;
	}
	
	public ArrayList<ListSpec> getGoesLists()
		throws BadConnectException
	{
		return dcpmonDBI.getGoesLists();
	}
	
	public void setGoesList(String goesList)
	{
		System.out.println("setGoesList setting to '" + goesList + "'");
		this.goesList = goesList;
	}

	public String getGoesList()
	{
		System.out.println("getGoesList returning '" + goesList + "'");
		return goesList;
	}

	public String getPolledDcpList()
	{
		System.out.println("getPolledDcpList returning '" + polledDcpList + "'");
		return polledDcpList;
	}

	public void setPolledDcpList(String polledDcpList)
	{
		System.out.println("setPolledDcpList(" + polledDcpList + ")");
		this.polledDcpList = polledDcpList;
	}

	public ArrayList<ListSpec> getPolledDcpLists()
		throws BadConnectException
	{
		return dcpmonDBI.getPolledDcpLists();
	}

	public String getIridiumList()
	{
		System.out.println("getIridiumList returning '" + iridiumList + "'");
		return iridiumList;
	}

	public void setIridiumList(String iridiumList)
	{
		System.out.println("setIridiumList(" + iridiumList + ")");
		this.iridiumList = iridiumList;
	}

	public ArrayList<ListSpec> getIridiumLists()
		throws BadConnectException
	{
		return dcpmonDBI.getIridiumLists();
	}

	public int getGoesChannel()
	{
		return goesChannel;
	}

	public void setGoesChannel(int goesChannel)
	{
		this.goesChannel = goesChannel;
System.out.println("goesChannel set to " + goesChannel);
	}


	public int getBacklog()
	{
		return backlog;
	}


	public void setBacklog(int backlog)
	{
		System.out.println("setBacklog(" + backlog + ")");
		this.backlog = backlog;
	}
	
	public int[] getBacklogSelections()
		throws BadConnectException
	{
		int[] ret = new int[dcpmonDBI.getNumDaysStorage()];
		for(int i=0; i<ret.length; i++)
			ret[i] = i;
		return ret;
	}

	public DcpmonDBI getDcpmonDBI()
	{
		return dcpmonDBI;
	}

	public void setDcpmonDBI(DcpmonDBI dcpmonDBI)
	{
		this.dcpmonDBI = dcpmonDBI;
	}

	public DayMessageStore[] getDaysData()
	{
		
		return daysData;
	}

	public void selectMsg(ActionEvent evt)
	{
		selectedMsg = (DcpMsg)evt.getComponent().getAttributes().get("msg");

		Logger.instance().debug1("Selected msg from " + selectedMsg.getDcpAddress().toString()
			+ " total length = " + selectedMsg.getMessageLength()
			+ ", length already read=" + selectedMsg.getData().length);
		if (selectedMsg.getMessageLength() > selectedMsg.getData().length)
			dcpmonDBI.fillMessage(selectedMsg);
	}
	
	public String navigateToMsgDetail()
	{
		if (dcpMsgDecoder == null)
			dcpMsgDecoder = new DcpMsgDecoder(dcpmonDBI);
		
		String basePage = 
			msgType.toLowerCase().contains("goes") ? "GOES-detail" :
			msgType.equalsIgnoreCase("iridium") ? "Iridium-detail" : "Polled-detail";

		// Attempt to decode the message
		decDataRow.clear();
		decMsg = dcpMsgDecoder.decodeData(selectedMsg, 
			msgType.toLowerCase().contains("goes") ? Constants.medium_Goes :
			msgType.equalsIgnoreCase("iridium") ? Constants.medium_IRIDIUM :
			Constants.medium_EDL);
		if (decMsg != null)
		{
			// Sort each time series and remove empty one.
			ArrayList<TimeSeries> tsa = decMsg.getTimeSeriesArray();
			for(Iterator<TimeSeries> tsit = tsa.iterator(); tsit.hasNext(); )
			{
				TimeSeries ts = tsit.next();
				if (ts.size() == 0)
					tsit.remove();
				else
					ts.sort();
			}

			// Now go through unique dates in order and construct the display rows.
			Date curDate = new Date(0L);
			while((curDate = findNextDate(tsa, curDate)) != null)
			{
				DataRow dataRow = new DataRow(sampleTimeSDF.format(curDate));
				for(TimeSeries ts : tsa)
				{
					int idx = sampleIndexByDate(ts, curDate);
					if (idx == -1)
						dataRow.addColumn("");
					else
						dataRow.addColumn(ts.formattedSampleAt(idx));
				}
				decDataRow.add(dataRow);
			}
			return basePage + "-decoded";
		}
		else
		{
			return basePage + "-raw";
		}
	}
	
	/** @return next date in any time series or null if we are at the end. */
	private Date findNextDate(ArrayList<TimeSeries> tsa, Date curDate)
	{
		Date ret = null;

	  nextTs:
		for(TimeSeries ts : tsa)
		{
			for(int i = 0; i<ts.size(); i++)
			{
				TimedVariable tv = ts.sampleAt(i);
				if (tv.getTime().after(curDate) 
				 && (ret == null || tv.getTime().before(ret)))
				{
					ret = tv.getTime();
					continue nextTs;
				}
			}
		}
		return ret;
	}
	
	private int sampleIndexByDate(TimeSeries ts, Date t)
	{
		for(int i = 0; i<ts.size(); i++)
		{
			TimedVariable tv = ts.sampleAt(i);
			if (tv.getTime().before(t))
				continue;
			else if (tv.getTime().equals(t))
				return i;
			else
				return -1;
		}
		return -1;
	}

	
	public ArrayList<TimeSeries> getTimeSeriesArray()
	{
		if (decMsg != null)
		{
			ArrayList<TimeSeries> ret = decMsg.getTimeSeriesArray();
System.out.println("getTimeSeriesArray returning " + ret.size() + " time series.");
for(TimeSeries ts : ret) 
System.out.println("ts: " + ts.getSensorName() + " - "
+ ts.getDataTypeCode() + " - " + ts.getUnits());
			return ret;
		}
		else return new ArrayList<TimeSeries>();
	}

	
	public DcpMsg getSelectedMsg()
	{
		return selectedMsg;
	}

	public ArrayList<DataRow> getDecDataRow()
	{
System.out.println("getDecDataRow there are " + decDataRow.size() + " rows.");
for(DataRow row : decDataRow)
{
 System.out.print("row: ");
 for(int i=0; i<row.getDataColumns().size(); i++) 
  System.out.print(" '" + row.getDataColumns().get(i) + "'");
 System.out.println("");
}
		return decDataRow;
	}
	
	public void downloadPlatformXml()
		throws IOException
	{
System.out.println("downloadPlatformXml");
	    FacesContext facesContext = FacesContext.getCurrentInstance();
	    ExternalContext externalContext = facesContext.getExternalContext();
	    HttpServletResponse response = (HttpServletResponse)externalContext.getResponse();

	    // Some JSF component library or some Filter might have set some headers in the buffer beforehand. We want to get rid of them, else it may collide.
	    response.reset();

	    // Check http://www.iana.org/assignments/media-types for all types. Use if necessary ServletContext#getMimeType() for auto-detection based on filename.
	    response.setContentType("application/xml"); 
	    
	    // The Save As popup magic is done here. You can give it any filename you want, 
	    // this only won't work in MSIE, it will use current request URL as filename instead.
	    response.setHeader("Content-disposition", "attachment; filename=\"name.xml\""); 

		Platform p = decMsg == null ? null : decMsg.getPlatform();
		if (p == null)
			throw new IOException("No platform or decoded message");
		
	    BufferedOutputStream output = null;
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    try
	    {
	        output = new BufferedOutputStream(response.getOutputStream());
	    	XmlOutputStream xos = new XmlOutputStream(output, XmlDbTags.Platform_el);
	    	xos.writeXmlHeader();
	    	PlatformParser pp = new PlatformParser(p);
	    	pp.writeXml(xos);
	    }
	    finally
	    {
	    	try { output.close(); } catch(IOException ex) {}
	    }

	    // Important! Else JSF will attempt to render the response which obviously will 
	    // fail since it's already written with a file and closed.
	    facesContext.responseComplete();
	}

	public String navigateToRtStat()
	{
		return dcpmonDBI.getDcpmonConfig().rtstatUrl;
	}
	
	public DcpMonitorConfig getConfig() { return dcpmonDBI.getDcpmonConfig(); }

	public void setDayNum(int dayNum)
	{
		this.dayNum = dayNum;
System.out.println("setDayNum(" + dayNum + ")");
	}

	public void setMediumId(String mediumId)
	{
		this.mediumId = mediumId;
System.out.println("setMediumId(" + mediumId + ")");
	}
	
	public void prepareForWazzup()
	{
System.out.println("prepareForWazzup dayNum=" + dayNum + ", mediumId='" + mediumId + "'");

		dayStr = DayUtil.daynumToDateStr(dayNum);

		dcpMessages = new ArrayList<DcpMsg>();
		dcpmonDBI.readXmitsByMediumId(dcpMessages, dayNum,
			mediumType.equalsIgnoreCase("logger") ? XmitMediumType.LOGGER :
			mediumType.equalsIgnoreCase("iridium") ? XmitMediumType.IRIDIUM :
			XmitMediumType.GOES, mediumId);
		
		Collections.sort(dcpMessages,
			new Comparator<DcpMsg>()
			{
				public int compare(DcpMsg o1, DcpMsg o2)
				{
					return o1.getXmitTime().compareTo(o2.getXmitTime());
				}
			});
		if (dcpMessages.size() > 0)
			makePlatformStat(
				mediumType.equalsIgnoreCase("logger") ? XmitMediumType.LOGGER :
				mediumType.equalsIgnoreCase("iridium") ? XmitMediumType.IRIDIUM :
				XmitMediumType.GOES,
				dcpMessages.get(0).getDcpAddress(), dayNum, dcpMessages.get(0).getGoesChannel());
	}

	public int getDayNum()
	{
		return dayNum;
	}

	public String getMediumId()
	{
		return mediumId;
	}
	
	public String getMsgStartStyle(DcpMsg msg)
	{
		DcpMonitorConfig cfg = dcpmonDBI.getDcpmonConfig();
		XmitWindow xmitWindow = msg.getXmitTimeWindow();
		if (xmitWindow == null)
			return "body";
		Date start = msg.getCarrierStart();
		if (start == null)
			start = msg.getXmitTime();
		int deltaSec = (int)((start.getTime() % MS_PER_DAY) / 1000L) - xmitWindow.thisWindowStart;
		if (deltaSec < cfg.redMsgTime)
			return "alarm";
		else if (deltaSec < cfg.yellowMsgTime)
			return "warning";
		return "body";
	}
	
	public String getMsgStopStyle(DcpMsg msg)
	{
		DcpMonitorConfig cfg = dcpmonDBI.getDcpmonConfig();
		XmitWindow xmitWindow = msg.getXmitTimeWindow();
		Date stop = msg.getCarrierStop();
		if (xmitWindow == null || stop == null)
			return "body";
		int deltaSec = (xmitWindow.thisWindowStart + xmitWindow.windowLengthSec)
			- (int)((stop.getTime() % MS_PER_DAY) / 1000L);
		
		if (deltaSec < cfg.redMsgTime)
			return "alarm";
		else if (deltaSec < cfg.yellowMsgTime)
			return "warning";
		return "body";
	}
	
	public String getFailureCodeStyle(DcpMsg msg)
	{
		DcpMonitorConfig cfg = dcpmonDBI.getDcpmonConfig();
		String codes = msg.getXmitFailureCodes();
		String ret = "body";
		for(int i = 0; i<codes.length(); i++)
		{
			char c = codes.charAt(i);
			if (cfg.omitFailureCodes != null && cfg.omitFailureCodes.indexOf(c) >= 0)
				continue;
			else if (cfg.redFailureCodes != null && cfg.redFailureCodes.indexOf(c) >= 0)
				return "alarm";
			else if (cfg.yellowFailureCodes != null && cfg.yellowFailureCodes.indexOf(c) >= 0)
				ret = "warning";
		}
		
		return ret;
	}

	public String getSignalStrengthStyle(DcpMsg msg)
	{
		DcpMonitorConfig cfg = dcpmonDBI.getDcpmonConfig();
		return msg.getSignalStrength() <= cfg.redSignalStrength ? "alarm" :
			msg.getSignalStrength() <= cfg.yellowSignalStrength ? "warning" : "body";
	}
	
	public String getFrequencyOffsetStyle(DcpMsg msg)
	{
		DcpMonitorConfig cfg = dcpmonDBI.getDcpmonConfig();
		int foAbs = msg.getFrequencyOffset();
		if (foAbs < 0) foAbs = -foAbs;
		return foAbs >= cfg.redFreqOffset ? "alarm" :
			foAbs >= cfg.yellowFreqOffset ? "warning" : "body";
	}
	
	public String getBattVoltStyle(DcpMsg msg)
	{
		DcpMonitorConfig cfg = dcpmonDBI.getDcpmonConfig();
		// If less than .01 assume we don't even have battery voltage.
		return msg.getBattVolt() < 0.01 ? "body" :
			msg.getBattVolt() <= cfg.redBattery ? "alarm" :
			msg.getBattVolt() <= cfg.yellowBattery ? "warning" : "body";
	}

	/** @return alarm if string contains the word error */
	public String getStatusStyle(String stat, Date lastError)
	{
		return stat.toLowerCase().contains("error") 
			&& lastError != null
			&& System.currentTimeMillis() - lastError.getTime() <
			dcpmonDBI.getDcpmonConfig().statusErrorThreshold*1000L
			? "alarm" : "body";
	}
	
	public String getRecentErrorStyle(Date errorTime)
	{
		if (errorTime == null)
			return "body";
		int ageSec = (int)((System.currentTimeMillis() - errorTime.getTime())/1000L);
		return ageSec < 24*3600 ? "alarm" :
			   ageSec < 72*3600 ? "warning" : "body";
	}

	public String getRecentErrorStyle(Date errorTime, Date msgTime)
	{
		if (errorTime == null)
			return "body";
		int ageSec = (int)((System.currentTimeMillis() - errorTime.getTime())/1000L);
		return (msgTime != null && msgTime.after(errorTime)) ? "warning" :
			ageSec < 24*3600 ? "alarm" :
			ageSec < 72*3600 ? "warning" : "body";
	}

	
	public DecodedMessage getDecMsg()
	{
		return decMsg;
	}

	public String getMediumType()
	{
		return mediumType;
	}

	public void setMediumType(String mediumType)
	{
		this.mediumType = mediumType;
	}

	public String getDayStr()
	{
		return dayStr;
	}
}
