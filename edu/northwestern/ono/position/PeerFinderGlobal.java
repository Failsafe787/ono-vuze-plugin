/**
 * Ono Project
 *
 * File:         PeerFinder.java
 * RCS:          $Id: PeerFinderGlobal.java,v 1.9 2010/03/29 16:48:03 drc915 Exp $
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
import edu.northwestern.ono.OnoConfiguration;
import edu.northwestern.ono.brp.BRPPeerManager;
import edu.northwestern.ono.brp.BRPPeerManager.BRPPeer;
import edu.northwestern.ono.position.OnoPeerManager.OnoPeer;
import edu.northwestern.ono.util.HashMapCache;
import edu.northwestern.ono.util.HashSetCache;
import edu.northwestern.ono.util.PluginInterface;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The PeerFinder class ...
 */
public class PeerFinderGlobal extends Thread implements DownloadTrackerListener {
	
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

//	Digger dig;
//	Download dl;
	boolean active = true;

	/** nodes added this cycle */
	private HashMap<Download, HashSet<String>> usedNodes =
		new HashMap<Download, HashSet<String>>();

	/** preferred nodes found */
	Set<GenericPeer> nodesToAdd;
	
	/** nodes found by CRP */
	private HashMap<Download, Set<OnoPeer>> onoNodesToAdd = 
		new HashMap<Download, Set<OnoPeer>>();
	
	/** nodes found by BRP*/
	private HashMap<Download, Set<BRPPeer>> BRPNodesToAdd = 
		new HashMap<Download, Set<BRPPeer>>();

	/** set of IPs that don't respond to pings */
	private HashMap<Download, HashSetCache<String>> badSet = new HashMap<Download, HashSetCache<String>>();

	/** nodes to check */
	private HashMap<Download, HashSet<PFPeer>> nodesToCheck = 
		new HashMap<Download, HashSet<PFPeer>>();

	private HashMap<Download, HashMapCache<String, PFPeer>> nodeCache = 
		new HashMap<Download, HashMapCache<String, PFPeer>>();
	
	private HashMap<Download, PeerManagerListener> pmls = new HashMap<Download, PeerManagerListener>();
	
	private HashMap<Download, Map<String, String>> allIpsSeen = 
		new HashMap<Download, Map<String, String>>();

	private PluginInterface pi;

	/**
	 * @param name
	 */
	public PeerFinderGlobal(PluginInterface pi) {
		super("OnoPeerFinder");
		this.pi = pi;		

	}
	
	private void initDownload(Download dl){
		   
		usedNodes.put(dl, new HashSet<String>());
		onoNodesToAdd.put(dl, new HashSet<OnoPeer>());
		nodesToCheck.put(dl, new HashSet<PFPeer>());
		nodeCache.put(dl, new HashMapCache<String, PFPeer>(MAX_NODES));
		badSet.put(dl, new HashSetCache<String>(300));
		dl.addTrackerListener(this, true);
		allIpsSeen.put(dl, new HashMap<String, String>());
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		super.run();

		while (!MainGeneric.isShuttingDown()) {

			if (pi.getDownloadManager()!=null && pi.getDownloadManager().getDownloads()!=null){
				for (Download dl : pi.getDownloadManager().getDownloads()){
					if (!(dl.getState()==Download.ST_DOWNLOADING 
							|| dl.getState() == Download.ST_SEEDING) ){
						continue;
					}
					
					PeerManager pm = dl.getPeerManager();
					if (pm!=null && pmls.get(dl)==null){
						if (dl.getTorrent().isPrivate()) continue;
						pmls.put(dl, new PeerManagerListener(){
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
									
									if (CIDRMatcher.getInstance().match(peer.getIp())){
										CDNClusterFinder.getInstance().addPeer(manager.getDownload(), peer.getIp(), peer.getPort());
									}
									
									if (OnoConfiguration.getInstance().enableBRP) {
										BRPPeerManager.getInstance().addPeer(
												peer.getIp(),
												peer.getTCPListenPort());
										BRPPeerManager.getInstance().registerPeerSource(
												peer.getIp(),
												BRPPeerManager.SOURCE_PEER_ADDED);
										BRPPeerManager.getInstance().registerPeerDownload(
												peer.getIp(),
												manager.getDownload());
									}
								} catch (DownloadException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}

							public void peerRemoved(PeerManager manager, Peer peer) {
								if (MainGeneric.isShuttingDown()){
									manager.removeListener(this);
									return;
								}

								if (CIDRMatcher.getInstance().match(peer.getIp())){
									if (!(peer.isInterested()||peer.isInteresting())){
										CIDRMatcher.getInstance().snub(peer.getIp());
									}
								}
							}});

						pm.addListener(pmls.get(dl));
					}

					if (MainGeneric.isShuttingDown()) 
						return;
				
                    
					onoNodesToAdd.put(dl, CDNClusterFinder.getInstance().getPreferredPeers(dl));
					BRPNodesToAdd.put(dl, BRPPeerManager.getInstance().getPreferredPeers(dl));
					
					nodesToAdd = new HashSet<GenericPeer>();
					nodesToAdd.addAll(onoNodesToAdd.get(dl));
					nodesToAdd.addAll(BRPNodesToAdd.get(dl));
					
					
					if (BRPNodesToAdd.get(dl).size() > 0) {
						BRPPeerManager.getInstance().BRPLog("BRP preferred peers:");
						for (BRPPeer p : BRPNodesToAdd.get(dl)) {
							BRPPeerManager.getInstance().BRPLog("Peer " + p.ip + " has cossim " + 
									Double.toString(p.cossim));
						}
					}
					
					// also add all those from CIDR matching
					int numConnections = -1;
					int addedPeers = 0;
					try {
						if (nodesToAdd.size() > 0){
							LinkedList<Peer> peersToPing = new LinkedList<Peer>();
							if (pm==null) continue;
							// remove any dupes
							numConnections = pm.getPeers().length;
							for ( Peer p : pm.getPeers()){
								if (p.isInterested() || p.isInteresting())
									CIDRMatcher.getInstance().updateLastTimeSeen(p.getIp());
								else if (CIDRMatcher.getInstance().match(p.getIp()) && 
										!CIDRMatcher.getInstance().usedLately(p.getIp())) pm.removePeer(p);
								if (!(nodesToAdd.remove(OnoPeerManager.getInstance().getOnoPeer(p.getIp())) ||
										nodesToAdd.remove(BRPPeerManager.getInstance().getBRPPeer(p.getIp())))) {
									peersToPing.add(p);
									
								} else addedPeers++; // was already in nodes to add
							}

							// add the rest
							int maxPeersToAdd = (int)Math.floor(numConnections/2.0);								
							for (GenericPeer peer : nodesToAdd){
								if (addedPeers >= maxPeersToAdd) break;
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
									Set<GenericPeer> preferredPeers = new HashSet<GenericPeer>();
									preferredPeers.addAll(CDNClusterFinder.getInstance().getPreferredPeers(dl));
									preferredPeers.addAll(BRPPeerManager.getInstance().getPreferredPeers(dl));
									Peer toRemove = null;
									for (Peer p : pm.getPeers()){
										if (toRemove==null) toRemove = p;
										else {
											if (p.getStats().getDownloadAverage()<=toRemove.getStats().getDownloadAverage()
													&& p.getStats().getUploadAverage()<=toRemove.getStats().getUploadAverage()
													&& !
													(preferredPeers.contains(OnoPeerManager.getInstance().getOnoPeer(p.getIp())) ||
													preferredPeers.contains(BRPPeerManager.getInstance().getBRPPeer(p.getIp())))) {
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
									addedPeers++;
								}
							}
						}
					} catch (Exception e){
						e.printStackTrace();
					}
				} // end for each download
			} // end if the download managing objects are not null
			else {
				pi = MainGeneric.getPluginInterface();
			}
			try {
				Thread.sleep(SLEEP_TIME * 1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 

		nodeCache = null;
		pmls = null;
		usedNodes = null;
		nodesToCheck = null;
		nodesToAdd = null;
		onoNodesToAdd = null;
		BRPNodesToAdd = null;
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
		if (pmls!=null && pmls.keySet()!=null){
		for (Download dl : pmls.keySet()){
			if (dl!=null){
				dl.removeTrackerListener(this);
				if (dl.getPeerManager()!=null){
					dl.getPeerManager().removeListener(pmls.get(dl));
				}		
			}
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
					
					if (CIDRMatcher.getInstance().match(p.getAddress())){
						CDNClusterFinder.getInstance().addPeer(result.getDownload(), p.getAddress(), p.getPort());
						// TODO put a timer on these peers and remove them if not seen 
						// in past 15' AND not used in past 15'
					}
//					if (!usedNodes.contains(p.getAddress())){
//					nodesToCheck.add(new PFPeer(p));
//					nodeCache.put(p.getAddress(), new PFPeer(p));
//					allIpsSeen.put(p.getAddress(), result.getDownload().getName());
//					}

					if (OnoConfiguration.getInstance().enableBRP) {
						BRPPeerManager.getInstance().addPeer(p.getAddress(), p.getPort());
						BRPPeerManager.getInstance().registerPeerSource(p.getAddress(),
								BRPPeerManager.SOURCE_ANNOUNCE);
						BRPPeerManager.getInstance().registerPeerDownload(p.getAddress(),
								result.getDownload());
					}
				}
			}
		}


	}

	public void scrapeResult(DownloadScrapeResult result) {

	}


}
