package com.covesw.dcpmon;

import ilex.xml.XmlOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import decodes.db.Platform;
import decodes.decoder.DecodedMessage;
import decodes.xml.PlatformParser;
import decodes.xml.XmlDbTags;

@ManagedBean
@RequestScoped
public class DecodesXmlView
{
	@ManagedProperty(value="#{dcpmonSession}")
	private DcpmonSession dcpmonSession;

	public DecodesXmlView()
	{
System.out.println("DecodesXmlView ctor");
	}

	public StreamedContent getFile()
	{
		StreamedContent file;
		DecodedMessage decMsg = dcpmonSession.getDecMsg();
		if (decMsg == null)
		{
			System.out.println("DecodesXmlView no decMsg");
			return null;
		}
		
		Platform p = decMsg.getPlatform();
		if (p == null)
		{
			System.out.println("DecodesXmlView decMsg with no platform");
			return null;
		}

		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			XmlOutputStream xos = new XmlOutputStream(baos, XmlDbTags.Platform_el);
			xos.writeXmlHeader();
			PlatformParser pp = new PlatformParser(p);
			pp.writeXml(xos);
			byte data[] = baos.toByteArray();
			System.out.println("DecodesXmlView size of xml=" + data.length);
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			file = new DefaultStreamedContent(bais, "application/xml", 
				p.getDcpAddress().toString() + ".xml");
			return file;
		}
		catch(Exception ex)
		{
			System.out.println("Exception creating xml download: " + ex);
			ex.printStackTrace();
			file = null;
		}
		System.out.println("DecodesXmlView returning null");
		return null;
		
	}

	public void setDcpmonSession(DcpmonSession dcpmonSession)
	{
		this.dcpmonSession = dcpmonSession;
	}

}
