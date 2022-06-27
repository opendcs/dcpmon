/**
 * $Id: DcpMsgDecoder.java,v 1.2 2017/01/02 21:18:44 mmaloney Exp $
 * 
 * Open Source Software
 * 
 * $Log
 */
package com.covesw.dcpmon;


import java.util.Iterator;

import javax.faces.context.FacesContext;

import ilex.var.Variable;
import lrgs.common.DcpMsg;
import decodes.comp.CompResolver;
import decodes.comp.ComputationProcessor;
import decodes.datasource.GoesPMParser;
import decodes.datasource.HeaderParseException;
import decodes.datasource.PMParser;
import decodes.datasource.RawMessage;
import decodes.db.DatabaseException;
import decodes.db.DecodesScript;
import decodes.db.Platform;
import decodes.db.PresentationGroup;
import decodes.db.TransportMedium;
import decodes.decoder.DecodedMessage;
import decodes.decoder.DecoderException;

/**
 * Encapsulate decoding.
 */
public class DcpMsgDecoder
{
	private DcpmonDBI dcpmonDBI;
	private String errorMsg = "";
	String module = "DcpMsgDecoder";
	private ComputationProcessor compProc = null;
	
	public DcpMsgDecoder(DcpmonDBI dcpmonDBI)
	{
		this.dcpmonDBI = dcpmonDBI;
		FacesContext ctx = FacesContext.getCurrentInstance();
		String compProcCfg =
		    ctx.getExternalContext().getInitParameter("ComputationProcessor");
		if (compProcCfg != null)
		{
			compProc = new ComputationProcessor();
			String resolverSpecs[] = compProcCfg.split(";");
			for(String className : resolverSpecs)
			{
				className = className.trim();
				String props = null;
				int colon = className.indexOf(':');
				if (colon > 0)
				{
					props = className.substring(colon + 1);
					className = className.substring(0, colon);
				}
				ClassLoader cl = Thread.currentThread().getContextClassLoader();
				try
				{
					Class cls = cl.loadClass(className);
					CompResolver cr = (CompResolver)cls.newInstance();
					if (props != null)
					{
						String propAssigns[] = props.split(",");
						for(String propAssign : propAssigns)
						{
							propAssign = propAssign.trim();
							int equals = propAssign.indexOf('=');
							if (equals <= 0)
								continue;
							cr.setProperty(propAssign.substring(0, equals), propAssign.substring(equals+1));
						}
					}
					compProc.addCompResolver(cr);
				}
				catch(Exception ex)
				{
					System.err.println("Cannot load comp resolver class '" + className + "': " + ex);
				}
			}
		}
	}
	
	/**
	 * Given the DcpMsg - decode and return a DecodedMessage object.
	 * If error, the error message is saved and can be retrieved with getErrorMsg.
	 * It will also be printed to stdout for the log.
	 * @return DecodedMessage successful, null if not.
	 */
	public synchronized DecodedMessage decodeData(DcpMsg dcpMsg, String mediumType)
	{
		errorMsg = "";

		RawMessage rawMsg = new RawMessage(dcpMsg.getData(), dcpMsg.getData().length);
		DecodesScript ds = null;
		PMParser pmp;
		try 
		{
			pmp = PMParser.getPMParser(mediumType);
			pmp.parsePerformanceMeasurements(rawMsg);
		}
		catch(HeaderParseException ex) 
		{
			errorMsg = "Could not parse message header for '"
				+ dcpMsg.getHeader() + "': " + ex;
			System.out.println(errorMsg);
			return null;
		}
		rawMsg.setTimeStamp(dcpMsg.getXmitTime());

		if (dcpMsg.getCarrierStart() != null)
			rawMsg.setPM(GoesPMParser.CARRIER_START, new Variable(dcpMsg.getCarrierStart()));
		if (dcpMsg.getCarrierStop() != null)
		rawMsg.setPM(GoesPMParser.CARRIER_STOP, new Variable(dcpMsg.getCarrierStop()));
		rawMsg.setPM(GoesPMParser.FAILURE_CODE, new Variable(dcpMsg.getXmitFailureCodes()));

		//Attempt to get platform record using type Goes
		// Note: Platform list will find any matching GOES TM type (ST or RD).
		Platform p = dcpmonDBI.getPlatform(mediumType, dcpMsg.getDcpAddress().toString());
		
		if (p == null)
		{
			errorMsg = "Cannot find Platform record for '" + dcpMsg.getHeader() + "'";
			System.err.println(errorMsg);
			return null;
		}

		rawMsg.setPlatform(p);  // Set platform reference in the raw message.
		
		// Determine transport medium
		TransportMedium transportMedium = null;
		for(Iterator<TransportMedium> tmit = p.transportMedia.iterator(); tmit.hasNext(); )
		{
			TransportMedium tm = tmit.next();
			if (!tm.getMediumId().equalsIgnoreCase(dcpMsg.getDcpAddress().toString()))
				continue;
			
			// For GOES messages, also have to match the channel.
			if (mediumType.toLowerCase().contains("goes"))
			{
				if (tm.channelNum != dcpMsg.getGoesChannel())
					continue;
			}
			transportMedium = tm;
			break;
		}

		if (transportMedium == null)
		{
			errorMsg = "Cannot find transport medium for mediumType '" + mediumType
				+ "', and header '" + dcpMsg.getHeader() + "'";
			System.err.println(errorMsg);
			return null;
		}

		rawMsg.setTransportMedium(transportMedium);
		DecodedMessage dm = null;

		try
		{
			if (!p.isPrepared())
				p.prepareForExec();
			if (!transportMedium.isPrepared())
				transportMedium.prepareForExec();
		
			// Get decodes script & use it to decode message.
			ds = transportMedium.getDecodesScript();
			if (ds != null)
			{
				dm = ds.decodeMessage(rawMsg);
				dm.applyScaleAndOffset();
				
				if (compProc != null)
					compProc.applyComputations(dm);
			}
			else
			{
				errorMsg = "Cannot find decodes script for header '" +
					dcpMsg.getHeader() + "'";
				System.err.println(errorMsg);
				return null;
			}
		}
		catch (DatabaseException ex)
		{
			errorMsg = "Database error decoding message '" + dcpMsg.getHeader() + "': " + ex;
			System.err.println(errorMsg);
			return null;
		}
		catch (DecoderException ex)
		{
			errorMsg = "Failed to decode message '" + dcpMsg.getHeader() + "': " + ex;
			System.err.println(errorMsg);
			return null;
		}
		catch(Exception ex)
		{
			errorMsg = "Unexpected exception decoding message '" + dcpMsg.getHeader() + "': " + ex;
			System.err.println(errorMsg);
			ex.printStackTrace(System.err);
			return null;
		}
		
		PresentationGroup presentationGroup = null;
//		dcpmonDBI.getDcpmonConfig().presentationGroup
		
//		presentationGroup =
//			Database.getDb().presentationGroupList.find(
//				rs.presentationGroupName);
//		
//		if (presentationGroup == null)
//		{
//			log(Logger.E_FAILURE,
//				"Cannot find presentation group '" +
//				rs.presentationGroupName + "'");
//			done = true;
//			currentStatus = "ERROR-PresGrpInit";
//			return;
//		}
		
		return dm;
	}
}
