package com.covesw.dcpmon;

import ilex.util.StringPair;
import decodes.db.Platform;
import decodes.util.PdtEntry;

/** Maps (mediumType,mediumId) to (name,description) */
class PlatformNameInfo
{
	StringPair nameDesc;
	Platform platform;
	PdtEntry pdtEntry;
	long cacheTime;
	public PlatformNameInfo(StringPair nameDesc, Platform platform, PdtEntry pdtEntry)
	{
		super();
		this.nameDesc = nameDesc;
		this.platform = platform;
		this.pdtEntry = pdtEntry;
		cacheTime = System.currentTimeMillis();
	}
}