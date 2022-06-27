/**
 * $Id: DcpmonDBI.java,v 1.9 2018/04/09 16:47:04 mmaloney Exp $
 * 
 * $Log: DcpmonDBI.java,v $
 * Revision 1.9  2018/04/09 16:47:04  mmaloney
 * Determine HDB by trying to read table hdb_damtype.
 *
 * Revision 1.8  2018/04/09 14:52:54  mmaloney
 * Servlet Context debugging
 *
 */
package com.covesw.dcpmon;

import ilex.util.Logger;
import ilex.util.TextUtil;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import lrgs.common.DcpMsg;
import opendcs.dai.DacqEventDAI;
import opendcs.dai.DeviceStatusDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.PlatformStatusDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.dai.XmitRecordDAI;
import decodes.polling.DacqEvent;
import decodes.polling.DeviceStatus;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;
import decodes.sql.OracleSequenceKeyGenerator;
import decodes.sql.SequenceKeyGenerator;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.util.ChannelMap;
import decodes.util.NwsXref;
import decodes.util.Pdt;
import decodes.util.PdtLoadListener;
import decodes.util.PropertySpec;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.NetworkList;
import decodes.db.Platform;
import decodes.db.PlatformStatus;
import decodes.db.RoutingSpec;
import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.dcpmon.DcpMonitorConfig;
import decodes.dcpmon.XmitMediumType;
import decodes.dcpmon.XmitRecSpec;
import opendcs.opentsdb.OpenTsdbSqlDbIO;

/**
 * DCP Monitor Database Interface
 * Singleton class used for all database i/o by the DCP Monitor Web App.
 * Also keeps cached copies of Oriole data and network lists.
 */
@ManagedBean(name="dcpmonDBI", eager=true)
@ApplicationScoped
public class DcpmonDBI
	implements Serializable, PdtLoadListener
{
	private Database theDb = null;
	private CompAppInfo dcpmonAppInfo = null;
	private RoutingSpec dcpmonRoutingSpec = null;
	private ArrayList<ListSpec> goesLists = new ArrayList<ListSpec>();
	private ArrayList<ListSpec> polledDcpLists = new ArrayList<ListSpec>();
	private ArrayList<ListSpec> iridiumLists = new ArrayList<ListSpec>();
	private DcpMonitorConfig dcpmonConfig = new DcpMonitorConfig();
	private DayCache[] dayCaches = null;
	private DcpNameDescResolver nameResolver = null;
	
	/**
	 * Private Constructor, access the singleton instance() method only.
	 */
	public DcpmonDBI()
		throws BadConnectException
	{
		Logger.instance().setMinLogPriority(Logger.E_DEBUG3);
		Logger.instance().debug3("DcpmonDBI constructor");
		theDb = new decodes.db.Database();
		decodes.db.Database.setDb(theDb);
		fillDecodesCache();
		dayCaches = new DayCache[dcpmonConfig.numDaysStorage];
		int daynum = DayUtil.getCurrentDay();
		nameResolver = new DcpNameDescResolver(decodes.db.Database.getDb(), 
			dcpmonConfig.dcpmonNameType, this);
		for(int idx = 0; idx < dcpmonConfig.numDaysStorage; idx++)
			dayCaches[idx] = new DayCache(daynum--, nameResolver);
		maintainGoesPdt();
	}
	
	
	/**
	 * Call when current day is not found in the day caches.
	 */
	public void rotateDayCaches()
	{
		Logger.instance().debug3("DcpmonDBI.rotateDayCaches");
		int daynum = DayUtil.getCurrentDay();
		int nds = dcpmonConfig.numDaysStorage;
		if (nds != dayCaches.length)
		{
			DayCache [] ndc = new DayCache[nds];
			for(int idx = 0; idx < ndc.length && idx < dayCaches.length; idx++)
				ndc[idx] = dayCaches[idx];
			for(int idx = dayCaches.length; idx < ndc.length; idx++)
				ndc[idx] = null;
			dayCaches = ndc;
		}
		for(int idx = dcpmonConfig.numDaysStorage-1; idx > 0; idx--)
			dayCaches[idx] = dayCaches[idx-1];
		dayCaches[0] = new DayCache(daynum, nameResolver);
	}
	
	/**
	 * Called by framework before destroying this object after app shuts down.
	 */
	@PreDestroy
	public void shutdown()
	{
		Logger.instance().debug3("DcpmonDBI.shutdown()");
		stopMaintainGoesPdt();
		dcpmonAppInfo = null;
		dcpmonRoutingSpec = null;
		theDb.setDbIo(null);
		theDb = null;
	}
	
	private void fillDecodesCache()
		throws BadConnectException
	{
		Logger.instance().debug3("DcpmonDBI.fillDecodesCache()");
		SqlDatabaseIO sqlDbIo = null;
		LoadingAppDAI loadingAppDAO = null;
		try
		{
			sqlDbIo = this.makeSqlDatabaseIO();
			theDb.setDbIo(sqlDbIo);
			
			System.out.println("engineeringUnitList");
			theDb.engineeringUnitList.read();
			System.out.println("siteList");
			theDb.siteList.read();
			System.out.println("platformConfigList");
			theDb.platformConfigList.read();
			System.out.println("platformList");
			theDb.platformList.read();
			System.out.println("platformList read " + theDb.platformList.size() + " platforms.");

			System.out.println("presentationGroupList");
			theDb.presentationGroupList.read();
			System.out.println("networkListList");
			theDb.networkListList.read();
			System.out.println("Read " + theDb.networkListList.size() + " network lists.");
			System.out.println("routingSpecList");
			theDb.routingSpecList.read();
			dcpmonRoutingSpec = theDb.routingSpecList.find("dcpmon");
			if (dcpmonRoutingSpec == null)
			{
				dcpmonRoutingSpec = new RoutingSpec();
				dcpmonRoutingSpec.setName("dcpmon");
			}

			loadingAppDAO = sqlDbIo.makeLoadingAppDAO();
			dcpmonAppInfo = loadingAppDAO.getComputationApp("dcpmon");
		}
		catch (DatabaseException ex)
		{
			String msg = "Error filling DECODES cache: " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new BadConnectException(msg);
		}
		catch (DbIoException ex)
		{
			String msg = "Error filling DECODES cache: " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new BadConnectException(msg);
		}
		catch (NoSuchObjectException ex)
		{
			String msg = "No 'dcpmon' Loading App record -- will proceed with defaults: " + ex;
			System.err.println(msg);
			dcpmonAppInfo = new CompAppInfo();
			dcpmonAppInfo.setAppName("dcpmon");
		}
		finally
		{
			if (loadingAppDAO != null)
				loadingAppDAO.close();
			if (sqlDbIo != null)
				freeSqlDbIo(sqlDbIo);
			theDb.setDbIo(null);
		}

		dcpmonConfig = new DcpMonitorConfig();
		dcpmonConfig.loadFromProperties(dcpmonAppInfo.getProperties());
		
		goesLists.clear();
		polledDcpLists.clear();
		iridiumLists.clear();
		
		for(Enumeration en = dcpmonAppInfo.getPropertyNames(); en.hasMoreElements(); )
		{
			String nm = (String)en.nextElement();
			if (nm.toLowerCase().startsWith("grp:"))
			{
				String listName = nm.substring(4);
				String displayName = dcpmonAppInfo.getProperty(nm);
				NetworkList netlist = theDb.networkListList.find(listName);
				if (netlist == null)
				{
					System.err.println("AppInfo specifies non-existent network list '" + nm + "'");
					continue;
				}
				addListSpec(new ListSpec(listName, displayName, netlist));
			}
		}
		String s = dcpmonAppInfo.getProperty("webUsesRoutingSpecNetlists");
		RoutingSpec rs = theDb.routingSpecList.find("dcpmon");
		if (s != null && s.trim().length() > 0 && TextUtil.str2boolean(s) == false)
			System.out.println("Not adding routing spec netlists to web lists.");
		else if (rs != null)
		{
		  nextNLName:
			for(String listName : rs.networkListNames)
			{
				NetworkList netlist = theDb.networkListList.find(listName);
				if (netlist == null)
				{
					System.out.println("Ignoring invalid network list name '" + listName + "'");
					continue;
				}
System.out.println("Considering netlist '" + netlist.name + "' with mediumType '" + netlist.transportMediumType + "'");
			
				for(ListSpec ls : goesLists)
					if (ls.getListName().equalsIgnoreCase(listName))
					{
System.out.println("Already in goesLists");
						continue nextNLName;
					}
				for(ListSpec ls : iridiumLists)
					if (ls.getListName().equalsIgnoreCase(listName))
					{
System.out.println("Already in iridiumLists");
						continue nextNLName;
					}
				for(ListSpec ls : polledDcpLists)
					if (ls.getListName().equalsIgnoreCase(listName))
					{
System.out.println("Already in polledDcpLists");
						continue nextNLName;
					}
				addListSpec(new ListSpec(listName, listName, netlist));
			}
		}
		Collections.sort(goesLists);
		Collections.sort(iridiumLists);
		Collections.sort(polledDcpLists);
	}
	
	private void addListSpec(ListSpec listSpec)
	{
		Logger.instance().debug3("DcpmonDBI.addListSpec()");
		NetworkList netlist = listSpec.getNetworkList();
System.out.println("addListSpec name=" + netlist.name + ", tmtype=" + netlist.transportMediumType);
		if (netlist.transportMediumType != null
			 && netlist.transportMediumType.toLowerCase().contains("goes"))
				goesLists.add(listSpec);
			else if (netlist.transportMediumType.equalsIgnoreCase(Constants.medium_IRIDIUM))
				iridiumLists.add(listSpec);
			else
				polledDcpLists.add(listSpec);
	}
	
	/**
	 * Thread safe method used by the other methods in this class to get a
	 * database connection from the pool.
	 * The method MUST call freeSqlDbIo with the connection when it is done.
	 * @return the SqlDatabaseIO object.
	 */
	private SqlDatabaseIO makeSqlDatabaseIO()
		throws DbIoException
	{
		Connection con = null;
		KeyGenerator kg = null;
		
		// First establish the database Connection object:
		try
		{
			Context initialCtx = new InitialContext();
			Context envCtx = (Context)initialCtx.lookup("java:comp/env");
			DataSource ds = (DataSource)envCtx.lookup("jdbc/dcpmondb");
			con = ds.getConnection();
			boolean isOracle = 
				con.getMetaData().getDatabaseProductName().toLowerCase().contains("oracle");
			kg = isOracle ? new OracleSequenceKeyGenerator() : new SequenceKeyGenerator();
			
		}
		catch(SQLException ex)
		{
			String msg = "Cannot connect to database for jdbc/dcpmondb: " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new DbIoException(msg);
		}
		catch (NamingException ex)
		{
			String msg = "Cannot lookup envCtx java:comp/env, and then jdbc/dcpmondb: " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new DbIoException(msg);
		}
		finally
		{
			
		}

		// Now, determine if this is HDB or OpenTSDB
		boolean isHdb = false;
		try
		{
			Statement statement = con.createStatement();
			// hdb_damtype table only exists in HDB.
			ResultSet rs = statement.executeQuery("select * from hdb_damtype");
			isHdb = true;
			try { rs.close(); } catch(Exception ex) {}
			try { statement.close(); } catch(Exception ex) {}
		}
		catch (SQLException e)
		{
			isHdb = false;
		}
		finally
		{
		}

		Logger.instance().info("This " + (isHdb ? "IS" : "is NOT") + " an HDB database.");

		String appDbIoClass = isHdb ? "decodes.hdb.HdbSqlDatabaseIO" 
			: "opendcs.opentsdb.OpenTsdbSqlDbIO";
		Logger.instance().info("appDbIoClass=" + appDbIoClass);
		
		try
		{
			SqlDatabaseIO sqlDbIo = (SqlDatabaseIO)Class.forName(appDbIoClass).newInstance();
			sqlDbIo.setConnection(con);
			sqlDbIo.setKeyGenerator(kg);
			sqlDbIo.determineVersion(con);
			
			
//			sqlDbIo.useExternalConnection(con, kg, "jdbc/dcpmondb");
			return sqlDbIo;
		}
		catch (Exception ex)
		{
			String msg = "Cannot Instantiate '" + appDbIoClass + "': " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			if (con != null)
				try { con.close(); } catch(Exception ex2) {}
			throw new DbIoException(msg);
		}
		finally
		{
		}
	}
	
	private void freeSqlDbIo(SqlDatabaseIO sqlDbIo)
	{
		Logger.instance().debug3("DcpmonDBI.freeSqlDbIo()");
		try
		{
			sqlDbIo.getConnection().close();
		}
		catch (SQLException ex)
		{
			System.err.println("Error closing connection: " + ex);
			ex.printStackTrace(System.err);
		}
	}
	
	public synchronized Platform getPlatform(String mediumType, String mediumId)
	{
		Logger.instance().debug3("DcpmonDBI.getPlatform()");
		SqlDatabaseIO sqlDbIo = null;
		try
		{
			sqlDbIo = makeSqlDatabaseIO();
			theDb.setDbIo(sqlDbIo);
			return theDb.platformList.getPlatform(mediumType, mediumId);
		}
		catch(Exception ex)
		{
			String errorMsg = "Cannot read platform record for TM " + mediumType
				+ ":" + mediumId + ": " + ex;
			System.out.println(errorMsg);
			return null;
		}
		finally
		{
			if (sqlDbIo != null)
				freeSqlDbIo(sqlDbIo);
			theDb.setDbIo(null);
		}
	}
	
	public RoutingSpec getDcpmonRoutingSpec()
	{
		checkCaches();
		return dcpmonRoutingSpec;
	}

	public CompAppInfo getDcpmonAppInfo()
	{
		checkCaches();
		return dcpmonAppInfo;
	}

	public ArrayList<ListSpec> getGoesLists()
	{
		checkCaches();
		return goesLists;
	}

	public ArrayList<ListSpec> getPolledDcpLists()
	{
		checkCaches();
		return polledDcpLists;
	}

	public ArrayList<ListSpec> getIridiumLists()
	{
		checkCaches();
		return iridiumLists;
	}
	
	private synchronized void checkCaches()
	{
		// TODO if it has been more than half hour since refreshing the
		// caches, do it now.
	}
	
	public int getNumDaysStorage()
	{
		return dcpmonConfig.numDaysStorage;
	}
	
	private void maintainGoesPdt()
	{
		Logger.instance().debug3("DcpmonDBI.maintainGoesPdt()");
		Pdt.instance().setPdtLoadListener(this);
		if (dcpmonConfig.pdtLocalFile != null 
		 && dcpmonConfig.pdtLocalFile.trim().length() > 0)
		{
			Pdt.instance().startMaintenanceThread(dcpmonConfig.pdtUrl, dcpmonConfig.pdtLocalFile);
		}
		if (dcpmonConfig.cdtLocalFile != null 
		 && dcpmonConfig.cdtLocalFile.trim().length() > 0)
		{
			ChannelMap.instance().startMaintenanceThread(dcpmonConfig.cdtUrl, dcpmonConfig.cdtLocalFile);
		}
		if (dcpmonConfig.nwsXrefLocalFile != null
		 && dcpmonConfig.nwsXrefLocalFile.trim().length() > 0)
		{
			NwsXref.instance().startMaintenanceThread(dcpmonConfig.nwsXrefUrl, dcpmonConfig.nwsXrefLocalFile);
		}
	}
	
	private void stopMaintainGoesPdt()
	{
		Logger.instance().debug3("DcpmonDBI.stopMaintainGoesPdt()");
		Pdt.instance().stopMaintenanceThread();
		ChannelMap.instance().stopMaintenanceThread();
		NwsXref.instance().stopMaintenanceThread();
		try { Thread.sleep(1000L); } catch(InterruptedException ex) {}
	}

	public DcpMonitorConfig getDcpmonConfig()
	{
		return dcpmonConfig;
	}
	
	public ArrayList<DeviceStatus> getDeviceStatusList()
	{
		Logger.instance().debug3("DcpmonDBI.getDeviceStatusList()");
		SqlDatabaseIO sqlDbIo = null;
		DeviceStatusDAI deviceStatusDAO = null;
		try
		{
			sqlDbIo = makeSqlDatabaseIO();
			deviceStatusDAO = sqlDbIo.makeDeviceStatusDAO();
			ArrayList<DeviceStatus> ret = deviceStatusDAO.listDeviceStatuses();
			for(Iterator<DeviceStatus> dsit = ret.iterator(); dsit.hasNext(); )
			{
				// Remove any device that has not been used in more than 72 hours.
				DeviceStatus ds = dsit.next();
				Date lastAct = ds.getLastActivityTime();
				if (lastAct == null 
				 || System.currentTimeMillis()-lastAct.getTime() > 72 * 3600000L)
					dsit.remove();
			}
			Collections.sort(ret, 
				new Comparator<DeviceStatus>()
				{
					public int compare(DeviceStatus o1, DeviceStatus o2)
					{
						return o1.getPortName().compareTo(o2.getPortName());
					}
				});
			
			return ret;
		}
		catch(Exception ex)
		{
			System.err.println("getDeviceStatusLists Error listing devices: " + ex);
			ex.printStackTrace();
			return new ArrayList<DeviceStatus>();
		}
		finally
		{
			if (deviceStatusDAO != null)
				deviceStatusDAO.close();
			if (sqlDbIo != null)
				freeSqlDbIo(sqlDbIo);
		}
	}
	
	public int fillDeviceEvents(String devName, ArrayList<DacqEvent> evtList)
	{
		Logger.instance().debug3("DcpmonDBI.fillDeviceEvents()");
		SqlDatabaseIO sqlDbIo = null;
		DacqEventDAI dacqEventDAO = null;
		try
		{
			sqlDbIo = makeSqlDatabaseIO();
			dacqEventDAO = sqlDbIo.makeDacqEventDAO();
			return dacqEventDAO.readEventsContaining(devName, evtList);
		}
		catch (Exception ex)
		{
			System.err.println("fillDeviceEvents Error listing events: " + ex);
			ex.printStackTrace();
			return 0;
		}
		finally
		{
			if (dacqEventDAO != null)
				dacqEventDAO.close();
			if (sqlDbIo != null)
				freeSqlDbIo(sqlDbIo);
		}
	}
	
	public ArrayList<ScheduleEntry> getScheduleEntryList()
	{
		Logger.instance().debug3("DcpmonDBI.getScheduleEntryList()");
		SqlDatabaseIO sqlDbIo = null;
		ScheduleEntryDAI scheduleEntryDAO = null;
		try
		{
			sqlDbIo = makeSqlDatabaseIO();
			scheduleEntryDAO = sqlDbIo.makeScheduleEntryDAO();
			return scheduleEntryDAO.listScheduleEntries(null);
		}
		catch(Exception ex)
		{
			System.err.println("getScheduleEntryList Error listing schedule entries: " + ex);
			ex.printStackTrace();
			return new ArrayList<ScheduleEntry>();
		}
		finally
		{
			if (scheduleEntryDAO != null)
				scheduleEntryDAO.close();
			if (sqlDbIo != null)
				freeSqlDbIo(sqlDbIo);
		}
	}

	public int fillSchedEntryEvents(DbKey id, ArrayList<DacqEvent> evtList)
	{
		Logger.instance().debug3("DcpmonDBI.fillSchedEntryEvents()");
		SqlDatabaseIO sqlDbIo = null;
		DacqEventDAI dacqEventDAO = null;
		try
		{
			sqlDbIo = makeSqlDatabaseIO();
			dacqEventDAO = sqlDbIo.makeDacqEventDAO();
			return dacqEventDAO.readEventsForScheduleStatus(id, evtList);
		}
		catch (DbIoException ex)
		{
			System.err.println("fillSchedEntryEvents Error listing events: " + ex);
			ex.printStackTrace();
			return 0;
		}
		finally
		{
			if (dacqEventDAO != null)
				dacqEventDAO.close();
			if (sqlDbIo != null)
				freeSqlDbIo(sqlDbIo);
		}
	}

	public ArrayList<ScheduleEntryStatus> getScheduleEntryStatusList(ScheduleEntry scheduleEntry)
	{
		Logger.instance().debug3("DcpmonDBI.getScheduleEntryStatusList()");
		SqlDatabaseIO sqlDbIo = null;
		ScheduleEntryDAI scheduleEntryDAO = null;
		try
		{
			sqlDbIo = makeSqlDatabaseIO();
			scheduleEntryDAO = sqlDbIo.makeScheduleEntryDAO();
			return scheduleEntryDAO.readScheduleStatus(scheduleEntry);
		}
		catch(Exception ex)
		{
			System.err.println("getScheduleEntryStatusList Error listing schedule entries: " + ex);
			ex.printStackTrace();
			return new ArrayList<ScheduleEntryStatus>();
		}
		finally
		{
			if (scheduleEntryDAO != null)
				scheduleEntryDAO.close();
			if (sqlDbIo != null)
				freeSqlDbIo(sqlDbIo);
		}
	}
	
	public ArrayList<PlatformStatus> getPlatformStatusList()
	{
		Logger.instance().debug3("DcpmonDBI.getPlatformStatusList()");
		SqlDatabaseIO sqlDbIo = null;
		PlatformStatusDAI platformStatusDAO = null;
		try
		{
			sqlDbIo = makeSqlDatabaseIO();
			platformStatusDAO = sqlDbIo.makePlatformStatusDAO();
			
			ArrayList<PlatformStatus> ret = platformStatusDAO.listPlatformStatus();
			
			String dcpmonNameType = dcpmonConfig.dcpmonNameType;
			if (dcpmonNameType != null && dcpmonNameType.trim().length() == 0)
				dcpmonNameType = null;
			
			for(PlatformStatus ps : ret)
			{
				Platform p = theDb.platformList.getById(ps.getId());
				
				if (p != null)
				{
					Site site = p.getSite();
					if (site != null)
					{
						SiteName siteName = dcpmonNameType != null ? site.getName(dcpmonNameType) : null;
						if (siteName == null)
							siteName = site.getPreferredName();
						if (siteName != null)
							ps.setSiteName(siteName.getNameValue());
					}
					else
						ps.setSiteName("p:" + p.getId());
					ps.setDesignator(p.getPlatformDesignator());
				}
				else
				{
					ps.setSiteName("");
					ps.setDesignator("");
				}
			}
			return ret;
		}
		catch(Exception ex)
		{
			System.err.println("getPlatformStatusList Error listing platform statuses: " + ex);
			ex.printStackTrace();
			return new ArrayList<PlatformStatus>();
		}
		finally
		{
			if (platformStatusDAO != null)
				platformStatusDAO.close();
			if (sqlDbIo != null)
				freeSqlDbIo(sqlDbIo);
		}
	}
	
	public int fillPlatformEvents(DbKey platformId, ArrayList<DacqEvent> evtList)
	{
		Logger.instance().debug3("DcpmonDBI.fillPlatformEvents()");
		SqlDatabaseIO sqlDbIo = null;
		DacqEventDAI dacqEventDAO = null;
			
		try
		{
			sqlDbIo = makeSqlDatabaseIO();
			dacqEventDAO = sqlDbIo.makeDacqEventDAO();
			return dacqEventDAO.readEventsForPlatform(platformId, evtList);
		}
		catch (Exception ex)
		{
			System.err.println("fillPlatformEvents Error listing events: " + ex);
			ex.printStackTrace();
			return 0;
		}
		finally
		{
			if (dacqEventDAO != null)
				dacqEventDAO.close();
			if (sqlDbIo != null)
				freeSqlDbIo(sqlDbIo);
		}
	}
	
	/**
	 * Gets the specified day's cache and refreshes it from the database.
	 * Because of the refresh, this method should not be called inside tight loops.
	 * @param daynum
	 * @return
	 */
	public DayCache getDayCache(int daynum)
	{
		Logger.instance().debug3("DcpmonDBI.getDayCache()");
		if (daynum > DayUtil.getCurrentDay())
			return null;
		
		DayCache ret = null;
		for(int idx = 0; idx < dayCaches.length; idx++)
			if (dayCaches[idx] != null && dayCaches[idx].getDayNum() == daynum)
			{
				ret = dayCaches[idx];
				break;
			}
		if (ret == null && daynum > dayCaches[0].getDayNum())
		{
			rotateDayCaches();
			ret = dayCaches[0];
		}
		if (ret == null)
			return null; // must be past the limit.
		
		SqlDatabaseIO sqlDbIo = null;
		XmitRecordDAI xmitRecordDAO = null;
		try
		{
			sqlDbIo = makeSqlDatabaseIO();
			xmitRecordDAO = sqlDbIo.makeXmitRecordDao(dcpmonConfig.numDaysStorage);
			ArrayList<XmitRecSpec> newrecs = xmitRecordDAO.readSince(daynum, ret.getLastRecId());
			int n = 0;
			for(XmitRecSpec xrs : newrecs)
			{
				ret.add(xrs);
				if (++n % 1000 == 0)
					Logger.instance().debug2("Added " + n + " to DayCache so far.");;
			}
			Logger.instance().debug2("Added " + n + " to DayCache total.");;
		}
		catch(DbIoException ex)
		{
			System.out.println("ERROR in xmitRecordDAO.readSince for daynum=" + daynum + ": " + ex);
			ex.printStackTrace();
		}
		finally
		{
			if (xmitRecordDAO != null)
				xmitRecordDAO.close();
			if (sqlDbIo != null)
				freeSqlDbIo(sqlDbIo);
		}
		return ret;
	}

	public void pdtLoaded()
	{
		System.out.println("PDT Load Complete. Reading current day's xmit recs.");
		getDayCache(DayUtil.getCurrentDay());
	}

	public DcpNameDescResolver getNameResolver()
	{
		return nameResolver;
	}
	
	public void fillMessage(DcpMsg msg)
	{
		Logger.instance().debug3("DcpmonDBI.fillMessage()");
		SqlDatabaseIO sqlDbIo = null;
		XmitRecordDAI xmitRecordDAO = null;
		try
		{
			sqlDbIo = makeSqlDatabaseIO();
			xmitRecordDAO = sqlDbIo.makeXmitRecordDao(getNumDaysStorage());
			xmitRecordDAO.fillCompleteMsg(msg);
		}
		catch(Exception ex)
		{
			Logger.instance().warning("DcpmonDBI.fillMessage(): " + ex);
		}
		finally
		{
			if (xmitRecordDAO != null)
				xmitRecordDAO.close();
			if (sqlDbIo != null)
				freeSqlDbIo(sqlDbIo);
		}
	}

	public void readXmitsByMediumId(ArrayList<DcpMsg> dcpMessages, int dayNum, XmitMediumType xmitMediumType,
		String mediumId)
	{
		Logger.instance().debug3("DcpmonDBI.readXmitsByMediumId()");
		SqlDatabaseIO sqlDbIo = null;
		XmitRecordDAI xmitRecordDAO = null;
		try
		{
			try
			{
				sqlDbIo = makeSqlDatabaseIO();
				xmitRecordDAO = sqlDbIo.makeXmitRecordDao(getNumDaysStorage());
				xmitRecordDAO.readXmitsByMediumId(dcpMessages, dayNum, xmitMediumType, mediumId);
			}
			catch (Exception ex)
			{
				System.err.println("prepareForWazzup Error in xmitRecordDAO.readXmitsByMediumId: " + ex);
				ex.printStackTrace();
			}
		}
		finally
		{
			if (xmitRecordDAO != null)
				xmitRecordDAO.close();
			if (sqlDbIo != null)
				freeSqlDbIo(sqlDbIo);
		}
		
	}
}
