/**
 * Ono Project
 *
 * File:         CDNClusterFinder.java
 * RCS:          $Id: CDNClusterFinder.java,v 1.47 2010/03/29 16:48:03 drc915 Exp $
 * Description:  CDNClusterFinder class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 17, 2007 at 10:37:57 AM
 * Language:     Java
 * Package:      edu.northwestern.ono.position
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
package edu.northwestern.ono.position;

import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.MessageManagerListener;
import org.gudy.azureus2.plugins.network.Connection;
import org.gudy.azureus2.plugins.network.IncomingMessageQueueListener;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.torrent.TorrentManager;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.dns.Digger;
import edu.northwestern.ono.messaging.OnoRatioMessage;
import edu.northwestern.ono.position.OnoPeerManager.OnoPeer;
import edu.northwestern.ono.stats.DownloadStats;
import edu.northwestern.ono.stats.EdgeServerRatio;
import edu.northwestern.ono.util.HashSetCache;
import edu.northwestern.ono.util.PluginInterface;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The CDNClusterFinder class takes a list of peers and
 * feeds cluster-based information into other components.
 */
public class CDNClusterFinder extends Thread implements MessageManagerListener, IncomingMessageQueueListener {
	
	public static class OnoPeerData {
		public InetSocketAddress sock;
		public Object key;
		public double cosSim;
		public String customerName;
		public OnoPeerData(InetSocketAddress sock, Object key, double cosSim, String customerName) {
			this.sock = sock;
			this.key = key;
			this.cosSim = cosSim;
			this.customerName = customerName;
		}
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof OnoPeerData){
				OnoPeerData opd = (OnoPeerData) obj;
				if (opd.key==key &&opd.customerName==customerName){
					if (opd.sock.getAddress().equals(sock.getAddress()))
						return true;
				}
			}
			return false;
		}
		@Override
		public int hashCode() {
			return customerName.hashCode()+key.hashCode()+sock.getAddress().hashCode();
		}
		@Override
		public String toString() {
		
			return "["+sock+": "+cosSim + "("+ key+", "+ customerName +")]";
		}
		
		
		
		
	}
	
    private static final long SLEEP_TIME = 15*1000;
	private static CDNClusterFinder self;
    private HashMap<Object, Set<OnoPeer>> clusterSources;
    private Map<String, EdgeServerRatio> myRatios;
    private Map<Object,Set<OnoPeer>> preferredPeers;
    private Map<String, Set<OnoPeer>> allPreferredPeers;
    public static final double COSINE_THRESHOLD = 0.15;
	public static final String CIDR_KEY = "Manual whitelist";
	private String myIp;
	/** the domain name to use for mapping */
	private String preferredName = null;
	private ArrayList<NeighborhoodListener> listeners;
	Set<String> goodNames;
	private PluginInterface pi;
	private static boolean active = true;

    private CDNClusterFinder() {
        clusterSources = new HashMap<Object, Set<OnoPeer>>();
        preferredPeers = new HashMap<Object, Set<OnoPeer>>();
        allPreferredPeers = new HashMap<String, Set<OnoPeer>>();
        listeners = new ArrayList<NeighborhoodListener>();
        pi = MainGeneric.getPluginInterface();
        
        if (MainGeneric.isAzureus()){
	        try {        	
				pi.getMessageManager().registerMessageType(new OnoRatioMessage());
			} catch (MessageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			pi.getMessageManager().locateCompatiblePeers(pi, new OnoRatioMessage(), this);	
        }
       
    }

    public synchronized static CDNClusterFinder getInstance() {
        if (self == null && !MainGeneric.isShuttingDown()) {
        	active = true;
            self = new CDNClusterFinder();
            self.setDaemon(true);
            self.setName("CDN Cluster Finder");
            self.start();  
            
        }

        return self;
    }

    public void addPeer(Object o, String peerIp, int port) {
    	if (myIp == null && Digger.getInstance().getIp()!=null){
    		myIp = Digger.getInstance().getIp();    		
    	}
    	if (myIp.equals(peerIp)) return;
        if (clusterSources.get(o) == null) {
            clusterSources.put(o, new HashSet<OnoPeer>());
        }

        synchronized (clusterSources.get(o)) {
         
        	if (port<=1){        	
        		port = 0;
        	}
			clusterSources.get(o).add(OnoPeerManager.getInstance().getOnoPeer(peerIp));
			
        }
    }

    @Override
    public void run() {
        // get ratios
        while (myRatios == null) {
            myRatios = OnoPeerManager.getInstance().getMyRatios();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // do edge-ratio lookups for each peer here
        Set<Object> keys;
        long startTime, sleepTime;
        while (active) {                
        	
        	startTime = System.currentTimeMillis();
        	
        	// get preferred CDN names
        	goodNames = getGoodCdnNames();
        	
        	// update parameters sent to tracker
        	updateAnnounce();
        	
        	
            keys = new HashSet<Object>(clusterSources.keySet());

            // for each key
            try {
	            for (Object key : keys) {
	            	if (key==null || clusterSources.get(key)==null) continue;
	                synchronized (clusterSources.get(key)) {
	                	// for each peer
	                    for (OnoPeer p : clusterSources.get(key)) {
	                        if (p==null) continue;
	                    	doLookup(p, key);
	                    }
	                }
	            }
            } catch (Exception e){
            	e.printStackTrace();
            }
            
            // determine sleep time and sleep
            sleepTime = (SLEEP_TIME) - (System.currentTimeMillis()-startTime);
            if (sleepTime>0){
            	try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            
        } // end while alive
        self = null;
        
    } // end run

    private void updateAnnounce() {
    	if (!MainGeneric.isAzureus()) return;  
    	PluginInterface pi = MainGeneric.getPluginInterface();
    	
    	TorrentManager tm = pi.getTorrentManager();
    	TorrentAttribute ta_tracker_extensions = tm.getAttribute( TorrentAttribute.TA_TRACKER_CLIENT_EXTENSIONS );
    	  
    	for (Download d : pi.getDownloadManager().getDownloads()){
    		if (d.getState()==Download.ST_DOWNLOADING || d.getState()== Download.ST_SEEDING || d.getState()==Download.ST_QUEUED){
    			String t_ext = d.getAttribute( ta_tracker_extensions );
    			if (t_ext==null) t_ext = "";
    			  String[] bits = t_ext.split("&");
    			  t_ext = "";
    			  for ( int i=0; i<bits.length; i++ ){
    			      String bit = bits[i].trim();
    			      if ( bit.length() == 0 ){
    			          continue;
    			      }
    			      if ( !bit.startsWith( "ono=" )){
    			    	  t_ext += "&" + bit;
    			      }
    			  }
    			  
    			  t_ext += "&ono=" + encodeESROno(myRatios);
    			  
    			  d.setAttribute(ta_tracker_extensions, t_ext);
    		}
    	}
		
	}

	private String encodeESROno(Map<String, EdgeServerRatio> ratios) {
		String value = "";
		NumberFormat nf = DecimalFormat.getNumberInstance();
		nf.setMaximumFractionDigits(5);
		value+="d";
		for (Entry<String, EdgeServerRatio> ent : ratios.entrySet()){
			value+=ent.getKey().length()+":"+ent.getKey();
			value += "d";
			for (Entry<String, Double> ratio : ent.getValue().getEntries()){
				if (ratio.getValue()>COSINE_THRESHOLD){
					value += ratio.getKey().length()+":"+ratio.getKey();
					String r = nf.format(ratio.getValue());
					value+= r.length()+":"+r;
				}
			}
			value+="e";
		}
		value+="e";
		return value;
	}


	private Set<String> getGoodCdnNames() {
		return OnoPeerManager.getInstance().getGoodCDNNames();
	
	}

	private void doLookup(OnoPeer p, Object key) {
        // TODO make sure that Digger isn't interfering with these values
        // add to lookups only if it's not using Ono
        boolean isOno = false;

        // TODO add message handler for Ono notification
//        for (org.gudy.azureus2.plugins.messaging.Message m : p.getSupportedMessages()) {
//            if (m.getDescription().equals("Ono data message")) {
//                isOno = true;
//
//                break;
//            }
//        }

        // now i think that i don't want this since ono standalones 
        // are already digging
//        if (!isOno) {
//            Digger.getInstance().addPeerIpForDigging(p.getAddress().getHostAddress());
//        }
        if (p==null) return;
        Map<String, EdgeServerRatio> ratios = OnoPeerManager.getInstance()
                                                    .getRatios(p.getIp(), true);
        
        
        boolean isClose = false;
        // look for cosine similarity
        

        // TODO Then add code to PeerFinderGlobal to check the CIDR key in addition to the download key
        // TODO
        if (ratios != null) {
            for (Entry<String, EdgeServerRatio> e : ratios.entrySet()) {
            	if ( (goodNames==null || goodNames.contains(e.getKey())) ){
	                if (myRatios.get(e.getKey()) != null /*&& (preferredPeers.get(key)==null || !preferredPeers.get(key).contains(p))*/) {
	                   double cosSim =myRatios.get(e.getKey()).cosineSimilarity(e.getValue());
	                   p.setCosSim(e.getKey(), cosSim);
	                   if (cosSim > COSINE_THRESHOLD) {
	                	   
	                	   if (preferredName==null || preferredName.equals("") || e.getKey().equals(preferredName)){
	                        if (preferredPeers.get(key)==null){
	                        	preferredPeers.put(key, new HashSetCache<OnoPeer>(100));
	                        	if (e.getKey().equals(preferredName) && allPreferredPeers.get(e.getKey())!=null){
	                        		preferredPeers.get(key).addAll(allPreferredPeers.get(e.getKey()));
	                        	}
	                        }
	                    	preferredPeers.get(key).add(p);	   
	                	   }
	                	   
	                	   if (allPreferredPeers.get(e.getKey())==null){
	                		   allPreferredPeers.put(e.getKey(), new HashSetCache<OnoPeer>(500));
	                        }
	                    	allPreferredPeers.get(e.getKey()).add(p);
	                    	
	                    	synchronized(this){
	                    		for (NeighborhoodListener l: listeners){
	                    			
	                    			l.foundNewNeighborhoodPeer(p);
	                    		}
	                    	}
	                    	isClose = true;
	                    	break;
	                    }
	                }
            	}
            }
        }
        if (!isClose && !CIDRMatcher.getInstance().match(p.getIp())){
        	if (preferredPeers.get(key)!=null)
        		preferredPeers.get(key).remove(p);
        } else if (!isClose){
            // put manual-specified peers in preferredPeers at a special key for CIDR
            if (CIDRMatcher.getInstance().match(p.getIp()) 
            		&& CIDRMatcher.getInstance().usedLately(p.getIp())){
            	p.setCIDRMatch(true);
            	if (preferredPeers.get(key)==null){
            		preferredPeers.put(key, new HashSetCache<OnoPeer>(100));        		
            	}
            	preferredPeers.get(key).add(p);
            	if (allPreferredPeers.get(CIDR_KEY)==null){
         		   allPreferredPeers.put(CIDR_KEY, new HashSetCache<OnoPeer>(500));
                 }
            	allPreferredPeers.get(CIDR_KEY).add(p);
            } else {
            	p.setCIDRMatch(false);
            	if (preferredPeers.get(key)!=null){
            		preferredPeers.get(key).remove(p);
            	}
            	if (allPreferredPeers.get(CIDR_KEY)!=null){
          		   allPreferredPeers.get(CIDR_KEY).remove(p);
                  }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Set<OnoPeer> getPreferredPeers(Object key) {
    	Digger.getInstance().lookupEdgeCluster();
    	if (preferredPeers.get(key)==null){
    		// look up on edges    		
    		return new HashSet<OnoPeer>();
    	}
    	else return (Set<OnoPeer>) ((HashSet<OnoPeer>)preferredPeers.get(key)).clone();
    }
    
    public Set<OnoPeer> getAllPreferredPeers(){
    	Set<OnoPeer> peers = new HashSet<OnoPeer>();
    	for (Entry<Object, Set<OnoPeer>> ent: preferredPeers.entrySet()){
    		peers.addAll(ent.getValue());
    	}    	
    	return peers;
    }

	public String getPreferredName() {
		return preferredName;
	}

	public void setPreferredName(String preferredName) {
		this.preferredName = preferredName;
	}
	
	public synchronized void addListener(NeighborhoodListener l){
		listeners.add(l);
	}
	
	public synchronized void removeListener(NeighborhoodListener l){
		listeners.remove(l);
	}

	public void compatiblePeerFound(Download download, Peer peer, Message message) {
		// TODO Auto-generated method stub
		
		try {
		Connection c = peer.getConnection();
		OnoRatioMessage orm = new OnoRatioMessage();
		c.getOutgoingMessageQueue().sendMessage(orm);
		addPeer(download, peer.getIp(), peer.getTCPListenPort());
		peer.getConnection().getIncomingMessageQueue().registerListener(this);
		DownloadStats.getInstance().addOnoCompatiblePeer(peer, download);
		} catch (NullPointerException e){
			// this happened on peer.getConnection(), just exit.
		}
	}

	public void peerRemoved(Download download, Peer peer) {
		// TODO Auto-generated method stub
		
	}
	
	public void processMessage(OnoRatioMessage orm, String peerIp){
		if (orm.getRatios()!=null){
			if (OnoPeerManager.getInstance().getOnoPeer(peerIp)==null){
				OnoPeerManager.getInstance().addPeer(peerIp, 0, MainGeneric.getClusterFinderObject(peerIp), false);
			}
			for (Entry<String, EdgeServerRatio> ent: orm.getRatios().entrySet()){
				OnoPeerManager.getInstance().getOnoPeer(peerIp).addRatio(ent.getKey(), ent.getValue());
			}
		}
	}

	public void bytesReceived(int byte_count) {
		// TODO Auto-generated method stub
		
	}

	public boolean messageReceived(Message message) {
		if (message instanceof OnoRatioMessage) return true;
		else 
			return false;
	}

	public void setActive(boolean active) {
		if (!active) {
			if (MainGeneric.isAzureus()) pi.getMessageManager().deregisterMessageType(new OnoRatioMessage());
			self = null;
		}
		this.active  = active;
		
	}


//	public void updatePeerSet(Map<String, String> allIpsSeen) {
//		for (NeighborhoodListener l : listeners){
//			l.foundNewIpAddress(allIpsSeen);
//		}
//		
//	}
}
