/**
 * Ono Project
 *
 * File:         StatManager.java
 * RCS:          $Id: StatManager.java,v 1.14 2010/03/29 16:48:04 drc915 Exp $
 * Description:  StatManager class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Apr 11, 2007 at 9:42:24 AM
 * Language:     Java
 * Package:      edu.northwestern.ono.stats
 * Status:       Experimental (Do Not Distribute)
 *
 * (C) Copyright 2007, Northwestern University, all rights reserved.
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
package edu.northwestern.ono.stats;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.OnoConfiguration;
import edu.northwestern.ono.timer.ITimer;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The StatManager class runs and controls statistics-gathering code.
 */
public class StatManager {

	private static StatManager self;
	private static ITimer peerStats;
	private static ITimer downloadStats;

	public static StatManager getInstance(){
		if (self == null){
			self = new StatManager();		
		}
		return self;
	}
	
	public static void start(){
		
		String myIp = MainGeneric.getPublicIpAddress();
		while (myIp==null){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			myIp = MainGeneric.getPublicIpAddress();
		} // end invalid IP address
		
		// register use w/ DB, get update interval
		MainGeneric.getReporter().setExtendedConnection(true);
		int myId = Statistics.getInstance().getSelfId();
		if (myId >0){
			MainGeneric.getReporter().registerUsage(myId, 
					Util.currentGMTTime(), 
							OnoConfiguration.getInstance().getUUID());
		}
		MainGeneric.getReporter().setExtendedConnection(false);
		int interval = OnoConfiguration.getInstance().getPeerUpdateIntervalSec()*1000;
		
		if (peerStats == null) {
			peerStats = MainGeneric.createTimer("PeerStatsTimer");
		}
		PeerStatistics.setRecordingInterval(interval); 
		peerStats.addPeriodicEvent(interval, PeerStatistics.getInstance());
		
		if (downloadStats == null){
			downloadStats = MainGeneric.createTimer("DownloadStatsTimer");			
		}
		DownloadStats.setRecordingInterval(interval);
		downloadStats.addPeriodicEvent(interval, DownloadStats.getInstance());
	}
	
	public static void stop(){
		if (peerStats!=null) {
			peerStats.destroy();
			peerStats = null;
		}
		
		
		if (downloadStats!=null) {
			downloadStats.destroy();
			downloadStats = null;
		}

		if (DownloadStats.getInstance()!=null){
			DownloadStats.getInstance().stop();
		}
		
		if (PeerStatistics.getInstance()!=null){
			PeerStatistics.getInstance().stop();
		}

		
		MainGeneric.getReporter().disconnect();
		
	}
	
	public static void flush(){
		MainGeneric.getReporter().setExtendedConnection(true);
		PeerStatistics.getInstance().flush();
		DownloadStats.getInstance().flush();
		MainGeneric.getReporter().setExtendedConnection(false);
		MainGeneric.getReporter().disconnect();
	}
	
}
