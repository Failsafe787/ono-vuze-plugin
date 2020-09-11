/**
 * Ono Project
 *
 * File:         BambooDHTManager.java
 * RCS:          $Id: BambooDHTManager.java,v 1.42 2010/03/29 16:48:04 drc915 Exp $
 * Description:  BambooDHTManager class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 31, 2007 at 10:44:37 AM
 * Language:     Java
 * Package:      edu.northwestern.ono.dht
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
package edu.northwestern.ono.dht.bamboo;

import static org.acplt.oncrpc.OncRpcProtocols.ONCRPC_TCP;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.Map.Entry;

import org.acplt.oncrpc.OncRpcException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import seda.sandStorm.api.ConfigDataIF;
import bamboo.dht.GatewayClient;
import bamboo.dht.bamboo_get_args;
import bamboo.dht.bamboo_get_result;
import bamboo.dht.bamboo_get_value;
import bamboo.dht.bamboo_hash;
import bamboo.dht.bamboo_key;
import bamboo.dht.bamboo_placemark;
import bamboo.dht.bamboo_put_arguments;
import bamboo.dht.bamboo_rm_arguments;
import bamboo.dht.bamboo_value;
import bamboo.dht.gateway_protClient;
import bamboo.lss.ASyncCore;
import bamboo.lss.ASyncCoreImpl;
import bamboo.lss.DustDevil;
import bamboo.util.Curry;
import bamboo.util.Pair;
import bamboo.util.StandardStage;
import bamboo.util.Curry.Thunk1;
import bamboo.util.Curry.Thunk3;
import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.dht.IDDBDeleteAction;
import edu.northwestern.ono.dht.IDDBReadAction;
import edu.northwestern.ono.dht.IDDBWriteAction;
import edu.northwestern.ono.dht.IDHTOption;
import edu.northwestern.ono.dht.IDistributedDatabase;
import edu.northwestern.ono.timer.ITimer;
import edu.northwestern.ono.timer.ITimerEvent;
import edu.northwestern.ono.timer.ITimerEventPerformer;
import edu.northwestern.ono.util.HashMapCache;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The BambooDHTManager class ...
 */
public class BambooDHTManager extends StandardStage implements IDistributedDatabase, Runnable  {

    protected static final int BRANCHING = (1024 - 4) / 20;
    protected static final int MAX_BUFFER = 100;
    protected static final int MAX_PARALLEL = 100;
    protected static final String APPLICATION = "OpenDHT Ono Interface";
	private static BambooDHTManager self;
	private static final boolean ENABLED = true;
	public static final boolean DEBUG = false;
	private static final int DEFAULT_TIMEOUT = 30;
	public static final int DEFAULT_TTL = 30*60;
	private static final boolean USE_GATEWAY = false;

    protected int ttl = 0;
    protected static MessageDigest md;
    protected static GatewayClient client; 
//    protected FileInputStream is; 
//    protected FileOutputStream os; 
    protected static byte [] secret; 
    protected static byte [] secret_hash;
    protected LinkedList<Pair<byte[],ByteBuffer>> ready;
    protected Vector<LinkedList<ByteBuffer>> wblocks;
//    protected Vector<PriorityQueue> rblocks;
//    protected Vector<Long> rnext;
    protected int outstanding;
//    protected byte [] key;
	private static ASyncCore acore;
	private static String secretString = "OnoRulesTwentyChars!";
	
	private static int MAX_THREADS = 10;
	private static int activeThreads = 0;
	private HashMapCache<String, Runnable> pendingRuns = new HashMapCache<String, Runnable>();
	
	private HashMap<String, Integer> staticServers = new HashMap<String, Integer>();
	
    /** set of DDB keys for which deletes are in progress. used to suppress multiple deletes to same key */
    private HashMap<String, Long> deletesInProgress = new HashMap<String, Long>();
	
    /** set of DDB keys for which deletes are in progress. used to suppress multiple deletes to same key */
    private HashMap<String, Long> writesInProgress = new HashMap<String, Long>();
    
    /** set of DDB keys for which deletes are in progress. used to suppress multiple deletes to same key */
    private HashMap<String, Long> readsInProgress = new HashMap<String, Long>();
    
    private HashMap<String, PendingRead> 
    	pendingReads = new HashMap<String, PendingRead>();
    private HashMap<String, PendingWrite> 
    	pendingWrites = new HashMap<String, PendingWrite>();
    private HashMap<String, PendingDelete> 
    	pendingDeletes = new HashMap<String, PendingDelete>();
	private boolean useStatic;
	private Random random = new Random();
	private static final boolean DO_DELETE = false;

    
	// stats
	private static int numReads;
	private static int numWrites;
	private static int numDeletes;
	
	
	private static class PendingRead{
		
		public bamboo_get_args get;
		public int options;
		public IDDBReadAction action;
		public Long seq;
		public String key;
		
	}
	
	private static class PendingWrite{
		public String key;
		public ArrayList<ByteArrayOutputStream> values;
		public int timeout;
		public IDDBWriteAction action;
		
		
	}
	
	private static class PendingDelete{
		final String key;
		final int timeout;
		final IDDBDeleteAction action;
		private ArrayList<ByteArrayOutputStream> values;
		
		public PendingDelete(final String key, final int timeout, 
				final IDDBDeleteAction action, ArrayList<ByteArrayOutputStream> values){
			this.key = key;
			this.timeout = timeout;
			this.action = action;
			this.values = values;
		}
		
	}
	
    
    public BambooDHTManager(){
    	
    }
    
    public BambooDHTManager (boolean init) throws Exception {
        if (init){
	        self = this;
        }
        
       staticServers.put("planetlab7.millennium.berkeley.edu",5852);
       staticServers.put("planet2.berkeley.intel-research.net",5852);
       staticServers.put("planet3.berkeley.intel-research.net",5852);
       staticServers.put("planetlab16.millennium.berkeley.edu",5852);
       staticServers.put("planetlab2.millennium.berkeley.edu",5852);
       staticServers.put("planetlab3.millennium.berkeley.edu",5852);
       staticServers.put("planetlab4.millennium.berkeley.edu",5852);
       staticServers.put("planetlab5.millennium.berkeley.edu",5852);
       staticServers.put("planetlab6.millennium.berkeley.edu",5852);
       staticServers.put("planet1.berkeley.intel-research.net",5852);
       staticServers.put("planetlab8.millennium.berkeley.edu",5852);
       staticServers.put("planetlab9.millennium.berkeley.edu",5852);
       staticServers.put("planetlab10.millennium.berkeley.edu",5852);
       staticServers.put("planetlab11.millennium.berkeley.edu",5852);
       staticServers.put("planetlab12.millennium.berkeley.edu",5852);
       staticServers.put("planetlab13.millennium.berkeley.edu",5852);
       staticServers.put("planetlab14.millennium.berkeley.edu",5852);
       staticServers.put("planetlab15.millennium.berkeley.edu",5852);
        
    }
    
    public static BambooDHTManager getInstance(){
    	if (self==null){
    		try {
				self = new BambooDHTManager(true);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	return self;
    }

    public void init (ConfigDataIF config) throws Exception {
        super.init (config);
//        final String mode = config_get_string (config, "mode");
        secret = Util.convertStringToBytes(config_get_string (config, "secret"));
        synchronized(md){
	        md.reset();
	        md.update(secret);
	        secret_hash = md.digest();
        }
//        String file = config_get_string (config, "file");
        String gwc = config_get_string (config, "client_stage_name");
        client = (GatewayClient) lookup_stage (config, gwc);
//        if (mode.equals ("write")) {
//            is = null;
//            try {
//                is = new FileInputStream (file);
//            }
//            catch (IOException e) {
//                logger.error ("couldn't open file: " + file);
//                System.exit (1);
//            }
//            ttl = config_get_int (config, "ttl");
//            wblocks = new Vector<LinkedList<ByteBuffer>> (10);
//            wblocks.add (new LinkedList<ByteBuffer> ());
//            ready = new LinkedList<Pair<byte[],ByteBuffer>> ();
//            for (int i = 0; i < MAX_PARALLEL; ++i) {
//                acore.register_timer (0, 
//                        new Runnable () { public void run() { write (); }});
//            }
//        }
//        else if (mode.equals ("read")) {
//            try {
//                os = new FileOutputStream (file);
//            }
//            catch (IOException e) {
//                logger.error ("couldn't open file: " + file);
//                System.exit (1);
//            }
//            String kstr = config_get_string (config, "key");
//            key = bi2bytes (new BigInteger (kstr.substring (2), 16));
//            rblocks = new Vector<PriorityQueue> (10);
//            rnext = new Vector<Long> (10);
//            acore.register_timer (0, 
//                    new Runnable () { public void run() { read (); }});
//        }
//        else {
//            logger.error ("mode \"" + mode + "\" not supported");
//            System.exit (1);
//        }
//
//        acore.register_timer (10*1000, new Runnable () { 
//            public void run() {
//                logger.info ("There are " + outstanding + 
//                    (mode.equals ("read") ? " gets" : " puts") + 
//                    " outstanding");
//                acore.register_timer (10*1000, this);
//            }
//        });
    }

    /**
     * Transfer wblocks from the wblocks array to the ready queue.
     */
    public void make_parents (boolean done) {

        for (int l = 0; l < wblocks.size (); ++l) {
            logger.debug ("level " + l + " of " + wblocks.size () + " size=" + 
                          wblocks.elementAt (l).size () + " done=" + done);
            while ((wblocks.elementAt (l).size () >= BRANCHING) ||
                   (done && (wblocks.elementAt (l).size () > 1))) {
                int count = Math.min (BRANCHING, wblocks.elementAt (l).size ());
                logger.debug ("count=" + count);
                for (int i = 0; i < count; ++i) {
                    ByteBuffer bb = wblocks.elementAt (l).removeFirst ();
                    bb.flip ();
                    byte [] dig;;
                    synchronized(md){
	                    md.update (secret);
	                    md.update (bb.array (), 0, bb.limit ());
	                    dig = md.digest ();
                    }
                    ready.addLast (new Pair<byte[],ByteBuffer> (dig,bb));
                    if (l+1 >= wblocks.size ()) {
                        wblocks.setSize (Math.max (wblocks.size (), l + 2));
                        wblocks.setElementAt(new LinkedList<ByteBuffer>(), l+1);
                    }
                    LinkedList<ByteBuffer> next_level = wblocks.elementAt (l+1); 
                    if (next_level.isEmpty () ||
                        (next_level.getLast ().position () == 1024)) {
                        logger.debug ("adding a new block to level " + (l+1));
                        next_level.addLast (ByteBuffer.wrap (new byte [1024]));
                        next_level.getLast ().putInt (l+1);
                    }
                    logger.debug ("adding a digest to level " + (l+1));
                    next_level.getLast ().put (dig);
                }

                if (done) break;
            }
        }
        logger.debug ("make_parents done");
    }

//    public void write () {
//            logger.debug ("write");
//
//            if (is != null) {
//                while (ready.size () < MAX_BUFFER) {
//                    ByteBuffer bb = ByteBuffer.wrap (new byte [1024]);
//                    bb.putInt (0);
//                    int len = 0;
//                    try { len = is.read (bb.array (), 4, bb.limit () - 4); }
//                    catch (IOException e) { is = null; break; }
//                    if (len == -1) { is = null; break; }
//                    logger.debug ("position=" + bb.position () + " read " +
//                                  len + " bytes");
//                    // We're going to flip this later, so set the position
//                    // where we want the limit to end up.
//                    bb.position (len+4); 
//                    wblocks.elementAt (0).addLast (bb);
//                    logger.debug ("read a block");
//                    if (wblocks.elementAt (0).size () == BRANCHING) 
//                        make_parents (false);
//                }
//                if (is == null) {
//                    make_parents (true);
//                    // There should now be only one non-empty level, at it
//                    // should have exactly one block in it.
//                    for (int l = 0; l < wblocks.size (); ++l) {
//                        if (! wblocks.elementAt (l).isEmpty ()) {
//                            ByteBuffer bb = wblocks.elementAt (l).removeFirst();
//                            bb.flip ();
//                            md.update (secret);
//                            md.update (bb.array (), 0, bb.limit ());
//                            byte [] dig = md.digest ();
//                            StringBuffer sb = new StringBuffer (100);
//                            bytes_to_sbuf (dig, 0, dig.length, false, sb);
//                            logger.info ("root digest is 0x" + sb.toString ());
//                            ready.addLast (new Pair<byte[],ByteBuffer>(dig,bb));
//                            break;
//                        }
//                    }
//                }
//            }
//
//            // Do put.
//
//            if (ready.isEmpty ()) {
//                if (outstanding == 0) {
//                    logger.info ("all puts finished successfully");
//                    System.exit (0);
//                }
//            }
//            else {
//                Pair<byte[],ByteBuffer> head = ready.removeFirst ();
//                outstanding++;
//
//                bamboo_put_args put = new bamboo_put_args ();
//                put.application = APPLICATION;
//                // GatewayClient will fill in put.client_library
//                put.value = new bamboo_value ();
//                if (head.second.limit () == head.second.array ().length)
//                    put.value.value = head.second.array ();
//                else {
//                    put.value.value = new byte [head.second.limit ()];
//                    head.second.get (put.value.value);
//                }
//                put.key = new bamboo_key ();
//                put.key.value = head.first;
//                put.ttl_sec = 3600; // TODO
//
//                StringBuffer sb = new StringBuffer (100);
//                bytes_to_sbuf (head.first, 0, head.first.length, false, sb);
//                logger.debug ("putting block size=" + put.value.value.length
//                        + " key=0x" + sb.toString ());
//                client.put (put, curry(put_done_cb, put));
//            }
//    }
//
//    public Thunk2<bamboo_put_args,Integer> put_done_cb = 
//        new Thunk2<bamboo_put_args,Integer> () {
//        public void run(final bamboo_put_args put, Integer result) {
//            if (result.intValue () == 0) {
//                outstanding--;
//                write ();
//            }
//            else {
//                StringBuffer sb = new StringBuffer (100);
//                bytes_to_sbuf (put.key.value, 0, 
//                        put.key.value.length, false, sb);
//                final String key = sb.toString ();
//                logger.debug ("got response " + result.intValue () + 
//                        " for 0x" + key);
//                acore.register_timer (1000, new Runnable () {
//                        public void run() {
//                            logger.debug ("reputting block 0x" + key);
//                            client.put (put, curry(put_done_cb, put));
//                        }
//                    });
//            }
//        }
//    };

//    public void read (byte key[], byte secret[], IDDBReadAction action) {
////        if (rblocks.size () == 0) {
////            rblocks.add (new PriorityQueue (BRANCHING));
////            logger.debug ("setting level 0 need to 0");
////            rnext.add (new Long (0));
////
////            outstanding++;
//            // Get the root block.
//            bamboo_get_args get = new bamboo_get_args();
//            get.application = APPLICATION;
//            // GatewayClient will fill in get.client_library
//            get.key = new bamboo_key();
//            get.key.value = key;
//            get.maxvals = 1;
//            get.placemark = new bamboo_placemark();
//            get.placemark.value = new byte[] {};
//            
//
//            logger.debug ("getting root: 0x" + bytes_to_str (key));
//            Long seq =  new Long (0);
//            client.get(get, curry(new BambooDHTReadHandler(action, option), get, seq));
            // TODO add support for timeouts
            // register pending get w/ seq number
            // register timer for timeout
            // see PutGetTest
            
//        }
//        else {
//            boolean empty = true;
//
//            while (outstanding < MAX_PARALLEL) {
//            // Start from the bottom and work our way up.
//            int l = 1; 
//            for (; l < rblocks.size (); ++l) {
//                if ((rblocks.elementAt (l) != null) && 
//                    (! rblocks.elementAt (l).isEmpty ())) {
//                    empty = false;
//
//                    if (rblocks.elementAt (l).getFirstPriority () ==
//                        rnext.elementAt (l).longValue ()) {
//
//                        long p = rblocks.elementAt (l).getFirstPriority ();
//                        ByteBuffer k = (ByteBuffer) 
//                            rblocks.elementAt (l).removeFirst ();
//                        logger.debug ("updating level " + l + 
//                                      " need to " + (p+1));
//                        rnext.setElementAt (new Long (p+1), l);
//                        logger.debug ("level " + l + 
//                                      " has need " + rnext.elementAt (l));
//
//                        outstanding++;
//                        // Get this block.
//                        bamboo_get_args get = new bamboo_get_args();
//                        get.application = APPLICATION;
//                        // GatewayClient will fill in get.client_library
//                        get.key = new bamboo_key();
//                        get.key.value = k.array ();
//                        get.maxvals = 1;
//                        get.placemark = new bamboo_placemark();
//                        get.placemark.value = new byte[] {};
//
//                        logger.debug ("getting lev=" + l + " pos=" + p +
//                                " key=0x" + bytes_to_str (k.array ()));
//
//                        client.get(get, curry(get_done_cb, get, new Long (p)));
//                        break;
//                    }
//                    else {
//                        logger.debug ("level " + l + " have " +
//                                rblocks.elementAt (l).getFirstPriority () +
//                                " need " + rnext.elementAt (l).longValue ());
//                    }
//                }
//            }
//            if (l == rblocks.size ())
//                break;
//            }
//
//            if (outstanding == 0) {
//                assert empty;
//                logger.info ("all gets finished successfully");
//                try { os.close (); } catch (IOException e) {
//                    logger.error ("could not close output file");
//                    System.exit (1);
//                }
//                System.exit (0);
//            }
//        }
//    }

    public void reReadKey(final bamboo_get_args get, final Long pos, final String key, 
    		final Thunk3<bamboo_get_args,Long,bamboo_get_result> get_done_cb) {
		acore.register_timer (1000, new Runnable () {
		        public void run() {
		            logger.debug ("trying to get 0x" + key + " again");
		            if (get==null){
		   			 throw new RuntimeException("Null get!");
		   		 	}
		            numReads++;
		            if (USE_GATEWAY) client.getDetails (get, Curry.curry(get_done_cb, get, pos));
		            else get (get, Curry.curry(get_done_cb, get, pos));
		        }
		    }); 
	}

	

//    public static void main (String [] args) throws Exception {
//        PatternLayout pl = new PatternLayout ("%d{ISO8601} %-5p %c: %m\n");
//        ConsoleAppender ca = new ConsoleAppender (pl);
//        Logger.getRoot ().addAppender (ca);
//        Logger.getRoot ().setLevel (Level.INFO);

//        // create Options object
//        Options options = new Options();
//
//        // add t option
//        options.addOption ("r", "read",  false, "read a file from the DHT");
//        options.addOption ("w", "write", false, "write a file to the DHT");
//        options.addOption ("g", "gateway", true, "the gateway IP:port");
//        options.addOption ("k", "key", true, "the key to read a file from");
//        options.addOption ("f", "file", true, "the file to read or write");
//        options.addOption ("s", "secret", true, "the secret used to hide data");
//        options.addOption ("t", "ttl", true, 
//                          "how long in seconds data should persist");
//
//        CommandLineParser parser = new PosixParser();
//        CommandLine cmd = parser.parse( options, args);
//
//        String gw = null;
//        String mode = null;
//        String secret = null;
//        String ttl = null;
//        String key = null;
//        String file = null;
//
//        if (cmd.hasOption ("r")) { mode = "read"; }
//        if (cmd.hasOption ("w")) { mode = "write"; }
//        if (cmd.hasOption ("g")) { gw = cmd.getOptionValue("g"); }
//        if (cmd.hasOption ("k")) { key = cmd.getOptionValue("k"); }
//        if (cmd.hasOption ("f")) { file = cmd.getOptionValue("f"); }
//        if (cmd.hasOption ("s")) { secret = cmd.getOptionValue("s"); }
//        if (cmd.hasOption ("t")) { ttl = cmd.getOptionValue("t"); }
//
//        if (mode == null) {
//            System.err.println ("ERROR: either --read or --write is required");
//            HelpFormatter formatter = new HelpFormatter ();
//            formatter.printHelp ("fileshare", options);
//            System.exit (1);
//        }
//
//        if (gw == null) {
//            System.err.println ("ERROR: --gateway is required");
//            HelpFormatter formatter = new HelpFormatter ();
//            formatter.printHelp ("fileshare", options);
//            System.exit (1);
//        }
//
//        if (file == null) {
//            System.err.println ("ERROR: --file is required");
//            HelpFormatter formatter = new HelpFormatter ();
//            formatter.printHelp ("fileshare", options);
//            System.exit (1);
//        }
//
//        if (secret == null) {
//            System.err.println ("ERROR: --secret is required");
//            HelpFormatter formatter = new HelpFormatter ();
//            formatter.printHelp ("fileshare", options);
//            System.exit (1);
//        }


//    }

	private static void initBamboo() throws IOException, Exception {
		md = MessageDigest.getInstance ("SHA");
		
		if (!USE_GATEWAY) return;
		StringBuffer sbuf = new StringBuffer (1000);
        sbuf.append ("<sandstorm>\n");
        sbuf.append ("<global>\n");
        sbuf.append ("<initargs>\n");
        sbuf.append ("node_id localhost:3630\n");
        sbuf.append ("</initargs>\n");
        sbuf.append ("</global>\n");
        sbuf.append ("<stages>\n");
        sbuf.append ("<GatewayClient>\n");
        sbuf.append ("class bamboo.dht.GatewayClient\n");
        sbuf.append ("<initargs>\n");
        sbuf.append ("debug_level -1\n");
        sbuf.append ("read_buffer_size 1500\n");
        sbuf.append ("write_buffer_size 65536\n");
        sbuf.append ("              gateway_count 18\n"+
                "gateway_0 planetlab7.millennium.berkeley.edu:5852\n"+
                "gateway_1 planet2.berkeley.intel-research.net:5852\n"+
                "gateway_2 planet3.berkeley.intel-research.net:5852\n"+
                "gateway_3 planetlab16.millennium.berkeley.edu:5852\n"+
                "gateway_4 planetlab2.millennium.berkeley.edu:5852\n"+
                "gateway_5 planetlab3.millennium.berkeley.edu:5852\n"+
                "gateway_6 planetlab4.millennium.berkeley.edu:5852\n"+
                "gateway_7 planetlab5.millennium.berkeley.edu:5852\n"+
                "gateway_8 planetlab6.millennium.berkeley.edu:5852\n"+
                "gateway_9 planet1.berkeley.intel-research.net:5852\n"+
                "gateway_10 planetlab8.millennium.berkeley.edu:5852\n"+
                "gateway_11 planetlab9.millennium.berkeley.edu:5852\n"+
                "gateway_12 planetlab10.millennium.berkeley.edu:5852\n"+
                "gateway_13 planetlab11.millennium.berkeley.edu:5852\n"+
                "gateway_14 planetlab12.millennium.berkeley.edu:5852\n"+
                "gateway_15 planetlab13.millennium.berkeley.edu:5852\n"+
                "gateway_16 planetlab14.millennium.berkeley.edu:5852\n"+
                "gateway_17 planetlab15.millennium.berkeley.edu:5852\n");
        sbuf.append ("</initargs>\n");
        sbuf.append ("</GatewayClient>\n");
        sbuf.append ("\n");
        sbuf.append ("<BambooDHTManager>\n");
        sbuf.append ("class edu.northwestern.ono.dht.bamboo.BambooDHTManager\n");
        sbuf.append ("<initargs>\n");
        sbuf.append ("debug_level -1\n");
        sbuf.append ("secret " + secretString  + "\n");
        
////        sbuf.append ("mode " + mode + "\n");
////        if (mode.equals ("write")) {
////            if (ttl == null) {
////                System.err.println ("ERROR: --ttl is required for write mode");
////                HelpFormatter formatter = new HelpFormatter ();
////                formatter.printHelp ("fileshare", options);
////                System.exit (1);
////            }
////
////            sbuf.append ("ttl " + ttl + "\n"); 
////            sbuf.append ("file " + file + "\n"); 
////        }
////        else {
//            if (key == null) {
//                System.err.println ("ERROR: --key is required for write mode");
//                HelpFormatter formatter = new HelpFormatter ();
//                formatter.printHelp ("fileshare", options);
//                System.exit (1);
//            }
//
//            sbuf.append ("key " + key + "\n"); 
//            sbuf.append ("file " + file + "\n"); 
//        }
        sbuf.append ("client_stage_name GatewayClient\n");
        sbuf.append ("</initargs>\n");
        sbuf.append ("</BambooDHTManager>\n");
        sbuf.append ("</stages>\n");
        sbuf.append ("</sandstorm>\n");
        acore = new bamboo.lss.ASyncCoreImpl();
		DustDevil dd = new DustDevil ();
		DustDevil.set_acore_instance (acore);
        dd.main (new CharArrayReader (sbuf.toString ().toCharArray ()));        
//        Thread t = new Thread(self);
//        t.setName("BambooManager");
//        t.setDaemon(false);
//        t.start();
        acore.async_main ();
	}
	
	

	public void delete(final String key, final int timeout, 
			final IDDBDeleteAction action, ArrayList<ByteArrayOutputStream> values) {
		if (!ENABLED) return;
		final bamboo_get_args get = buildGetArgs(key);
		/**********************************
		 * 
		 * NOTE: Not doing deletes anymore because entries will expire to have TTLs extended.
		 * 
		 */
		if (action!=null) action.handleComplete(new BambooDistriubtedDatabaseEvent(
				new BambooDistributedDatabaseKey(get, key), true));
		if (!DO_DELETE ) return;
		// TODO make sure that deletes never last longer than current value, 
		// this probably requires ALWAYS reading before deleting
		
		// also, i may not be able to write a value again until its delete 
		// expires, so try to account for this in the code
		
		
		assert get!=null;
		String myKey = Util.convertByteToString(get.key.value);
		if (isModifying(myKey)){
			if (DEBUG) System.out.println("Caching delete " + key + "!");
			synchronized (this){
				if (!pendingDeletes.containsKey(myKey)){
					pendingDeletes.put(myKey, new PendingDelete(key, timeout, action, values));
				}
			}
			return;
		}
	
		// TODO check for null values
		if (values!=null){
			doDelete(key, timeout, action, values);
		}
		else {
			deletesInProgress.put(myKey, System.currentTimeMillis()+timeout*1000);
			doReadAndDelete(key, timeout, action, get);
		}
	
		
		
		
	}

	private void doDelete(final String key, final int timeout, final IDDBDeleteAction action, ArrayList<ByteArrayOutputStream> values) {
		if (values==null) {
			System.err.println("doDelete: Null values!");
			return;
		}
		for (ByteArrayOutputStream value : values){
			bamboo_rm_arguments args = new bamboo_rm_arguments();
			args.application = APPLICATION;
			args.key = new bamboo_key();
			synchronized(md){
				md.reset();
				md.update(Util.convertStringToBytes(key));
				args.key.value = md.digest();
			}
			args.ttl_sec = DEFAULT_TTL;
			args.secret = secret;
			args.secret_hash_alg = "SHA";
//			 TODO synchronize access to md
			synchronized(md){
				md.reset();
				md.update(value.toByteArray());
				args.value_hash = new bamboo_hash();
				args.value_hash.algorithm = "SHA";
				args.value_hash.hash = md.digest();
			}
			numDeletes++;
			BambooDistributedDatabaseKey bddk = 
				new BambooDistributedDatabaseKey(args.key, key); 
			
			if (USE_GATEWAY) client.remove(args, new BambooDeleteHandler(action, 
					bddk));
			else rm(args, new BambooDeleteHandler(action, 
					bddk));
		}
	}

	private void doReadAndDelete(final String key, final int timeout, final IDDBDeleteAction action, final bamboo_get_args get) {
		
		if (USE_GATEWAY){
		client.getDetails(get, Curry.curry(new Thunk3<bamboo_get_args,Long,bamboo_get_result>(){

			public void run(final bamboo_get_args get, final Long pos, 
					bamboo_get_result result) {
				if (result.values!=null && result.values.length>0){
					for (bamboo_get_value value : result.values){
						bamboo_rm_arguments args = new bamboo_rm_arguments();
						args.application = APPLICATION;
						args.key = new bamboo_key();
						synchronized(md){
							md.reset();
							md.update(Util.convertStringToBytes(key));
							args.key.value = md.digest();
						}
						args.ttl_sec = value.ttl_sec_rem;
						args.secret = secret;
						args.secret_hash_alg = "SHA";
//						 TODO synchronize access to md
						synchronized(md){
							md.reset();
							md.update(value.value.value);
							args.value_hash = new bamboo_hash();
							args.value_hash.algorithm = "SHA";
							args.value_hash.hash = md.digest();
						}
						numDeletes++;
						BambooDistributedDatabaseKey bddk = 
							new BambooDistributedDatabaseKey(args.key, key);
						
						client.remove(args, new BambooDeleteHandler(action, 
								bddk));
					}
				}
				else {
					System.err.println("Could not read value to delete!");
					doneWithDelete(Util.convertByteToString(get.key.value));
				}
				
			}}, get, new Long(0) ));
		} else {
			get(get, Curry.curry(new Thunk3<bamboo_get_args,Long,bamboo_get_result>(){

				public void run(final bamboo_get_args get, final Long pos, 
						bamboo_get_result result) {
					if (result.values!=null && result.values.length>0){
						for (bamboo_get_value value : result.values){
							bamboo_rm_arguments args = new bamboo_rm_arguments();
							args.application = APPLICATION;
							args.key = new bamboo_key();
							synchronized(md){
								md.reset();
								md.update(Util.convertStringToBytes(key));
								args.key.value = md.digest();
							}
							args.ttl_sec = value.ttl_sec_rem;
							args.secret = secret;
							args.secret_hash_alg = "SHA";
//							 TODO synchronize access to md
							synchronized(md){
								md.reset();
								md.update(value.value.value);
								args.value_hash = new bamboo_hash();
								args.value_hash.algorithm = "SHA";
								args.value_hash.hash = md.digest();
							}
							numDeletes++;
							BambooDistributedDatabaseKey bddk = 
								new BambooDistributedDatabaseKey(args.key, key);
							
							rm(args, new BambooDeleteHandler(action, 
									bddk));
						}
					}
					else {
						System.err.println("Could not read value to delete!");
						doneWithDelete(Util.convertByteToString(get.key.value));
					}
					
				}}, get, new Long(0) ));
		}
	}

	protected void handleDeleteDone(String key) {
		synchronized (this){
		// first try to schedule writes, then if no writes, do reads
		if (pendingWrites.containsKey(key)){
			synchronized (pendingWrites){
				PendingWrite p = pendingWrites.remove(key);		
				if (p==null) {
					handleDeleteDone(key);
					return;
				}
				write(p.key, p.values, p.timeout, p.action);
			}
		}
		else if (pendingReads.containsKey(key)){
			readsInProgress.put(key, System.currentTimeMillis()+DEFAULT_TIMEOUT*1000);
			PendingRead p = pendingReads.remove(key);
			if (p==null){
				handleDeleteDone(key);
				return;
			}
			numReads++;
			if (USE_GATEWAY) client.getDetails(p.get, Curry.curry(new BambooDHTReadHandler(
					p.action, p.options, p.key), p.get, p.seq));
			else get(p.get, Curry.curry(new BambooDHTReadHandler(
					p.action, p.options, p.key), p.get, p.seq));
		} else {
			pendingDeletes.remove(key);
		}
		}
		
	}
	
	protected void handleWriteDone(String key) {
		synchronized (this){
			// first try to schedule reads, then if no reads, do deletes
			if (pendingReads.containsKey(key)){
				readsInProgress.put(key, System.currentTimeMillis()+DEFAULT_TIMEOUT*1000);
				PendingRead p = pendingReads.remove(key);
				if (p==null) {
					handleWriteDone(key);
					return;
				}
				numReads++;
				if (USE_GATEWAY) client.getDetails(p.get, Curry.curry(new BambooDHTReadHandler(
						p.action, p.options, p.key), p.get, p.seq));
				else get(p.get, Curry.curry(new BambooDHTReadHandler(
						p.action, p.options, p.key), p.get, p.seq));
			}
			else if (pendingDeletes.containsKey(key)){
				deletesInProgress.put(key, System.currentTimeMillis()+DEFAULT_TIMEOUT*1000);
				PendingDelete p = pendingDeletes.remove(key);			
				delete(p.key, p.timeout, p.action, p.values);
			}
			else if (pendingWrites.containsKey(key)){
				PendingWrite p = pendingWrites.remove(key);
				write(p.key, p.values, p.timeout, p.action);
			}
		}
	}
	
	protected void handleReadDone(String key){
		synchronized (this){
		// first do deletes, then writes
		if (pendingDeletes.containsKey(key)){
			deletesInProgress.put(key, System.currentTimeMillis()+DEFAULT_TIMEOUT*1000);
			PendingDelete p = pendingDeletes.remove(key);
			
			doDelete(p.key, p.timeout, p.action, p.values);
		}
		else if (pendingWrites.containsKey(key)){
			PendingWrite p = pendingWrites.remove(key);
			write(p.key, p.values, p.timeout, p.action);
		}
		else if (pendingReads.containsKey(key)){
			readsInProgress.put(key, System.currentTimeMillis()+DEFAULT_TIMEOUT*1000);
			PendingRead p = pendingReads.remove(key);
			numReads++;
			if (USE_GATEWAY) client.getDetails(p.get, Curry.curry(new BambooDHTReadHandler(
					p.action, p.options, p.key), p.get, p.seq));
		}
		}
	}
	
	protected boolean isBusy(String key){
		long currentTime = System.currentTimeMillis();
		return (writesInProgress.containsKey(key) && writesInProgress.get(key) > currentTime)
		   || (readsInProgress.containsKey(key) && readsInProgress.get(key) > currentTime)
		   || (deletesInProgress.containsKey(key) && deletesInProgress.get(key) > currentTime);
	}
	
	/**
	 * Allow reads concurrent with eveything but other reads
	 * @param key
	 * @return
	 */
	protected boolean isReading(String key){
		long currentTime = System.currentTimeMillis();
		return readsInProgress.containsKey(key) 
			&& readsInProgress.get(key) > currentTime;
		  

	}
	
	/**
	 * Allow writes concurrent with eveything but other writes
	 * @param key
	 * @return
	 */
	protected boolean isModifying(String key){
		long currentTime = System.currentTimeMillis();
		synchronized (writesInProgress){
			if (writesInProgress.containsKey(key) 
			&& writesInProgress.get(key) > currentTime) return true;
		}
		synchronized (deletesInProgress){
				if(deletesInProgress.containsKey(key) 
						&& deletesInProgress.get(key) > currentTime) return true;
		}
		return false;
				  
	}


	public IDHTOption getOptions() {
		// TODO Auto-generated method stub
		return new BambooDHTOptions();
	}

	public int maximumValueSize() {		
		return 1024;
	}

	public void read(String key, String description, int timeout, 
			IDDBReadAction action, int option) {
		if (!ENABLED || USE_GATEWAY && client==null) return;
		
		 bamboo_get_args get = buildGetArgs(key);
		 
		 if (get==null){
			 throw new RuntimeException("Null get!");
		 }
         
		 
//         logger.debug ("getting root: 0x" + bytes_to_str (key));
         Long seq =  new Long (0);
         String myKey = Util.convertByteToString(get.key.value);
	 		if (isReading(myKey)){
	 			if (DEBUG) System.out.println("Caching read " + key + "!");
	 			synchronized (this){
		 			PendingRead pr = pendingReads.get(key);
		 			if (pr==null){
		 				pr = new PendingRead();
		 			}
		 			pr.action = action;
		 			pr.get = get;
		 			pr.options = option;
		 			pr.seq = seq;
		 			pr.key = key;
		 			
		 				pendingReads.put(myKey, pr);
	 			}
	 				return;
	 			}
	 		numReads++;
         readsInProgress.put(myKey, System.currentTimeMillis()+timeout*1000);
         
         if (USE_GATEWAY) client.getDetails(get, Curry.curry(new BambooDHTReadHandler(action, option, key), get, seq));
         else get(get, Curry.curry(new BambooDHTReadHandler(action, option, key), get, seq));
         //         client.get(get, curry(new BambooDHTReadHandler(action, option), get, seq));
         //       TODO add support for timeouts
         // register pending get w/ seq number
         // register timer for timeout
         // see PutGetTest
	}

	private bamboo_get_args buildGetArgs(String key) {
		bamboo_get_args get = new bamboo_get_args();
         get.application = APPLICATION;
         // GatewayClient will fill in get.client_library
         get.key = new bamboo_key();
         synchronized(md){
	         md.reset();
	         md.update(Util.convertStringToBytes(key));
	         get.key.value = md.digest();
         }
         get.maxvals = 10;
         get.placemark = new bamboo_placemark();
         get.placemark.value = new byte[] {};
         
		return get;
	}

	public void write(String key, ArrayList<ByteArrayOutputStream> values, 
			int timeout, IDDBWriteAction action) {
		if (!ENABLED) return;
		
		 byte key2[];
		 if (md==null) return;
		 synchronized(md){
			md.reset();
			md.update(Util.convertStringToBytes(key));
			
			key2= md.digest();
		 }
		
        String myKey = Util.convertByteToString(key2);
        if (isModifying(myKey)){
        	if (DEBUG) System.out.println("Caching write to " + key + "!");
        	synchronized (this){
        	PendingWrite pw = pendingWrites.get(myKey);
        	if (pw==null){
        		pw = new PendingWrite();
        	}
        	pw.action = action;
        	pw.key = key;
        	pw.timeout = timeout;
        	pw.values = values;
        	
        		pendingWrites.put(myKey, pw);
        	}
        	return;
        }
		
		logger.debug ("write");
		 while (USE_GATEWAY && client == null){
         	try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
         }


		for (ByteArrayOutputStream value : values){
			if (value==null) continue;

            bamboo_put_arguments put = new bamboo_put_arguments ();
            put.application = APPLICATION;
            // GatewayClient will fill in put.client_library
            put.value = new bamboo_value ();
//            if (head.second.limit () == head.second.array ().length)
//                put.value.value = head.second.array ();
//            else {
//                put.value.value = new byte [head.second.limit ()];
//                head.second.get (put.value.value);
//            }
            put.value.value = value.toByteArray();
            put.key = new bamboo_key ();
            put.key.value = key2;
            put.ttl_sec = DEFAULT_TTL; // TODO
            put.secret_hash = new bamboo_hash();
            put.secret_hash.algorithm = "SHA";
            put.secret_hash.hash = secret_hash;

//            StringBuffer sb = new StringBuffer (100);
//            bytes_to_sbuf (head.first, 0, head.first.length, false, sb);
//            logger.debug ("putting block size=" + put.value.value.length
//                    + " key=0x" + sb.toString ());
            

            
           numWrites++;
           assert put!=null;
           writesInProgress.put(myKey, System.currentTimeMillis()+timeout*1000);
           if (USE_GATEWAY)  client.putSecret(put, new BambooDHTWriteHandler(action, put, key));
           else put(put, new BambooDHTWriteHandler(action, put, key));

		}
//        }
		
	}

	public void reWrite(final BambooDHTWriteHandler handler, final bamboo_put_arguments put, final byte[] key) {
		acore.register_timer (1000, new Runnable () {
            public void run() {
                logger.debug ("reputting block 0x" + key);
                if (put==null){
       			 throw new RuntimeException("Null put!");
       		 }
                numWrites++;
                if (USE_GATEWAY) client.putSecret(put, handler);
                else put(put, handler);
            }
        });
		
	}

	public void run() {
		try {
			if (!ENABLED) return;
			Thread.currentThread().setName("BambooDHTManager");
			Logger.getRoot().setLevel(Level.OFF);
			Logger.getLogger(ASyncCoreImpl.class).setLevel(Level.OFF);
			if (DEBUG){
				ITimer timer = MainGeneric.createTimer("BambooStats");
				timer.addPeriodicEvent(30*1000, new ITimerEventPerformer(){
	
					public void perform(ITimerEvent event) {
						System.out.println("Bamboo stats:\n\tReads:   "
								+numReads+"\n\tWrites:  "+numWrites+
								"\n\tDeletes: "+numDeletes);
						
						
					}});
			}
			
			initBamboo();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		acore.async_main ();
		
	}
	
	public void doneWithRead(String key){
		readsInProgress.remove(key);
		handleReadDone(key);
	}
	
	public void doneWithWrite(String key){
		writesInProgress.remove(key);
		handleWriteDone(key);
	}
	
	public void doneWithDelete(String key){
		deletesInProgress.remove(key);
		handleDeleteDone(key);
	}

	public void doDirectRead(String key, bamboo_get_args get, BambooDHTReadHandler action) {
		Long seq = 1L;
		if (USE_GATEWAY) client.getDetails(get, Curry.curry(action, get, seq));
		else get(get, Curry.curry(action, get, seq));
	}
	
	public InetAddress getHost(){
		InetAddress serverHost=null;
		while (serverHost==null){
			if (useStatic){
				int i = 0;
				int rand = random.nextInt(staticServers.size()-1);
				for (Map.Entry<String, Integer> ent: staticServers.entrySet()){
					if (i==rand){
						try {
							serverHost = InetAddress.getByName(ent.getKey());
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					}
				}
				useStatic = false;
			} else {
				try {
					serverHost = InetAddress.getByName("opendht.nyuld.net");
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
//					e.printStackTrace();
				} 
				useStatic = true;
			}
		}
		return serverHost;
	}
	
	public int getPort(){
		return 5852;
	}
	
	public void get(final bamboo_get_args args, 
            final Thunk1<bamboo_get_result> resp){
		Runnable r = new Runnable(){

			public void run(){
				try {
					//		if (args.length < 3) {
//					System.out.println("usage: java bamboo.dht.Get <server_host> "
//					+ "<server_port> <key> [max_vals]");
//					System.exit(1);
//					}
					InetAddress h = getHost();
					int p = getPort();
//					bamboo_get_args getArgs = new bamboo_get_args();
//					args.application = Get.class.getName();
					args.client_library = "Remote Tea ONC/RPC";
//					MessageDigest md = MessageDigest.getInstance("SHA");
//					getArgs.key = new bamboo_key();
					// If key is of the form 0x12345678, parse it as a hexidecimal value
					// of the first 8 digits of the key, rather than hashing it.
//					if (args[2].substring(0, 2).equals("0x")) {
//					// Must use a long to handle keys whose first binary digit is 1.
//					long keyPrefix = Long.parseLong(args[2].substring(2), 16);
//					getArgs.key.value = new byte[20];
//					ByteBuffer.wrap(getArgs.key.value).putInt((int) keyPrefix);
//					}
//					else {
//					getArgs.key.value = md.digest(secretString.getBytes());
//					}
//					if (args.length > 3) 
//					getArgs.maxvals = Integer.parseInt(args[3]);
//					else
//					getArgs.maxvals = Integer.MAX_VALUE;
//					getArgs.placemark = new bamboo_placemark();
//					getArgs.placemark.value = new byte [0];
					gateway_protClient client;
					bamboo_get_result res = new bamboo_get_result();
					try {
						client = new gateway_protClient(h, p, ONCRPC_TCP);

						while (true) {
							res = client.BAMBOO_DHT_PROC_GET_3(args);
							//            for (int i = 0; i < res.values.length; ++i)
							//                System.out.println(new String (res.values[i].value));
							resp.run(res);
							if (res.placemark.value.length == 0)
								break;
							args.placemark = res.placemark;
						}
					} catch (OncRpcException e) {
						// TODO Auto-generated catch block
//						e.printStackTrace();
						resp.run(res);
					} catch (IOException e) {
						// TODO Auto-generated catch block
//						e.printStackTrace();
						resp.run(res);

					}
				} finally {
					operationDone();
				}
			}
		};

		synchronized (pendingRuns){
			if (activeThreads >= MAX_THREADS){
				pendingRuns.put("GET-"+Util.convertByteToString(args.key.value), r);
			}
			else {
				activeThreads++;
				MainGeneric.createThread("GET-"+Util.convertByteToString(args.key.value), r);
			}
		}
		
	}
	
	public void put(final bamboo_put_arguments args, final Thunk1<Integer> resp) {
		Runnable r = new Runnable(){

			public void run(){
				try {
//					if (args.length < 5) {
//					System.out.println("usage: java bamboo.dht.Put <server_host> "
//					+ "<server_port> <key> <value> <TTL> [secret]");
//					System.exit(1);
//					}
//					String [] results = {"BAMBOO_OK", "BAMBOO_CAP", "BAMBOO_AGAIN"};
					InetAddress h = getHost();
					int p = getPort();
					int result = -1;
					args.client_library = "Remote Tea ONC/RPC";
//					bamboo_put_arguments putArgs = new bamboo_put_arguments();
//					putArgs.application = Put.class.getName();
//					putArgs.client_library = "Remote Tea ONC/RPC";
					MessageDigest md;
					try {
						md = MessageDigest.getInstance("SHA");

//						putArgs.key = new bamboo_key();
//						// If key is of the form 0x12345678, parse it as a hexidecimal value
//						// of the first 8 digits of the key, rather than hashing it.
//						if (args[2].substring(0, 2).equals("0x")) {
//						// Must use a long to handle keys whose first binary digit is 1.
//						long keyPrefix = Long.parseLong(args[2].substring(2), 16);
//						putArgs.key.value = new byte[20];
//						ByteBuffer.wrap(putArgs.key.value).putInt((int) keyPrefix);
//						}
//						else {
//						putArgs.key.value = md.digest(args[2].getBytes());
//						}
//						putArgs.value = new bamboo_value();
//						putArgs.value.value = args[3].getBytes();
//						putArgs.ttl_sec = Integer.parseInt(args[4]);
//						putArgs.secret_hash = new bamboo_hash();
//						if (args.length > 5) {
						args.secret_hash.algorithm = "SHA";
						args.secret_hash.hash = md.digest(secretString.getBytes());
//						}
//						else {
//						putArgs.secret_hash.algorithm = "";
//						putArgs.secret_hash.hash = new byte[0];
//						}
						gateway_protClient client;
//						int result;
						try {
							client = new gateway_protClient(h, p, ONCRPC_TCP);



							result = client.BAMBOO_DHT_PROC_PUT_3(args);
						} catch (OncRpcException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
							result = 1;
						} catch (IOException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
							result = 1;
						}
					} catch (NoSuchAlgorithmException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
						result = 1;
					}
					resp.run(result);

				}
				finally {
					operationDone();
				}
			}
		};
		
		synchronized (pendingRuns){
			if (activeThreads >= MAX_THREADS){
				pendingRuns.put("PUT-"+Util.convertByteToString(args.value.value), r);
			}
			else {
				activeThreads++;
				MainGeneric.createThread("PUT-"+Util.convertByteToString(args.value.value), r);
			}
		}
		
	}
	
	public void rm(final bamboo_rm_arguments args, final Thunk1<Integer> resp){
		Runnable r = new Runnable(){

			public void run(){
				try {
					InetAddress h = getHost();
					int p = getPort();
					gateway_protClient client;
					int result = 0;
					args.client_library = "Remote Tea ONC/RPC";
					MessageDigest md;
					try {
						md = MessageDigest.getInstance("SHA");

						args.secret_hash_alg = "SHA";
						args.secret = md.digest(secretString.getBytes());
						try {
							client = new gateway_protClient(h, p, ONCRPC_TCP);

							result = client.BAMBOO_DHT_PROC_RM_3(args);
						} catch (OncRpcException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
							result = 1;
						} catch (IOException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
							result = 1;
						}
					} catch (NoSuchAlgorithmException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
						result = 1;
					}
					resp.run(result);

				} finally {
					operationDone();
				}
			}
		};
		
		synchronized (pendingRuns){
			if (activeThreads >= MAX_THREADS){
				pendingRuns.put("RM-"+Util.convertByteToString(args.value_hash.hash), r);
			}
			else {
				activeThreads++;
				MainGeneric.createThread("RM-"+Util.convertByteToString(args.value_hash.hash), r);
			}
		}
		
	}
	
	public void operationDone(){
		synchronized (pendingRuns){
			activeThreads--;
			if (activeThreads < MAX_THREADS){
				Iterator<Entry<String, Runnable>> it = pendingRuns.entrySet().iterator();

				if (it.hasNext()){
					Entry<String, Runnable> ent = it.next();
					it.remove();
					activeThreads++;
					MainGeneric.createThread(ent.getKey(), ent.getValue());

				}
			}
		}

	}

}
