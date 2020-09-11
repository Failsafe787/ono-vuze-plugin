/**
 * Ono Project
 *
 * File:         PingRunner.java
 * RCS:          $Id: PingRunner.java,v 1.16 2010/03/29 16:48:04 drc915 Exp $
 * Description:  PingRunner class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 19, 2006 at 3:11:27 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.experiment
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
package edu.northwestern.ono.experiment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Map.Entry;

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.messaging.MessageManagerListener;
import org.gudy.azureus2.plugins.network.Connection;
import org.gudy.azureus2.plugins.network.IncomingMessageQueueListener;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.utils.Monitor;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.dht.IDDBReadAction;
import edu.northwestern.ono.dht.IDistributedDatabaseEvent;
import edu.northwestern.ono.stats.Statistics;
import edu.northwestern.ono.timer.ITimer;
import edu.northwestern.ono.util.PingManager;
import edu.northwestern.ono.util.PluginInterface;
import edu.northwestern.ono.util.Util;
import edu.northwestern.ono.util.Util.PingResponse;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The PingRunner class ...
 */
public class PingRunner implements IExperimentRunner, MessageManagerListener,
    IncomingMessageQueueListener {
    private static final boolean DEBUG = true;
    private static PingRunner self;
    private static PluginInterface pi;

    /** list of current OnoExperiments */
    private ArrayList<OnoExperiment> experiments;

    private PingRunner() {
       
        experiments = new ArrayList<OnoExperiment>();
        pi = MainGeneric.getPluginInterface();
        if (MainGeneric.isAzureus())
        	pi.getMessageManager()
        	.locateCompatiblePeers(pi, new PingExperimentMessage(), this);
    }

    /**
     * @param result
     */
    public void processExperiment(OnoExperiment onoExp) {
        if (DEBUG) {
            System.out.println("Found experiment!");
        }

        // do checks to see if we are even going to do this
        if (onoExp.version < OnoExperiment.CURRENT_VERSION) {
            return;
        }

        Calendar myTime = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        if (!experiments.contains(onoExp)) {
            experiments.add(onoExp);
        }

        // TODO remove this debugging garbage when done
        if (onoExp.beginTime == -1) {
            // special value for making experiment start in ten seconds
            onoExp.beginTime = Util.currentGMTTime() + (10 * 1000);
            onoExp.endTime = onoExp.beginTime + (60 * 10 * 1000);

            // make it last ten minutes...
        }

        // now check the start time and discard if it's too late
        long timeDiff = onoExp.beginTime - Util.currentGMTTime();

        if (timeDiff <= 0) {
            if (onoExp.endTime > Util.currentGMTTime()) {
                timeDiff = 0;
            } else {
                return;
            }
        }

        if (DEBUG) {
            System.out.println("Starting experiment in " + (timeDiff / 1000) +
                " seconds...");
        }

        ITimer timer = MainGeneric.createTimer("Experiment" + onoExp.beginTime);
        timer.addEvent(System.currentTimeMillis() + timeDiff,
            new ExperimentTimer(this, onoExp, timer));
    }

    // TODO figure out all details of all graphs before running more experiments
    public void beginExperiment(OnoExperiment onoExp) {
        String ipAddress = onoExp.getOtherEndIp();
        int port = onoExp.getPort();

        // TODO fix this hack
        // I am currently using the DHT to get edge server info
        MainGeneric
          .createThread("OnoExperimentPeerPinger",
            new OnoExperimentPeerPinger(onoExp, null));

        /*for (Download d : pi.getDownloadManager().getDownloads()){
            if (d.getPeerManager()!=null ){
                for (Peer p : d.getPeerManager().getPeers()){
                    if (p.getIp().equals(onoExp.otherEndIp)){
                        p.getConnection().getIncomingMessageQueue().registerListener(
                                this);
        
                        PingExperimentMessage pem = new PingExperimentMessage();
                        Map<String, HashSet<String>> myEdgeServers = Digger.getInstance().getMyEdgeServers();
                        HashSet<String> edgesToSend = new HashSet<String>();
                        // grab all edges and add unique ones to message
                        for (String cust : myEdgeServers.keySet()){
                            HashSet<String> edges = myEdgeServers.get(cust);
                            for (String edge: edges){
                                if (edgesToSend.add(edge)) pem.addEdgeIp(edge);
                            }
                        }
                        p.getConnection().getOutgoingMessageQueue().sendMessage(pem);
        
                    }
                }
        
        
        
        
        
        
            }
        }*/
    }

    public static PingRunner getInstance() {
        if (self == null) {
            self = new PingRunner();
        }

        return self;
    }

    public void compatiblePeerFound(Download download, Peer peer,
        Message message) {
        for (final OnoExperiment onoExp : experiments) {
            if (!peer.getIp().equals(onoExp.otherEndIp)) {
                continue;
            }

            Connection c = peer.getConnection();

            //c.connect(null);

            /*c.getIncomingMessageQueue().registerListener(new IncomingMessageQueueListener(){
            
                public boolean messageReceived(Message message) {
                    System.err.println("Received Message!");
                    if (message instanceof PingExperimentMessage){
                        PingExperimentMessage pem = (PingExperimentMessage)message;
                        if (experimentSeen.contains(String.valueOf(onoExp.getExperimentId()))) return false;
                        // grab edge server ips and start extracting peers
                        pi.getUtilities().createThread("OnoExperimentPeerPinger",
                                new OnoExperimentPeerPinger(onoExp, pem.edgeServerIps));
                        experimentSeen.add(String.valueOf(onoExp.getExperimentId()));
                        return true;
                    }
                    return false;
                }
            
                public void bytesReceived(int byte_count) {
                    return; // Don't care
                }
            
            });
            
            PingExperimentMessage pem = new PingExperimentMessage();
            Map<String, HashSet<String>> myEdgeServers = dig.getMyEdgeServers();
            HashSet<String> edgesToSend = new HashSet<String>();
            // grab all edges and add unique ones to message
            for (String cust : myEdgeServers.keySet()){
                HashSet<String> edges = myEdgeServers.get(cust);
                for (String edge: edges){
                    if (edgesToSend.add(edge)) pem.addEdgeIp(edge);
                }
            }
            c.getOutgoingMessageQueue().sendMessage(pem);
            //                System.out.println("Sent Message!");
            */
        }
    }

    public void peerRemoved(Download download, Peer peer) {
        // TODO Auto-generated method stub
    }

    public boolean messageReceived(Message message) {
        System.err.println("Received Message!");

        if (message instanceof PingExperimentMessage) {
            PingExperimentMessage pem = (PingExperimentMessage) message;

            // grab edge server ips and start extracting peers
            return true;
        }

        return false;
    }

    public void bytesReceived(int byte_count) {
        return; // Don't care
    }

    /**
    * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
    *
    * The OnoExperimentPeerPinger class ...
    */
    public class OnoExperimentPeerPinger implements Runnable, IDDBReadAction {
        /** time between rounds of pinging (milliseconds) */
        private static final long SLEEP_TIME = 15 * 1000;
        OnoExperiment onoExp;
        private ArrayList<String> edges;

        /** a set of pairs of edge server -> peer that have been observed */
        private HashMap<String, ArrayList<String>> peers;
        private ByteArrayInputStream bais;
        private ObjectInputStream ois;
        private String[] output;
        private Monitor peer_mon;

        /**
         * @param onoExp
         * @param edges
         *
         */
        public OnoExperimentPeerPinger(OnoExperiment onoExp,
            ArrayList<String> edges) {
            super();
            this.onoExp = onoExp;

            if (edges == null) {
                this.edges = new ArrayList<String>();
            } else {
                this.edges = edges;
            }

            peers = new HashMap<String, ArrayList<String>>();
            peers.put(String.valueOf(-1), new ArrayList<String>()); // add the direct path
            peers.get(String.valueOf(-1)).add(onoExp.otherEndIp);
            this.peer_mon = pi.getUtilities().getMonitor();
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            String myIp = null;

            while (myIp == null) {
                myIp = MainGeneric.getPublicIpAddress();

                try {
                    Thread.sleep(15 * 1000); // don't start until DHT manager is working
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // start a new DHT manager and listener for these edges
            while (MainGeneric.getDistributedDatabase() == null) {
                System.err.println("DHT Manager is null!");

                try {
                    Thread.sleep(15 * 1000); // don't start until DHT manager is working
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // get Calendar for time
            Calendar myCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

            // set up while loop to terminate at end of experiment
            long myTime = Util.currentGMTTime();
            long diff = onoExp.endTime - myTime;
            long endLocalTime = Util.currentGMTTime() + diff;

            // while experiment is still active
            while (Util.currentGMTTime() < endLocalTime) {
                long begin = Util.currentGMTTime(); // time this inner loop

                // start up DHT reads
                synchronized (edges) {
                    for (String edge : edges)
                    	MainGeneric.getDistributedDatabase().read(
                    			edge, edge, 30, this, MainGeneric.getDistributedDatabase().getOptions().getExhaustiveRead());
                      
                }

                MainGeneric.getDistributedDatabase()
                      .read(onoExp.otherEndIp, onoExp.otherEndIp, 30, this,
                    		  MainGeneric.getDistributedDatabase().getOptions().getExhaustiveRead());

                // hash set to keep track of peers seen
                HashMap<String, Double> seen = new HashMap<String, Double>();
                // feed from a list of peers to ping
                peer_mon.enter();

                for (Entry<String, ArrayList<String>> pair : peers.entrySet()) {
                    for (String ip : pair.getValue()) {
                        doPing(ip, myIp, pair.getKey(), seen);
                    } // end nested for

                    doPing(pair.getKey(), myIp, pair.getKey(), seen);
                } // end for

                peer_mon.exit();

                // try to insert ping results before next round...
                Statistics.getInstance().commitPings();

                long end = Util.currentGMTTime();
                long sleepTime = (SLEEP_TIME - (end - begin));

                if (sleepTime < 0) {
                    sleepTime = 0;
                }

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } // end while experiment still active 
        }

        /**
         * @param ip the ip to ping
         * @param seen a set of values already seen
         * @param myIp my own ip address
         * @param edgeServer the associated edge server
         */
        private void doPing(final String ip, String myIp, String edgeServer,
            final HashMap<String, Double> seen) {
            if (ip.equals(myIp)) {
                return;
            }

            // for each middle point (and the other peer)
            // do ping
            if (seen.get(ip) == null) {
                // this will have to be done using Runtime.process...
            	PingManager.getInstance().doPing(ip, pi, new PingResponse(){

					public void response(double rtt) {
						seen.put(ip, rtt);
						
					}});
                
            } // end if not seen

            // record in DB
            Statistics.getInstance()
                      .addPingResult(edgeServer, ip, onoExp.getOtherEndIp(),
                myIp, seen.get(ip),
                Util.currentGMTTime(), onoExp.experimentId);
        }

        /* (non-Javadoc)
         * @see edu.northwestern.ono.dht.IDHTReadAction#handleRead(byte[], org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent)
         */
        public void handleRead(byte[] b, IDistributedDatabaseEvent event) {
            System.out.println("Value read: " + new String(b));

            byte[] bytes = b;
            bais = new ByteArrayInputStream(bytes);

            try {
                ois = new ObjectInputStream(bais);

                Object o = ois.readObject();

                if ((o instanceof String[])) {
                    output = (String[]) o;

                    //                  System.out.println("Value read from DB: " + 
                    //                  "(" + event.getKey().getDescription() + ", " +
                    //                  Arrays.deepToString(output) + ")");

                    // quit if using old version
                    if (Constants.compareVersions(pi.getPluginVersion(),
                                output[0]) < 0) {
                        return;
                    }

                    // get existing entries
                    if (event.getKey().getDescription().equals(onoExp.otherEndIp)) {
                        for (String value : output) {
                            if (value.charAt(0) == '/') {
                                value = value.substring(1);
                            }

                            if (org.xbill.DNS.Address.isDottedQuad(value)) {
                                if (!edges.contains(value)) {
                                    synchronized (edges) {
                                        edges.add(value);
                                    }
                                }
                            }
                        }
                    } else {
                        for (String value : output) {
                            if (value.charAt(0) == '/') {
                                value = value.substring(1);
                            }

                            if (org.xbill.DNS.Address.isDottedQuad(value)) {
                                peer_mon.enter();

                                if (!value.equals(onoExp.otherEndIp)) {
                                    if (peers.get(event.getKey().getDescription()) == null) {
                                        peers.put(event.getKey().getDescription(),
                                            new ArrayList<String>());
                                    }

                                    ArrayList<String> ps = peers.get(event.getKey()
                                                                          .getDescription());

                                    if (!ps.contains(value)) {
                                        ps.add(value);
                                    }
                                }

                                peer_mon.exit();
                            }
                        }
                    }
                }

                // while we're here, issue another read

                MainGeneric.getDistributedDatabase()
                .read((String) event.getKey().getKey(),
                event.getKey().getDescription(), 30, this,
              		  MainGeneric.getDistributedDatabase().getOptions().getExhaustiveRead());

            } catch (IOException e) {
                System.err.println(e.toString());

                // generally a corrupted stream and i have to figure out why
                //e.printStackTrace();
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
//            } catch (DistributedDatabaseException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
            } finally {
                //peer_mon.exit();
            }
        }

        /* (non-Javadoc)
         * @see edu.northwestern.ono.dht.IDHTReadAction#handleTimeout(org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent)
         */
        public void handleTimeout(IDistributedDatabaseEvent event) {
//            try {
            	 MainGeneric.getDistributedDatabase()
                 .read((String) event.getKey().getKey(),
                 event.getKey().getDescription(), 30, this,
               		  MainGeneric.getDistributedDatabase().getOptions().getExhaustiveRead());
//
//            } catch (DistributedDatabaseException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
        }

        /* (non-Javadoc)
         * @see edu.northwestern.ono.dht.IDHTReadAction#handleComplete(org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent)
         */
        public void handleComplete(IDistributedDatabaseEvent event) {
            // don't care            
        }
    }
}
