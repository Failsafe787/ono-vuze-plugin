/**
 * Ono Project
 *
 * File:         Digger.java
 * RCS:          $Id: Digger.java,v 1.79 2010/11/07 19:42:13 drc915 Exp $
 * Description:  Digger class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jun 13, 2006 at 8:55:41 AM
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.xbill.DNS.DClass;
import org.xbill.DNS.ExtendedFlags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.ReverseMap;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import org.xbill.DNS.Options;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.OnoConfiguration;
import edu.northwestern.ono.dht.IDistributedDatabase;
import edu.northwestern.ono.position.CDNClusterFinder;
import edu.northwestern.ono.position.OnoPeerManager;
import edu.northwestern.ono.position.OnoPeerManager.OnoPeer;
import edu.northwestern.ono.stats.DNSEvent;
import edu.northwestern.ono.stats.EdgeServerRatio;
import edu.northwestern.ono.stats.Statistics;
import edu.northwestern.ono.stats.DNSEvent.EventType;
import edu.northwestern.ono.util.HashMapCache;
import edu.northwestern.ono.util.HashSetCache;
import edu.northwestern.ono.util.ILoggerChannel;
import edu.northwestern.ono.util.Pair;
import edu.northwestern.ono.util.PingManager;
import edu.northwestern.ono.util.PluginInterface;
import edu.northwestern.ono.util.Util;
import edu.northwestern.ono.util.Util.PingResponse;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 * 
 * The Digger class hunts for Akamai or other CDN entries and reports them
 * appropriately.
 * 
 * Note: If DIG_FOR_OTHERS is set to true, you will have to make sure that you
 * reduce the TCP timeout and increase the number of user ports available on
 * your OS. For help, see
 * http://www.minq.se/products/pureload/tuning/connections.html.
 */
public class Digger extends Thread {
	// private static boolean DIG_FOR_OTHERS = false;

	/** bits to shift for assigning digging of peers w/o plugin */
	private static final int bitShift = 30;
	private static int PEER_IP_MAX = 100;

	private static final boolean DO_PINGS = true;
	private static final boolean NO_LOOKUP = false;

	private static boolean isServer = false;
	private static Digger self;

	/** sleep interval, in seconds */
	private int sleepInterval = 30;

	/** the CDNs to check */
	private ArrayList<String> cdns;
	private ILoggerChannel log;
	private Record[] records;

	/** maps CDN names to edge servers */
	private HashMapCache<String, ArrayList<String>> edgeServers;

	/** maps CDN names to edge servers FOR EACH PEER, indexed by peer IP */
	private HashMapCache<String, HashMapCache<String, ArrayList<String>>> allEdgeServers;

	/** all CDN edges seen by this node */
	private HashSetCache<String> edges;

	/** cache of edge server IPs mapped to a list of node IPs */
	private Map<String, HashSetCache<String>> edgeServerCache;

	/** map of dl hashes to a boolean about whether it is being watched */
	private HashMapCache<String, Boolean> watchedDLs;

	/** set of Peer IPs to track */
	private LinkedHashSet<String> peerIPs;

	/** map of peer IPs to corresponding ports */
	private Map<String, Integer> peerPorts;

	/** IPs that won't forward DNS requests or won't respond to reverse lookups */
	private HashSetCache<String> badIPs;

	/** maps CDN names to edge servers for this node */
	private Map<String, LinkedHashSet<String>> myEdgeServers;

	/** map of download names to a set of peers */
	private Map<String, Set<String>> peersPerDownload;
	/** the set of akamai edges that I've already pinged this round */
	HashSet<String> edgesPinged = new HashSet<String>();
	
	/** set of DNS servers that do not support */
	private HashSetCache<String> nonCompliantDNS;

	// TODO make this something read from a file
	HashMap<String, Integer> customers = new HashMap<String, Integer>();

	private boolean isActive;
	private Statistics stats;
	private IDistributedDatabase ddb;
	private PluginInterface pi;

	/** manages interaection between map of edge servers and DHT */
	EdgeServerMapManager esmm;
	int myId = -1;

	/** manages DHT read and write operations */
	// private DHTManager dhtManager;
	/** map of node ip to a map of domain name to edge server ratio */
	private HashMapCache<String, Map<String, EdgeServerRatio>> edgeServerRatio;

	/** map of edge ip to ping value */
	private HashMapCache<String, Double> edgePingMap;

	/** set of all ips searched for in DHT */
	private Set<String> dhtIps;

	/** set of all ips digged on */
	private Set<String> digIps;

	/** my ratios */
	private Map<String, EdgeServerRatio> myRatio;
	private int sleepIntervalDivisor = 1;
	private int numDnsQueries;

	private static final boolean DEBUG = false;

	private int dnsServerIndex = 0;
	private String dnsServer = null;
	
	public Digger() {
		super("Digger");
		sleepInterval = MainGeneric.getDigSleepTime();
		this.pi = MainGeneric.getPluginInterface();
		this.log = MainGeneric.getLoggerChannel("OnoDigger");
		if (DEBUG) log.setDiagnostic();
		customers.put("a1921.g.akamai.net", 4); // cnn
		// "a1794.l.akamai.net",
		// //"a1921.aol.akamai.net", // this is not a cdn
		// "a1593.g.akamai.net", "a1116.x.akamai.net", "a932.g.akamai.net",
		// "a840.g.akamai.net",
		customers.put("a20.g.akamai.net", 1); // fox news
		customers.put("e100.g.akamaiedge.net", 2); // air asia, for diversity
		customers.put("a245.g.akamai.net", 3); // lemonde
		customers.put("wdig.vo.llnwd.net", 5); // limelight -- abc content
		// this.ddb = pi.getDistributedDatabase();
		cdns = new ArrayList<String>();

		for (Entry<String, Integer> c : customers.entrySet())
			cdns.add(c.getKey());

		edgeServers = new HashMapCache<String, ArrayList<String>>();
		allEdgeServers = new HashMapCache<String, HashMapCache<String, ArrayList<String>>>();
		edgeServerCache = new LinkedHashMap<String, HashSetCache<String>>();
		watchedDLs = new HashMapCache<String, Boolean>();
		peerIPs = new LinkedHashSet<String>();
		edges = new HashSetCache<String>();
		badIPs = new HashSetCache<String>();
		peerPorts = new HashMapCache<String, Integer>();
		myEdgeServers = new HashMapCache<String, LinkedHashSet<String>>();
		peersPerDownload = new HashMapCache<String, Set<String>>();
		isActive = true;
		stats = Statistics.getInstance();
		edgeServerRatio = new HashMapCache<String, Map<String, EdgeServerRatio>>();
		edgePingMap = new HashMapCache<String, Double>();
		nonCompliantDNS = new HashSetCache<String>();

		dhtIps = new HashSet<String>();
		digIps = new HashSet<String>();

		PEER_IP_MAX = MainGeneric.getMaxPeers();
		isServer = MainGeneric.isServer();

		if (ddb != null) {
			initDHTStuff(ddb);
		}
		

	}

	public Digger(int sleep, ILoggerChannel l, IDistributedDatabase dd,
			PluginInterface pi) {
		super("Digger");
	}

	/**
	 * @param ddb
	 * 
	 */
	private void initDHTStuff(IDistributedDatabase ddb) {
		// dhtManager = new DHTManager(ddb);
		esmm = new EdgeServerMapManager(ddb, edgeServerCache, edgeServers, pi);

		OnoPeerManager.getInstance().setEdgeServerMapMananger(esmm);

		// set up peer registry (DISABLED for now)
		/*
		 * PeerRegistry pr = new PeerRegistry(dhtManager, pi);
		 * pr.setDaemon(true); pr.start();
		 */
	}

	/**
	 * Initialization code.
	 * 
	 * @param sleep
	 * @param l
	 * @param dd
	 * @param pi
	 */
	public void initialize(int sleep, ILoggerChannel l,
			IDistributedDatabase dd, PluginInterface pi) {
		sleepInterval = sleep;
		cdns = new ArrayList<String>();

		for (String c : customers.keySet())
			cdns.add(c);

		log = l;
		edgeServers = new HashMapCache<String, ArrayList<String>>();
		allEdgeServers = new HashMapCache<String, HashMapCache<String, ArrayList<String>>>();
		edgeServerCache = new LinkedHashMap<String, HashSetCache<String>>();
		watchedDLs = new HashMapCache<String, Boolean>();
		peerIPs = new LinkedHashSet<String>();
		edges = new HashSetCache<String>();
		badIPs = new HashSetCache<String>();
		peerPorts = new HashMapCache<String, Integer>();
		myEdgeServers = new HashMapCache<String, LinkedHashSet<String>>();
		peersPerDownload = new HashMapCache<String, Set<String>>();
		isActive = true;
		stats = Statistics.getInstance();
		this.ddb = dd;
		this.pi = pi;

		if (ddb != null) {
			initDHTStuff(ddb);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {

		HashSet<String> ipsToLookup = new HashSet<String>();
		long startTime, sleepTime;
		while (!MainGeneric.isShuttingDown()) {
			if (myRatio != null && getIp() != null) {
				edgeServerRatio.put(getIp(), myRatio);
				myRatio = null;
			}
			startTime = System.currentTimeMillis();
			try {
				ipsToLookup.clear();

				// get list of IPs to lookup
				if (OnoConfiguration.getInstance().isDoRemoteDig()) {
					// DownloadManager dm = pi.getDownloadManager();
					// Download[] dls = dm.getDownloads();
					//
					// for (Download dl : dls) {
					// if (watchedDLs.get(dl.getTorrent().getName()) == null) {
					// if (dl.getPeerManager() != null) {
					// watchedDLs.put(dl.getTorrent().getName(), true);
					// }
					//
					// addPeerListener(dl);
					// }
					// }

					ipsToLookup = OnoPeerManager.getInstance().getIpsToDig();
					// synchronized (peerIPs) {
					// for (String ip : peerIPs) {
					// // TODO also limit the number per round and use round
					// robin, perhaps
					// if (!badIPs.contains(ip)) {
					// ipsToLookup.add(ip);
					// }
					// }
					// }
				}

				edgesPinged.clear();

				if (getIp() != null) {
					String peerIp = getIp();
					getCDNView(peerIp);
					
					if (!isActive)
						return;

					// put ratio in DDB
					if (OnoPeerManager.getInstance().getOnoPeer(peerIp) != null
							&& OnoPeerManager.getInstance().getOnoPeer(peerIp)
									.getRatios() != null && esmm != null) {
						esmm.updateDatabase(peerIp, OnoPeerManager
								.getInstance().getOnoPeer(peerIp).getRatios(),
								ddb.getOptions().getNone());

						// TODO for each edge w/ ratio > thresh, add
						// this peer's ip to the value corresponding to the edge
					}
				}

				// iterate over each ip address
				for (String peerIp : ipsToLookup) {
					if ((System.currentTimeMillis() - startTime) > sleepInterval * 1000)
						break;
					getCDNView(peerIp);
					if (!isActive)
						return;

					// put ratio in DDB
					if (OnoPeerManager.getInstance().getOnoPeer(peerIp) != null
							&& OnoPeerManager.getInstance().getOnoPeer(peerIp)
									.getRatios() != null && esmm != null) {
						esmm.updateDatabase(peerIp, OnoPeerManager
								.getInstance().getOnoPeer(peerIp).getRatios(),
								ddb.getOptions().getNone());

						// TODO for each edge w/ ratio > thresh, add
						// this peer's ip to the value corresponding to the edge
					}

				} // end for nodes to lookup

				// put my edge servers in the DHT
				// HashSet<String> allMyEdges = new HashSet<String>();
				//
				// for (HashSet<String> edges : myEdgeServers.values()) {
				// for (String e : edges)
				// allMyEdges.add(e);
				// }

				// add each edge one-by-one
				String ip = getIp();
				if (!MainGeneric.isAzureus() && !MainGeneric.isShuttingDown()) {
					CDNClusterFinder.getInstance().getPreferredPeers(
							MainGeneric.getClusterFinderObject(ip));
				}

				// update DHT w/ ratios

				// for (String e : allMyEdges)
				// esmm.readAndUpdateDDB(ip, e,
				// DistributedDatabase.OP_EXHAUSTIVE_READ);

				// log.log("My edge server entries: " + myEdgeServers);
				// log.log("Total number of edge servers witnessed (" +
				// Statistics.getInstance().getNumEdges() + ")");

				// : " +Statistics.getInstance().getEdgeSet());

				// pruneDataStructures();

				sleepTime = sleepInterval * 1000
						- (System.currentTimeMillis() - startTime);
				if (DEBUG)
					System.out.println("Sleeping for " + sleepTime / 1000.0
							+ " seconds...");
				if (sleepTime > 0) {
					Thread.sleep(sleepTime);
				}

				for (String p : OnoPeerManager.getInstance().getIpsToDig()) {
					OnoPeer op = OnoPeerManager.getInstance().getOnoPeer(p);
					if (op != null) {
						Map<String, EdgeServerRatio> m = op.getRatios();
						for (EdgeServerRatio esr : m.values()) {
							if (esr.getSleepInterval() < sleepInterval) {
								sleepInterval = esr.getSleepInterval();
							}
						}
					}
				}
				// if (OnoConfiguration.getInstance().isAdaptiveDig()){
				// if (sleepIntervalDivisor > 1){
				// sleepInterval /= sleepIntervalDivisor;
				// sleepIntervalDivisor = 1;
				// if (sleepInterval <
				// OnoConfiguration.getInstance().getDigStart())
				// sleepInterval =
				// (int)OnoConfiguration.getInstance().getDigStart();
				// } else {
				// sleepInterval +=
				// (int)OnoConfiguration.getInstance().getDigIncrement();
				// if (sleepInterval >
				// OnoConfiguration.getInstance().getDigMaxInterval()){
				// sleepInterval = (int)
				// OnoConfiguration.getInstance().getDigMaxInterval();
				// }
				// }
				// }
				if (sleepInterval < OnoConfiguration.getInstance()
						.getDigStart())
					sleepInterval = (int) OnoConfiguration.getInstance()
							.getDigStart();
				if (DEBUG)
					System.out.println("Dig rate at " + sleepInterval
							+ " seconds");
				
//				System.out.println("Number of queries: "+numDnsQueries+
//						", sleep interval: "+sleepInterval);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				if (DEBUG) log.log(e);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		} // end infinite while

		self = null;
		allEdgeServers = null;
		badIPs = null;
		cdns = null;
		customers = null;
		ddb = null;
		dhtIps = null;
		digIps = null;
		edgePingMap = null;
		edges = null;
		edgeServerRatio = null;
		edgeServerCache = null;
		edgeServers = null;
		edgesPinged = null;
		myEdgeServers = null;
		peerIPs = null;
		peerPorts = null;
		peersPerDownload = null;
		pi = null;
		stats = null;
		watchedDLs = null;
	}

	/**
	 * This reduces the memory consumption of the plugin (in theory).
	 * 
	 * @deprecated
	 */
	private void pruneDataStructures() {
		while (peerIPs.size() > OnoConfiguration.getInstance().getMaxPeers()) {
			String ip = peerIPs.iterator().next();
			peerIPs.remove(ip);
			badIPs.remove(ip);
			peerPorts.remove(ip);
			allEdgeServers.remove(ip);
		}
	}

	/**
	 * Returns true if the ip address should be the target of remote DNS
	 * lookups.
	 * 
	 * @param ip
	 *            the string representation of the peer's IP address
	 * @return
	 */
	private boolean isProper(String ip) {
		if (isServer) {
			return true;
		}

		// do this based on the peer id
		// for now, give everyone 1/256 of the space (2<<8)
		int hash;

		if (MainGeneric.getPublicIpAddress() != null) {
			hash = (int) (Math
					.abs((MainGeneric.getPublicIpAddress().hashCode() << bitShift) >> bitShift));
		} else {
			if (myId != -1) {
				myId = ((int) Math.random() * Integer.MAX_VALUE);
			}

			hash = (int) (Math.abs((myId << bitShift) >> bitShift));
		}

		return (hash == (int) ((ip.hashCode() << bitShift) >> bitShift));
	}

	/**
	 * Foe each IP, gets the CDN view of the system and updates data structures.
	 * 
	 * @param peerIp
	 */
	private void getCDNView(String peerIp) {
		ArrayList<String> entries;
		ArrayList<String> perPeerEntries;

		digIps.add(peerIp);

		// for each CDN customer name
		String cdnNames[] = cdns.toArray(new String[] {});
		OnoPeer op = OnoPeerManager.getInstance().getOnoPeer(peerIp);
		// make array to avoid synchronization issues
		for (String customerName : cdnNames) {
			if (!isActive)
				return;
			if (op != null) {
				Map<String, EdgeServerRatio> m = op.getRatios();
				if (m != null && m.containsKey(customerName)
						&& !m.get(customerName).shouldLookup()) {
					continue;
				}
			}
			boolean orderChanged = true;

			// set up per peer info
			if (!allEdgeServers.containsKey(peerIp)) {
				allEdgeServers.put(peerIp,
						new HashMapCache<String, ArrayList<String>>());
				allEdgeServers.get(peerIp).put(customerName,
						new ArrayList<String>());
			}

			perPeerEntries = allEdgeServers.get(peerIp).get(customerName);

			if (perPeerEntries == null) {
				perPeerEntries = new ArrayList<String>();
				allEdgeServers.get(peerIp).put(customerName, perPeerEntries);
			}

			// create list for records
			if (!edgeServers.containsKey(customerName)) {
				edgeServers.put(customerName, new ArrayList<String>());
			}

			// get the list
			entries = edgeServers.get(customerName);

			// do lookup
			try {
				if (peerIp.equals(getIp())) {
					records = doLookup(new String[] { customerName });
				} else {
					String ldns = null;
					if (OnoPeerManager
					.getInstance().getOnoPeer(peerIp)!=null){
						ldns =  OnoPeerManager
						.getInstance().getOnoPeer(peerIp).getLdns();
					}
					if (ldns!=null && nonCompliantDNS.contains(ldns)) return;
					else if (ldns!=null){
						records = doLookup(new String[] { "@" + ldns,
								customerName });

						if ((records == null) || (records.length == 0)) {
							if (DEBUG)
								System.out.println("Remote lookup for "
										+ peerIp + " failed!");

							// lifo queue, so remove and add
							nonCompliantDNS.remove(ldns);
							nonCompliantDNS.add(ldns);
						}
					}
					else {
					
					// do reverse lookup
					records = doLookup(new String[] { "-x", peerIp });

					if ((records != null) && (records.length > 0)) {
						boolean badIp = true;

						// iterate over all of the authoritative servers
						for (Record r : records) {
							ldns = r.rdataToString();
							ldns = ldns.substring(0, ldns.length() - 1);

							
							if (ldns.contains(" ")) {
								ldns = ldns.split(" ")[0];
							}

							// if an MX record, ignore
							if (ldns.endsWith(".mx")) {
								break;
							}
							if (ldns.contains(".arin.") || ldns.endsWith(".arpa.") 
									|| ldns.contains(".ripe.") || ldns.contains(".apnic.")) {
								break;
							}
							if (ldns.endsWith("twtelecom.net")) {
								break;
							}

							// dig wrt the returned LDNS
							if (nonCompliantDNS.contains(ldns)) 
								break;
							records = doLookup(new String[] { "@" + ldns,
									customerName });

							if ((records == null) || (records.length == 0)) {
								if (DEBUG)
									System.out.println("Remote lookup for "
											+ peerIp + " failed!");

								// lifo queue, so remove and add
								nonCompliantDNS.remove(ldns);
								nonCompliantDNS.add(ldns);
								continue;
							} else {
								badIp = false;
								if (OnoPeerManager
								.getInstance().getOnoPeer(peerIp)!=null)
									OnoPeerManager
									.getInstance().getOnoPeer(peerIp).setLdns(ldns);
								break;
							}
						}

						if (badIp) {
							OnoPeerManager.getInstance().digFailed(peerIp);
							// badIPs.add(peerIp);

							return;
						}
					} else {
						if (DEBUG)
							System.out.println("Reverse lookup on " + peerIp
									+ " failed!");
						OnoPeerManager.getInstance().digFailed(peerIp);
						// badIPs.add(peerIp);

						return;
					}
				}
				}
			} catch (IOException e) {
				// all kinds of problems that may occur during lookup
				if ((e.getMessage() != null)
						&& e.getMessage().contains(
								"Unable to establish loopback connection")) {
					if (DEBUG)
						System.err.println("Out of ports!");

					try {
						Thread.sleep(1000);
						records = null;
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} else if (e instanceof SocketTimeoutException) {
					if (DEBUG)
						System.err.println("Timeout for "+peerIp+" (my ip: "+getIp()+")!");

					if (!getIp().equals(peerIp)) {
						OnoPeerManager.getInstance().digFailed(peerIp);
					} else {
						org.xbill.DNS.ResolverConfig dnsConfig = 
							new org.xbill.DNS.ResolverConfig();;
						dnsServerIndex = (dnsServerIndex+1)%dnsConfig.servers().length;
						dnsServer = dnsConfig.servers()[dnsServerIndex];
						if (DEBUG)
							System.err.println("Now trying DNS server "+dnsServer+"!");
					}

					records = null;
				}
			}

			// check if we got any records
			if (records != null) {
				// if so, record them
				HashSet<String> ips = new HashSet<String>();
				for (Record r : records) {
					final String ipAddressResponse = r.rdataToString();

					if (!org.xbill.DNS.Address.isDottedQuad(ipAddressResponse)) {
						continue;
					}

					// add to set of all nodes
					if (stats.addEdge(ipAddressResponse)) {
						log.log("@" + System.currentTimeMillis() + ": "
								+ "New edge server recorded: "
								+ ipAddressResponse + "!");
					}

					// completely new entry
					if (!perPeerEntries.contains(ipAddressResponse)) {
						log.log("@" + System.currentTimeMillis() + ": "
								+ "New Akamai server witnessed by [" + peerIp
								+ "]: " + ipAddressResponse + "!");
						perPeerEntries.add(0, ipAddressResponse);

						if (!entries.contains(ipAddressResponse)) {
							entries.add(0, ipAddressResponse);
						}

						stats.addEvent(new DNSEvent(peerIp, customerName,
								ipAddressResponse, EventType.NEW_ENTRY));
					}
					// entry swapped places
					else if (perPeerEntries.indexOf(ipAddressResponse) >= records.length) {
						log.log("@" + System.currentTimeMillis() + ": "
								+ "Order changed for " + customerName
								+ ", according to [" + peerIp + "]: "
								+ ipAddressResponse + "!");
						perPeerEntries.remove(ipAddressResponse);
						perPeerEntries.add(0, ipAddressResponse);

						if (entries.indexOf(ipAddressResponse) >= records.length) {
							entries.remove(ipAddressResponse);
							entries.add(0, ipAddressResponse);
						}

						stats.addEvent(new DNSEvent(peerIp, customerName,
								ipAddressResponse, EventType.REORDER));
					} else {
						orderChanged = false;
					}

					String classCSubnet = Util
							.getClassCSubnet(ipAddressResponse);
					// now do the actual recording of mapping
					if (edgeServerCache.get(classCSubnet) == null) {
						edgeServerCache.put(classCSubnet,
								new HashSetCache<String>());
					}

					synchronized (edgeServerCache.get(classCSubnet)) {
						edgeServerCache.get(classCSubnet).remove(peerIp);
						edgeServerCache.get(classCSubnet).add(peerIp);
					}

					// also record the ratios
					// if (edgeServerRatio.get(peerIp) == null) {
					// edgeServerRatio.put(peerIp,
					// new HashMap<String, EdgeServerRatio>());
					// edgeServerRatio.get(peerIp).put(customerName,
					// new EdgeServerRatio());
					// }
					if (OnoPeerManager.getInstance().getOnoPeer(peerIp) == null)
						return;
					ips.add(ipAddressResponse);

					// if the lookup we're doing is on behalf of ourselves, then
					// we
					// update our own data structures
					if (getIp().equals(peerIp)) {
						LinkedHashSet<String> edges = myEdgeServers
								.get(customerName);

						if (edges == null) {
							edges = new LinkedHashSet<String>();
						}

						edges.remove(ipAddressResponse);
						edges.add(ipAddressResponse);
						myEdgeServers.put(customerName, edges);

						// now do ping, too...
						if (DO_PINGS
								&& !edgesPinged.contains(ipAddressResponse)) {
							edgesPinged.add(ipAddressResponse);
							final String cName = customerName;
							PingManager.getInstance().doPing(ipAddressResponse,
									pi, new PingResponse() {

										public void response(double rtt) {
											if (rtt >= 0) {
												Statistics
														.getInstance()
														.addPingResult(
																ipAddressResponse,
																"-1",
																"-1",
																getIp(),
																rtt,
																Util
																		.currentGMTTime(),
																-1);
											}
											updateEdgePingMap(
													ipAddressResponse, rtt);

										}
									});

						}
					}

					// log.log(LoggerChannel.LT_WARNING,"@" +
					// System.currentTimeMillis() +": " + r.toString());
				}
				if (OnoPeerManager.getInstance().getOnoPeer(peerIp) == null)
					return;
				Map<String, EdgeServerRatio> ratios = OnoPeerManager
						.getInstance().getOnoPeer(peerIp).getRatios();
				

				if (ratios.get(customerName) == null) {
					ratios.put(customerName, new EdgeServerRatio());
				}

				boolean significantChange = ratios.get(customerName)
						.reportSeen(ips);
				if (getIp().equals(peerIp) && significantChange) {
					sleepIntervalDivisor = 2;
				}
			} else {
				// if records are null, probably means that DNS
				// server is not responding
				continue;
			}

			// if there was a change in mappings, add data to distributed db
			// if (orderChanged && (ddb != null) && ddb.isAvailable()) {
			// //log.log("Trying to read data from DHT...");
			// int limit = (perPeerEntries.size() < 2) ? perPeerEntries.size()
			// : 2;
			//
			// for (int i = 0; i < limit; i++) {
			// if (!Address.isDottedQuad(perPeerEntries.get(i))) {
			// System.err.println("Wtf!");
			// }
			//
			// esmm.readAndUpdateDDB(perPeerEntries.get(i), peerIp,
			// DistributedDatabase.OP_EXHAUSTIVE_READ);
			// }
			// }

			if (ddb == null) {
				ddb = MainGeneric.getDistributedDatabase();
				if (ddb != null) {
					initDHTStuff(ddb);
				}
			}
		} // end for CDNS

	}

	protected void updateEdgePingMap(String ipAddressResponse, double rtt) {
		synchronized (edgePingMap) {
			if (edgePingMap.get(ipAddressResponse) == null
					|| edgePingMap.get(ipAddressResponse) == -1) {
				edgePingMap.put(ipAddressResponse, rtt);
			} else if (rtt > 0) {
				edgePingMap.put(ipAddressResponse, (0.5 * edgePingMap
						.get(ipAddressResponse))
						+ 0.5 * rtt);
			}
		}

	}

	/**
	 * Gets the local public IP address as a string.
	 */
	public String getIp() {
		return MainGeneric.getPublicIpAddress();
	}

	/**
	 * Does a remote lookup. If the -x option is specified, only authoritative
	 * name servers are returned. Otherwise, an array of resolved IP addresses
	 * is returned
	 * 
	 * @param argv
	 *            the dig command parameterse
	 * @return
	 * @throws IOException
	 */
	public Record[] doLookup(String[] argv) throws IOException {
		if (NO_LOOKUP) {
			return null;
		}

		org.xbill.DNS.Name name = null;
		int type = org.xbill.DNS.Type.A;
		int dclass = DClass.IN;
		String server = null;
		int arg;
		Message query;
		Message response = null;
		Record rec;
		SimpleResolver res = null;
		boolean printQuery = false;		
		
		if (argv.length < 1) {
			log.log("Invalid dig format!");
			throw new RuntimeException("Invalid dig format!");
		}

		try {
			arg = 0;

			if (argv[arg].startsWith("@")) {
				server = argv[arg++].substring(1);
				res = new SimpleResolver(server);
			}

			String nameString = argv[arg++];

			if (nameString.equals("-x")) {
				name = ReverseMap.fromAddress(argv[arg++]);
				type = Type.PTR;
				dclass = DClass.IN;
			} else {
				name = Name.fromString(nameString, Name.root);
				type = Type.value(argv[arg]);

				if (type < 0) {
					type = Type.A;
				} else {
					arg++;
				}

				dclass = DClass.value(argv[arg]);

				if (dclass < 0) {
					dclass = DClass.IN;
				} else {
					arg++;
				}
			}

			while (arg < argv.length && argv[arg].startsWith("-") && (argv[arg].length() > 1)) {
				switch (argv[arg].charAt(1)) {
				case 'p':

					String portStr;
					int port;

					if (argv[arg].length() > 2) {
						portStr = argv[arg].substring(2);
					} else {
						portStr = argv[++arg];
					}

					port = Integer.parseInt(portStr);

					if ((port < 0) || (port > 65536)) {
						System.out.println("Invalid port");

						return null;
					}

					res.setPort(port);

					break;

				case 'b':

					String addrStr;

					if (argv[arg].length() > 2) {
						addrStr = argv[arg].substring(2);
					} else {
						addrStr = argv[++arg];
					}

					InetAddress addr;

					try {
						addr = InetAddress.getByName(addrStr);
					} catch (Exception e) {
						System.out.println("Invalid address");

						return null;
					}

					res.setLocalAddress(addr);

					break;

				case 'k':

					String key;

					if (argv[arg].length() > 2) {
						key = argv[arg].substring(2);
					} else {
						key = argv[++arg];
					}

					res.setTSIGKey(TSIG.fromString(key));

					break;

				case 't':
					res.setTCP(true);

					break;

				case 'i':
					res.setIgnoreTruncation(true);

					break;

				case 'e':

					String ednsStr;
					int edns;

					if (argv[arg].length() > 2) {
						ednsStr = argv[arg].substring(2);
					} else {
						ednsStr = argv[++arg];
					}

					edns = Integer.parseInt(ednsStr);

					if ((edns < 0) || (edns > 1)) {
						System.out.println("Unsupported " + "EDNS level: "
								+ edns);

						return null;
					}

					res.setEDNS(edns);

					break;

				case 'd':
					res.setEDNS(0, 0, ExtendedFlags.DO, null);

					break;

				case 'q':
					printQuery = true;

					break;

				default:
					System.out.print("Invalid option: ");
					System.out.println(argv[arg]);
				}

				arg++;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			if (name == null) {
				// log.log("Name is null!");
				throw new RuntimeException("Name is null!");
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			if (DEBUG)
				log.log(e);
		}catch (TextParseException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			if (DEBUG)
				log.log(e);
		}

		if (res == null) {
			try {
				res = new SimpleResolver(dnsServer);
				
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				if (DEBUG) log.log(e);
			}
		}

		if (name == null) {
			return null;
		}

		numDnsQueries++;
		
		rec = Record.newRecord(name, type, dclass);	
		query = Message.newQuery(rec);

		if (printQuery) {
			System.err.println(query);
		}
		
		if (DEBUG) Options.set("verbose");
		response = res.send(query);

		// return only the authority section
		if ((response != null) && argv[0].equals("-x")) {
			if (argv.length == 2)
				return response.getSectionArray(Section.AUTHORITY);
			else
				return response.getSectionArray(Section.ANSWER);
		}

		// return the actual ip addresses
		if (response != null) {
			return response.getSectionArray(Section.ANSWER);
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#start()
	 */
	@Override
	public synchronized void start() {
		// TODO Auto-generated method stub
		super.start();
	}

	/**
	 * @return Returns the sleepInterval.
	 */
	public int getSleepInterval() {
		return sleepInterval;
	}

	/**
	 * @param sleepInterval
	 *            The sleepInterval to set.
	 */
	public void setSleepInterval(int sleepInterval) {
		this.sleepInterval = sleepInterval;
	}

	public void addCDN(String cdn, Integer index) {
		if (customers.containsKey(cdn))
			return;
		cdns.add(cdn);
		customers.put(cdn, index);
	}

	public void removeCDN(String cdn) {
		cdns.remove(cdn);
	}

	/**
	 * @return Returns the isActive.
	 */
	public boolean isActive() {
		return isActive;
	}

	/**
	 * @param isActive
	 *            The isActive to set.
	 */
	public void setActive(boolean isActive) {
		this.isActive = isActive;
		if (!isActive) {
			self = null;
		}
	}

	/**
	 * @return Returns the myEdgeServers.
	 */
	public Map<String, LinkedHashSet<String>> getMyEdgeServers() {
		return myEdgeServers;
	}

	/**
	 * cache of edge server IPs mapped to a list of node IPs
	 * 
	 * @return Returns the edgeServerCache.
	 */
	public Map<String, HashSetCache<String>> getEdgeServerCache() {
		return edgeServerCache;
	}

	/**
	 * @return Returns the peerPorts.
	 * @deprecated
	 */
	public Map<String, Integer> getPeerPorts() {
		return peerPorts;
	}

	/**
	 * @return
	 * @deprecated
	 */
	public Map<String, Set<String>> getPeersPerDownload() {
		// TODO Auto-generated method stub
		return peersPerDownload;
	}

	/**
	 * @return
	 */
	public Statistics getStats() {
		// TODO Auto-generated method stub
		return stats;
	}

	/**
	 * Gets a reference to the DHT manager, which may not have been initialized
	 * previously because it was not loaded yet.
	 */
	// public static DHTManager getDHTManager() {
	// if (self.ddb == null) {
	// System.out.println("DDB not initialized...trying now!");
	//
	// PluginInterface[] pis = self.pi.getPluginManager().getPlugins();
	// String s;
	//
	// for (PluginInterface pi : pis) {
	// s = pi.getPluginName();
	//
	// if (s.contains("Distributed")) {
	// self.ddb = pi.getDistributedDatabase();
	// self.initDHTStuff(self.ddb);
	//
	// break;
	// }
	// }
	// }
	//
	// return self.dhtManager;
	// }
	/**
	 * The static interface to the Digger.
	 * 
	 * @return
	 */
	public synchronized static Digger getInstance() {
		if (self == null) {
			self = new Digger();
			self.setDaemon(true);

			return self;
		} else {
			return self;
		}
	}

	/**
	 * Find nodes discovered via DHT that are also in the "OnoExp" torrent.
	 * 
	 * Returns a map of peer ip to its port and associated edge server
	 * 
	 * @deprecated
	 */
	public HashMap<String, Pair<Integer, String>> getNearbyOnoPeers(int limit) {
		HashMap<String, Integer> peerIps;
		HashMap<String, Pair<Integer, String>> ipsToSend = new HashMap<String, Pair<Integer, String>>();
		HashSet<String> myEdges = new HashSet<String>();

		// get all known ono peer ips
		peerIps = MainGeneric.getOnoPeers(limit);
		if (peerIps.size() == 0)
			return ipsToSend;

		// get list of all my edge servers
		synchronized (myEdgeServers) {
			for (LinkedHashSet<String> edges : myEdgeServers.values()) {
				// TODO reevaluate: chose only first two from each customer
				int i = 0;

				for (String s : edges) {
					myEdges.add(s);
					i++;

					if (i >= 2) {
						break;
					}
				}
			}
		}

		Map<String, HashSetCache<String>> esc = esmm.getEdgeServerCache();

		synchronized (esc) {
			for (Entry<String, HashSetCache<String>> e : esc.entrySet()) {
				if (myEdges.contains(e.getKey())) {
					for (String peer : e.getValue()) {
						if (peerIps.containsKey(peer)) {
							ipsToSend.put(peer, new Pair<Integer, String>(
									peerIps.get(peer), e.getKey()));
						}
					}
				}
			}
		}

		// TODO remove when done
		// if (!ipsToSend.containsKey("165.124.182.59")){
		// ipsToSend.put("165.124.182.59", 7000);
		// }

		// finally, return list of peers
		return ipsToSend;
	}

	/**
	 * Adds a peerIp to the list for checking.
	 * 
	 * @param peerIp
	 *            string of the Ip address of the peer to check
	 * @deprecated
	 */
	public void addPeerIpForDigging(String peerIp) {
		synchronized (peerIPs) {
			// if (badIPs.contains(peerIp)) return;
			peerIPs.add(peerIp);
		}
	}

	/**
	 * Returns the edge-server ratios for the current node.
	 * 
	 * @return
	 */
	public Map<String, EdgeServerRatio> getMyEdgeServerRatio() {
		return edgeServerRatio.get(getIp());

	}

	public Map<String, Double> getEdgePings() {
		return edgePingMap;
	}

	public void setEdgeServerRatios(Map<String, EdgeServerRatio> values) {
		myRatio = values;
	}

	public short getCustomerIndex(String key) {
		// TODO Auto-generated method stub
		if (customers.get(key) == null)
			return -1;
		return customers.get(key).shortValue();
	}

	public String getCustomerIndex(int customerIndex) {
		// TODO Auto-generated method stub
		for (Entry<String, Integer> e : customers.entrySet()) {
			if (e.getValue() == customerIndex)
				return e.getKey();
		}
		return null;
	}

	public void addRatio(int customerIndex, String peerIp, EdgeServerRatio esr) {
		String cust = Digger.getInstance().getCustomerIndex(customerIndex);

		if (OnoPeerManager.getInstance().getOnoPeer(peerIp) == null)
			return;
		OnoPeerManager.getInstance().addRatio(peerIp, cust, esr);
		OnoPeerManager.getInstance().findCluster(peerIp);
		// CDNClusterFinder.getInstance().addPeer(MainGeneric.getClusterFinderObject(peerIp),
		// peerIp, MainGeneric.getPeerPort(peerIp));

	}

	// /**
	// * Returns of a map of domain name to edge server ratios for
	// * the specified peer ip.
	// * @param peerIp the peer ip.
	// * @return
	// */
	// public Map<String, EdgeServerRatio> getRatios(String peerIp){
	//		
	// if (edgeServerRatio.containsKey(peerIp)){
	//		
	// for (Entry<String, EdgeServerRatio> e:
	// edgeServerRatio.get(peerIp).entrySet()){
	// if (Util.currentGMTTime() - e.getValue().getLastUpdate()
	// < RATIO_VALIDITY_INTERVAL){
	// return edgeServerRatio.get(peerIp);
	// }
	// }
	//		
	// }
	//
	// // do lookup
	// if (esmm!=null) {
	// dhtIps.add(peerIp);
	// esmm.lookup(peerIp);
	// }
	//		
	// return null;
	// }

	// @SuppressWarnings("unchecked")
	// public HashMapCache<String, Map<String,EdgeServerRatio>> getAllRatios() {
	// return (HashMapCache<String,
	// Map<String,EdgeServerRatio>>)edgeServerRatio.clone();
	//		
	// }

	public void lookupEdgeCluster() {
		Set<String> edgeClusters = new HashSet<String>();
		OnoPeer op = OnoPeerManager.getInstance().getOnoPeer(getIp());
		if (op == null)
			return;
		Map<String, EdgeServerRatio> myRatios = op.getRatios();
		if (myRatios != null) {
			synchronized (myRatios) {
				for (Entry<String, EdgeServerRatio> e : myRatios.entrySet()) {			
					edgeClusters.addAll(e.getValue().getTightlyBoundEdgeClusters());
				}
			}
		}

		if (esmm != null) {
			for (String edgeCluster : edgeClusters) {
				esmm.lookup(edgeCluster);
			}
		}
	}

	public EdgeServerMapManager getEsmm() {
		return esmm;
	}

}
