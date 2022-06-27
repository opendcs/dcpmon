package com.covesw.dcpmon;

import java.util.ArrayList;

public class DataRow
{
	private String timeStamp = "";
	private ArrayList<String> dataColumns = new ArrayList<String>();
	
	public DataRow(String timeStamp)
	{
		this.timeStamp = timeStamp;
	}
	
	public void addColumn(String col)
	{
		dataColumns.add(col);
	}

	public String getTimeStamp()
	{
		return timeStamp;
	}

	public ArrayList<String> getDataColumns()
	{
		return dataColumns;
	}
	
}
