/**
 * Ono Project
 *
 * File:         PeerFinder.java
 * RCS:          $Id: PeerFinder.java,v 1.17 2010/03/29 16:48:03 drc915 Exp $
 * Description:  PeerFinder class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jun 23, 2006 at 12:49:14 PM
 * Language:     Java
 * Package:      edu.northwestern.ono
 * Status:       Experimental (Do Not Distribute)
 *
 * (C) Copyright 2006, Northwestern University, all rights reserved.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResultPeer;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.download.DownloadTrackerListener;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.peers.PeerManagerListener;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.dns.Digger;
import edu.northwestern.ono.position.OnoPeerManager.OnoPeer;
import edu.northwestern.ono.util.HashMapCache;
import edu.northwestern.ono.util.HashSetCache;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The PeerFinder class ...
 */
public class PeerFinder extends Thread implements DownloadTrackerListener {
	
	public static class PFPeer{

		private String address;
		private int port;
		private int udpPort;
		private short protocol;

		public PFPeer(DownloadAnnounceResultPeer p) {
			this.address =p.getAddress();
			this.port = p.getPort();
			this.udpPort = p.getUDPPort();
			this.protocol= p.getProtocol();
		}

		public PFPeer(Peer peer) {
			this.address = peer.getIp();
			this.port = peer.getTCPListenPort();
			this.udpPort = peer.getUDPListenPort();
			this.protocol = 0;
		}

		public String getAddress() {
			// TODO Auto-generated method stub
			return address;
		}

		public int getPort() {
			// TODO Auto-generated method stub
			return port;
		}

		public int getUDPPort() {
			// TODO Auto-generated method stub
			return udpPort;
		}

		public short getProtocol() {
			// TODO Auto-generated method stub
			return protocol;
		}
		
	}
	
	/** max number of ppers to add each iteration */
	final static int MAX_PEERS = 10;

	/** max number of edges before a set is considered useless */
	final static int MAX_EDGES = 10;

	/** sleep time between attempts to add peers */
	private static final long SLEEP_TIME = 30;

	/** max number of nodes to cache */
	private static final int MAX_NODES = 1000;

	Digger dig;
	Download dl;
	boolean active = true;

	/** nodes added this cycle */
	private HashSet<String> usedNodes;

	/** preferred nodes found */
	private Set<OnoPeer> nodesToAdd;

	/** set of IPs that don't respond to pings */
	private HashSetCache<String> badSet = new HashSetCache<String>(300);

	/** nodes to check */
	private HashSet<PFPeer> nodesToCheck;

	private HashMapCache<String, PFPeer> nodeCache;
	
	private PeerManagerListener pml = null;
	
	private Map<String, String> allIpsSeen;

	/**
	 * @param name
	 */
	public PeerFinder(Digger dig, Download d, String name) {
		super(name);
		this.dig = dig;
		dl = d;        
		usedNodes = new HashSet<String>();
		nodesToAdd = new HashSet<OnoPeer>();
		nodesToCheck = new HashSet<PFPeer>();
		nodeCache = new HashMapCache<String, PFPeer>(MAX_NODES);
		dl.addTrackerListener(this, true);
		allIpsSeen = new HashMap<String, String>();
		

	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		// TODO Auto-generated method stub
		super.run();

		while (!MainGeneric.isShuttingDown()) {

			if (!(dl.getState()==Download.ST_DOWNLOADING 
					|| dl.getState() == Download.ST_SEEDING) ){
				setActive(false);
				return;
			}
			PeerManager pm = dl.getPeerManager();
			if (pm!=null && pml==null){
				pml = new PeerManagerListener(){

					public void peerAdded(PeerManager manager, Peer peer) {
						try {
							if (MainGeneric.isShuttingDown()){
								manager.removeListener(this);
								return;
							}
							OnoPeerManager.getInstance().addPeer(peer.getIp(), 
									peer.getTCPListenPort(), 
									manager.getDownload(), true);
							OnoPeerManager.getInstance().findCluster(peer.getIp());
							OnoPeerManager.getInstance().registerPeerSource(peer.getIp(), OnoPeerManager.SOURCE_PEER_ADDED);							
						} catch (DownloadException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
//						synchronized(nodesToCheck){
//														
//							if (!usedNodes.contains(peer.getIp())){
//								nodesToCheck.add(new PFPeer(peer));
//								nodeCache.put(peer.getIp(), new PFPeer(peer));
//								try {
//									allIpsSeen.put(peer.getIp(), manager.getDownload().getName());
//								} catch (DownloadException e) {
//									// TODO Auto-generated catch block
//									e.printStackTrace();
//								}
//							}
//								
//							
//						}
						
					}

					public void peerRemoved(PeerManager manager, Peer peer) {
						if (MainGeneric.isShuttingDown()){
							manager.removeListener(this);
							return;
						}
						
					}};
					
				pm.addListener(pml);
			}

			// for each peer that should be checked, inform the digger
//			synchronized(nodesToCheck){
//				for (PFPeer p : nodesToCheck)
//					CDNClusterFinder.getInstance().addPeer(dl, p.getAddress(), p.getPort());
//				nodesToCheck.clear();
//			}

			if (MainGeneric.isShuttingDown()) 
				return;
			nodesToAdd = CDNClusterFinder.getInstance().getPreferredPeers(dl);

			
			try {
			if (nodesToAdd!=null){
				LinkedList<Peer> peersToPing = new LinkedList<Peer>();
				int size = nodesToAdd.size();
				if (pm==null) continue;
				// remove any dupes
				for ( Peer p : pm.getPeers()){
					
					if (!nodesToAdd.remove(OnoPeerManager.getInstance().getOnoPeer(p.getIp()))){
						peersToPing.add(p);
						
					}
				}

				/*
				// ping them and some random other ones
				for (OnoPeer peer : nodesToAdd){

					final String peerIp = peer.getIp();

					PingManager.getInstance().doPing(peerIp, MainGeneric.getPluginInterface(), new PingResponse(){

						public void response(double rtt) {
//							 check on validity of ping
							if (rtt>=0){

								Statistics.getInstance().addPingResult("-1", "-1", peerIp, dig.getIp(), rtt, System.currentTimeMillis(), -1);
							}			    	
							else{
//								badSet.add(peerIp);
							}
							
						}});
					
					
				}
				if (size>0){
					int peerCount = 0;
					for (Peer p : peersToPing){
						if (peerCount >= 10) break;
						final String peerIp = p.getIp();

						PingManager.getInstance().doPing(peerIp, MainGeneric.getPluginInterface(), new PingResponse(){

							public void response(double rtt) {
//								 check on validity of ping
								if (rtt>=0){

									Statistics.getInstance().addPingResult("-1", "-1", peerIp, dig.getIp(), rtt, System.currentTimeMillis(), -1);
								}			    	
								else{
//									badSet.add(peerIp);
								}
								
							}});											

						peerCount++;
					}
				}

			*/



			// add the rest
			for (OnoPeer peer : nodesToAdd){
//				String peerIp = peer.getIp();
//				PFPeer p = nodeCache.get(peerIp);
//				if (p==null) continue;
				if (peer.getPort()>1){
					
					// TODO check whether this is successful; if not, remove then add a peer.
					pm.addPeer(peer.getIp(), peer.getPort(), peer.getPort(), 
						peer.getProtocol()==DownloadAnnounceResultPeer.PROTOCOL_CRYPT);
				} else {
					continue;
				}
				boolean added = false;
				if (pm.getPeers(peer.getIp())!=null){
					for (Peer p : pm.getPeers(peer.getIp())){
						if (p.getIp().equals(peer.getIp())) {
							added = true;
							break;
						}
					}	
				}
				if (!added){
					Set<OnoPeer> preferredPeers = CDNClusterFinder.getInstance().getPreferredPeers(dl);
					Peer toRemove = null;
					for (Peer p : pm.getPeers()){
						if (toRemove==null) toRemove = p;
						else {
							if (p.getStats().getDownloadAverage()<=toRemove.getStats().getDownloadAverage()
									&& p.getStats().getUploadAverage()<=toRemove.getStats().getUploadAverage()
									&& !preferredPeers.contains(OnoPeerManager.getInstance().getOnoPeer(p.getIp()))){
								toRemove = p;
							}
						}
					}
					pm.removePeer(toRemove);
					pm.addPeer(peer.getIp(), peer.getPort(), peer.getPort(), 
							peer.getProtocol()==DownloadAnnounceResultPeer.PROTOCOL_CRYPT);
					added = false;
					if (pm.getPeers(peer.getIp())!=null){
						for (Peer p : pm.getPeers(peer.getIp())){
							if (p.getIp().equals(peer.getIp())) {
								added = true;
								break;
							}
						}	
					}
					if (!added){
						pm.addPeer(peer.getIp(), peer.getPort(), peer.getPort(), 
								peer.getProtocol()!=DownloadAnnounceResultPeer.PROTOCOL_CRYPT);
					}
				}
			}
		
			}
			} catch (Exception e){
				//e.printStackTrace();
			}
		// add peer 

//		usedNodes = new HashSet<String>();

//		int peersFound = 0;
//		PeerManager pm = dl.getPeerManager();
//		int startPeerCount = (pm != null) ? pm.getPeers().length : 0;

//		// find peers nearby and add them to download
//		Map<String, LinkedHashSet<String>> myEdgeServers = dig.getMyEdgeServers();
//		TreeMap<Integer, HashSet<String>> sortedList = new TreeMap<Integer, HashSet<String>>();

//		for (HashSet<String> edges : myEdgeServers.values()) {
//		sortedList.put(edges.size(), edges);
//		}

//		// now edge list is sorted in ascending order

//		// this means that edges at the front of the list are more likely 
//		// to be close to this node, since akamai's server diversity 
//		// is typically small for such cases.

//		// TODO clear the cache every so often

//		// next step is to look for overlap with these nearby edges
//		// TODO more sophisticated overlap metric
//		Map<String, LinkedHashSet<String>> edgeServerCache = dig.getEdgeServerCache();

//		for (HashSet<String> edges : sortedList.values()) {
//		if (edges.size() > MAX_EDGES) {
//		break;
//		}

//		if (peersFound > MAX_PEERS) {
//		break;
//		}

//		// for each edge
//		for (String edge : edges) {
//		if (peersFound > MAX_PEERS) {
//		break;
//		}

//		// get the list of its corresponding peers
//		LinkedHashSet<String> peers = edgeServerCache.get(edge);

//		if (peers != null) {
//		synchronized (edgeServerCache) {
//		for (String peer : peers) {
//		if (peersFound > MAX_PEERS) {
//		break;
//		}

//		if ((dig.getPeerPorts().get(peer) == null) ||
//		usedNodes.contains(peer) ||
//		peer.equals(dig.getIp()) ||
//		(dig.getPeersPerDownload()
//		.get(dl.getName()) == null) ||
//		!dig.getPeersPerDownload()
//		.get(dl.getName()).contains(peer)) {
//		continue;
//		}

//		pm.addPeer(peer, dig.getPeerPorts().get(peer),
//		false);
//		usedNodes.add(peer);
//		System.out.println("Added peer: " + peer + ":" +
//		dig.getPeerPorts().get(peer));
//		peersFound++; // TODO better way of tracking this
//		}
//		}
//		}
//		}
//		}

//			CDNClusterFinder.getInstance().updatePeerSet(allIpsSeen);
			
		try {
			Thread.sleep(SLEEP_TIME * 1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} 
		dig = null;
		dl = null;
		nodeCache = null;
		pml = null;
		usedNodes = null;
		nodesToCheck = null;
		nodesToAdd = null;
		allIpsSeen = null;
		badSet = null;
}

/**
 * @return Returns the active.
 */
public boolean isActive() {
	return active;
}

/**
 * @param active The active to set.
 */
public void setActive(boolean active) {
	this.active = active;
	if (dl!=null){
		dl.removeTrackerListener(this);
		if (dl.getPeerManager()!=null){
			dl.getPeerManager().removeListener(pml);
		}		
	}
	// TODO remove listeners
}


public void announceResult(DownloadAnnounceResult result) {
	if (nodesToCheck == null){
		return;
	}
	synchronized(nodesToCheck){		
		if (result.getPeers()!=null){
			for (DownloadAnnounceResultPeer p: result.getPeers()){
				OnoPeerManager.getInstance().addPeer(p.getAddress(), p.getPort(), result.getDownload(), true);
				OnoPeerManager.getInstance().setPeerProtocol(p.getAddress(), p.getProtocol());
				OnoPeerManager.getInstance().registerPeerSource(p.getAddress(), OnoPeerManager.SOURCE_ANNOUNCE);
//				if (!usedNodes.contains(p.getAddress())){
//					nodesToCheck.add(new PFPeer(p));
//					nodeCache.put(p.getAddress(), new PFPeer(p));
//					allIpsSeen.put(p.getAddress(), result.getDownload().getName());
//				}
			}
		}
	}


}

public void scrapeResult(DownloadScrapeResult result) {

}


}
