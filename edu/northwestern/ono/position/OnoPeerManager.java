/**
 * Ono Project
 *
 * File:         OnoPeerManager.java
 * RCS:          $Id: OnoPeerManager.java,v 1.45 2010/03/29 16:48:03 drc915 Exp $
 * Description:  OnoPeerManager class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Apr 20, 2007 at 11:23:26 AM
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.peers.Peer;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.OnoConfiguration;
import edu.northwestern.ono.dns.Digger;
import edu.northwestern.ono.dns.EdgeServerMapManager;
import edu.northwestern.ono.stats.EdgeServerRatio;
import edu.northwestern.ono.timer.ITimer;
import edu.northwestern.ono.timer.ITimerEvent;
import edu.northwestern.ono.timer.ITimerEventPerformer;
import edu.northwestern.ono.util.HashMapCache;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 * 
 * The OnoPeerManager class ...
 */
public class OnoPeerManager {

	/** closeness states */
	public static byte CS_UNKNOWN = 0;
	public static byte CS_CLOSE = 1;
	public static byte CS_NOT_CLOSE = -1;

	/** the duration that an edge-server ratio is valid */
	private static final long RATIO_VALIDITY_INTERVAL = 12 * 60 * 60 * 1000;

	/** peer source */
	public static final byte SOURCE_DHT = 1;
	public static final byte SOURCE_VIVALDI = 2;
	public static final byte SOURCE_ANNOUNCE = 3;
	public static final byte SOURCE_PEER_ADDED = 4;

	/** transfer status */
	public static final String NOT_CONNECTED = "NC";
	public static final String CANT_COMPARE = "CC";
	public static final String NOT_UPLOADING = "NU";
	public static final String NOT_DOWNLOADING = "ND";
	public static final String NOT_APPLICABLE = "NA";

	public static final int MAX_PEERS_TO_DIG = 100;
	private static final boolean VERBOSE = false;
	public static final String NOT_INTERESTING = "NID";
	public static final String NOT_INTERESTED = "NIU";

	public static class OnoPeer extends GenericPeer implements ITimerEventPerformer {
		ArrayList<String> dns;
		HashMapCache <String, HashMap<String, EdgeServerRatio>> perDnsServerRatios;
		HashMap<Object, Boolean> keys;
		boolean getRatios;
		Map<String, EdgeServerRatio> edgeServerRatios;

		/** whether a node is close or not */
		byte closenessState;

		/** what component found this peer */
		boolean isInDHT = false;
		private boolean findCluster;
		public boolean dhtLookupAttempted;
		private boolean digFailed = false;
		private HashMap<String, Double> cosSimMappings;
		private boolean isCIDRMatch;
		private String ldns;

		public OnoPeer(String ip, int port, Object key, boolean getRatios) {
			super(ip, port);
			this.peer_type = ONO_PEER;
			keys = new HashMap<Object, Boolean>();
			keys.put(key, false);
			this.getRatios = getRatios;
			closenessState = 0;
			edgeServerRatios = new HashMap<String, EdgeServerRatio>();
			perDnsServerRatios = new HashMapCache<String, HashMap<String, EdgeServerRatio>>();
			cosSimMappings = new HashMap<String, Double>();

		}

		@Override
		public int hashCode() {
			return ip.hashCode();
		}

		public void addKey(Object key) {
			synchronized (keys) {
				if (!keys.containsKey(key))
					keys.put(key, false);
			}
		}

		public void updatePort(int port) {
			if (port > 0 && this.port <= 0)
				this.port = port;

		}
		
		public void updateDnsList(ArrayList<String> dnsList) {
			if (dnsList != null)
				this.dns = dnsList;
		}

		public void updateGetRatios(boolean getRatios) {

			this.getRatios = getRatios;

		}

		public void addRatio(String cust, EdgeServerRatio esr) {
			if (edgeServerRatios.get(cust) != null
					&& edgeServerRatios.get(cust).getLastUpdate() > esr
							.getLastUpdate()) {
				return;
			}
			synchronized (edgeServerRatios) {edgeServerRatios.put(cust, esr);}

		}

		public void findCluster() {
			synchronized (keys) {
				for (Object o : keys.keySet()) {
					if (!keys.get(o)) {
						CDNClusterFinder.getInstance().addPeer(o, ip, port);
						keys.put(o, true);
					}
				}
			}
		}

		public Map<String, EdgeServerRatio> getRatios() {
			if (edgeServerRatios == null) {
				edgeServerRatios = new HashMap<String, EdgeServerRatio>();
			}
			return edgeServerRatios;
		}

		public Map<String, EdgeServerRatio> getPerDnsRatios(String peerDns) {
			if (dns.indexOf(peerDns) != -1) { 
		
				if (perDnsServerRatios.get(peerDns) == null) {
					perDnsServerRatios.put(peerDns, new HashMap<String, EdgeServerRatio>());
				}
				return perDnsServerRatios.get(peerDns);
			}
			return null;
		}

		public HashMapCache<String, HashMap<String, EdgeServerRatio>> getAllPerDnsRatios() {
			return perDnsServerRatios;
		}
		
		
		public void registerPeerSource(byte source) {
			this.source = source;

		}

		public void digFailed() {
			digFailed = true;

		}

		public boolean shouldDig() {
			return getRatios && closenessState >= 0 && !digFailed;
		}

		public String getIp() {
			return ip;
		}

		public int getPort() {
			if (!MainGeneric.isAzureus()) {
				return MainGeneric.getPeerPort(ip);
			}

			if (port <= 1) {
				if (keys.size() > 0) {
					for (Object key : keys.keySet()) {
						if (key instanceof Download) {
							Download dl = (Download) key;
							if (dl.getPeerManager() == null)
								continue;
							Peer peers[] = dl.getPeerManager().getPeers(ip);
							for (Peer p : peers) {
								if (p.getTCPListenPort() > 0) {
									port = p.getTCPListenPort();
									return port;
								}
							}
						}
					}
				}
			}
			return port;
		}

		public short getProtocol() {
			return protocol;
		}

		public void setCosSim(String customer, double cosSim) {
			cosSimMappings.put(customer, cosSim);
			if (edgeServerRatios.get(customer) == null
					|| !edgeServerRatios.get(customer).valid())
				return;
			byte tempCloseState = CS_NOT_CLOSE;
			for (Entry<String, Double> ent : cosSimMappings.entrySet()) {
				if (ent.getValue() > CDNClusterFinder.COSINE_THRESHOLD) {
					tempCloseState = CS_CLOSE;
					break;
				}
			}

			closenessState = tempCloseState;

			if (closenessState == CS_NOT_CLOSE) {
				getRatios = false;

				// TODO set timer to change this property far in the future
				// so as to recheck
			}

		}

		public boolean isSameCluster() {
			return closenessState == CS_CLOSE;
		}

		@Override
		public String printCosSim(boolean strongOnly) {
			if (cosSimMappings.size() == 0)
				return "{}";
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			for (Entry<String, Double> ent : cosSimMappings.entrySet()) {
				if (strongOnly
						&& ent.getValue() < CDNClusterFinder.COSINE_THRESHOLD) {
					continue;
				}
				sb.append(ent.getValue());
				sb.append(",");
				closenessState = CS_CLOSE;
			}
			sb.replace(sb.length() - 1, sb.length(), "}");
			return sb.toString();
		}

		public Entry<String, Double> getMaxCosSim() {
			Entry<String, Double> toReturn = null;
			double maxValue = 0;
			for (Entry<String, Double> ent : cosSimMappings.entrySet()) {
				if (ent.getValue() > CDNClusterFinder.COSINE_THRESHOLD
						&& ent.getValue() > maxValue) {
					maxValue = ent.getValue();
					toReturn = ent;
				}

			}
			return toReturn;
		}

		public String printCustomers(boolean strongOnly) {
			if (cosSimMappings.size() == 0)
				return "{}";
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			for (Entry<String, Double> ent : cosSimMappings.entrySet()) {
				if (strongOnly
						&& ent.getValue() < CDNClusterFinder.COSINE_THRESHOLD) {
					continue;
				}
				sb.append(ent.getKey());
				sb.append(",");
			}
			if (sb.length() == 1)
				return "{}";
			sb.replace(sb.length() - 1, sb.length(), "}");
			return sb.toString();
		}

		public String getKeyNames() {
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			for (Object o : keys.keySet()) {
				if (o instanceof Download) {
					sb.append(((Download) o).getName());
				} else {
					sb.append("DHT");
				}
				sb.append(",");
			}
			sb.replace(sb.length() - 1, sb.length(), "}");
			return sb.toString();
		}

		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("Ip: ");
			sb.append(ip);
			sb.append("\n");

			sb.append("Port: ");
			sb.append(port);
			sb.append("\n");

			sb.append("Dig Failed: ");
			sb.append(digFailed);
			sb.append("\n");

			sb.append("Ratios: ");
			sb.append(edgeServerRatios.toString());
			sb.append("\n");

			sb.append("Cos Sim: ");
			sb.append(printCosSim(false));
			sb.append("\n");
			sb.append(printCustomers(false));
			sb.append("\n");

			return sb.toString();
		}

		/**
		 * Called as a periodic timeout
		 * 
		 * @param event
		 */
		public void perform(ITimerEvent event) {
			if (MainGeneric.isShuttingDown()) {
				event.cancel();
			} else if (cosSimMappings.size() == 0 || digFailed) {
				OnoPeerManager.getInstance().remove(ip);
				event.cancel();
			}
		}

		public Set<String> getGoodServerIps() {
			Set<String> toReturn = new HashSet<String>();
			if (edgeServerRatios == null)
				return toReturn;
			for (Entry<String, EdgeServerRatio> ent : edgeServerRatios
					.entrySet()) {
				for (Entry<String, Double> ratio : ent.getValue().getEntries()) {
					if (ratio.getValue() >= OnoConfiguration.getInstance()
							.getCosSimThreshold())
						toReturn.add(ent.getKey());
				}
			}
			return toReturn;
		}

		/**
		 * Copies the argument's data into this data
		 * 
		 * @param peer
		 *            the data to copy
		 */
		public void copy(OnoPeer peer) {
			super.copy(peer);
			this.closenessState = peer.closenessState;
			this.cosSimMappings = peer.cosSimMappings;
			this.edgeServerRatios = peer.edgeServerRatios;
			this.keys = peer.keys;
		}

		public String getRelativeDownloadPerformance() {
			String toReturn = null;
			if (closenessState < CS_CLOSE && !isCIDRMatch()) {
				toReturn = NOT_APPLICABLE;
			} else {
				if (keys.size() == 0) {
					toReturn = NOT_APPLICABLE;
				}
				Download toAdd = null;
				for (Object o : keys.keySet()) {
					if (o instanceof Download) {
						float sum = 0f;
						int count = 0;
						if (((Download) o).getPeerManager() == null) {
							toAdd = MainGeneric.getPluginInterface()
									.getDownloadManager().getDownload(
											((Download) o).getTorrent());
							break;
						}
						for (Peer p : ((Download) o).getPeerManager()
								.getPeers()) {
							if (OnoPeerManager.getInstance().getOnoPeer(
									p.getIp()) == null)
								continue;
							if (!OnoPeerManager.getInstance().getOnoPeer(
									p.getIp()).isCIDRMatch() && OnoPeerManager.getInstance().getOnoPeer(
									p.getIp()).closenessState < 1
									&& p.getStats().getDownloadAverage() > 1000) {
								sum += p.getStats().getDownloadAverage();
								count++;
							}

						}

						Peer ps[] = ((Download) o).getPeerManager().getPeers(
								getIp());
						if (ps.length > 0) {
							if (ps[0].getStats().getDownloadAverage() < 1000) {
								if (!ps[0].isInteresting()) {
									toReturn = NOT_INTERESTING;
								} else
									toReturn = NOT_DOWNLOADING;
								break;
							}
							if (count == 0 || sum == 0)
								continue;
							float otherAverage = sum / count;

							return ((100 * (ps[0].getStats()
									.getDownloadAverage() / otherAverage)) + "%");
						}
						toReturn = NOT_CONNECTED;
					}
				}
				if (toAdd != null) {
					Boolean b = keys.remove(toAdd);
					keys.put(toAdd, b);
				}
			}

			if (toReturn == null)
				toReturn = CANT_COMPARE;
			return toReturn;
		}

		public String getRelativeUploadPerformance() {
			String toReturn = null;
			if (closenessState < CS_CLOSE && !isCIDRMatch()) {
				toReturn = NOT_APPLICABLE;
			} else {
				if (keys.size() == 0) {
					toReturn = NOT_APPLICABLE;
				}
				Download toAdd = null;
				for (Object o : keys.keySet()) {
					if (o instanceof Download) {
						float sum = 0f;
						int count = 0;
						if (((Download) o).getPeerManager() == null) {
							toAdd = MainGeneric.getPluginInterface()
									.getDownloadManager().getDownload(
											((Download) o).getTorrent());
							break;
						}
						for (Peer p : ((Download) o).getPeerManager()
								.getPeers()) {
							if (OnoPeerManager.getInstance().getOnoPeer(
									p.getIp()) == null)
								continue;
							if (!OnoPeerManager.getInstance().getOnoPeer(
									p.getIp()).isCIDRMatch() && 
									OnoPeerManager.getInstance().getOnoPeer(
									p.getIp()).closenessState < 1
									&& p.getStats().getUploadAverage() > 1000) {
								sum += p.getStats().getUploadAverage();
								count++;
							}

						}

						Peer ps[] = ((Download) o).getPeerManager().getPeers(
								getIp());
						if (ps.length > 0) {
							if (ps[0].getStats().getUploadAverage() < 1000) {
								if (!ps[0].isInteresting()) {
									toReturn = NOT_INTERESTED;
								} else
									toReturn = NOT_UPLOADING;
							}
							if (count == 0 || sum == 0)
								continue;
							float otherAverage = sum / count;
							if (ps[0].getStats().getUploadAverage() == 0)
								continue;
							return ((100 * (ps[0].getStats().getUploadAverage() / otherAverage)) + "%");
						}
						toReturn = NOT_CONNECTED;
					}
				}
				if (toAdd != null) {
					Boolean b = keys.remove(toAdd);
					keys.put(toAdd, b);
				}
			}

			if (toReturn == null)
				toReturn = CANT_COMPARE;
			return toReturn;
		}

		public String getToolTipText(String text) {
			if (text.equals(CANT_COMPARE)) {
				return "No other peers to compare with";
			} else if (text.equals(NOT_CONNECTED)) {
				return "Not currently connected to this peer";
			} else if (text.equals(NOT_UPLOADING)) {
				return "Not uploading to this peer";
			} else if (text.equals(NOT_INTERESTING)) {
				return "Peer doesn't have data that you want";
			} else if (text.equals(NOT_INTERESTED)) {
				return "You don't have data that peer wants";
			} else if (text.equals(NOT_DOWNLOADING)) {
				return "Not downloading from this peer";
			} else if (text.equals(NOT_APPLICABLE)) {
				return "No download to observe";
			}

			return text + " better than the average for \"not close\" peers";
		}

		public boolean isCIDRMatch() {
			return isCIDRMatch;
		}
		
		public void setCIDRMatch(boolean b){
			isCIDRMatch = b;
		}

		public void setLdns(String ldns) {
			this.ldns = ldns;
			
		}

		public String getLdns() {
			return ldns;
		}

	}

	private static OnoPeerManager self;

	private HashMap<String, OnoPeer> peers;
	private EdgeServerMapManager esmm;
	private String myIp;
	private ITimer timeoutTimer;

	public OnoPeerManager() {
		peers = new HashMap<String, OnoPeer>();
		if (MainGeneric.getPublicIpAddress() != null) {
			peers.put(MainGeneric.getPublicIpAddress(), new OnoPeer(MainGeneric
					.getPublicIpAddress(), 0, null, true));
			peers.get(MainGeneric.getPublicIpAddress()).edgeServerRatios = MainGeneric
					.getEdgeServerRatios();
		}
		timeoutTimer = MainGeneric.createTimer("OnoPeerManagerTimeout");
	}

	public void remove(String ip) {
		synchronized (peers) {
			peers.remove(ip);
		}

	}

	public static synchronized OnoPeerManager getInstance() {
		if (self == null) {
			self = new OnoPeerManager();
		}
		return self;
	}

	public void addPeer(String ip, int port, Object key, boolean getRatios) {

		synchronized (peers) {
			if (peers.get(ip) != null) {
				peers.get(ip).addKey(key);
				peers.get(ip).updatePort(port);
				peers.get(ip).updateGetRatios(getRatios);
				// TODO decide to dig based on a closeness and previous state
			} else {
				peers.put(ip, new OnoPeer(ip, port, key, getRatios));
				if (esmm == null) {
					esmm = Digger.getInstance().getEsmm();
				}
				if (getRatios && esmm != null) {
					esmm.lookup(ip);
				}
				if (!MainGeneric.isShuttingDown())
					timeoutTimer.addPeriodicEvent(60 * 60 * 1000, peers.get(ip));
			}
		}

	}

	public void updatePeerCloseness(String ip, byte closenessState) {
		if (peers.get(ip) != null) {
			peers.get(ip).closenessState = closenessState;
		} else {
			System.err.println("Unknown peer!");
		}
	}

	public void addRatio(String peerIp, String cust, EdgeServerRatio esr) {

		if (peers.get(peerIp) != null) {
			peers.get(peerIp).addRatio(cust, esr);
		} else {
			throw new RuntimeException(
					"Need to register peer ip before adding ratio!");
		}

	}

	public void findCluster(String peerIp) {
		if (peers.get(peerIp) != null) {
			peers.get(peerIp).findCluster();
		} else {
			throw new RuntimeException(
					"Need to register peer ip before finding cluster!");
		}

	}

	/**
	 * Returns of a map of domain name to edge server ratios for the specified
	 * peer ip.
	 * 
	 * @param peerIp
	 *            the peer ip.
	 * @return
	 */
	public Map<String, EdgeServerRatio> getRatios(String peerIp,
			boolean tryDhtOnFail) {

		if (peers.containsKey(peerIp)) {

			// TODO cover NPE
			for (Entry<String, EdgeServerRatio> e : peers.get(peerIp)
					.getRatios().entrySet()) {
				if (Util.currentGMTTime() - e.getValue().getLastUpdate() < RATIO_VALIDITY_INTERVAL) {
					return peers.get(peerIp).getRatios();
				}
			}

		}

		// do lookup
		if (esmm != null && tryDhtOnFail) {
			if (peers.get(peerIp) != null) {
				esmm.lookup(peerIp);
				if (peers.get(peerIp) != null) {
					peers.get(peerIp).dhtLookupAttempted = true;
				}
			}
		}

		return null;
	}

	public void setEdgeServerMapMananger(EdgeServerMapManager esmm) {
		this.esmm = esmm;

	}

	public void registerPeerSource(String peerIp, byte source) {
		if (peers.get(peerIp) != null) {
			peers.get(peerIp).registerPeerSource(source);
		} else {
			throw new RuntimeException("Need to register peer ip before "
					+ "registering source!");
		}

	}

	public HashMapCache<String, Map<String, EdgeServerRatio>> getAllRatios() {
		HashMapCache<String, Map<String, EdgeServerRatio>> toReturn = new HashMapCache<String, Map<String, EdgeServerRatio>>();
		synchronized (peers) {
			for (Entry<String, OnoPeer> ent : peers.entrySet()) {
				if (ent.getValue().getRatios().size() > 0) {
					toReturn.put(ent.getKey(), ent.getValue().getRatios());
				}
			}
		}
		return toReturn;
	}

	public void digFailed(String peerIp) {
		if (peers.get(peerIp) != null) {
			peers.get(peerIp).digFailed();
		} else {
			if (VERBOSE)
				System.err
						.println("Need to register peer ip before notifying of dig failure!");
		}

	}

	public HashSet<String> getIpsToDig() {
		HashSet<String> toReturn = new HashSet<String>();
		synchronized (peers) {
			for (Entry<String, OnoPeer> ent : peers.entrySet()) {
				if (ent.getValue().shouldDig()) {
					toReturn.add(ent.getKey());
				}
				if (toReturn.size() >= MAX_PEERS_TO_DIG)
					break;
			}
		}
		return toReturn;
	}

	public void foundInDht(String peerIp) {
		if (peers.get(peerIp) != null) {
			peers.get(peerIp).isInDHT = true;
		} else {
			throw new RuntimeException(
					"Need to register peer ip before notifying of entry in DHT!");
		}

	}

	public OnoPeer getOnoPeer(String peerIp) {
		if (peers.get(peerIp) == null) {
			// System.err.println("Warning: Accessing peer ip not in manager!");
			return null;
		}
		return peers.get(peerIp);
	}

	public void setPeerProtocol(String peerIp, short protocol) {
		if (peers.get(peerIp) != null) {
			peers.get(peerIp).protocol = protocol;
		} else {
			throw new RuntimeException(
					"Need to register peer ip before setting protocol!");
		}

	}

	public Map<String, EdgeServerRatio> getMyRatios() {
		if (myIp == null) {
			if (MainGeneric.getPublicIpAddress() == null)
				return null;
			myIp = MainGeneric.getPublicIpAddress();
		}
		synchronized (peers) {
			if (peers.get(MainGeneric.getPublicIpAddress()) == null)
				return null;
			else
				return peers.get(MainGeneric.getPublicIpAddress()).getRatios();
		}
	}

	public Set<String> getGoodCDNNames() {
		Map<String, EdgeServerRatio> myRatios = getMyRatios();
		if (myRatios == null)
			return new HashSet<String>();
		HashSet<String> namesToReturn = new HashSet<String>();
		HashMap<String, Double> weightedNames = new HashMap<String, Double>();
		double weightedPingSum = 0;
		double minPing = Double.MAX_VALUE;
		int total = myRatios.size();
		for (Entry<String, EdgeServerRatio> ent : myRatios.entrySet()) {
			double weightedPing = 0;
			double totalRatio = 0;
			for (Entry<String, Double> ratio : ent.getValue().getEntries()) {
				double pingDist = classCSubnetContainsKey(Digger.getInstance()
						.getEdgePings(), ratio.getKey());
				if (pingDist >= 0) {
					weightedPing += ratio.getValue() * pingDist;
					totalRatio += ratio.getValue();
					if (pingDist < minPing) {
						minPing = pingDist;
					}
				}
			}
			if (totalRatio > 0) {
				weightedNames.put(ent.getKey(), weightedPing / totalRatio);
				weightedPingSum += weightedPing / totalRatio;
			}
		}

		if (weightedNames.size() == 0)
			return namesToReturn;
		double average = weightedPingSum / weightedNames.size();

		if (average < 1)
			average = 1;
		if (minPing == 0)
			minPing = 1;

		for (Entry<String, Double> ent : weightedNames.entrySet()) {
			if (ent.getValue() < 5
					|| (ent.getValue() <= 1.5 * average && ent.getValue() < 4 * minPing)) {
				namesToReturn.add(ent.getKey());
			}
		}

		return namesToReturn;
	}

	private double classCSubnetContainsKey(Map<String, Double> edgePings,
			String subnet) {
		HashSet<String> keys = new HashSet<String>();
		keys.addAll(edgePings.keySet());
		for (String key : keys) {
			if (key.equals(subnet) || Util.getClassCSubnet(key).equals(subnet)) {
				return edgePings.get(key);
			}
		}
		return -1;
	}

	public void addPeer(OnoPeer op) {
		peers.put(op.getIp(), op);

	}

	public void stop() {
		if (timeoutTimer != null) {
			timeoutTimer.addEvent(System.currentTimeMillis() + 100,
					new ITimerEventPerformer() {

						public void perform(ITimerEvent event) {
							// TODO Auto-generated method stub

						}
					});
			timeoutTimer.destroy();
		}
		self = null;

	}

}
