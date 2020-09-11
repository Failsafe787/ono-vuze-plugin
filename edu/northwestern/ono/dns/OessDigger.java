/**
 * Ono Project
 *
 * File:         Digger.java
 * RCS:          $Id: OessDigger.java,v 1.8 2010/10/15 15:55:33 drc915 Exp $
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
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

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.OnoConfiguration;
import edu.northwestern.ono.dht.IDistributedDatabase;
import edu.northwestern.ono.experiment.TraceRouteRunner;
import edu.northwestern.ono.position.OnoPeerManager;
import edu.northwestern.ono.position.OnoPeerManager.OnoPeer;
import edu.northwestern.ono.stats.DNSEvent;
import edu.northwestern.ono.stats.EdgeServerRatio;
import edu.northwestern.ono.stats.Statistics;
import edu.northwestern.ono.stats.DNSEvent.EventType;
import edu.northwestern.ono.util.HashMapCache;
import edu.northwestern.ono.util.HashSetCache;
import edu.northwestern.ono.util.ILoggerChannel;
import edu.northwestern.ono.util.PingManager;
import edu.northwestern.ono.util.PluginInterface;
import edu.northwestern.ono.util.Util;
import edu.northwestern.ono.util.Util.PingResponse;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 * 
 *         The Digger class hunts for Akamai or other CDN entries and reports
 *         them appropriately.
 * 
 *         Note: If DIG_FOR_OTHERS is set to true, you will have to make sure
 *         that you reduce the TCP timeout and increase the number of user ports
 *         available on your OS. For help, see
 *         http://www.minq.se/products/pureload/tuning/connections.html.
 */
public class OessDigger extends Thread {
	
	/** bits to shift for assigning digging of peers w/o plugin */
	public static HashMap<String, Integer> dnsMap = Statistics.dnsMap;
	private static final int bitShift = 30;

	private static final boolean DO_PINGS = true;
	private static final boolean NO_LOOKUP = false;

	private static boolean isServer = false;
	private static OessDigger self;

	/** sleep interval, in seconds */
	private int sleepInterval = 600;

	/** the CDNs to check */
	private ArrayList<String> cdns;
	private ILoggerChannel log;
	private Record[] records;

	/** maps CDN names to edge servers */
	private HashMapCache<String, ArrayList<String>> edgeServers;

	/** maps CDN names to edge servers FOR EACH PEER, indexed by peer IP */
	private HashMapCache<String, HashMapCache<String, ArrayList<String>>> allEdgeServers;

	/** cache of edge server IPs mapped to a list of node IPs */
	private Map<String, HashSetCache<String>> edgeServerCache;

	/** maps CDN names to edge servers for this node */
	private Map<String, LinkedHashSet<String>> myEdgeServers;

	/** the set of akamai edges that I've already pinged this round */
	HashSet<String> edgesPinged = new HashSet<String>();

	// TODO make this something read from a file
	HashMap<String, Integer> customers = new HashMap<String, Integer>();

	private boolean isActive;
	private Statistics stats;
	private PluginInterface pi;

	/** manages interaection between map of edge servers and DHT */
	EdgeServerMapManager esmm;
	int myId = -1;

	/** map of node ip to a map of domain name to edge server ratio */
	private HashMapCache<String, Map<String, EdgeServerRatio>> edgeServerRatio;
	
	/** map of edge ip to ping value */
	private HashMapCache<String, Double> edgePingMap;

	/** my ratios */
	private Map<String, EdgeServerRatio> myRatio;
	private int sleepIntervalDivisor = 1;

	private static final boolean DEBUG = false;
	
	private HashMap<String, Long> timeoutIsRunning;
	
	
	public OessDigger() {
		super("OessDigger");
//		sleepInterval = MainGeneric.getOessDigSleepTime();
		this.pi = MainGeneric.getPluginInterface();
		this.log = MainGeneric.getLoggerChannel("OessOnoDigger");
		cdns = new ArrayList<String>();	
		edgeServers = new HashMapCache<String, ArrayList<String>>();
		allEdgeServers = new HashMapCache<String, HashMapCache<String, ArrayList<String>>>();
		edgeServerCache = new LinkedHashMap<String, HashSetCache<String>>();
		new HashMapCache<String, Boolean>();
		new LinkedHashSet<String>();
		new HashSetCache<String>();
		new HashMapCache<String, Integer>();
		myEdgeServers = new HashMapCache<String, LinkedHashSet<String>>();
		new HashMapCache<String, Set<String>>();
		isActive = true;
		stats = Statistics.getInstance();
		edgeServerRatio = new HashMapCache<String, Map<String, EdgeServerRatio>>();
		edgePingMap = new HashMapCache<String, Double>();
		isServer = MainGeneric.isServer();
		timeoutIsRunning = new HashMap<String, Long>();
		
	}
	
	public HashMap<String, Integer> getCustomerNames() {
		
		Properties clientNamesProperties = null;
		HashMap<String, Integer> clientNames = new HashMap<String, Integer>();
		

		while (clientNamesProperties == null || clientNamesProperties.isEmpty()) {
			String url2 = "http://www.aqua-lab.org/ono/ana_cdn_names.properties";

			if (DEBUG)
				System.out.println("Connecting to " + url2);

			clientNamesProperties = MainGeneric.downloadFromURL(url2, 30*1000);

			if (clientNamesProperties.isEmpty()) {
				if (DEBUG) {
					System.out.println("OessDigger: ERROR loading " + url2);
					System.out.println("OessDigger: Retrying url in 60 seconds... ");
				}

				try {
					Thread.sleep(60*1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		Iterator ent = clientNamesProperties.entrySet().iterator();
		
		while (ent.hasNext()) {
			Entry e = (Entry) ent.next();
			String k = (String) e.getKey();
		    clientNames.put((String) e.getValue(), Integer.parseInt(k));
		    if (DEBUG) {
		    	System.out.println("OessDigger: Loading edgeserver info: " + e.getValue() + " " + k);
		    }
		}
		
		return clientNames;

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
		new HashMapCache<String, Boolean>();
		new LinkedHashSet<String>();
		new HashSetCache<String>();
		new HashMapCache<String, Integer>();
		myEdgeServers = new HashMapCache<String, LinkedHashSet<String>>();
		new HashMapCache<String, Set<String>>();
		isActive = true;
		stats = Statistics.getInstance();
		this.pi = pi;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		
		long startTime, sleepTime;
		String myIp = getIp();
		ArrayList<String> myDns;
//		ArrayList<String> myDns = getMyDns(myIp);
		long commitRatiosInterval = 10;
		
		customers = getCustomerNames();
//		for (String dnsIp : myDns ) {
//			traceRouteDnsServers(dnsIp);
//			pingDnsServers(dnsIp);
//		}
				
		

		while (customers == null) {
		
			if (DEBUG)
				System.out.println("OessDigger: Retrying url in 60 seconds... ");
			try {
				Thread.sleep(60*1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			customers = getCustomerNames();	
		}

		for (Entry<String, Integer> c : customers.entrySet()) {
			cdns.add(c.getKey());
		}
		
		long commitRatiosStartTime = System.currentTimeMillis();
		
		while (!MainGeneric.isShuttingDown()) {
			if (myRatio != null && getIp() != null) {
				edgeServerRatio.put(getIp(), myRatio);
				myRatio = null;
			}
			startTime = System.currentTimeMillis();
			
			/**/
			myDns = getMyDns(myIp);
			for (String dnsIp : myDns ) {
				traceRouteDnsServers(dnsIp);
				pingDnsServers(dnsIp);
			}
			/**/

			try {
				
				if (getIp() != null) {
												
					if (( myDns != null) && (isActive)) {

						OnoPeerManager.getInstance().getOnoPeer(myIp).updateDnsList(myDns);
						for (String dnsIp : myDns ) {

							getCDNView(myIp, dnsIp);
							
							if (System.currentTimeMillis() - commitRatiosStartTime >= commitRatiosInterval*60*1000)  {
									
								stats.commitRatiosOess();
								pingDnsServers(dnsIp);
								if (DEBUG) {
							    	System.out.println("OessDigger: commitRatiosOessInterval is " + commitRatiosInterval + " minutes...");
							    }
								
//								if (commitRatiosInterval < 160)
//									commitRatiosInterval *= 2;
//								else
//									commitRatiosInterval += 180;

							}
						}
						
					}
					if (!isActive)
						return;
				}
				
					
				sleepTime = sleepInterval * 1000
						- (System.currentTimeMillis() - startTime);
								
				if (DEBUG)
					System.out.println("OessDigger Sleeping for " + sleepTime / 1000.0
							+ " seconds...");
				if (sleepTime > 0) {
					Thread.sleep(sleepTime);
				}

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.log(e);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		} // end infinite while

		self = null;
		allEdgeServers = null;
		cdns = null;
		customers = null;
		edgePingMap = null;
		edgeServerRatio = null;
		edgeServerCache = null;
		edgeServers = null;
		edgesPinged = null;
		myEdgeServers = null;
		pi = null;
		stats = null;
	}


	/**
	 * Foe each IP, gets the CDN view of the system and updates data structures.
	 * 
	 * @param peerIp
	 */
	private void getCDNView(String peerIp, String peerDns) {
		ArrayList<String> entries;
		ArrayList<String> perPeerEntries;

		// for each CDN customer name
		String cdnNames[] = cdns.toArray(new String[] {});
		OnoPeer op = OnoPeerManager.getInstance().getOnoPeer(peerIp);
		
		// make array to avoid synchronization issues
		for (String customerName : cdnNames) {
			if (!isActive)
				return;
			
			
			if (op != null) {
				Map<String, EdgeServerRatio> m = op.getPerDnsRatios(peerDns);
				if (m != null && m.containsKey(customerName)
						&& !m.get(customerName).shouldLookup()) {
					//if the customer name already exists for the OnoPeer (or it's
					// not the right time), do nothing
					continue;  
				}

				
			}
			// insert the record into allEdgeServers if it doesn't already exist and
			// set up the per peer info
			if (!allEdgeServers.containsKey(peerIp)) {
				allEdgeServers.put(peerIp,
						new HashMapCache<String, ArrayList<String>>());
				allEdgeServers.get(peerIp).put(customerName,
						new ArrayList<String>());
			}

			//perPeerEntries contains the IP addresses of all the EdgeServers this peer has seen
			perPeerEntries = allEdgeServers.get(peerIp).get(customerName);

			//if there is no EdgeServer info for this peer, add it
			if (perPeerEntries == null) {
				perPeerEntries = new ArrayList<String>();
				allEdgeServers.get(peerIp).put(customerName, perPeerEntries);
			}

			// create list of these CDN customer names if they don't exist (independent
			// of the OnoPeer in question
			if (!edgeServers.containsKey(customerName)) {
				edgeServers.put(customerName, new ArrayList<String>());
			}

			// get the list
			entries = edgeServers.get(customerName);

			// do lookup
			try {
				if (peerIp.equals(getIp())) {
					records = doLookup(new String[] { "@" + peerDns, customerName });

				}
			}
			catch (IOException e) {
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
						System.err.println("Dns Timeout!");
					records = null;
					
					if (OnoConfiguration.getInstance().getOessDnsTimeout() != 0)
						 launchOessTimeout(peerDns, peerIp);
						
					
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

					// completely new entry (if it does'nt exist, add it)
					if (!perPeerEntries.contains(ipAddressResponse)) {
						log.log("@" + System.currentTimeMillis() + ": "
								+ "New Akamai server witnessed by [" + peerIp
								+ "]: " + ipAddressResponse + "!");
						perPeerEntries.add(0, ipAddressResponse);

						if (!entries.contains(ipAddressResponse)) {
							entries.add(0, ipAddressResponse);

							stats.addEventOess(new DNSEvent(peerIp, customerName,
									ipAddressResponse, EventType.NEW_ENTRY));
						

						}

					}
					// entry swapped places (i.e. if the IP was sent 2nd instead of 1st by DNS)
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

							stats.addEventOess(new DNSEvent(peerIp, customerName,
								ipAddressResponse, EventType.REORDER));


						}

					} else {
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


					if (OnoPeerManager.getInstance().getOnoPeer(peerIp) == null)
						return;

					ips.add(ipAddressResponse);

					// if the lookup we're doing is on behalf of ourselves, then
					// we update our own data structures
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
						if (DO_PINGS && !edgesPinged.contains(ipAddressResponse)) {
							edgesPinged.add(ipAddressResponse);
							pingEdgeServers(ipAddressResponse);
						}
						
					}
				}
				
				
				if (OnoPeerManager.getInstance().getOnoPeer(peerIp) == null)
					return;
				Map<String, EdgeServerRatio> ratios = OnoPeerManager
						.getInstance().getOnoPeer(peerIp).getPerDnsRatios(peerDns);
				
				//remove DNS server from timeoutIsRunning since it came back up & report
				if (timeoutIsRunning.containsKey(peerDns))
					if (timeoutIsRunning.get(peerDns) != 0) {
						stats.addTimeoutOess(peerDns, peerIp, 
								System.currentTimeMillis() - timeoutIsRunning.get(peerDns));
						timeoutIsRunning.remove(peerDns);
					}
				
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
	
	 public boolean isPrivateIp(String ip) {

		 if (ip.startsWith("127.") || ip.startsWith("192.168") || ip.startsWith("10.") || 
				 ip.matches("172.1[6-9].[\\d]{1,3}\\.[\\d]{1,3}") || ip.matches("172.2[0-9].[\\d]{1,3}\\.[\\d]{1,3}") ||
				 ip.matches("172.3[0-1].[\\d]{1,3}\\.[\\d]{1,3}"))
 			 return true;
		 
		 return false;  

	 }   
	
	public ArrayList<String> getMyDns(String myIp) {
		
		String publicDnsIp, publicDnsIpAna;
		int publicDnsId, publicDnsIdAna, dnsId;
		ArrayList<String> myDns = new ArrayList<String>();	
		org.xbill.DNS.ResolverConfig DNSConfig = new org.xbill.DNS.ResolverConfig();
		int order = 0;
	
		for (String n : DNSConfig.servers()) {

			order++;
			publicDnsIp = null;
			publicDnsIpAna = null;
			publicDnsId = -1;
			publicDnsIdAna = -1;
			dnsId = -1;
			
			dnsId = MainGeneric.getReporter().makeId("dnsips", "ip", n);
			
			publicDnsIp = getMyPublicDnsIp( n, myIp );	
			
			if (OnoConfiguration.getInstance().isOessUseDelegation())
				 publicDnsIpAna = getPublicDnsIpAna(n, myIp);
			
			if (DEBUG)
				System.out.println("DNSIPS: " + n + " " + publicDnsIpAna + " " + order);

			if ( (!publicDnsIpAna.equals(n) || (publicDnsIpAna.equals(n) && (!isPrivateIp(n)))) ) {
				publicDnsIdAna = MainGeneric.getReporter().makeId("dnsips", "ip", publicDnsIpAna);

				if (!dnsMap.containsKey(publicDnsIpAna)) {
					MainGeneric.getReporter().insertDnsMapping(stats.getPeerId(myIp), publicDnsIdAna, dnsId, "A", order);
					pingDnsServers(publicDnsIpAna);
					traceRouteDnsServers(publicDnsIpAna);				
					dnsMap.put(publicDnsIpAna, dnsMap.size());
				}

			}

			else {
				
			  if ( isPrivateIp(n)) {
				publicDnsId = MainGeneric.getReporter().makeId("dnsips", "ip", publicDnsIp);

				if (!dnsMap.containsKey(publicDnsIp)) {
					MainGeneric.getReporter().insertDnsMapping(stats.getPeerId(myIp), publicDnsId, dnsId, "P", order);
					pingDnsServers(publicDnsIp);
					traceRouteDnsServers(publicDnsIp);
					dnsMap.put(publicDnsIp, dnsMap.size());
				}
			  }
			  			  
			}
			
			myDns.add(n);
			
			if (!dnsMap.containsKey(n))
				dnsMap.put(n, dnsMap.size());
		}
		
		return myDns;
	}
	
	public String getPublicDnsIpAna (String dnsIp, String myIp) {
	
		String returnDnsIp = dnsIp;
		try {
			records = doLookup(new String[] {"@"+dnsIp, 
					myIp+".ana-aqualab.cs.northwestern.edu" });
			if (records.length != 0) {
				//System.out.println("PublicDnsIpAna: " + records[0].rdataToString());
				returnDnsIp = records[0].rdataToString();	
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return returnDnsIp; 
	}
	
	public String getMyPublicDnsIp (String dnsIp, String myIp) {

		String returnDnsIp = dnsIp;
		try {
			records = doLookup(new String[] { "-x", myIp });
			if (records!=null && records.length != 0) {
				String ipAddressResponse = records[0].rdataToString();
				//System.out.println("MyPublicDns: " + ipAddressResponse);
				ipAddressResponse = 
					ipAddressResponse.substring(ipAddressResponse.indexOf("."));
				//System.out.println("MyPublicDns: " + ipAddressResponse);
				records = doLookup(new String[] { ipAddressResponse.substring(1), "ns"});

				if (records.length != 0) {
					//System.out.println("MyPublicDns: " + records[0].rdataToString());
					returnDnsIp = records[0].rdataToString();
					records = doLookup(new String[] { records[0].rdataToString() });
					
					if (records.length != 0) {
						returnDnsIp = records[0].rdataToString();
						//System.out.println("LOOKUP " + records[0].rdataToString());
					}
				}
			}
			
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if (!isValidIp(returnDnsIp))
			return dnsIp;
		
		return returnDnsIp;
	}
	
	public static boolean isValidIp(final String ip)
    {
        return ip.matches("^[\\d]{1,3}\\.[\\d]{1,3}\\.[\\d]{1,3}\\.[\\d]{1,3}$");
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

			while (argv[arg].startsWith("-") && (argv[arg].length() > 1)) {
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
		} catch (TextParseException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			if (DEBUG)
				log.log(e);
		}

		if (res == null) {
			try {
				res = new SimpleResolver();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.log(e);
			}
		}

		if (name == null) {
			return null;
		}

		rec = Record.newRecord(name, type, dclass);
		query = Message.newQuery(rec);

		if (printQuery) {
			System.out.println(query);
		}


		response = res.send(query);

		// return only the authority section

		if ((response != null) && argv[0].equals("-x")) {
			if (argv.length == 2)
//				return response.getSectionArray(Section.AUTHORITY);
//			else
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
	 * @return
	 */
	public Statistics getStats() {
		// TODO Auto-generated method stub
		return stats;
	}

	/**
	 * The static interface to the OessDigger.
	 * 
	 * @return
	 */
	public synchronized static OessDigger getInstance() {
		if (self == null) {
			self = new OessDigger();
			self.setDaemon(true);

			return self;
		} else {
			return self;
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

	public void pingEdgeServers(final String ipAddress)
	{

		PingManager.getInstance().doPing(ipAddress, 
				pi, new PingResponse() {

					public void response(double rtt) {
						if (rtt >= 0) {
							Statistics.getInstance()
									.addPingResultOess(
											getIp(),
											ipAddress,
											rtt,
											Util.currentGMTTime(),
											2
											);
						}
						updateEdgePingMap(ipAddress, rtt);
					}
				});

	}
	
	public void pingDnsServers(final String ipAddress)
	{
		
			PingManager.getInstance().doPing(ipAddress, 
					pi, new PingResponse() {

						public void response(double rtt) {
							if (rtt >= 0) {
								Statistics.getInstance()
										.addPingResultOess(
												getIp(),
												ipAddress,
												rtt,
												Util.currentGMTTime(),
												1
												);
							}
							updateEdgePingMap(ipAddress, rtt);
						}
					});
	}

	
	public void traceRouteDnsServers(String ipAddress)
	{
		TraceRouteRunner.getInstance().addIpOess(ipAddress, 1);
	}
	
	
	public void launchOessTimeout(final String dnsIp, final String peerIp)
	{
	  
	  if (!timeoutIsRunning.containsKey(dnsIp)) {

		pingDnsServers(dnsIp);  
		timeoutIsRunning.put(dnsIp, (long)0 );
		//stats.addTimeoutOess(dnsIp, peerIp, 0);
		
	  	MainGeneric.createThread("TimeoutDigger-" + dnsIp, new Runnable(){

			public void run() {
				org.xbill.DNS.Name name = null;
				int type = org.xbill.DNS.Type.A;
				int dclass = DClass.IN;
				boolean again = true;

				Message query;
				Message response = null;
				Record rec;
				SimpleResolver res = null;
				int timeout = OnoConfiguration.getInstance().getOessDnsTimeout();
				
				long b4 = System.currentTimeMillis();
				int i = 1;
				while (again) {
				  try {
					//name = Name.fromString("timeout.ana-aqualab.cs.northwestern.edu", Name.root);
					name = Name.fromString("google.com", Name.root);
					res = new SimpleResolver(dnsIp);
					rec = Record.newRecord(name, type, dclass);
					query = Message.newQuery(rec);
					response = res.send(query);
					again = false;

				  
				  } catch (IOException e1) {

					  if (DEBUG)
						  System.out.println(dnsIp + " dns TIMEOUT: try again in " + timeout + " seconds...");
					  
					  
					  if (System.currentTimeMillis() - b4 >= i*5*60*1000) {
						  timeout *= 2;
						  i++;
						  
						  if (DEBUG)
							  System.out.println(dnsIp + " timeout: " + timeout);
					  }
						  
					  //try again until timeout >= 10 minutes, then give up...
					  if ( timeout >= 10*60 ) {
						  if (DEBUG)
							  System.out.println(dnsIp + " dns TIMEOUT: giving up...");
						  
						  timeoutIsRunning.put(dnsIp, b4);  
						  return;
					  }
				  
					  try {
							Thread.sleep(timeout*1000);
						  } catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						  }
						  again = true;
				  }
				  
				} //while
					
				if (DEBUG) {
				  System.out.println(dnsIp + " dns TIMEOUT: back online...");
				  System.out.println(dnsIp + ": " + Long.toString(System.currentTimeMillis() - b4));
				}
				stats.addTimeoutOess(dnsIp, peerIp, System.currentTimeMillis() - b4);
				timeoutIsRunning.remove(dnsIp);		
			} //run
		     	
	  	 });


	  }	// if

	}
	
	
}
