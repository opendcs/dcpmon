package com.covesw.dcpmon;

import java.util.ArrayList;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import decodes.db.PlatformStatus;
import decodes.polling.DacqEvent;

/**
 * Managed Bean for the Station Status Page
 */
@ManagedBean
@SessionScoped
public class StationStatusBean
{
	private ArrayList<PlatformStatus> stationStatusList = null;
	@ManagedProperty(value="#{dcpmonDBI}")
	private DcpmonDBI dcpmonDBI;
	private ArrayList<DacqEvent> evtList = new ArrayList<DacqEvent>();
	private PlatformStatus selectedStationStatus = null;

	public StationStatusBean()
	{
	}

	public void setDcpmonDBI(DcpmonDBI dcpmonDBI)
	{
		this.dcpmonDBI = dcpmonDBI;
	}

	public void refreshAll()
	{
		System.out.println("StationStatusBean.refreshAll()");
		stationStatusList = dcpmonDBI.getPlatformStatusList();
		if (selectedStationStatus != null)
		{
			for(PlatformStatus ps : stationStatusList)
				if (ps.getId().equals(selectedStationStatus.getId()))
				{
					selectedStationStatus = ps;
					break;
				}
		}
	}
	
	public ArrayList<DacqEvent> getEvtList()
	{
System.out.println("StationStatusBean.getEvtList returning " + evtList.size() + " events.");
		return evtList;
	}

	public PlatformStatus getSelectedStationStatus()
	{
		return selectedStationStatus;
	}

	public void setSelectedStationStatus(PlatformStatus stat)
	{
		// If not the same as the currently selected one.
		if (stat == null
		 || selectedStationStatus == null
		 || !stat.getId().equals(selectedStationStatus.getId()))
		{
			evtList.clear();
			selectedStationStatus = stat;
			if (selectedStationStatus != null)
				dcpmonDBI.fillPlatformEvents(selectedStationStatus.getId(), evtList);
		}
		selectedStationStatus = stat;
	}

	public ArrayList<PlatformStatus> getStationStatusList()
	{
		if (stationStatusList == null || stationStatusList.size() == 0)
			refreshAll();
		return stationStatusList;
	}
}
