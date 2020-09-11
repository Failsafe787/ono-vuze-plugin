/**
 * Ono Project
 *
 * File:         OptimalEdgeServerExperimentManager.java
 * RCS:          $Id: OessExperiment.java,v 1.8 2010/10/15 15:55:33 drc915 Exp $
 * Description:  OptimalEdge class (see below)
 * Author:       Mario Sanchez
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Oct 15, 2008 at 07:38:30 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.experiment
 * Status:       Experimental (Do Not Distribute)
 *
 * (C) Copyright 2008, Northwestern University, all rights reserved.
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
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
import edu.northwestern.ono.stats.Statistics;
import edu.northwestern.ono.util.HashMapCache;
import edu.northwestern.ono.util.PingManager;
import edu.northwestern.ono.util.PluginInterface;
import edu.northwestern.ono.util.Util;
import edu.northwestern.ono.util.Util.PingResponse;

/**
 * @author Mario Sanchez &lt;msanchez@u.northwestern.edu&gt;
 * 
 *         The OptimalEdge class manages the Optimal Edge Server
 *         Selection experiment.
 */

public class OessExperiment extends Thread {	
	
	private boolean isActive;
	private static OessExperiment self;
	private String url;
	private PluginInterface pi;
	private static final boolean DEBUG = false;
	
	/** map of edge ip to ping value */
	private HashMapCache<String, Double> edgePingMap;

	public OessExperiment() {
		super("OessExperiment");
		this.pi = MainGeneric.getPluginInterface();
		edgePingMap = new HashMapCache<String, Double>();
		self = this;
		isActive = true;
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
	 * The static interface to the OessExperiment.
	 * 
	 * @return
	 */
	public synchronized static OessExperiment getInstance() {
		if (self == null) {
			self = new OessExperiment();
			self.setDaemon(true);

			return self;
		} else {
			return self;
		}
	}

	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */

	public void run() {
	
	  //Obtain my IP address & DNS servers
	  Properties edgeServerIpsProperties = null;
	  ArrayList<String> edgeServerIps;
	  Integer sleepTime = 0;
		
	  while (this.isActive) {
		
		String myIp = MainGeneric.getPublicIpAddress();
		
		//Obtain list of EdgeServers' IP addresses to perform traceroutes and pings
		while (edgeServerIpsProperties == null || edgeServerIpsProperties.isEmpty())  {	
			if (myIp.split("\\.").length!=4) break;
			String queryString = getASN(myIp);
			String url = "http://haleakala.cs.northwestern.edu/ono/ws/ana_edges.php?ipaddress="+queryString;
			if (DEBUG)
				System.out.println("OessExperiment: Connecting to " + url);
			
			edgeServerIpsProperties = MainGeneric.downloadFromURL(url, 30*1000);
			if (edgeServerIpsProperties.isEmpty()) {
				if (DEBUG) {
					System.out.println("OessExperiment: ERROR loading " + url);
					System.out.println("OessExperiment: Retrying url in 60 seconds... ");
				}
				try {
					Thread.sleep(60*1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			}
		}
		
				
		if (edgeServerIpsProperties != null) {
			String everySeconds = (String) edgeServerIpsProperties.getProperty("0");
			if (everySeconds == null) 
				everySeconds="0";
			
			sleepTime = Integer.parseInt(edgeServerIpsProperties.getProperty("1"));
			
			if ( (sleepTime == null) || (sleepTime == 0) )
				sleepTime = 360; //6 hours default (in minutes)

			if (DEBUG)
				System.out.println("OessExperiment: everySeconds=" + everySeconds + " seconds, " +
						"sleepTime=" + sleepTime + " minutes...");
			
			edgeServerIps = getEdgeServerIps(edgeServerIpsProperties);
						
			for ( final String ipAddress : edgeServerIps) {
		
				if (!isActive) return;
				
				try {
					Thread.sleep(Integer.parseInt(everySeconds) * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}	
				Statistics.getInstance().addEdgeServerForLookup(ipAddress);
				pingServers(ipAddress);
				traceRouteEdgeServers(ipAddress);
				
				if (DEBUG)
					System.out.println("OessExperiment: Ping and TraceRoute server " + ipAddress );
				
			}		
			//OessExperimentManager.getInstance().oessExperimentFinished = true;
			//return;
			
		}	
		
		if (DEBUG)
			System.out.println("OessExperiment: Sleeping for " + sleepTime + " minutes...");
		try {
			Thread.sleep(sleepTime * 60 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		edgeServerIpsProperties.clear();

	  } // while(isActive)

	}
	

	public ArrayList<String> getEdgeServerIps(Properties edgeServerIpsProperties) {
		
		ArrayList<String> edgeServerIps = new ArrayList<String>();
		Iterator ent = edgeServerIpsProperties.entrySet().iterator();
		
		while (ent.hasNext()) {
			Entry e = (Entry) ent.next();
			String ipAddress = (String) e.getValue();
			String k = (String) e.getKey();

			try {
				if ( (!k.equals("0")) && (!k.equals("1")) ) {
				    edgeServerIps.add(ipAddress);
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		return edgeServerIps;
		
	}
	
	
	public void traceRouteEdgeServers(String ipAddress)
	{
		TraceRouteRunner.getInstance().addIpOess(ipAddress, 2);
	}
	
	
	public void pingServers(final String ipAddress)
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

	public String getIp() {
		return MainGeneric.getPublicIpAddress();
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

	
	public ArrayList<String> getMyDns(String myIp) {
		
		ArrayList<String> myDns = new ArrayList<String>();;	
		org.xbill.DNS.ResolverConfig DNSConfig = new org.xbill.DNS.ResolverConfig();
	
		if (DNSConfig.server().startsWith("192.168") || DNSConfig.server().startsWith("10.") || 
				DNSConfig.server().startsWith("172.16"))
		{
			myDns.add(DNSConfig.servers()[0]);			
			
			MainGeneric.getReporter().makeId("dnsips", "ip", DNSConfig.servers()[0]);
			
		}
		
		else
			for (String n : DNSConfig.servers()) {
				myDns.add(n);
				MainGeneric.getReporter().makeId("dnsips", "ip", n);		

			}
		
		return myDns;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
		if (!isActive) {
			self = null;
		}
	}

	
	
	public Record[] doLookup(String[] argv) throws IOException {

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
		} catch (TextParseException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}

		if (res == null) {
			try {
				res = new SimpleResolver();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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

	
	
	public String getASN (String myIp) {

		String queryString = myIp + "&asn=0&cc=0";
		String[] splitIp = myIp.split("\\.");
		String reverseIp = splitIp[3] + "." + splitIp[2] + "." + splitIp[1] + "." + splitIp[0];
		
		try {
			Record[] records = doLookup(new String[] { reverseIp + ".origin.asn.cymru.com", "txt" });
			if (records.length != 0) {
				String response = records[0].rdataToString();
				response = response.substring(response.indexOf('\"')+1, response.lastIndexOf("\"")-1);
				String[] split = response.split("\\|");
				
				queryString = myIp + "&asn=" + split[0].trim() + "&cc=" + split[2].trim();

			}
			
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			queryString = myIp + "&asn=0&cc=0";
		}
		
		return queryString;
	}
	
	
}
