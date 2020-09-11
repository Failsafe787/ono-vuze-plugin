/**
 * Ono Project
 *
 * File:         CIDRMatcher.java
 * RCS:          $Id: CIDRMatcher.java,v 1.5 2010/03/29 16:48:03 drc915 Exp $
 * Description:  CIDRMatcher class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Oct 22, 2008 at 2:07:58 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.position
 * Status:       Experimental (Do Not Distribute)
 *
 * (C) Copyright 2008, Northwestern University, all rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package edu.northwestern.ono.position;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.gudy.azureus2.plugins.logging.LoggerChannel;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.OnoConfiguration;
import edu.northwestern.ono.util.HashMapCache;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The CIDRMatcher class take a list of CIDR addresses and biases 
 * connections toward peers matching them in the same way that Ono does.
 * 
 */
public class CIDRMatcher {

	public static long TIMEOUT = 5*60*1000;
	private static CIDRMatcher self;
	
	private HashMap<Long, Integer> ipAndOffsets;

	private HashMapCache<String, Long> firstTimeSeen;
	
	public static CIDRMatcher getInstance(){
		if (self==null) self = new CIDRMatcher();
		return self;
	}
	
	private CIDRMatcher(){
		ipAndOffsets = new HashMap<Long, Integer>();
		firstTimeSeen = new HashMapCache<String, Long>(500);
	}
	
	public void loadFromFile(String fileName){
		ipAndOffsets.clear();
		if (fileName==null || fileName.length()==0){
			OnoConfiguration.getInstance().setCidrFile(null);
			return;
		}
		File f = new File(fileName);
		if (!f.exists()){
			MainGeneric.getPluginInterface().getLogger().getChannel(
					"CIDRMatcher").logAlert(LoggerChannel.LT_WARNING, 
							"CIDR file ("+fileName+") not found!");
			return;
		}
		if (!f.canRead()){
			MainGeneric.getPluginInterface().getLogger().getChannel(
			"CIDRMatcher").logAlert(LoggerChannel.LT_WARNING, 
					"CIDR file ("+fileName+") is not readable!");
		}
		
		try {
			BufferedReader in = new BufferedReader(
					new FileReader(fileName));
			String line;
			while ((line = in.readLine()) != null) {
				try {
				String parts[] = line.split("/");
//				Inet4Address addr = (Inet4Address) Inet4Address.getByName(parts[0]);
				int block = Integer.parseInt(parts[1]);
				long ip = Util.convertIpToLong(parts[0]);
				ip = (ip >> (32-block)) << (32-block);
				ipAndOffsets.put(ip, block);
				} catch (Exception e){
					MainGeneric.getPluginInterface().getLogger().getChannel(
					"CIDRMatcher").logAlert(LoggerChannel.LT_WARNING, 
							"Problem parsing line: "+line);
				}

			}
		}
		catch (IOException e){
			MainGeneric.getPluginInterface().getLogger().getChannel(
			"CIDRMatcher").logAlert(LoggerChannel.LT_WARNING, 
					"Error reading CIDR file ("+fileName+")!");
		}
		
	}
	
	public boolean match(String peerIp){
		if (ipAndOffsets.size()==0) return false;
		
		try {
//			Inet4Address addr = (Inet4Address) Inet4Address.getByName(peerIp);
			long ip = Util.convertIpToLong(peerIp);
			for (Entry<Long, Integer> ent : ipAndOffsets.entrySet()){
				if ((ip>>(32-ent.getValue()))<<(32-ent.getValue()) == ent.getKey()){
					if (firstTimeSeen.get(peerIp)==null){
						firstTimeSeen.put(peerIp, System.currentTimeMillis());
					}
					return true;
				}
			}
			} catch (Exception e){
				// TODO check this out
			}
		return false;
	}
	
	public void updateLastTimeSeen(String peerIp){
		firstTimeSeen.put(peerIp, System.currentTimeMillis());
	}
	
	public boolean usedLately(String peerIp){
		Long time = firstTimeSeen.get(peerIp);
		if (time==null) return true;
		if (System.currentTimeMillis()-time < TIMEOUT) return true;
		return false;
	}

	public void snub(String peerIp) {
		firstTimeSeen.put(peerIp, 0L);
		
	}
}
