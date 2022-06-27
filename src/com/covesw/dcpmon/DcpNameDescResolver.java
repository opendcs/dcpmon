package com.covesw.dcpmon;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import ilex.util.Logger;
import ilex.util.StringPair;
import lrgs.common.DcpAddress;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.NetworkListEntry;
import decodes.db.Platform;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.dcpmon.XmitMediumType;
import decodes.util.NwsXref;
import decodes.util.NwsXrefEntry;
import decodes.util.Pdt;
import decodes.util.PdtEntry;
import decodes.util.hads.Hads;
import decodes.util.hads.HadsEntry;

/**
 * This class is used by the Dcp Monitor to determine what
 * dcp name to display on the web pages. 
 *
 */
public class DcpNameDescResolver
{
	private Database database = null;
	private String dcpmonNameType = null;
	private PdtEntry pdtEntry = null;
	private Platform platform = null;
	private DcpmonDBI dcpmonDBI = null;
	
	private static final long CACHE_TIME = 3600000L * 4;
	private HashMap<StringPair, PlatformNameInfo> mediumNameMap = new HashMap<StringPair, PlatformNameInfo>();
	
	/** Constructor */
	public DcpNameDescResolver(Database database, String dcpmonNameType, DcpmonDBI dcpmonDBI)
	{
		this.database = database;
		this.dcpmonNameType = dcpmonNameType;
		this.dcpmonDBI = dcpmonDBI;
Logger.instance().info("DcpNameDescResolver dcpmonNameType='" + dcpmonNameType + "'");
	}
	
	
//	/**
//	 * Finds out the dcp name to display on the Dcp Monitor web pages.
//	 */
//	public String getBestName(String mediumType, String mediumId)
//	{
//		StringPair sp = getBestNameDesc(mediumType, mediumId);
//		return sp.first;
//	}
//	
//	
//	/**
//	 * Finds the best name and description, returning them as a StringPair.
//	 * Also sets Platform and PdtEntry for subsequent return by the get methods.
//	 * @param mediumType
//	 * @param mediumId
//	 * @return
//	 */
//	public StringPair getBestNameDesc(String mediumType, String mediumId)
//	{
//		PlatformNameInfo pni = getPlatformNameInfo(mediumType, mediumId);
//		return pni.nameDesc;
//	}
	
	/**
	 * Build a platform info structure containing the best available name and description.
	 * If this is in the DECODES database, set the platform entry.
	 * If this is GOES, lookup the pdt entry.
	 * Always succeeds. Never returns null.
	 * @param mediumType The medium type
	 * @param mediumId The medium ID
	 * @return the cached or new PlatformNameInfo structure
	 */
	public synchronized PlatformNameInfo getPlatformNameInfo(XmitMediumType mediumType, String mediumId)
	{
boolean doDebug = mediumId.equalsIgnoreCase("CE5E7ABA") || mediumId.equalsIgnoreCase("CE2D5026");
		StringPair medium = new StringPair(mediumType.toString(), mediumId);
		PlatformNameInfo pi = mediumNameMap.get(medium);
		if (pi != null && System.currentTimeMillis() - pi.cacheTime < CACHE_TIME)
		{
			platform = pi.platform;
			pdtEntry = pi.pdtEntry;
if (doDebug)Logger.instance().info("getPlatformNameInfo read info from map, name="
+ pi.nameDesc.first);
			return pi;
		}
		
		StringPair newNameDesc = new StringPair(null, null);
		
		// Have to get pdtEntry and platform for subsequent calls to get methods.
		DcpAddress dcpAddress = new DcpAddress(mediumId);
		pdtEntry = mediumType == XmitMediumType.GOES ? Pdt.instance().find(dcpAddress) : null;
		String decodesMediumType = 
			mediumType == XmitMediumType.GOES ? decodes.db.Constants.medium_Goes :
			mediumType == XmitMediumType.IRIDIUM ? decodes.db.Constants.medium_IRIDIUM :
			Constants.medium_EDL;
		platform = database.platformList.findPlatform(decodesMediumType, mediumId, null);
		NetworkListEntry nle = null;

		// First Choice: Get naming info from DECODES platform record.
		if (platform != null)
		{
			newNameDesc.second = platform.getBriefDescription();
			Site site = platform.getSite();
			if (site != null)
			{
				if (newNameDesc.second == null || newNameDesc.second.trim().length() == 0)
					newNameDesc.second = site.getBriefDescription();
				SiteName siteName = dcpmonNameType != null ? site.getName(dcpmonNameType) : null;
if (doDebug) Logger.instance().info("DcpNameDescResolver.getNameInfo(" + mediumId + ") siteName("
+ dcpmonNameType + "): " + siteName);
				if (siteName == null)
					siteName = site.getPreferredName();
				if (siteName != null)
				{
					newNameDesc.first = siteName.getNameValue();
					PlatformNameInfo pni = new PlatformNameInfo(newNameDesc, platform, pdtEntry);
					mediumNameMap.put(medium, pni);
					return pni;
				}
			}
		}
		// Second choice: get naming info from any network list containing this ID.
		else if ((nle = findNetlistEntry(mediumType, mediumId)) != null)
		{
if (doDebug)Logger.instance().info("DcpNameDescResolver.getNameInfo(" + mediumId 
+ ")--No platform record, using netlistname=" + nle.getPlatformName()); 
			newNameDesc.first = nle.getPlatformName();
			newNameDesc.second = nle.getDescription();
		}
		// Third choice (GOES only) get naming info from PDT and/or National Weather Service
		else if (mediumType == XmitMediumType.GOES)
		{
if (doDebug)Logger.instance().info("DcpNameDescResolver.getNameInfo(" + mediumId 
	+ ")--No platform record or netlist entry."); 

			if (pdtEntry != null && newNameDesc.second == null)
				newNameDesc.second = pdtEntry.description;
			
			NwsXrefEntry nwsXrefEntry = NwsXref.instance().getByAddr(dcpAddress);
			if (nwsXrefEntry != null)
			{
				newNameDesc.first = nwsXrefEntry.getNwsId();
				newNameDesc.second = nwsXrefEntry.getLocationName();
			}
		}

		// Finally, If no dcpname found anywhere - set it to dcp address and a blank description.
		if (newNameDesc.first == null)
		{
if (doDebug)Logger.instance().info("DcpNameDescResolver.getNameInfo(" + mediumId 
	+ ")-- also no NWS entry. Using mediumID as name.");

			newNameDesc.first = mediumId;
			newNameDesc.second = "";
		}
		PlatformNameInfo pni = new PlatformNameInfo(newNameDesc, platform, pdtEntry);
		mediumNameMap.put(medium, pni);
		return pni;
	}

	/**
	 * Search the network lists used by DCP monitor.
	 * @param mediumType the medium Type
	 * @param mediumId the medium ID
	 * @return the network list entry if found, null if not.
	 */
	private NetworkListEntry findNetlistEntry(XmitMediumType mediumType, String mediumId)
	{
		ArrayList<ListSpec> lists2search = 
			mediumType == XmitMediumType.GOES ? dcpmonDBI.getGoesLists() :
			mediumType == XmitMediumType.IRIDIUM ? dcpmonDBI.getIridiumLists() :
			dcpmonDBI.getPolledDcpLists();
		if (lists2search == null)
			return null;

		NetworkListEntry nle = null;
		for(ListSpec listSpec : lists2search)
			if ((nle = listSpec.getNetworkList().getEntry(mediumId)) != null)
				return nle;
		
		return null;
	}
}
