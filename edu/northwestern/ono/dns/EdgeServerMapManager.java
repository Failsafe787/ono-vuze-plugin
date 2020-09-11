/**
 * Ono Project
 *
 * File:         EdgeServerMapManager.java
 * RCS:          $Id: EdgeServerMapManager.java,v 1.46 2010/03/29 16:48:04 drc915 Exp $
 * Description:  EdgeServerMapManager class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 2, 2006 at 5:14:28 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.dns
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
package edu.northwestern.ono.dns;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.dht.IDDBReadAction;
import edu.northwestern.ono.dht.IDDBWriteAction;
import edu.northwestern.ono.dht.IDistributedDatabase;
import edu.northwestern.ono.dht.IDistributedDatabaseEvent;
import edu.northwestern.ono.position.CDNClusterFinder;
import edu.northwestern.ono.position.OnoPeerManager;
import edu.northwestern.ono.stats.EdgeServerRatio;
import edu.northwestern.ono.util.HashSetCache;
import edu.northwestern.ono.util.PluginInterface;
import edu.northwestern.ono.util.Util;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The EdgeServerMapManager class manages the mappings between peers and edge servers.
 */
public class EdgeServerMapManager implements IDDBReadAction, IDDBWriteAction {
    private static final boolean DEBUG = false;

    /** if set to false, means that DHT will not be used */
    private static final boolean DISABLED = !MainGeneric.getDhtEnabled();
    private IDistributedDatabase ddb;

    /** cache of edge server IPs mapped to a list of node IPs */
    private Map<String, HashSetCache<String>> edgeServerCache;

    /** data pending to be written to DDB -- maps edge server IPs to list of node IPs */
    private LinkedHashMap<String, ArrayList<String>> pendingWrites;
    private HashMap<String, Long> writtenThisSession = new HashMap<String, Long>();
    LinkedHashSet<String> newList;
    String[] output;
    ByteArrayOutputStream baos;
    ByteArrayInputStream bais;
    ObjectOutputStream oos;
    ObjectInputStream ois;
    private PluginInterface pi;
    
    HashMap<String, Long> nextRatioUpdate = new HashMap<String, Long>();
    HashMap<String, Long>  nextClusterToPeerUpdate = new HashMap<String, Long>();
    /** time between updates, in ms */
    private final int UPDATE_INTERVAL = 60*1000;
    
    public static final byte RATIO_ENTRY = 1;
    public static final byte PEER_RATIO_MAP = 2;

	private static final byte MAP_VERSION = 2;

    /**
     * @param manager
     * @param edgeServerCache
     * @param pendingWrites
     */
    public EdgeServerMapManager(IDistributedDatabase ddb,
        Map<String, HashSetCache<String>> edgeServerCache,
        LinkedHashMap<String, ArrayList<String>> pendingWrites,
        PluginInterface pi) {
        this.ddb = ddb;
        this.edgeServerCache = edgeServerCache;
        this.pendingWrites = pendingWrites;
        this.pi = pi;
    }

    /* (non-Javadoc)
     * @see edu.northwestern.ono.dht.IDHTReadAction#handleRead(java.lang.String)
     */
    public void handleRead(byte[] b, IDistributedDatabaseEvent event) {
    	// 80 is the current size of a value now
    	if (DEBUG){
    		System.out.println("Read succeeded for "+event.getKey().getDescription()+"!");
    	}
    	// TODO first test length, then test for type, then test value
    	if (b.length>0){
    		ByteBuffer bBuffer = ByteBuffer.wrap(b);
    		bBuffer.order(ByteOrder.LITTLE_ENDIAN);
    		byte version = bBuffer.get();
    		if (version!=MAP_VERSION) {
    			//System.err.println("Read value from old version!");
    			return;
    		}
    		byte type = bBuffer.get();
    		
    		if (type == RATIO_ENTRY){
	    		long timestamp = bBuffer.getLong();
	    		int customerIndex = bBuffer.getShort();
	    		EdgeServerRatio esr = new EdgeServerRatio();
	    		esr.deserialize(bBuffer);
	    		
	    		esr.setLastTimestamp(timestamp);
	    		//System.out.println(esr.toString());
	    		Digger.getInstance().addRatio(customerIndex, 
	    				(String)event.getKey().getDescription(), esr);
	    		OnoPeerManager.getInstance().foundInDht((String)event.getKey().getDescription());
    		}
    		else if (type== PEER_RATIO_MAP){
    			
    			byte ip[] = new byte[4];    			
    			bBuffer.get(ip);
    			String ipText;
				try {
					ipText = InetAddress.getByAddress(ip).getHostAddress();
				    			
					OnoPeerManager.getInstance().addPeer(ipText, 0, 
							MainGeneric.getClusterFinderObject(ipText), true);
					OnoPeerManager.getInstance().getRatios(ipText, true);
					OnoPeerManager.getInstance().registerPeerSource(ipText, OnoPeerManager.SOURCE_DHT);
//					Digger.getInstance().getRatios(ipText);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			
    		}
    		
    	}
//        if (newList == null) {
//            newList = new LinkedHashSet<String>();
//        }
//
//        synchronized (newList) {
//            newList = new LinkedHashSet<String>();
//
//            if (b.length > 0) {
//                if (DEBUG) {
//                    System.out.println("Value read: " + new String(b));
//                }
//
//                byte[] bytes = b;
//                bais = new ByteArrayInputStream(bytes);
//
//                try {
//                    ois = new ObjectInputStream(bais);
//
//                    Object o = ois.readObject();
//
//                    if ((o instanceof String[])) {
//                        output = (String[]) o;
//
//                        //            if (DEBUG) System.out.println("Value read from DB: " + 
//                        //                    "(" + event.getKey().getDescription() + ", " +
//                        //                    Arrays.deepToString(output) + ")");
//                        if (edgeServerCache.get(event.getKey().getDescription()) == null) {
//                            synchronized (edgeServerCache) {
//                                edgeServerCache.put(event.getKey()
//                                                         .getDescription(),
//                                    new LinkedHashSet<String>());
//                            }
//                        }
//
//                        // quit if using old version
//                        if (Constants.compareVersions(pi.getPluginVersion(),
//                                    output[0]) < 0) {
//                            return;
//                        }
//
//                        if (org.xbill.DNS.Address.isDottedQuad(output[0])) {
//                            newList.add(pi.getPluginVersion());
//                        }
//
//                        // get existing entries
//                        for (String value : output) {
//                            if (value.charAt(0) == '/') {
//                                value = value.substring(1);
//                            }
//
//                            newList.add(value);
//
//                            if (org.xbill.DNS.Address.isDottedQuad(value)) {
//                                edgeServerCache.get(event.getKey()
//                                                         .getDescription())
//                                               .add(value);
//
//                                if (DEBUG) {
//                                    System.out.println("Adding " + value);
//                                }
//                            }
//                        }
//                    }
//                } catch (IOException e) {
//                    System.err.println(e.toString());
//
//                    // generally a corrupted stream and i have to figure out why
//                    //e.printStackTrace();
//                } catch (ClassNotFoundException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            } // end if array length is greater than zero
//            else {
//            	// failed write, so retry
//            }
//
//            // if a write is in progress, leave here
//            if (manager.isWriteInProgress(event)) {
//                return;
//            }
//
//            // add new entries
//            for (String value : pendingWrites.get(event.getKey().getDescription())) {
//                if (value.charAt(0) == '/') {
//                    newList.add(value.substring(1));
//                } else {
//                    newList.add(value);
//                }
//            }
//
//            // don't write if there are no new entries
//            if ((edgeServerCache.get(event.getKey().getDescription()) != null) &&
//                    edgeServerCache.get(event.getKey().getDescription())
//                                       .containsAll(pendingWrites.get(
//                            event.getKey().getDescription()))) {
//                if (DEBUG) {
//                    System.out.println("No new entries!");
//                }
//
//                return;
//            }
//
//            doWrite(event);
//        }
    }


	/**
     *
     * @param event
     */
    private void doWrite(IDistributedDatabaseEvent event, int blah) {
//        ByteArrayOutputStream baos;
//        ObjectOutputStream oos;
//
//        try {
//            // create output and write
//            //            output = new String[newList.size()];
//            //            output = newList.toArray(output);
//            //            if (DEBUG) System.out.println("Trying to write "+Arrays.deepToString(output));
//            baos = null;
//            output = null;
//
//            while ((baos == null) || (baos.toString().length() > 250)) { // TODO may have to be half this
//
//                if (baos != null) {
//                    // length too long
//                    newList.remove(output[2]);
//                }
//
//                output = new String[newList.size()];
//                output = newList.toArray(output);
//                baos = new ByteArrayOutputStream();
//                oos = new ObjectOutputStream(baos);
//                oos.writeObject(output);
//                oos.flush();
//            }
//
//            if (DEBUG) {
//                System.out.println("Writing (" + baos.toString().length() +
//                    "): " + baos.toString("ISO-8859-1"));
//            }
//
//            manager.doWrite(event, baos, this);
//        } catch (DistributedDatabaseException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }

    /* (non-Javadoc)
     * @see edu.northwestern.ono.dht.IDHTWriteAction#handleWrite(org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent)
     */
    public void handleWrite(IDistributedDatabaseEvent event) {
//        try {
            if (DEBUG) {
                System.out.println("Data written to DB: ");// + "(" +
//                    event.getKey().getDescription() + ", " +
//                    event.getValue().getValue(String.class) + ")");
            }

            // now that value has been written, clear the pending values 
            // and update edge server cache
//            LinkedHashSet<String> nodes = edgeServerCache.get(event.getKey()
//                                                                   .getDescription());
//
//            if (nodes == null) {
//                nodes = new LinkedHashSet<String>();
//                edgeServerCache.put(event.getKey().getDescription(), nodes);
//            }

            //for (String node: nodes) edgeServerCache.get(event.getKey().getDescription()).add(node);
//            pendingWrites.get(event.getKey().getDescription()).clear();
//        } catch (DistributedDatabaseException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }

    /* (non-Javadoc)
     * @see edu.northwestern.ono.dht.IDHTReadAction#handleTimeout(org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent)
     */
    public void handleTimeout(IDistributedDatabaseEvent event) {
//        try {
//            if (pendingWrites.get(event.getKey().getKey()).size() > 0) {
//                handleRead(new byte[0], event);
//            }
//        } catch (DistributedDatabaseException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }

    /**
     * @param edgeServer the IP of the edge server
     * @param localAddress the IP address that saw the edge server
     */
    public void readAndUpdateDDB(int customerIndex, String nodeIp, EdgeServerRatio esr,
        int option) {
        if (DISABLED) {
            return;
        }
		if (DEBUG) System.out.println("Read and update for "+nodeIp);

//        edgeServer = Util.getClassCSubnet(edgeServer);
//
//        ArrayList<String> currentPendingWrites = pendingWrites.get(edgeServer);
//
//        if (currentPendingWrites == null) {
//            currentPendingWrites = new ArrayList<String>();
//            pendingWrites.put(edgeServer, currentPendingWrites);
//        }
//
//        if (!currentPendingWrites.contains(localAddress)) {
//            currentPendingWrites.add(localAddress);
//        } else {
//            return;
//        }
//
//        manager.doRead(edgeServer, edgeServer, this, option);
    }

    /* (non-Javadoc)
     * @see edu.northwestern.ono.dht.IDHTReadAction#handleComplete(org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent)
     */
    public void handleComplete(IDistributedDatabaseEvent event) {
//        if (event.getValue() == null) {
//            if (newList == null) {
//                newList = new LinkedHashSet<String>();
//            }
//
//            synchronized (newList) {
//                newList = new LinkedHashSet<String>();
//                newList.add(pi.getPluginVersion());
//
//                // add new entries
//                for (String value : pendingWrites.get(
//                        event.getKey().getDescription())) {
//                    if (value.charAt(0) == '/') {
//                        newList.add(value.substring(1));
//                    } else {
//                        newList.add(value);
//                    }
//                }
//
//                // if a write is in progress, leave here
//                if (manager.isWriteInProgress(event)) {
//                    return;
//                }
//
//                doWrite(event);
//            }
//        }
    }

    /** cache of edge server IPs mapped to a list of node IPs */
    public Map<String, HashSetCache<String>> getEdgeServerCache() {
        return edgeServerCache;
    }

	public void updateDatabase(String nodeIp, Map<String,EdgeServerRatio> esrs,
        int option) {
		
		try {
			if (esrs.size()==0) return;
			if (DISABLED) return;
			if (DEBUG) System.out.println("Doing write for "+nodeIp);
			
			
            // create output and write
            //            output = new String[newList.size()];
            //            output = newList.toArray(output);
            //            if (DEBUG) System.out.println("Trying to write "+Arrays.deepToString(output));
            baos = null;
            output = null;
            long currentTimeGMT;
//            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            currentTimeGMT = Util.currentGMTTime();
//            int entrySize = 3+4; // one cluster-ratio pair 
//            int totalSize = 10;
            
            // add timestamp and customer index
            baos = new ByteArrayOutputStream();
            
            
            
            if (nextRatioUpdate.get(nodeIp)== null || 
            		nextRatioUpdate.get(nodeIp) <= currentTimeGMT){
            	nextRatioUpdate.put(nodeIp, currentTimeGMT+UPDATE_INTERVAL);
            	if (esrs.size()>0)
            		writeRatios(nodeIp, esrs, currentTimeGMT);
            }
            
            
            if (nextClusterToPeerUpdate.get(nodeIp)== null ||
            		nextClusterToPeerUpdate.get(nodeIp)<= currentTimeGMT){
            	nextClusterToPeerUpdate.put(
            			nodeIp,currentTimeGMT+UPDATE_INTERVAL);
            	if (esrs.size()>0)
            		writePeerRatioMaps(nodeIp, esrs, currentTimeGMT);
            }
            
            
            
//		}	catch (DistributedDatabaseException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void writeRatios(String nodeIp, Map<String, EdgeServerRatio> esrs, 
			long currentTimeGMT) 
	throws IOException, UnsupportedEncodingException {
		
//		 do delete first
		boolean doDelete = false;
		if (writtenThisSession.containsKey(nodeIp)) doDelete = true;
		else writtenThisSession.put(nodeIp, currentTimeGMT);
		
		ArrayList<ByteArrayOutputStream> baoss = new ArrayList<ByteArrayOutputStream>();
		int count = 0;
		for (Entry<String, EdgeServerRatio> e: esrs.entrySet()){
			// create output and add time
			baos = new ByteArrayOutputStream();
//           	 TODO add type field, change size appropriately
			baos.write(MAP_VERSION);
			baos.write(RATIO_ENTRY);
		    baos.write(Util.convertLong(currentTimeGMT));
		    
		    // add customer index
			short customerIndex = Digger.getInstance().getCustomerIndex(e.getKey());
			if (customerIndex>=0){
			
				baos.write(Util.convertShort(customerIndex));
				
				// add values
				e.getValue().serialize(baos);
				
				
//				if (DEBUG) {
//		            System.out.println("Writing (" + baos.toString().length() +
//		                "): " + baos.toString("ISO-8859-1"));
//		        }
				
		        baoss.add(baos);
		        
			}
		}
		
		
		
//          ArrayList<String> currentPendingWrites = pendingWrites.get(nodeIp);
//            
//                    if (currentPendingWrites == null) {
//                        currentPendingWrites = new ArrayList<String>();
//                        pendingWrites.put(nodeIp, currentPendingWrites);
//                    }
//            
//                    if (!currentPendingWrites.contains(localAddress)) {
//                        currentPendingWrites.add(localAddress);
//                    } else {
//                        return;
//                    }

		if (doDelete){
			ddb.delete(nodeIp, UPDATE_INTERVAL/1000, null, null);
		}
		ddb.write(nodeIp, baoss, UPDATE_INTERVAL/1000, this);
	}
	
	private void writePeerRatioMaps(String nodeIp, 
			Map<String, EdgeServerRatio> esrs, 
			long currentTimeGMT) throws IOException {
		int count = 0;
		ArrayList<ByteArrayOutputStream> baoss = new ArrayList<ByteArrayOutputStream>();
		
		
		
		// create set of edge clusters for this node ip, 
		// then we can use node ips to look up edge server ratios
		Set<String> clusters = new HashSet<String>();
		for (Entry<String, EdgeServerRatio> e: esrs.entrySet()){
			
			EdgeServerRatio esr = e.getValue();
			for (Entry<String, Double> ent : esr.getEntries()){
				if (ent.getValue() > CDNClusterFinder.COSINE_THRESHOLD){
					if (!clusters.contains(ent.getKey())){
						baos = new ByteArrayOutputStream();
						baos.write(MAP_VERSION);
						baos.write(PEER_RATIO_MAP);
					    baos.write(InetAddress.getByName(nodeIp).getAddress());
					    baoss.add(baos);
					    
					    // is the value new or expired?
					    if (writtenThisSession.containsKey(ent.getKey())){
					    	if ( (writtenThisSession.get(ent.getKey()) 
					    			<currentTimeGMT-MainGeneric.getDdbTTL()*1000)){
					    	writtenThisSession.put(ent.getKey(), currentTimeGMT);
							 ddb.write(ent.getKey(), baoss, UPDATE_INTERVAL/1000, this);
					    	}
					    }
						else {
							writtenThisSession.put(ent.getKey(), currentTimeGMT);
							 ddb.write(ent.getKey(), baoss, UPDATE_INTERVAL/1000, this);
						}
					    
					   
					    baoss.clear();
					    clusters.add(ent.getKey());
					}
					
				}
			}
		}
		
		// delete stale entries
		/*LinkedList<String> toRemove = new LinkedList<String>();
		for (String key : writtenThisSession.keySet()){
			if (!key.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}") // not a full ip 
					&& !clusters.contains(key)){
				baos = new ByteArrayOutputStream();

				baos.write(PEER_RATIO_MAP);
			    baos.write(InetAddress.getByName(nodeIp).getAddress());
			    baoss.add(baos);
			    
			    ddb.delete(key, 30, null, baoss);
			}
		}
		for (String s : toRemove)
			writtenThisSession.remove(s); */
		
		
	}



	public void lookup(String peerIp) {
		if (DISABLED) return;
		if (DEBUG) System.out.println("Doing lookup for "+peerIp);
		ddb.read(peerIp, "Looking up Ono position for IP "+peerIp,
				30, this, ddb.getOptions().getExhaustiveRead());
		
	}

}
