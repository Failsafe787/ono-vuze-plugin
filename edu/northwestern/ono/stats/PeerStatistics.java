/**
 * Ono Project
 *
 * File:         PeerStats.java
 * RCS:          $Id: PeerStatistics.java,v 1.52 2010/03/29 16:48:04 drc915 Exp $
 * Description:  PeerStats class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Apr 10, 2007 at 12:38:43 PM
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Map.Entry;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerEvent;
import org.gudy.azureus2.plugins.peers.PeerListener2;
import org.gudy.azureus2.plugins.peers.PeerStats;

import com.azureus.plugins.aznetmon.main.RSTPacketStats;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.OnoConfiguration;
import edu.northwestern.ono.brp.BRPPeerManager;
import edu.northwestern.ono.brp.BRPPeerManager.BRPPeer;
import edu.northwestern.ono.experiment.TraceRouteRunner;
import edu.northwestern.ono.position.OnoPeerManager;
import edu.northwestern.ono.position.OnoPeerManager.OnoPeer;
import edu.northwestern.ono.timer.ITimerEvent;
import edu.northwestern.ono.timer.ITimerEventPerformer;
import edu.northwestern.ono.util.HashMapCache;
import edu.northwestern.ono.util.PingManager;
import edu.northwestern.ono.util.Util;
import edu.northwestern.ono.util.Util.PingResponse;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The PeerStats class represents general data collected for a peer.
 */
public class PeerStatistics implements ITimerEventPerformer{
	
	/** Download rate update interval, in ms */
	private static int RATE_UPDATE_INTERVAL = 5*1000;
	
	/** Peer statistics update interval, in ms */
	private static int PEER_UPDATE_INTERVAL = 30*1000;

	/** max number of dynamic local peer stat entries to cache */
	public static final int MAX_DLPS_ENTRIES = 100;

	/** max number of dynamic (remote) peer stat entries to cache */
	public static final int MAX_DPS_ENTRIES = 100;

	private static final int MAX_PEER_ENTRIES = 200;

	/** max time between reporting peer data */
	private static final long PEER_REPORT_INTERVAL = 30*60*1000;

	/** max time between reporting static data */
	private static final long STATIC_DATA_FLUSH_INTERVAL = 60*60*1000;
	
	/** 
	 * The PeerStaticData class represents data that needs 
	 * to be reported only once during a peer's lifetime.
	 */
	public static class StaticPeerStat {
		final Peer peer;
		int peerId;
		long currentLifetime;
		ArrayList<DynamicPeerStat> dynamicStats = new ArrayList<DynamicPeerStat>();
		private int tcpPort;
		private int udpPort;
		private boolean sameCluster = false;
		private long firstSeen;
		private long lastTraceroute = -1;
		private int peerClient;
		private boolean done = false;
		private boolean sameBRPCluster = false;
		
		
		public StaticPeerStat(Peer p) {
			this.peer = p;
			this.peerClient = -1;
			this.peerId = -1;
			this.currentLifetime = p.getStats().getTimeSinceConnectionEstablished();
			this.firstSeen = Util.currentGMTTime() - currentLifetime;			
			this.tcpPort = p.getTCPListenPort();
			this.udpPort = p.getUDPListenPort();
			p.addListener(new PeerListener2 (){

				public void eventOccurred(PeerEvent event) {
					if (event.getType()==PeerEvent.ET_STATE_CHANGED){
						if (peer.getState()==Peer.DISCONNECTED){
							currentLifetime = Util.currentGMTTime()-firstSeen;
							stop();
						}
					}
					
				}});
			if (OnoConfiguration.getInstance().isDoTraceroute()
					&& !MainGeneric.isShuttingDown()){
				TraceRouteRunner.getInstance().addIp(p.getIp(), true);
			}
			
		}

		public void updateStats(){
			PeerStats ps = peer.getStats();
			final DynamicPeerStat stat = new DynamicPeerStat();
			stat.bytesSentToPeer = (int) ps.getTotalSent();
			stat.bytesReceivedFromPeer = (int) ps.getTotalReceived();
			stat.currentSendRate = ps.getUploadAverage();
			stat.currentReceiveRate = ps.getDownloadAverage();
			stat.isSeed = peer.isSeed();
			stat.rttPending = true;
			stat.isInterested = peer.isInterested();
			stat.isInteresting = peer.isInteresting();
			stat.discarded = peer.getStats().getTotalDiscarded();
			
//			List	l = peer.getRequests();
//			
//			int[]	res = new int[l.size()];
//			
//			for (int i=0;i<l.size();i++){
//				
//				res[i] = ((PeerReadRequest)l.get(i))..getPieceNumber();
//			}
			if (!MainGeneric.isShuttingDown()) 
				PingManager.getInstance().doPing(peer.getIp(), MainGeneric.getPluginInterface(), new PingResponse(){

				public void response(double rtt) {
					if (rtt==-1){
						//System.err.println("Ping failed to " + peer.getIp() + "!");						
					} else {
						//System.out.println("Ping succeeded!");
					}
					stat.ping = rtt;
					stat.rttPending = false;
					
				}});
			
			if (dynamicStats.size()>MAX_DPS_ENTRIES){
				flush();
			}
			synchronized (dynamicStats){
				if (dynamicStats.size()>MAX_DPS_ENTRIES) dynamicStats.remove(0);
				dynamicStats.add(stat);
			}
		}

		public void setSameCluster(boolean sc){
			this.sameCluster = sc;
		}
		
		public void setSameBRPCluster(boolean sc) {
			this.sameBRPCluster = sc;
		}

		/**
		 * Report dynamic data.
		 *
		 */
		public void flush() {
			if (peerId == -1 ){
				peerId = Statistics.getInstance().getPeerId(peer.getIp());
			}
			
			synchronized (dynamicStats){
				LinkedList<DynamicPeerStat> toKeep = new LinkedList<DynamicPeerStat>();
				for (DynamicPeerStat dps : dynamicStats){
					if (dps.rttPending){
						toKeep.add(dps);
						continue;
					}
					if (OnoConfiguration.getInstance().isSendToDatabase()){
						MainGeneric.getReporter().insertRemotePeerStatDynamic(
								Statistics.getInstance().getSelfId(), 
								peerId, dps.currentTime, dps.bytesReceivedFromPeer, 
								dps.bytesSentToPeer, dps.currentReceiveRate, 
								dps.currentSendRate, dps.isSeed, dps.ping, 
								dps.isInterested, dps.isInteresting, 
								(int)dps.discarded);
					}
				}
				
				dynamicStats.clear();
				dynamicStats.addAll(toKeep);
			}
			
		}

		/**
		 * Report once-per-session data.
		 *
		 */
		public void stop() {
//			/HashMap<String, StaticPeerStat> pd = PeerStatistics.getInstance().;
			done  = true;
			
		}

		public boolean isDone() {		
//			if (!done && (peer.getState() == Peer.DISCONNECTED || 
//				peer.getState()== Peer.CLOSING)){
//				System.out.println("Didn't catch closed connection!");
//			}
			return done || 
				peer.getState() == Peer.DISCONNECTED || 
				peer.getState()== Peer.CLOSING;
			
		}
	
	}
	
	/**
	 * Dynamic peer statistics
	 * 
	 */
	public static class DynamicPeerStat {
		
		public long discarded;
		public boolean isInteresting;
		public boolean isInterested;
		public boolean rttPending;
		protected double ping = -2;
		long currentTime = Util.currentGMTTime();
		int bytesSentToPeer;
		int bytesReceivedFromPeer;
		int currentSendRate;
		int currentReceiveRate;
		boolean isSeed= false;

	}
	
	public static class StaticLocalPeerStat {
		/** in seconds */
		public long uptime; 
		public int maxUploadRate = -1;
		public int maxDownloadRate = -1;

		ArrayList<DynamicLocalPeerStat> stats = new ArrayList<DynamicLocalPeerStat>();
		
		public void update() {
			if (maxUploadRate < MainGeneric.getPluginInterface().getDownloadManager().getStats().getDataSendRate()){
				maxUploadRate = MainGeneric.getPluginInterface().getDownloadManager().getStats().getDataSendRate();
			}
			if (maxDownloadRate < MainGeneric.getPluginInterface().getDownloadManager().getStats().getDataReceiveRate()){
				maxDownloadRate = MainGeneric.getPluginInterface().getDownloadManager().getStats().getDataReceiveRate();
			}
			
			uptime = MainGeneric.getPluginInterface().getDownloadManager().getStats().getSessionUptimeSeconds();
		}

		/**
		 * Periodic flushing of accumulated data.
		 *
		 */
		public void flush() {			
			
			synchronized (stats){
				LinkedList<DynamicLocalPeerStat> toRemove = new LinkedList<DynamicLocalPeerStat>();
				for (DynamicLocalPeerStat dlps : stats){
					if (OnoConfiguration.getInstance().isSendToDatabase()){
							MainGeneric.getReporter().insertLocalPeerStatDynamic(
							Statistics.getInstance().getSelfId(), 
							dlps.currentTime, dlps.uploadRate, dlps.downloadRate, 
							dlps.maxDownloadRate, dlps.maxUploadRate, dlps.autoSpeed, dlps.rst, 
							dlps.numConnections, dlps.passiveOpens, dlps.activeOpens, dlps.failedConns);
//							){
//						break;
//					} else {
//						toRemove.add(dlps);
					}
				}
				stats.clear();
			}
			
			
		}

		/**
		 * Reporting of data that is updated once per session.
		 *
		 */
		public void stop() {
			if (!MainGeneric.isAzureus()) return;
			// TODO check that this reports correctly when app is closed
			if (OnoConfiguration.getInstance().isSendToDatabase()) {
				long sessionTime = -1;
				try {
					sessionTime = MainGeneric.getPluginInterface().getDownloadManager().getStats().getSessionUptimeSeconds();
				} catch (Exception e){
					e.printStackTrace();
				}
				MainGeneric.getReporter().insertLocalPeerStatStatic(Statistics.getInstance().getSelfId(), 			
					sessionTime, 
					maxUploadRate, maxDownloadRate);
			}
			flush();
			//stats = null;
			
		}

		public void add(DynamicLocalPeerStat dlps) {			
			if (stats.size()>=MAX_DLPS_ENTRIES){
				flush();
				MainGeneric.getReporter().disconnect();
			}
			synchronized(stats){
				if (stats.size()>MAX_DLPS_ENTRIES) stats.remove(0);
				stats.add(dlps);
			}
		}

		public void flushStatic() {
			if (OnoConfiguration.getInstance().isSendToDatabase()) {
				MainGeneric.getReporter().insertLocalPeerStatStatic(Statistics.getInstance().getSelfId(), 			
					MainGeneric.getPluginInterface().getDownloadManager().getStats().getSessionUptimeSeconds(), 
					maxUploadRate, maxDownloadRate);
			}
			
		}
	}
	
	public static class DynamicLocalPeerStat{
		long currentTime = Util.currentGMTTime();
		int uploadRate;
		int downloadRate;
		public int maxUploadRate;
		public int maxDownloadRate;
		public boolean autoSpeed;
		public int rst;
		public int numConnections;
		public int passiveOpens;
		public int activeOpens;
		public int failedConns;
	}
	
	private static PeerStatistics self;
	private static HashMapCache<String, StaticPeerStat> peerData;
	/** the (database) id for this peer */
	private static int selfId = -1;
	/** local peer stats */
	private static StaticLocalPeerStat myStats;
	/** last update time */
	private static long lastUpdateTime = -1;
	private static long lastStaticFlushTime = -1;

	private static long lastFlushTime;

	/**
	 * Creats a new PeerStatistics object
	 *
	 */
	private PeerStatistics(){
		peerData = new HashMapCache<String, StaticPeerStat>(400);
		myStats = new StaticLocalPeerStat();
	}
	
	/**
	 * Gets a PeerStatistics object.
	 * @return
	 */
	public static PeerStatistics getInstance(){
		if (self == null){
			self = new PeerStatistics();
		}
		return self;
	}
	
	/**
	 * Retrieves stat data for the specified peer
	 * @param p the peer to get data from
	 * @return
	 */
	private static StaticPeerStat getPeerData(Peer p){
		StaticPeerStat psd;
		psd = getInstance().peerData.get(p.getIp());
		if (psd==null) {
			psd = new StaticPeerStat(p);
			synchronized(peerData) {
				peerData.put(p.getIp(), psd);
			}
		}
		
		if (peerData.size()>MAX_PEER_ENTRIES || System.currentTimeMillis()-lastFlushTime > PEER_REPORT_INTERVAL){
			synchronized (peerData){
				
				Set<String> toRemove = new HashSet<String>();
				
				// get ids in one batch
				for (Entry<String, StaticPeerStat> ent : peerData.entrySet()){
					Statistics.getInstance().addPeerForLookup(ent.getKey());
					Statistics.getInstance().addPeerClientForLookup(ent.getValue().peer.getClient());
				}
				Statistics.getInstance().batchProcessIds();
	
				for (Entry<String, StaticPeerStat> sps : peerData.entrySet()){
					if (sps.getValue().isDone()){

						reportStaticPeerStat(sps.getValue());

//						// remove this guy since he is done
//						peerData.remove(peer.getIp());

						toRemove.add(sps.getKey());
					}
					
				}
				
				for (String key: toRemove) peerData.remove(key);
			}
			lastFlushTime=System.currentTimeMillis();
			MainGeneric.getReporter().disconnect();
		}
		
		
		return psd;
	}

	/**
	 * Implements the periodic action, gets peer data.
	 */
	public void perform(ITimerEvent event) {		
		
		if (MainGeneric.isShuttingDown()){			
			if (event!=null) event.cancel();
			return;
		}
		
		// TODO this won't work w/ more than two intervals--must be craftier about 
		// "last update time".
		if (Util.currentGMTTime()-lastUpdateTime >= RATE_UPDATE_INTERVAL){
			myStats.update(); // note that this updates only, does not add data
			
			// TODO add updates for all peers being monitored
		}
		
		if (Util.currentGMTTime()-lastStaticFlushTime >= STATIC_DATA_FLUSH_INTERVAL){
			myStats.flushStatic();
			if (lastStaticFlushTime>0) MainGeneric.getReporter().updateConfig();
			lastStaticFlushTime = Util.currentGMTTime();
			
			Statistics.getInstance().batchProcessIds();
			
			String keys[] = peerData.keySet().toArray(new String[]{});
			synchronized(peerData){
				for (String peerIp : keys){
					StaticPeerStat sps = peerData.get(peerIp);
					if (sps!=null && sps.isDone()){
						peerData.remove(peerIp);
						reportStaticPeerStat(sps);
						
					}
				}
			}
		

		}
		
		if (Util.currentGMTTime()-lastUpdateTime >= 
				OnoConfiguration.getInstance().getPeerUpdateIntervalSec()*1000){
		
			//	collect stats about peer running plugin
			int totalUploadRate=0;
			int totalDownloadRate=0;
			int totalConnections=0;
			int maxDownloadRate = -1;
			int maxUploadRate = -1;
			boolean autoSpeedActive = false;
			
			// TODO limits on number of peers to collect data from
			DownloadManager dm = MainGeneric.getPluginInterface().getDownloadManager();
			totalUploadRate = dm.getStats().getDataSendRate();
			totalDownloadRate = dm.getStats().getDataReceiveRate();		

			try {
				maxDownloadRate = COConfigurationManager
					.getIntParameter("Max Download Speed KBs");
				maxUploadRate = COConfigurationManager
					.getIntParameter("Max Upload Speed KBs");
				
				autoSpeedActive = checkAutoSpeed();

			} catch (Exception e){
			 // some problem w/ getting parameters...
			} catch (Error e ){
				// same thing, particularly if classes change under us...
			}
			
			Set<BRPPeer> brpClose = BRPPeerManager.getInstance().getAllPreferredPeers();
			
			Download dls[] = dm.getDownloads();
			for (Download d : dls){		
				if (!(d.getState()==Download.ST_DOWNLOADING 
						|| d.getState() == Download.ST_SEEDING)) continue;
				if (d.getPeerManager()==null) continue;
				Peer peers[] = d.getPeerManager().getPeers();
				totalConnections += peers.length;
//				System.out.println("Dumping peer data!");
				MainGeneric.getReporter().setExtendedConnection(true);
				for (Peer p : peers){
					OnoPeerManager.getInstance().addPeer(p.getIp(), 
							p.getTCPListenPort(), d, true);
					
					//Digger.getInstance().addPeerIpForDigging(p.getIp());
					getPeerData(p).updateStats();
					// TODO check 
					OnoPeer op = OnoPeerManager.getInstance().getOnoPeer(p.getIp());
					getPeerData(p).setSameCluster(op==null?false:op.isSameCluster());
					if (brpClose.contains(BRPPeerManager.getInstance().getBRPPeer(p.getIp())))
						getPeerData(p).setSameBRPCluster(true);
					else getPeerData(p).setSameBRPCluster(false);
				}
//				System.out.println("Done dumping peer data!");
				MainGeneric.getReporter().setExtendedConnection(false);
				MainGeneric.getReporter().disconnect();
			}
			
			myStats.uptime = dm.getStats().getSessionUptimeSeconds();
			DynamicLocalPeerStat dlps = new DynamicLocalPeerStat();
			dlps.downloadRate = totalDownloadRate;
			dlps.uploadRate = totalUploadRate;
			dlps.maxUploadRate = maxUploadRate;
			dlps.maxDownloadRate = maxDownloadRate;
			dlps.autoSpeed = autoSpeedActive;
			try {				
				dlps.rst = RSTPacketStats.getInstance().getMostRecent().nConnReset;
				dlps.numConnections = RSTPacketStats.getInstance().getMostRecent().nCurrentOpen;
				dlps.passiveOpens = RSTPacketStats.getInstance().getMostRecent().nPassiveOpens;
				dlps.activeOpens = RSTPacketStats.getInstance().getMostRecent().nActiveOpens;
				dlps.failedConns = RSTPacketStats.getInstance().getMostRecent().nFailedConnAttempt;
				
//				System.out.println("RST, found: "+RSTPacketStats.getInstance().getMostRecent().deltaConnReset);
				} catch (Exception e){
					// do nothing
					dlps.rst = -1;
					dlps.numConnections = -1;
					dlps.passiveOpens = -1;
					dlps.activeOpens = -1;
					dlps.failedConns = -1;
				}
			
			myStats.add(dlps);
			
			lastUpdateTime = Util.currentGMTTime();
			
			// poll for vivaldi data
			
		}
		
		
		
	}

	private boolean checkAutoSpeed() {
		String config = "";
		if ( COConfigurationManager.getBooleanParameter(TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY)){
    		
			config = ( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY );
    	} else {
    	
	    	if ( MainGeneric.getPluginInterface().getDownloadManager().isSeedingOnly()){
	        	
	    		config = ( TransferSpeedValidator.AUTO_UPLOAD_SEEDING_ENABLED_CONFIGKEY );
	        	
	      	}else{
	      		
	      		config = ( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY );
	      	}
    	}
    	
    	return COConfigurationManager.getBooleanParameter(config);
	}

	/**
	 * 
	 * @return time, in ms, between executions of the perform method
	 */
	public static long getInterval() {
		return Math.min(PEER_UPDATE_INTERVAL, RATE_UPDATE_INTERVAL);
	}

	/**
	 * Sends data back to the db.
	 *
	 */
	public void flush() {
		
		if (!MainGeneric.isAzureus()) return;
		perform(null);
		
		// flush local data
		myStats.flush();
		
		
		// flush other peer data
		synchronized (peerData){
			String keys[] = peerData.keySet().toArray(new String[]{});
			
			// get ids in one batch
			for (Entry<String, StaticPeerStat> ent : peerData.entrySet()){
				Statistics.getInstance().addPeerForLookup(ent.getKey());
				Statistics.getInstance().addPeerClientForLookup(ent.getValue().peer.getClient());
			}
			Statistics.getInstance().batchProcessIds();
			
			for (String peerIp : keys){
				StaticPeerStat sps = peerData.get(peerIp);
				sps.flush();
				peerData.remove(sps);
			}
		}
		
	}
	
	/**
	 * Indicates that "static" data should be reported.
	 *
	 */
	public void stop(){
		myStats.stop();
		
		// flush other peer data
		String keys[] = peerData.keySet().toArray(new String[]{});
		for (String peerIp : keys){
			StaticPeerStat sps = peerData.get(peerIp);
			if (sps!=null) sps.stop();
		}
		synchronized (peerData) {
			peerData.clear();
		}
		peerData = null;
		myStats = null;
		self = null;
	}

	public static void setRecordingInterval(int interval) {
		PEER_UPDATE_INTERVAL = interval;
		
	}
	
	private static void reportStaticPeerStat(StaticPeerStat sps) {		
		// one last try (3 is the "" string)
		if (sps.peerClient == -1 || sps.peerClient == 3 ) {
			sps.peerClient = Statistics.getInstance().getPeerClientId(sps.peer.getClient());
		}
		if (sps.peerId==-1){
			sps.peerId = Statistics.getInstance().getPeerId(sps.peer.getIp());
		}
//						if (!sps.sameCluster) {
//							NewsPeer op = NewsPeerManager.getInstance().getNewsPeer(p.getIp());
//							psd.setSameCluster(op==null?false:op.isSameCluster());
//						}
		//			 TODO check that this reports correctly when app is closed
		if (OnoConfiguration.getInstance().isRecordStats()) {
			MainGeneric.getReporter().insertRemotePeerStatStatic(Statistics.getInstance().getSelfId(), 
					sps.peerId, 				
					sps.peerClient, sps.currentLifetime, sps.tcpPort, 
					sps.udpPort, sps.sameCluster, sps.sameBRPCluster);
		}

		sps.flush();

//						// remove this guy since he is done
//						peerData.remove(peer.getIp());

		// do another traceroute if at least 5 minutes passed
		if (MainGeneric.isShuttingDown()) return;
		if (sps.sameCluster || sps.sameBRPCluster || Util.currentGMTTime()-sps.firstSeen > 5*60*1000){
			TraceRouteRunner.getInstance().addIp(sps.peer.getIp(), true, 
					sps.sameCluster || sps.sameBRPCluster);
			//sps.getValue().lastTraceroute = Util.currentGMTTime();
		}
	}


}
