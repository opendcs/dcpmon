package com.covesw.dcpmon;

import java.util.ArrayList;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.event.ActionEvent;

import org.primefaces.event.SelectEvent;

import decodes.polling.DacqEvent;
import decodes.polling.DeviceStatus;

/**
 * Managed Bean for the Device Status Page
 */
@ManagedBean
@SessionScoped
public class DeviceStatusBean
{
	private ArrayList<DeviceStatus> deviceList = null;
	@ManagedProperty(value="#{dcpmonDBI}")
	private DcpmonDBI dcpmonDBI;
//	private String selectedDeviceName = null;
	private ArrayList<DacqEvent> evtList = new ArrayList<DacqEvent>();
	private DeviceStatus selectedDevice = null;
	private int updateInterval = 5;
	private boolean autoUpdate = false;

	public DeviceStatusBean()
	{
	}

	public ArrayList<DeviceStatus> getDeviceList()
	{
		if (deviceList == null)
			deviceList = dcpmonDBI.getDeviceStatusList();
		return deviceList;
	}
	
	public void refreshDevList()
	{
		System.out.println("Refreshing device list");
		deviceList = dcpmonDBI.getDeviceStatusList();
		if (selectedDevice != null)
			dcpmonDBI.fillDeviceEvents(selectedDevice.getPortName(), evtList);
	}
	
	public void onRowSelect(SelectEvent event)
	{
		DeviceStatus devstat = (DeviceStatus)event.getObject();
System.out.println("onRowSelect selected " + devstat.getPortName());
	}

	
	public void setDcpmonDBI(DcpmonDBI dcpmonDBI)
	{
		this.dcpmonDBI = dcpmonDBI;
	}
	
//	public void selectDevice(ActionEvent evt)
//	{
//		String nm = (String)evt.getComponent().getAttributes().get("devName");
//		System.out.println("Selected Device: " + nm);
//	}

	public boolean isDeviceSelected()
	{
System.out.println("isDeviceSelected returning " + (selectedDevice != null));
		return selectedDevice != null;
	}

	public ArrayList<DacqEvent> getEvtList()
	{
System.out.println("getEvtList returning " + evtList.size() + " events.");
		return evtList;
	}

	public String getSelectedDeviceName()
	{
		return selectedDevice == null ? null : selectedDevice.getPortName();
	}

	public DeviceStatus getSelectedDevice()
	{
		return selectedDevice;
	}

	public void setSelectedDevice(DeviceStatus sd)
	{
		if (selectedDevice != null && !selectedDevice.getPortName().equals(sd.getPortName()))
			evtList.clear();
		selectedDevice = sd;
		if (selectedDevice != null)
			dcpmonDBI.fillDeviceEvents(selectedDevice.getPortName(), evtList);

System.out.println("selected device is now " + getSelectedDeviceName());
	}

	public int getUpdateInterval()
	{
		return updateInterval;
	}

	public void setUpdateInterval(int updateInterval)
	{
		this.updateInterval = updateInterval;
System.out.println("Update interval is now " + updateInterval);
	}

	public boolean isAutoUpdate()
	{
		return autoUpdate;
	}

	public void setAutoUpdate(boolean autoUpdate)
	{
		this.autoUpdate = autoUpdate;
	}
	
	public void autoUpdateChanged()
	{
		System.out.println("Auto Update is now " + autoUpdate);
	}
	
}
