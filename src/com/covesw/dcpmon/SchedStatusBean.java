package com.covesw.dcpmon;

import java.util.ArrayList;
import java.util.Iterator;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.event.ActionEvent;

import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.polling.DacqEvent;
import decodes.polling.DeviceStatus;

/**
 * Managed Bean for the Schedule Status Page
 */
@ManagedBean
@SessionScoped
public class SchedStatusBean
{
	private ArrayList<ScheduleEntry> schedEntryList = null;
	private ArrayList<ScheduleEntryStatus> schedEntryStatusList = null;
	@ManagedProperty(value="#{dcpmonDBI}")
	private DcpmonDBI dcpmonDBI;
	private ArrayList<DacqEvent> evtList = new ArrayList<DacqEvent>();
	private ScheduleEntry selectedSchedEntry = null;
	private ScheduleEntryStatus selectedStatus = null;

	public SchedStatusBean()
	{
	}

	public void setDcpmonDBI(DcpmonDBI dcpmonDBI)
	{
		this.dcpmonDBI = dcpmonDBI;
	}

	public ArrayList<ScheduleEntry> getScheduleEntryList()
	{
		if (schedEntryList == null)
		{
			schedEntryList = dcpmonDBI.getScheduleEntryList();
			for(Iterator<ScheduleEntry> seit = schedEntryList.iterator(); seit.hasNext(); )
			{
				ScheduleEntry se = seit.next();
				if (se.getName().endsWith("-manual") || !se.isEnabled())
					seit.remove();
			}
		}
		return schedEntryList;
	}
	
	public void refreshAll()
	{
		System.out.println("Refreshing all lists");
		schedEntryList = null;
		schedEntryList = getScheduleEntryList();
		if (selectedSchedEntry != null)
		{
			schedEntryStatusList = dcpmonDBI.getScheduleEntryStatusList(selectedSchedEntry);
			if (selectedStatus != null)
			{
				for(ScheduleEntryStatus ses : schedEntryStatusList)
					if (selectedStatus.getId() == ses.getId())
					{
						// this will also fill the device events
						setSelectedStatus(ses);
						break;
					}
			}
		}
	}
	
	public boolean isScheduleEntrySelected()
	{
System.out.println("isScheduleEntrySelected returning " + (selectedSchedEntry != null));
		return selectedSchedEntry != null;
	}

	public ArrayList<DacqEvent> getEvtList()
	{
System.out.println("getEvtList returning " + evtList.size() + " events.");
		return evtList;
	}

	public String getSelectedSchedEntryName()
	{
		return selectedSchedEntry == null ? null : selectedSchedEntry.getName();
	}

	public ScheduleEntry getSelectedSchedEntry()
	{
		return selectedSchedEntry;
	}

	public void setSelectedSchedEntry(ScheduleEntry sse)
	{
		if (selectedSchedEntry == null 
		 || !selectedSchedEntry.getName().equals(sse.getDisplayName()))
		{
			selectedSchedEntry = sse;
			schedEntryStatusList = dcpmonDBI.getScheduleEntryStatusList(selectedSchedEntry);
			setSelectedStatus(null);
		}
System.out.println("setSelectedSchedEntry(" + sse.getName() + "), there are "
+ schedEntryStatusList.size() + " status entries.");
	}
	
	public void setSelectedStatus(ScheduleEntryStatus stat)
	{
		// If not the same as the currently selected one.
		if (stat == null
		 || selectedStatus == null
		 || !stat.getScheduleEntryId().equals(selectedStatus.getScheduleEntryId())
		 || !stat.getRunStart().equals(selectedStatus.getRunStart()))
		{
			evtList.clear();
			selectedStatus = stat;
			if (selectedStatus != null)
				dcpmonDBI.fillSchedEntryEvents(selectedStatus.getId(), evtList);
		}
		selectedStatus = stat;
	}

	public ArrayList<ScheduleEntryStatus> getSchedEntryStatusList()
	{
		return schedEntryStatusList;
	}

	public ScheduleEntryStatus getSelectedStatus()
	{
		return selectedStatus;
	}
}
