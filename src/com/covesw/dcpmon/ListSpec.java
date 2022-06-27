package com.covesw.dcpmon;

import decodes.db.NetworkList;

public class ListSpec
	implements Comparable<ListSpec>
{
	private String listName;
	private String displayName;
	private NetworkList networkList;
	
	public ListSpec(String listName, String displayName, NetworkList networkList)
	{
		super();
		this.listName = listName;
		this.displayName = displayName;
		this.networkList = networkList;
	}
	public String getListName()
	{
		return listName;
	}
	public String getDisplayName()
	{
		return displayName;
	}
	public NetworkList getNetworkList()
	{
		return networkList;
	}
	public int compareTo(ListSpec rhs)
	{
		return this.displayName.compareTo(rhs.displayName);
	}
}