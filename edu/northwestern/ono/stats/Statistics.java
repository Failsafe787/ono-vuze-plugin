/**
 * Ono Project
 *
 * File:         Statistics.java
 * RCS:          $Id: Statistics.java,v 1.92 2010/03/29 16:48:04 drc915 Exp $
 * Description:  Statistics class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jun 14, 2006 at 7:51:32 AM
 * Language:     Java
 * Package:      edu.northwestern.ono.stats
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
package edu.northwestern.ono.stats;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.OnoConfiguration;
import edu.northwestern.ono.brp.BRPPeerManager;
import edu.northwestern.ono.database.DatabaseReporter;
import edu.northwestern.ono.database.MySQLReporter;
import edu.northwestern.ono.experiment.TraceRouteRunner.TraceEntry;
import edu.northwestern.ono.experiment.TraceRouteRunner.TraceResult;
import edu.northwestern.ono.net.OnoMessage;
import edu.northwestern.ono.position.OnoPeerManager;
import edu.northwestern.ono.timer.ITimer;
import edu.northwestern.ono.timer.ITimerEvent;
import edu.northwestern.ono.timer.ITimerEventPerformer;
import edu.northwestern.ono.util.HashMapCache;
import edu.northwestern.ono.util.HashSetCache;
import edu.northwestern.ono.util.Pair;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The Statistics class manages statistics collected by this plugin.
 */
public class Statistics implements ITimerEventPerformer {
    private static final int THRESHOLD_ENTRIES = 200;
    private static Statistics self;
    public final static HashMap<String, Integer> peerMap = new HashMap<String, Integer>();
    public final static HashMap<String, Integer> peerClientMap = new HashMap<String, Integer>();
    public final static HashMap<String, Integer> routerMap = new HashMap<String, Integer>();
    public final static HashMap<String, Integer> dnsMap = new HashMap<String, Integer>();
    public final static HashMap<String, Integer> edgeServerMap = new HashMap<String, Integer>();
    public final static HashMap<String, Integer> branchPointMap = new HashMap<String, Integer>();
    private static final int MAX_VIVALDI_RESULTS = 200;
	private static final int MAX_TRACE_RESULTS = 200;
	private static final int MAX_PING_RESULTS = 200;
    private final long UPDATE_INTERVAL;
    private final long JITTER_BASE;
    private final long RATIO_UPDATE_INTERVAL = 30 * 60 *1000;
    private long nextRatioUpdateTime = 0;
    private long nextUpdateInterval;
    private boolean DEBUG = false;
    /**
     * List of things that can be reported.
     *
     */

    /** set of all edges */
    static HashSet<String> edges;
    /** list of all events */
    static HashSetCache<DNSEvent> events;
    /** list of Oess events */
    static HashSetCache<DNSEvent> eventsOess;
 
    /** list of all edge server mapping */
    static HashSetCache<DigResult> digResults;
    /** list of all ping results */
    static HashSetCache<PingResult> pingResults;
    static HashSetCache<PingResultOess> pingResultsOessSet;
    /** list of all vivaldi results */
    static HashSetCache<VivaldiResult> vivaldiResults;
    /** list of all data transfer events */
    static LinkedList<DataTransferEvent> dataTransferResults;
    /** list of all peers */
    static HashSet<String> peers;
    /** list of all trace route results*/
    static HashSetCache<TraceResult> traceResults;
    static HashSetCache<TraceResult> traceResultsOess;
    
    static HashMap<String, Pair<String, Double>> edgePings = new HashMap<String, Pair<String, Double>>();

    /**
     * Stuff for the database
     */
    static private DatabaseReporter db = null;

    /** random object for jitter */
    static private Random r = new Random();

    /** maps of internal ids to DB ids */
    static private HashMap<Integer, Integer> customerIds;
    static private HashMap<Integer, Integer> edgeIds;
    static private HashMap<Integer, Integer> edgeServerIds;
    static private HashMap<Integer, Integer> edgeClusterIds;
    static private HashMap<Integer, Integer> branchPointIds;
    static private HashMap<Integer, Integer> peerIds;
    static private HashMap<Integer, Integer> peerClientIds;
    static private HashMap<Integer, Integer> routerIds;
    static private HashMap<Integer, Integer> dnsIds;
    private int timeZoneId;
    private long lastUpdate;
	private HashSetCache<Exception> errors;
	private int selfId = -1;
	ITimer timer;
	private Boolean isDumpingStats = false;

    private Statistics() {
    	if (r==null){
    		r = new Random();
    	}
        edges = new HashSet<String>();
        peers = new HashSet<String>();
        events = new HashSetCache<DNSEvent>(THRESHOLD_ENTRIES);
        eventsOess = new HashSetCache<DNSEvent>(THRESHOLD_ENTRIES);
        digResults = new HashSetCache<DigResult>(200);
        traceResults = new HashSetCache<TraceResult>(MAX_TRACE_RESULTS);
        traceResultsOess = new HashSetCache<TraceResult>(MAX_TRACE_RESULTS);
        pingResults = new HashSetCache<PingResult>(MAX_PING_RESULTS);
        pingResultsOessSet = new HashSetCache<PingResultOess>(MAX_PING_RESULTS);
        vivaldiResults = new HashSetCache<VivaldiResult>(MAX_VIVALDI_RESULTS);
        customerIds = new HashMap<Integer, Integer>();
        edgeIds = new HashMap<Integer, Integer>();
        edgeClusterIds = new HashMap<Integer, Integer>();
        peerIds = new HashMap<Integer, Integer>();
        dnsIds = new HashMap<Integer, Integer>();
        edgeServerIds = new HashMap<Integer, Integer>();
        branchPointIds = new HashMap<Integer, Integer>();
        routerIds = new HashMap<Integer, Integer>();
        peerClientIds = new HashMap<Integer, Integer>();
        errors = new HashSetCache<Exception>();
        timeZoneId = -1;
        lastUpdate = System.currentTimeMillis();
        UPDATE_INTERVAL = OnoConfiguration.getInstance().getDbUpdateFreqSec() * 1000;
        JITTER_BASE = UPDATE_INTERVAL / 4;
        nextUpdateInterval = UPDATE_INTERVAL +
            (long) (r.nextDouble() * JITTER_BASE);

        timer = MainGeneric.createTimer("OnoStatistics");
        timer.addPeriodicEvent(UPDATE_INTERVAL, this);
        dataTransferResults = new LinkedList<DataTransferEvent>();
        
    }

    public static synchronized Statistics getInstance() {
        if (self == null) {
            self = new Statistics();
        }

        return self;
    }

    public boolean addEdge(String edge) {
        return edges.add(edge);
    }
    
    public boolean addPeerClient(String peerClient) {
        if (db == null) {
            db = MainGeneric.getReporter();
            if (!MainGeneric.getReporter().canConnect()) return false;
        }

        int p;

        synchronized (peerClientMap) {
            if (peerClientMap.get(peerClient) == null) {
            	peerClientMap.put(peerClient, peerClientMap.size());
            }

            p = peerClientMap.get(peerClient);

            if (!peerClientIds.containsKey(p)) {
            	MainGeneric.getReporter().connect();
                int peerClientId = MainGeneric.getReporter().makeId("peerclients", "name", peerClient);

                if (peerClientId == -1) {
                    return false;
                }
                MainGeneric.getReporter().disconnect();
                peerClientIds.put(p, peerClientId);
            }
        }
        
        return true;
    }

    public boolean addPeer(String peer) {
        if (db == null) {
            db = MainGeneric.getReporter();
            if (!MainGeneric.getReporter().canConnect()) return false;
        }

        int p;

        synchronized (peerMap) {
            if (peerMap.get(peer) == null) {
                peerMap.put(peer, peerMap.size());
            }

            p = peerMap.get(peer);

            if (!peerIds.containsKey(p)) {
            	MainGeneric.getReporter().connect();
                int peerId = MainGeneric.getReporter().makeId("peerips", "ip", peer);

                if (peerId == -1) {
                    return false;
                }
                MainGeneric.getReporter().disconnect();
                peerIds.put(p, peerId);
            }
        }
        
        return peers.add(peer);
    }

    public void addEvent(DNSEvent e) {
        synchronized (events) {
            events.add(e);
        }

        if (events.size() >= THRESHOLD_ENTRIES) {
            
        	reportEvents();
            synchronized (events) {events.clear();}
            lastUpdate = System.currentTimeMillis();
            nextUpdateInterval = UPDATE_INTERVAL +
                (long) (r.nextDouble() * JITTER_BASE);
        }
    }

    public void addEventOess(DNSEvent e) {
        synchronized (eventsOess) {
            eventsOess.add(e);
        }

        if (eventsOess.size() >= THRESHOLD_ENTRIES) {
            
        	reportEventsOess();
            synchronized (eventsOess) {eventsOess.clear();}
            lastUpdate = System.currentTimeMillis();
            nextUpdateInterval = UPDATE_INTERVAL +
                (long) (r.nextDouble() * JITTER_BASE);
        }
    }

    
    public void addDigResult(DigResult d) {
        synchronized (digResults) {
            digResults.add(d);
        }

        if ((digResults.size() * 24) > THRESHOLD_ENTRIES) {
            reportDigs();
            digResults.clear();
            lastUpdate = System.currentTimeMillis();
            nextUpdateInterval = UPDATE_INTERVAL +
                (long) (r.nextDouble() * JITTER_BASE);
        }
    }

    public void reportEvents() {
        //        Map<String, Pair<String, Object>> values = new HashMap<String, Pair<String, Object>>();
        int edgeId = -1;

        //        Map<String, Pair<String, Object>> values = new HashMap<String, Pair<String, Object>>();
        int customerId = -1;

        //        Map<String, Pair<String, Object>> values = new HashMap<String, Pair<String, Object>>();
        int peerId = -1;

        if (db == null) {
            db = MainGeneric.getReporter();            
        }
        if (!MainGeneric.getReporter().connect() || 
        		!OnoConfiguration.getInstance().isSendToDatabase()) return;
        
        batchProcessIds();

        // update map of ids from database
        synchronized (events) {
            LinkedList<DNSEvent> toRemove = new LinkedList<DNSEvent>();

            for (DNSEvent d : events) {
                if (d == null) {
                    continue; // TODO find source of null ones
                }

                
                if (!edgeIds.containsKey(d.edgeServer)) {
                    edgeId = MainGeneric.getReporter().makeId("edgeservers", "ipaddress",
                            getString(DNSEvent.edges, d.edgeServer));

                    if (edgeId == -1) {
                        continue;
                    }

                    edgeIds.put(d.edgeServer, edgeId);
                } else {
                    edgeId = edgeIds.get(d.edgeServer);
                }

                if (timeZoneId == -1) {
                    timeZoneId = MainGeneric.getReporter().makeId("timezones", "name",
                            java.util.TimeZone.getDefault()
                                              .getDisplayName(true,
                                java.util.TimeZone.LONG));
                }

                if (timeZoneId == -1) {
                    continue;
                }

                if (!customerIds.containsKey(d.customer)) {
                    customerId = MainGeneric.getReporter().makeId("customers", "name",
                            getString(DNSEvent.customers, d.customer));

                    if (customerId == -1) {
                        continue;
                    }

                    customerIds.put(d.customer, customerId);
                } else {
                    customerId = customerIds.get(d.customer);
                }

                if (!peerIds.containsKey(d.peer)) {
                    peerId = MainGeneric.getReporter().makeId("peerips", "ip",
                            getString(peerMap, d.peer));

                    if (peerId == -1) {
                        continue;
                    }

                    peerIds.put(d.peer, peerId);
                } else {
                    peerId = peerIds.get(d.peer);
                }

                boolean success = MainGeneric.getReporter().insertEvent(customerId, edgeId, peerId,
                        d.event.ordinal(), d.time, timeZoneId);

                if (success) {
                    toRemove.add(d);
                }
            }

            events.removeAll(toRemove);
        }
        MainGeneric.getReporter().disconnect();

        // TODO setup
        //        values.put("configId", new Pair<String, Object>("int", configId));
        //        values.put("minCopies", new Pair<String, Object>("int", minCopies));
        //        values.put("numReplications", new Pair<String, Object>("int", numReplications));
        //        values.put("totalNodesWithFiles", new Pair<String, Object>("int", totalNodesWithFiles));
        //        values.put("totalOutages", new Pair<String, Object>("int", totalOutages));
    }

    
    public void reportEventsOess() {
        //        Map<String, Pair<String, Object>> values = new HashMap<String, Pair<String, Object>>();
        int edgeId = -1;

        //        Map<String, Pair<String, Object>> values = new HashMap<String, Pair<String, Object>>();
        int customerId = -1;

        //        Map<String, Pair<String, Object>> values = new HashMap<String, Pair<String, Object>>();
        int peerId = -1;

//        if (db == null) {
//            db = MainGeneric.getReporter();            
//        }
//        if (!MainGeneric.getReporter().connect() || 
//        		!OnoConfiguration.getInstance().isSendToDatabase()) return;
        
        batchProcessIds();

        // update map of ids from database
        synchronized (eventsOess) {
            LinkedList<DNSEvent> toRemove = new LinkedList<DNSEvent>();

            for (DNSEvent d : eventsOess) {
                if (d == null) {
                    continue; // TODO find source of null ones
                }
                
                if (!edgeIds.containsKey(d.edgeServer)) {
                    edgeId = MainGeneric.getReporter().makeId("edgeservers", "ipaddress",
                            getString(DNSEvent.edges, d.edgeServer));

                    if (edgeId == -1) {
                        continue;
                    }

                    edgeIds.put(d.edgeServer, edgeId);
                } else {
                    edgeId = edgeIds.get(d.edgeServer);
                }

                if (timeZoneId == -1) {
                    timeZoneId = MainGeneric.getReporter().makeId("timezones", "name",
                            java.util.TimeZone.getDefault()
                                              .getDisplayName(true,
                                java.util.TimeZone.LONG));
                }

                if (timeZoneId == -1) {
                    continue;
                }

                if (!customerIds.containsKey(d.customer)) {
                    customerId = MainGeneric.getReporter().makeId("customers", "name",
                            getString(DNSEvent.customers, d.customer));

                    if (customerId == -1) {
                        continue;
                    }

                    customerIds.put(d.customer, customerId);
                } else {
                    customerId = customerIds.get(d.customer);
                }

                if (!peerIds.containsKey(d.peer)) {
                    peerId = MainGeneric.getReporter().makeId("peerips", "ip",
                            getString(peerMap, d.peer));

                    if (peerId == -1) {
                        continue;
                    }

                    peerIds.put(d.peer, peerId);
                } else {
                    peerId = peerIds.get(d.peer);
                }

//                boolean success = MainGeneric.getReporter().insertEvent(customerId, edgeId, peerId,
//                        d.event.ordinal(), d.time, timeZoneId);
//
//                if (success) {
                    toRemove.add(d);
 //               }
            }

            eventsOess.removeAll(toRemove);
        }
//        MainGeneric.getReporter().disconnect();

    }
    
    public void reportDigs() {
        Map<String, Pair<String, Object>> values = new HashMap<String, Pair<String, Object>>();

        int edge1Id = -1;
        int edge2Id = -1;
        int customerId = -1;
        int peerId = -1;

        if (db == null) {
            db = MainGeneric.getReporter();            
        }
        if (!MainGeneric.getReporter().connect()) return;

        batchProcessIds();
        
        // update map of ids from database
        synchronized (digResults) {
            for (DigResult d : digResults) {
                if (!edgeIds.containsKey(d.edge1)) {
                    edge1Id = MainGeneric.getReporter().makeId("edgeservers", "ipaddress",
                            getString(DigResult.edges, d.edge1));

                    if (edge1Id == -1) {
                        continue;
                    }

                    edgeIds.put(d.edge1, edge1Id);
                } else {
                    edge1Id = edgeIds.get(d.edge1);
                }

                if (!edgeIds.containsKey(d.edge2)) {
                    edge2Id = MainGeneric.getReporter().makeId("edgeservers", "ipaddress",
                            getString(DigResult.edges, d.edge2));

                    if (edge2Id == -1) {
                        continue;
                    }

                    edgeIds.put(d.edge2, edge2Id);
                } else {
                    edge2Id = edgeIds.get(d.edge2);
                }

                //            if (timeZoneId==-1) timeZoneId =  MainGeneric.getReporter().makeId("timezones", "name",
                //                    java.util.TimeZone.getDefault().getDisplayName(true, java.util.TimeZone.LONG));
                if (!customerIds.containsKey(d.customer)) {
                    customerId = MainGeneric.getReporter().makeId("customers", "name",
                            getString(DigResult.customers, d.customer));

                    if (customerId == -1) {
                        continue;
                    }

                    customerIds.put(d.customer, customerId);
                } else {
                    customerId = customerIds.get(d.customer);
                }

                if (!peerIds.containsKey(d.peer)) {
                    peerId = MainGeneric.getReporter().makeId("peerips", "ip",
                            getString(peerMap, d.peer));

                    if (peerId == -1) {
                        continue;
                    }

                    peerIds.put(d.peer, peerId);
                } else {
                    peerId = peerIds.get(d.peer);
                }

                MainGeneric.getReporter().insertDigResult(customerId, peerId, edge1Id, edge2Id, d.time);
            }
        }
        MainGeneric.getReporter().disconnect();
        // TODO setup
        //        values.put("configId", new Pair<String, Object>("int", configId));
        //        values.put("minCopies", new Pair<String, Object>("int", minCopies));
        //        values.put("numReplications", new Pair<String, Object>("int", numReplications));
        //        values.put("totalNodesWithFiles", new Pair<String, Object>("int", totalNodesWithFiles));
        //        values.put("totalOutages", new Pair<String, Object>("int", totalOutages));
    }

    /**
     * @param map
     * @param edgeServer
     * @return
     */
    private String getString(HashMap<String, Integer> map, int edgeServer) {
    	synchronized (map){
	        for (Map.Entry<String, Integer> e : map.entrySet()) {
	            if (e.getValue().equals(edgeServer)) {
	                return e.getKey();
	            }
	        }
    	}

        return null;
    }

    /**
     *
     * @param configId
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws SQLException
     */
    public void writeToDatabase(String tableName,
        Map<String, Pair<String, Object>> values)
        throws IOException, ClassNotFoundException, SQLException {
        StringBuffer sqlBuffer = new StringBuffer();

        createTable(values, tableName, false);

        sqlBuffer.append("INSERT INTO " + tableName + " (`" + "id" + "`, ");

        Set<String> keys = new LinkedHashSet<String>();

        Iterator<String> it = values.keySet().iterator();

        while (it.hasNext()) {
            String name = it.next();
            sqlBuffer.append(MySQLReporter.sqlEncode(name));
            keys.add(name);

            if (it.hasNext()) {
                sqlBuffer.append(", ");
            }
        }

        sqlBuffer.append(") VALUES (0, ");

        Iterator<String> ito = keys.iterator();

        while (ito.hasNext()) {
            Object o = values.get(ito.next()).getValue();
            sqlBuffer.append("'" + o.toString() + "'");

            if (ito.hasNext()) {
                sqlBuffer.append(", ");
            }
        }

        sqlBuffer.append(")");
        MainGeneric.getReporter().doInsert(sqlBuffer.toString(), null);
    }

    /**
     * @param values
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws SQLException
     */
    private void createTable(Map<String, Pair<String, Object>> values,
        String tableName, boolean isUnique)
        throws SQLException, IOException, ClassNotFoundException {
        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append("CREATE TABLE IF NOT EXISTS " + tableName + " (" +
            "id" + " INT(" + 11 + ") AUTO_INCREMENT, ");

        Set<String> s = new HashSet<String>();

        Iterator<String> fieldNames = values.keySet().iterator();

        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            s.add(fieldName);

            String typeString = values.get(fieldName).getKey();
            sqlBuffer.append(fieldName + " " + typeString);

            if (fieldNames.hasNext()) {
                sqlBuffer.append(", ");
            }
        }

        //        
        //        for (int i = 0; i < fields.length; i++) {
        //          String fieldName = fields[i].getName();
        //          
        //          Class type = fields[i].getClass();
        //          String typeString = getTypeString (type);
        //          sqlBuffer.append (sqlEncode (fieldName) + " " + typeString);
        //          if (i!=fields.length-1) sqlBuffer.append (", ");
        //        }

        // make sure each entry is unique
        if (isUnique) {
            sqlBuffer.append(", UNIQUE(");

            Iterator<String> it = s.iterator();

            while (it.hasNext()) {
                sqlBuffer.append(it.next());

                if (it.hasNext()) {
                    sqlBuffer.append(", ");
                }
            }

            sqlBuffer.append(")");
        }

        sqlBuffer.append(", PRIMARY KEY (id))");

        MainGeneric.getReporter().doCreateTable(sqlBuffer.toString());
    }

    /**
     * @return the number of edges seen by this nodes
     */
    public int getNumEdges() {
        return edges.size();
    }

    /**
     * @return the list of edges
     */
    public HashSet<String> getEdgeSet() {
        return edges;
    }

    /**
     * Called right before plugin unloads.
     */
    public void flush() {
    	if (OnoConfiguration.getInstance().isRecordStats() 
    			&& OnoConfiguration.getInstance().isSendToDatabase()){
    		MainGeneric.getReporter().setExtendedConnection(true);
	        reportEvents();
	        reportDigs();
	        commitPings();
	        commitDataTransfers();
	        commitVivaldiPositions();
	        reportTraces();
	        commitErrors();
	        commitRatios();
	        commitBranchPoints();
	        commitRatiosOess();
	        commitPingsOess();
	        reportTracesOess();
	        MainGeneric.getReporter().setExtendedConnection(false);
    	}
        MainGeneric.getReporter().disconnect();
    }


	/**
     * @param key
     * @param value
     * @param otherEndIp
     * @param double1
     * @param timeInMillis
     * @param experimentId
     */
    public void addPingResult(String edge, String middle, String otherEndIp,
        String myIp, Double rtt, long timeInMillis, int experimentId) {
        PingResult pr = new PingResult(edge, middle, otherEndIp, myIp,
                rtt.doubleValue(), timeInMillis, experimentId);

        synchronized (pingResults){
        	if (pingResults.size()>= MAX_PING_RESULTS){
        		commitPings();
        	}
        }
        
        synchronized (pingResults) {
            pingResults.add(pr);
        }

        // TODO update size
        /*if (pingResults.size() > 100) {
            commitPings();
            lastUpdate = System.currentTimeMillis();
            nextUpdateInterval = UPDATE_INTERVAL +
                (long) (r.nextDouble() * JITTER_BASE);
        }*/
    }

    public void addPingResultOess(String sourceIp, String destIp, Double rtt,
            long timeInMillis, int destType) {

    	PingResultOess pr = new PingResultOess(sourceIp, destIp, rtt, timeInMillis, destType);

    	if (DEBUG) {
    		System.out.println("ADDPINGRESULTOESS " + "destIp: " + destIp + " sourceIp: " + 
				sourceIp + " rtt: " + rtt + " time: " + timeInMillis + ";" + " destType: " + 
				destType);
    	}

    	synchronized (pingResultsOessSet) {
            pingResultsOessSet.add(pr);
    	}
    	
    	synchronized (pingResultsOessSet) {
          	if (pingResultsOessSet.size()>= MAX_PING_RESULTS){
          		commitPingsOess();
        	}	
    	}
         
    }
    
    public void addDataTransferEvent(String source, String current,
        String dest, int id, int size, long time, byte type, double race1,
        double race2) {
        DataTransferEvent dte = new DataTransferEvent(source, current, dest,
                id, size, time, type);
        
//        if (type==OnoMessage.TYPE_PRIMARY_PATH || type==OnoMessage.TYPE_RACE){
//        	if (OnoConfiguration.getInstance().isVisualize()){
//        		SideStep.getInstance().addDlDataPoint(source, time, size);
//        	}
//        }

        if (type == OnoMessage.TYPE_RACE_RESULT) {
            dte.setRaceResults(race1, race2);
        }

        synchronized (dataTransferResults) {
            dataTransferResults.add(dte);
        }

        // TODO update size
        if (dataTransferResults.size() > 5000 && !isDumpingStats) {
        	MainGeneric.createThread("StatsWriter", new Runnable(){

				public void run() {
					commitDataTransfers();
					
				}});
            isDumpingStats = true;
            lastUpdate = System.currentTimeMillis();
            nextUpdateInterval = UPDATE_INTERVAL +
                (long) (r.nextDouble() * JITTER_BASE);
        }
    }

    public void commitDataTransfers() {
        int sourceId = -1;
        int destId = -1;
        int currentId = -1;
        synchronized (isDumpingStats){
        
        isDumpingStats = true;
        if (db == null) {
            db = MainGeneric.getReporter();            
        }
        if (!MainGeneric.getReporter().connect()) return;
        
        batchProcessIds();

        LinkedList<DataTransferEvent> dtesToRemove = new LinkedList<DataTransferEvent>();
        LinkedList<DataTransferEvent> localCopy = new LinkedList<DataTransferEvent>();
        for (int i = 0; i < dataTransferResults.size(); i++)
        	localCopy.add(dataTransferResults.get(i));
        
        // update map of ids from database
        //synchronized (dataTransferResults) {
            for (DataTransferEvent dte : localCopy) {
                if (!peerIds.containsKey(dte.source)) {
                    sourceId = MainGeneric.getReporter().makeId("peerips", "ip",
                            getString(peerMap, dte.source));

                    if (sourceId == -1) {
                        continue;
                    }

                    peerIds.put(dte.source, sourceId);
                } else {
                    sourceId = peerIds.get(dte.source);
                }

                if (!peerIds.containsKey(dte.dest)) {
                    destId = MainGeneric.getReporter().makeId("peerips", "ip",
                            getString(peerMap, dte.dest));

                    if (destId == -1) {
                        continue;
                    }

                    peerIds.put(dte.dest, destId);
                } else {
                    destId = peerIds.get(dte.dest);
                }

                if (!peerIds.containsKey(dte.current)) {
                    currentId = MainGeneric.getReporter().makeId("peerips", "ip",
                            getString(peerMap, dte.current));

                    if (currentId == -1) {
                        continue;
                    }

                    peerIds.put(dte.current, currentId);
                } else {
                    currentId = peerIds.get(dte.current);
                }

                try {
                	if (MainGeneric.getReporter().insertDataTransferResult(dte.time, dte.experimentId,
                			dte.type, dte.size, sourceId, destId, currentId,
                			dte.raceResultPrimary, dte.raceResultSecondary)) {
                		dtesToRemove.add(dte);
                	}
                }catch (Exception e){
                	e.printStackTrace();
                	dtesToRemove.add(dte);
                }
            }
        //}

            for (DataTransferEvent dte : dtesToRemove){
            	synchronized (dataTransferResults) {dataTransferResults.remove(dte);}
            }
        
                 
        MainGeneric.getReporter().disconnect();
        isDumpingStats = false;
        }
    }

    /**
    *
    */
    public void commitPings() {
        int edgeId = -1;
        int myId = -1;
        int destId = -1;
        int middleId = -1;

        if (db == null) {
            db = MainGeneric.getReporter();
            if (!MainGeneric.getReporter().canConnect() || 
            		!OnoConfiguration.getInstance().isSendToDatabase()) return;
        }

        batchProcessIds();

        LinkedList<PingResult> pingsToRemove = new LinkedList<PingResult>();

        // update map of ids from database
        synchronized (pingResults) {
            for (PingResult p : pingResults) {
                if (db == null) {
                    db = MainGeneric.getReporter();
                }

                if (p.edge == -1) {
                    edgeId = -1;
                } else if (!edgeIds.containsKey(p.edge)) {
                    edgeId = MainGeneric.getReporter().makeId("edgeservers", "ipaddress",
                            getString(PingResult.edges, p.edge));

                    if (edgeId == -1) {
                        continue;
                    }

                    edgeIds.put(p.edge, edgeId);
                } else {
                    edgeId = edgeIds.get(p.edge);
                }

                if (!peerIds.containsKey(p.myIp)) {
                    myId = MainGeneric.getReporter().makeId("peerips", "ip", getString(peerMap, p.myIp));

                    if (myId == -1) {
                        continue;
                    }

                    peerIds.put(p.myIp, myId);
                } else {
                    myId = peerIds.get(p.myIp);
                }

                if (!peerIds.containsKey(p.dest)) {
                    destId = MainGeneric.getReporter().makeId("peerips", "ip",
                            getString(peerMap, p.dest));

                    if (destId == -1) {
                        continue;
                    }

                    peerIds.put(p.dest, destId);
                } else {
                    destId = peerIds.get(p.dest);
                }

                if (p.middle == -1) {
                    middleId = -1;
                } else if (!peerIds.containsKey(p.middle)) {
                    middleId = MainGeneric.getReporter().makeId("peerips", "ip",
                            getString(peerMap, p.middle));

                    if (middleId == -1) {
                        continue;
                    }

                    peerIds.put(p.middle, middleId);
                } else {
                    middleId = peerIds.get(p.middle);
                }

                if (MainGeneric.getReporter().insertPingResult(edgeId, myId, destId, middleId, p.rtt,
                            p.time, p.experimentId)) {
                    pingsToRemove.add(p);
                }
            }
        }

        synchronized (pingResults) {
            pingResults.removeAll(pingsToRemove);
        }
        
        MainGeneric.getReporter().disconnect();
        
    }
    
    /**
    *
    */
    public void commitPingsOess() {
    	
        if (db == null) {
            db = MainGeneric.getReporter();
            if (!MainGeneric.getReporter().canConnect() || 
            		!OnoConfiguration.getInstance().isSendToDatabase()) return;
        }

        batchProcessIds();
        LinkedList<PingResultOess> pingsToRemove = new LinkedList<PingResultOess>();
        // update map of ids from database
        synchronized (pingResultsOessSet) {
        	for (PingResultOess p : pingResultsOessSet) {
                if (db == null) {
                    db = MainGeneric.getReporter();
                }
//                
                if (!peerIds.containsKey(peerMap.get(p.sourceIp))) {
                    p.sourceId = MainGeneric.getReporter().makeId("peerips", "ip", p.sourceIp);
                    
                    if (p.sourceId == -1)
                    	return;

                    peerIds.put(peerMap.get(p.sourceIp), p.sourceId);
                }
  
//                
                p.sourceId = peerIds.get(peerMap.get(p.sourceIp));
                
                if (p.destType == 1)
                	p.destId = dnsIds.get(dnsMap.get(p.destIp));
                	
            	//System.out.println("edgeIds.get(DNSEvent.edges.get(p.destIp) " 
            	//		+ (DNSEvent.edges.get(p.destIp)));	

                if (p.destType == 2) {
                	if (edgeServerMap.get(p.destIp) != null )
                			p.destId = edgeServerIds.get(edgeServerMap.get(p.destIp));
                }	
                
                if (p.destId != -1 && p.sourceId!= -1)
                	if (MainGeneric.getReporter().insertPingResultOess(p.sourceId, p.destId,  
                		p.rtt, p.time, p.destType))
                		pingsToRemove.add(p);
                	
        	}
        	
        }
        
        synchronized (pingResultsOessSet) {
            pingResultsOessSet.removeAll(pingsToRemove);
        }
        MainGeneric.getReporter().disconnect();
                
    }

    
    @SuppressWarnings("unchecked")
	public void commitRatios() {

    	int myId = -1;
    	int customerId = -1;
    	int edgeClusterId = -1;

    	if (db == null) {
    		db = MainGeneric.getReporter();
    		if (!MainGeneric.getReporter().canConnect()) return;
    	}
    	if (!MainGeneric.getReporter().connect() || 
        		!OnoConfiguration.getInstance().isSendToDatabase()) return;
    	batchProcessIds();
    	

    	HashMapCache<String, Map<String,EdgeServerRatio>> ratios = 
    		OnoPeerManager.getInstance().getAllRatios();

    	// update map of ids from database

    	for (Entry<String, Map<String,EdgeServerRatio>> map : ratios.entrySet()) {
    		// get local peer id
    		
    		if (peerMap.get(map.getKey())==null) continue;
    		int tempPeerIp = peerMap.get(map.getKey());

    		for (Entry<String, EdgeServerRatio> ratio : (
    				(HashMap<String, EdgeServerRatio>)
    					((HashMap<String, EdgeServerRatio>)map.getValue())
    					.clone()).entrySet()){

    			// get local customer id
    			if (DNSEvent.customers==null || ratio == null) continue;
    			if (DNSEvent.customers.get(ratio.getKey()) == null) continue;
    			int tempCustId = DNSEvent.customers.get(ratio.getKey());

    			// get mappings for storing entry
    			if (!peerIds.containsKey(tempPeerIp)) {
    				myId = MainGeneric.getReporter().makeId("peerips", "ip", getString(peerMap, tempPeerIp));

    				if (myId == -1) {
    					continue;
    				}

    				peerIds.put(tempPeerIp, myId);
    			} else {
    				myId = peerIds.get(tempPeerIp);
    			}

    			if (!customerIds.containsKey(tempCustId)) {
    				customerId = MainGeneric.getReporter().makeId("customers", "name",
    						getString(DigResult.customers, tempCustId));

    				if (customerId == -1) {
    					continue;
    				}

    				customerIds.put(tempCustId, customerId);
    			} else {
    				customerId = customerIds.get(tempCustId);
    			}

    			synchronized(MainGeneric.getReporter()){
    			// store the entry
    			long entryId = MainGeneric.getReporter().insertRatioEntry(myId, 
    					customerId, ratio.getValue().getEntries().size());
    			if (entryId!=-1){

    				for (Entry<String, Double> edgeToRatio : ratio.getValue().getEntries()) {

    					// get cluster id
    					Integer tempClusterId = EdgeServerRatio.edgeClusters.get(edgeToRatio.getKey());
    					if (tempClusterId==null || tempClusterId == -1) continue;

    					if (tempClusterId == -1) {
    						edgeClusterId = -1;
    					} else if (!edgeClusterIds.containsKey(tempClusterId)) {
    						edgeClusterId = MainGeneric.getReporter().makeId("edgeclusters", "subnet",
    								edgeToRatio.getKey());

    						if (edgeClusterId == -1) {
    							continue;
    						}

    						edgeClusterIds.put(tempClusterId, edgeClusterId);
    					} else {
    						edgeClusterId = edgeClusterIds.get(tempClusterId);
    					}

    					MainGeneric.getReporter().insertRatio(entryId, edgeClusterId, edgeToRatio.getValue());
    					//commitRatiosOess();

    				}
    				
    			}
    			} // end synchronized 
    			
    		}
    		
    	}

    	MainGeneric.getReporter().flush();
    	MainGeneric.getReporter().disconnect();
        
    }
    
    public void commitBranchPoints() {
    	int myId = -1; 
    	int bpId = -1;

    	if (db == null) {
    		db = MainGeneric.getReporter();
    		if (!MainGeneric.getReporter().canConnect()) return;
    	}

    	if (!MainGeneric.getReporter().connect() || 
    			!OnoConfiguration.getInstance().isSendToDatabase()) return;
    	
    	myId = getSelfId();
    	
    	// we want to send these back
    	for (byte version : BRPPeerManager.supportedVersions){
    	HashMap<String, Double> bps = BRPPeerManager.getInstance().getMyBranchPoints(
    			BRPPeerManager.BP_WEIGHT_THRESHOLD, version);

    	for (String bp : bps.keySet()) addBranchPointForLookup(bp);
    	}
    	
    	batchProcessIds();
    	
    	for (byte version : BRPPeerManager.supportedVersions){
    		HashMap<String, Double> bps = BRPPeerManager.getInstance().getMyBranchPoints(
        			BRPPeerManager.BP_WEIGHT_THRESHOLD, version);
    	synchronized(MainGeneric.getReporter()){
    		

    		long entryId = MainGeneric.getReporter().insertBranchPointEntry(myId, version, bps.size());

    		if (entryId!=-1){

    			for (String branchPoint : bps.keySet()) {
    				// Get the id for this bp.
    				if (!branchPointIds.containsKey(branchPointMap.get(branchPoint))) {
    					int tempBpId = branchPointMap.get(branchPoint); 
    					bpId = MainGeneric.getReporter().makeId("branchpoints",
    							"subnet", branchPoint);
    					branchPointIds.put(tempBpId, bpId);
    				} else {
    					bpId = branchPointIds.get(branchPointMap.get(branchPoint));
    				}

    				MainGeneric.getReporter().insertBranchPoint(entryId, bpId, bps.get(branchPoint));
    			}
    		}
    	} // end synchronized
    	} // end for each version

    	MainGeneric.getReporter().flush();
    	MainGeneric.getReporter().disconnect();

    }
    
	@SuppressWarnings("unchecked")
	public void commitRatiosOess() {
		
    	int myId = -1;
    	int myDnsId = -1;
    	int customerId = -1;
    	int edgeClusterId = -1;
    	String peerIp = MainGeneric.getPublicIpAddress();

    	if (OnoPeerManager.getInstance().getOnoPeer(peerIp) == null)
    		return;
    	HashMapCache<String, HashMap<String, EdgeServerRatio>> ratios = OnoPeerManager.getInstance().getOnoPeer(peerIp).getAllPerDnsRatios();

    	if (DEBUG) {

    	for (Entry<String, HashMap<String, EdgeServerRatio>> map : ratios.entrySet()) {

    		System.out.println("START: " + map.getKey() + "===========");
    		for (Entry<String, EdgeServerRatio> ratio : (
    				(HashMap<String, EdgeServerRatio>)
    					((HashMap<String, EdgeServerRatio>)map.getValue())
    					.clone()).entrySet()){
    				System.out.println(ratio.getKey() +"=> "+ ratio.getValue());
    		}
    	}
 
    	}
 		
    	if (db == null) {
    		db = MainGeneric.getReporter();
    		if (!MainGeneric.getReporter().canConnect()) return;
    	}
    	if (!MainGeneric.getReporter().connect() || 
        		!OnoConfiguration.getInstance().isSendToDatabase()) return;
    	batchProcessIds();
    	

    	for (Entry<String, HashMap<String,EdgeServerRatio>> map : ratios.entrySet()) {
    		// get local peer id
    		
    		if (peerMap.get(peerIp)==null) continue;
    		int tempPeerIp = peerMap.get(peerIp);
    		
    		for (Entry<String, EdgeServerRatio> ratio : (
    				(HashMap<String, EdgeServerRatio>)
    					((HashMap<String, EdgeServerRatio>)map.getValue())
    					.clone()).entrySet()){

    			
    			// get local customer id
    			if (DNSEvent.customers==null || ratio == null) continue;
    			if (DNSEvent.customers.get(ratio.getKey()) == null) continue;
    			int tempCustId = DNSEvent.customers.get(ratio.getKey());

    			// get mappings for storing entry
    			if (!peerIds.containsKey(tempPeerIp)) {
    				myId = MainGeneric.getReporter().makeId("peerips", "ip", getString(peerMap, tempPeerIp));

    				if (myId == -1) {
    					continue;
    				}

    				peerIds.put(tempPeerIp, myId);
    			} else {
    				myId = peerIds.get(tempPeerIp);
    			}

    			
    			int tempPeerDnsIp = dnsMap.get(map.getKey());
        		
    			if (!dnsIds.containsKey(tempPeerDnsIp)) {
    				myDnsId = MainGeneric.getReporter().makeId("dnsips", "ip", getString(dnsMap, tempPeerDnsIp));
    				if (myDnsId == -1) {
    					continue;
    				}

    				dnsIds.put(tempPeerDnsIp, myDnsId);
    			} else {
    				myDnsId = dnsIds.get(tempPeerDnsIp);
    			}
    			
    			
    			
    			if (!customerIds.containsKey(tempCustId)) {
    				customerId = MainGeneric.getReporter().makeId("customers", "name",
    						getString(DigResult.customers, tempCustId));

    				if (customerId == -1) {
    					continue;
    				}

    				customerIds.put(tempCustId, customerId);
    			} else {
    				customerId = customerIds.get(tempCustId);
    			}

    			synchronized(MainGeneric.getReporter()){
    			// store the entry
    			int i=0;
    			long entryId = MainGeneric.getReporter().insertRatioOessEntry(myId, myDnsId, 
    					customerId, ratio.getValue().getEntries().size());
    			if (entryId!=-1){

    				for (Entry<String, Double> edgeToRatio : ratio.getValue().getEntries()) {
 
    					if (i>0) {
    						MainGeneric.getReporter().insertRatioOessEntry(myId, myDnsId, 
    		    					customerId, ratio.getValue().getEntries().size());
    					}
    					// get cluster id
    					Integer tempClusterId = EdgeServerRatio.edgeClusters.get(edgeToRatio.getKey());
    					if (tempClusterId==null || tempClusterId == -1) continue;

    					if (tempClusterId == -1) {
    						edgeClusterId = -1;
    					} else if (!edgeClusterIds.containsKey(tempClusterId)) {
    						edgeClusterId = MainGeneric.getReporter().makeId("edgeclusters", "subnet",
    								edgeToRatio.getKey());

    						if (edgeClusterId == -1) {
    							continue;
    						}

    						edgeClusterIds.put(tempClusterId, edgeClusterId);
    					} else {
    						edgeClusterId = edgeClusterIds.get(tempClusterId);
    					}

    					MainGeneric.getReporter().insertRatioOess(entryId, myDnsId, edgeClusterId, edgeToRatio.getValue());
    					i++;

    				}
    				
    			}
    			} // end synchronized 
    			
    		}
    		
    	}

    	MainGeneric.getReporter().flush();
    	MainGeneric.getReporter().disconnect();
        
    }
    
    
    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.utils.UTTimerEventPerformer#perform(org.gudy.azureus2.plugins.utils.UTTimerEvent)
     */
    public void perform(ITimerEvent event) {
    	try {
    	if (OnoConfiguration.getInstance().isRecordStats() 
    			&& OnoConfiguration.getInstance().isSendToDatabase()){
    		MainGeneric.getReporter().setExtendedConnection(true);
    		

	        reportDigs();
	        digResults.clear();
	        reportEvents();
	        synchronized (events) {events.clear();}
	        commitPings();
	        commitVivaldiPositions();
	        reportTraces();
	        commitErrors();
 	        reportTracesOess();
 	        commitPingsOess();
 	        
	        if (System.currentTimeMillis()>nextRatioUpdateTime){
	        	nextRatioUpdateTime = System.currentTimeMillis()+
	        		OnoConfiguration.getInstance().getRatioUpdateFrequencySec()*1000;
	        	commitRatios();
	        	commitBranchPoints();
	        }
	        
	        MainGeneric.getReporter().setExtendedConnection(false);
	        
        	MainGeneric.saveEdgeRatios();
    	}
    	else {
    		digResults.clear();
    		synchronized (events) {events.clear();}
    		pingResults.clear();
    		vivaldiResults.clear();
    		traceResults.clear();
    		
    	}
    	} catch (Exception e){
    		e.printStackTrace();
    	}
        
    	MainGeneric.getReporter().disconnect();
        //vivaldiResults.clear();
    }

	private void commitErrors() {
		if (db == null) {
            db = MainGeneric.getReporter();
        }
        if (!MainGeneric.getReporter().connect()) return;
		while (errors.size()>0){
			synchronized(errors){
				Exception e = errors.iterator().next();
				MainGeneric.getReporter().reportError(e);
				errors.remove(e);
			}
		}
		
		MainGeneric.getReporter().disconnect();
		
	}

	public void addVivaldiResult(VivaldiResult v) {
		if (Math.random()*100 < 
				OnoConfiguration.getInstance().getDropVivaldiPct()
				&& v.peerIp!=v.observedBy){
			return;
		}
		
		synchronized (vivaldiResults) {
        	if (vivaldiResults.size()>=MAX_VIVALDI_RESULTS){
        		commitVivaldiPositions();
        	}
        	vivaldiResults.add(v);
        }

        // TODO update size
        /*if (vivaldiResults.size() > 100) {
            commitVivaldiPositions();
            lastUpdate = System.currentTimeMillis();
            nextUpdateInterval = UPDATE_INTERVAL +
                (long) (r.nextDouble() * JITTER_BASE);
        }*/
		
	}

	private void commitVivaldiPositions() {
		int peerId = -1;
		int observedById = -1;

        if (db == null) {
            db = MainGeneric.getReporter();
        }
        if (!MainGeneric.getReporter().connect() || 
        		!OnoConfiguration.getInstance().isSendToDatabase()) return;
        
        batchProcessIds();
        
        LinkedList<VivaldiResult> vivaldisToRemove = new LinkedList<VivaldiResult>();

        // update map of ids from database
        synchronized (vivaldiResults) {
            for (VivaldiResult v : vivaldiResults) {
                if (db == null) {
                    db = MainGeneric.getReporter();
                    if (!MainGeneric.getReporter().connect()) return;
                }

                // get peer id
                if (!peerIds.containsKey(v.peerIp)) {
                	peerId = MainGeneric.getReporter().makeId("peerips", "ip",
                            getString(peerMap, v.peerIp));

                    if (peerId == -1) {
                        continue;
                    }

                    peerIds.put(v.peerIp, peerId);
                } else {
                	peerId = peerIds.get(v.peerIp);
                }
                
                // get local id
                if (!peerIds.containsKey(VivaldiResult.observedBy)) {
                	observedById = MainGeneric.getReporter().makeId("peerips", "ip",
                            getString(peerMap, VivaldiResult.observedBy));

                    if (observedById == -1) {
                        continue;
                    }

                    peerIds.put(VivaldiResult.observedBy, observedById);
                } else {
                	observedById = peerIds.get(VivaldiResult.observedBy);
                }

                if (MainGeneric.getReporter().insertVivaldiResult(v.timestamp, peerId, observedById, 
                		v.getX(), v.getY(), v.getH(), 
                		v.errorEstimate, v.rttEstimate,
                		v.pingResult, v.rttEstimateV2, v.getV2Coords(), DHTNetworkPosition.POSITION_TYPE_VIVALDI_V2)) {
                    
                	vivaldisToRemove.add(v);
                }
            }
        }

        synchronized (vivaldiResults) {
            vivaldiResults.removeAll(vivaldisToRemove );
        }
        MainGeneric.getReporter().disconnect();
		
	}

	public void reportError(Exception e) {
		errors.add(e);
		
	}

	public void addTraceRouteResult(TraceResult tr) {
        synchronized (traceResults) {
        	if (traceResults.size()>MAX_TRACE_RESULTS){
        		reportTraces();
        	}
        	traceResults.add(tr);
        }		
	}	

	public void addTraceRouteResultOess(TraceResult tr) {
        synchronized (traceResultsOess) {
        	if (traceResultsOess.size()>MAX_TRACE_RESULTS){
        		reportTracesOess();
        	}
        	traceResultsOess.add(tr);
        }		
	}	

	public void addTimeoutOess(String dnsIp, String myIp, long timeout) {
  
		int myDnsId = -1;
    	int myId = -1;
    	
		if (!dnsIds.containsKey(dnsIp)) {
			myDnsId = MainGeneric.getReporter().makeId("dnsips", "ip", dnsIp);

		} else {
			myDnsId = dnsIds.get(dnsIp);
		}
 		
 		myId = peerIds.get(peerMap.get(myIp));
  
 		MainGeneric.getReporter().insertTimeoutOess(myDnsId, myId, timeout);
        
    }
	
	
	
    public void reportTraces() {
    	int sourceId = -1;
    	int destId = -1;

    	if (db == null) {
    		db = MainGeneric.getReporter();
    	}
    	if (!MainGeneric.getReporter().connect() || 
        		!OnoConfiguration.getInstance().isSendToDatabase()) return;
    	
    	batchProcessIds();

    	while (traceResults.size()>0){
    		TraceResult tr = null;
    		// update map of ids from database
    		synchronized (traceResults){
    			Iterator<TraceResult> it = traceResults.iterator();
    			tr = it.next();
    			it.remove();
    		}
    		
//    		 get mappings for storing entry
    		addPeer(tr.source);
    		int tempPeerIp = peerMap.get(tr.source);
			if (!peerIds.containsKey(tempPeerIp)) {
				sourceId = MainGeneric.getReporter().makeId("peerips", "ip", getString(peerMap, tempPeerIp));

				if (sourceId == -1) {
					continue;
				}

				peerIds.put(tempPeerIp, sourceId);
			} else {
				sourceId = peerIds.get(tempPeerIp);
			}
			
			addPeer(tr.dest);
			tempPeerIp = peerMap.get(tr.dest);
			if (!peerIds.containsKey(tempPeerIp)) {
				destId = MainGeneric.getReporter().makeId("peerips", "ip", getString(peerMap, tempPeerIp));

				if (destId == -1) {
					continue;
				}

				peerIds.put(tempPeerIp, destId);
			} else {
				destId = peerIds.get(tempPeerIp);
			}
    		
			
			int lastIndex = tr.entries.size()-1;
			float rtts[];
			for (int i = tr.entries.size()-1; i >=0 ; i--){
				rtts = tr.entries.get(i).rtt;
				if (rtts[0]+rtts[1]+rtts[2]==-3){
					lastIndex = i;
				} else {
					break;
				}
			}
			
			 synchronized(MainGeneric.getReporter()){
    		// make top-level entry
    		long entryId = MainGeneric.getReporter().insertTraceEntry(
    				sourceId, destId, tr.timestamp, lastIndex);
    		
    		// if valid, insert trace route entries
    		if (entryId>-1){

    			TraceEntry te;
    			for (int j = 0; j <= lastIndex; j++){
    				te = tr.entries.get(j);
    				int myRouterIds[] = new int[3];
    				for (int i = 0; i < te.router.length; i++){
    					String router;
    					
    					if (te.numRouters-1 < i) {
    						router = te.router[0];
    					} else {
    						router = te.router[i];
    					}
    					
		    			if (!routerMap.containsKey(router)){
		    				routerMap.put(router,routerMap.size());
		    			}
		    			int tempRouterId = routerMap.get(router);
		    			if (routerIds.get(tempRouterId)==null) {
		    				myRouterIds[i] = MainGeneric.getReporter().makeId("routerips", "ip", 
		    						getString(routerMap, tempRouterId));
		
		    				if (myRouterIds[i] == -1) {
		    					continue;
		    				}
		
		    				routerIds.put(tempRouterId, myRouterIds[i]);
		    			} else {
		    				myRouterIds[i] = routerIds.get(tempRouterId);
		    			}
    				} // end for router
    				
    				// insert data
    				MainGeneric.getReporter().insertTraceEntryData(entryId, myRouterIds, te);
    			} // end for trace entries
    			//MainGeneric.getReporter().flush();
    			
    		} // end case valid entry
    		else {
    			traceResults.add(tr);
    			break; // exit for now
    		}
			 } // end synchronized part
    		
    	}

    	MainGeneric.getReporter().disconnect();

	}

    public void reportTracesOess() {
    	int sourceId = -1;
    	int destId = -1;
    	int destType = -1;

    	if (db == null) {
    		db = MainGeneric.getReporter();
    	}
    	if (!MainGeneric.getReporter().connect() || 
        		!OnoConfiguration.getInstance().isSendToDatabase()) return;
    	
    	batchProcessIds();

    	while (traceResultsOess.size()>0){
    		TraceResult tr = null;
    		// update map of ids from database
    		synchronized (traceResultsOess){
    			Iterator<TraceResult> it = traceResultsOess.iterator();
    			tr = it.next();
    			it.remove();
    		}
    		
    		
//    		 get mappings for storing entry
//
              if (!peerIds.containsKey(peerMap.get(tr.source))) {
                sourceId = MainGeneric.getReporter().makeId("peerips", "ip", tr.source);
                
                if (sourceId == -1)
                	return;

                peerIds.put(peerMap.get(tr.source), sourceId);
            }

//
    		
    		sourceId = peerIds.get(peerMap.get(tr.source));
            destType = tr.destType;
            
            if (tr.destType == 1)
            	destId = dnsIds.get(dnsMap.get(tr.dest));
                		
            if ((tr.destType ==2) && (edgeServerMap.get(tr.dest) != null ))
            		destId = edgeServerIds.get(edgeServerMap.get(tr.dest));
			
			int lastIndex = tr.entries.size()-1;
			float rtts[];
			for (int i = tr.entries.size()-1; i >=0 ; i--){
				rtts = tr.entries.get(i).rtt;
				if (rtts[0]+rtts[1]+rtts[2]==-3){
					lastIndex = i;
				} else {
					break;
				}
			}
			
			 synchronized(MainGeneric.getReporter()){
    		// make top-level entry
    		long entryId = MainGeneric.getReporter().insertTraceEntryOess(
    				sourceId, destId, tr.timestamp, lastIndex, destType);
    		
    		// if valid, insert trace route entries
    		if (entryId>-1){

    			TraceEntry te;
    			for (int j = 0; j <= lastIndex; j++){
    				te = tr.entries.get(j);
    				int myRouterIds[] = new int[3];
    				for (int i = 0; i < te.router.length; i++){
    					String router;
    					
    					if (te.numRouters-1 < i) {
    						router = te.router[0];
    					} else {
    						router = te.router[i];
    					}
    					
		    			if (!routerMap.containsKey(router)){
		    				routerMap.put(router,routerMap.size());
		    			}
		    			int tempRouterId = routerMap.get(router);
		    			if (routerIds.get(tempRouterId)==null) {
		    				myRouterIds[i] = MainGeneric.getReporter().makeId("routerips", "ip", 
		    						getString(routerMap, tempRouterId));
		
		    				if (myRouterIds[i] == -1) {
		    					continue;
		    				}
		
		    				routerIds.put(tempRouterId, myRouterIds[i]);
		    			} else {
		    				myRouterIds[i] = routerIds.get(tempRouterId);
		    			}
    				} // end for router
    				
    				// insert data
    				MainGeneric.getReporter().insertTraceEntryDataOess(entryId, myRouterIds, te);
    			} // end for trace entries
//    			MainGeneric.getReporter().flush();
    			
    		} // end case valid entry
    		else {
    			traceResultsOess.add(tr);
    			break; // exit for now
    		}
			 } // end synchronized part
    		
    	}

    	MainGeneric.getReporter().disconnect();

	}
    
	public int getPeerId(String ip) {
		if (!OnoConfiguration.getInstance().isRecordStats() || 
				!OnoConfiguration.getInstance().isSendToDatabase()) return -1;
		if (peerMap.get(ip)==null || (peerIds.get(
				peerMap.get(ip))==null || peerIds.get(
						peerMap.get(ip))==-1)){
			if (!addPeer(ip)) 
				return -1;
		}
		if ( peerIds.get(
				peerMap.get(ip)) == null ) {
			return -1;
		}
		return peerIds.get(
				peerMap.get(ip));
	}

	public int getPeerClientId(String client) {
		if (!OnoConfiguration.getInstance().isRecordStats() || 
				!OnoConfiguration.getInstance().isSendToDatabase()) return -1;
		if (client.matches("(.*)\\[(.*)\\]") || 
				client.contains("Unknown") || client.contains("Inconnu") || 
				client.contains("Ukjent") || client.contains("Unbekannt")
				|| client.contains("Sconosciuto")) {
			if (client.toLowerCase().contains("exeem")) client = "Exeem";
			else client = "Unknown";
		}
			
		if (peerClientMap.get(client)==null){
			if (!addPeerClient(client)) return -1;
		}
		if (peerClientIds.get(peerClientMap.get(client))==null) return -1;
		else return peerClientIds.get(peerClientMap.get(client));
	}

	public void setEdgeRtt(String name, String ipAddressResponse, double rtt) {
		edgePings.put(name, new Pair<String, Double>(ipAddressResponse, rtt));		
	}
	
	public Map<String, Pair<String, Double>> getEdgeRtts(){
		return edgePings;
	}

	public int getSelfId() {
//		if (selfId  == -1){
			selfId = getPeerId(MainGeneric.getPublicIpAddress());
//		}
		return selfId;
	}
	
	public void stop(){
		if (timer!=null){
			timer.destroy();
		}
		self = null;
		customerIds.clear();
		synchronized (dataTransferResults) {dataTransferResults.clear();}
		db = null;
		digResults.clear();
		edgeClusterIds.clear();
		edgeIds.clear();
		edgePings.clear();
		edges.clear();
		synchronized(events){events.clear();}
		peerClientIds.clear();
		peerClientMap.clear();
		peerIds.clear();
		peerMap.clear();
		peers.clear();
		pingResults.clear();
		r  = null;
		routerIds.clear();
		routerMap.clear();
		traceResults.clear();
		vivaldiResults.clear();
	}
	
	public int batchProcessTable(String tableName, String columnName, 
			HashMap<String, Integer> localMap, HashMap<Integer, Integer> dbMap){
		
		int numRemaining = 0;
		synchronized(localMap){
			LinkedList<String> keys = new LinkedList<String>();
			for (Entry<String, Integer> ent : localMap.entrySet()){
				 if (!dbMap.containsKey(ent.getValue())){
					 keys.add(ent.getKey());
				 }
			}
			
			if (keys.size()==0) return 0;
			int size = 0;
			for (String s : keys) size+=s.length();
			if (size==0) return 0;
			Map<String, Integer> dbKeys = MainGeneric.getReporter().makeIdBatch(tableName, 
					columnName, keys);
			
			if (dbKeys==null) return 0;
			
			int numFound = 0;
			for (Entry<String, Integer> ent: dbKeys.entrySet()){
				dbMap.put(localMap.get(ent.getKey()), ent.getValue());
				numFound++;
			}
			numRemaining = keys.size()-numFound;
		}
		return numRemaining;
	}
	
	public void batchProcessIds(){
		if (!OnoConfiguration.getInstance().isRecordStats() || 
				!OnoConfiguration.getInstance().isSendToDatabase()) return;
		
		int totalLeft;
		int numLoops = 0; // to ensure termination
		do {
			totalLeft = 0;
			try {
			totalLeft+= batchProcessTable("dnsips", "ip", dnsMap, dnsIds);
			totalLeft+= batchProcessTable("peerips", "ip", peerMap, peerIds);		
			totalLeft+= batchProcessTable("peerclients", "name", peerClientMap, peerClientIds);
			totalLeft+= batchProcessTable("routerips", "ip", routerMap, routerIds);
			totalLeft+= batchProcessTable("customers", "name",  DNSEvent.customers, customerIds);
			totalLeft+= batchProcessTable("edgeservers", "ipaddress", DNSEvent.edges, edgeIds);
			totalLeft+= batchProcessTable("edgeclusters", "subnet", EdgeServerRatio.edgeClusters, 
					edgeClusterIds);
			totalLeft+= batchProcessTable("edgeservers", "ipaddress", edgeServerMap, edgeServerIds);
			totalLeft+= batchProcessTable("branchpoints", "subnet", branchPointMap, branchPointIds);
			
			} catch (Exception e){
				// TODO log
			}
			numLoops++;
		} while (totalLeft>0 && MainGeneric.getReporter().canConnect()
				&& numLoops < 100);
		if (numLoops==100) MainGeneric.getReporter().flush();

	}
	
	/** 
	 * Add peer only for lookup, don't connect to DB to get id
	 * @param peer
	 * @return
	 */
	public void addPeerForLookup(String peer) {
       
        synchronized (peerMap) {
            if (peerMap.get(peer) == null) {
                peerMap.put(peer, peerMap.size());
            }

        }
       peers.add(peer);
	}

	/**
	 * Register peer client name for DB lookup
	 * @param peerClient
	 */
	public void addPeerClientForLookup(String peerClient) {
		synchronized (peerClientMap) {
            if (peerClientMap.get(peerClient) == null) {
            	peerClientMap.put(peerClient, peerClientMap.size());
            }
		}
		
	}
	
	
	/**
	 * Register peer client name for DB lookup
	 * @param peerClient
	 */
	public void addRouterForLookup(String routerIp) {
		synchronized (routerMap) {
            if (routerMap.get(routerIp) == null) {
            	routerMap.put(routerIp, routerMap.size());
            }
		}
		
	}

	public void addDnsForLookup(String dnsIp) {
		synchronized (dnsMap) {
            if (dnsMap.get(dnsIp) == null) {
            	dnsMap.put(dnsIp, dnsMap.size());
            }
		}
		
	}

	public void addEdgeServerForLookup(String edgeServerIp) {
		synchronized (edgeServerMap) {
            if (edgeServerMap.get(edgeServerIp) == null) {
            	edgeServerMap.put(edgeServerIp, edgeServerMap.size());
            }
		}
		
	}
	
	public void addBranchPointForLookup(String bpIp) {
		synchronized (branchPointMap) {
            if (branchPointMap.get(bpIp) == null) {
            	branchPointMap.put(bpIp, branchPointMap.size());
            }
		}
		
	}

	
	public void reportIpChange(String oldIp, String newIp, String localIp,
			long currentGMTTime) {
		int pub = getPeerId(oldIp);
		int nouvel = getPeerId(newIp);
		int local = getPeerId(localIp);
		MainGeneric.getReporter().reportIpChange(pub, nouvel, local, currentGMTTime);
		
	}
       
}
