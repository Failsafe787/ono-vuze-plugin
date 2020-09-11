package edu.northwestern.ono.brp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Map.Entry;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.peers.Peer;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.OnoConfiguration;
import edu.northwestern.ono.experiment.TraceRouteReceiver;
import edu.northwestern.ono.experiment.TraceRouteRunner;
import edu.northwestern.ono.experiment.TraceRouteRunner.TraceEntry;
import edu.northwestern.ono.experiment.TraceRouteRunner.TraceResult;
import edu.northwestern.ono.messaging.OnoRatioMessage;
import edu.northwestern.ono.position.GenericPeer;
import edu.northwestern.ono.position.NeighborhoodListener;
import edu.northwestern.ono.stats.Statistics;
import edu.northwestern.ono.util.HashMapCache;


public class BRPPeerManager implements TraceRouteReceiver {
	/* peer source */
	public static final byte SOURCE_UNKNOWN = 0;
	public static final byte SOURCE_ANNOUNCE = 3;
	public static final byte SOURCE_PEER_ADDED = 4;

	/* BRP threshold */
	public static float BP_WEIGHT_THRESHOLD = 0.01F;
	public static final float BP_WEIGHT_THRESHOLD_DEFAULT = 0.01F;
	public static final float COS_SIM_THRESHOLD_DEFAULT = 0.00001F;
	public static float COS_SIM_THRESHOLD = 0.00001F;
	
	/** transfer status */
	public static final String NOT_CONNECTED = "NC";
	public static final String CANT_COMPARE = "CC";
	public static final String NOT_UPLOADING = "NU";
	public static final String NOT_DOWNLOADING = "ND";
	public static final String NOT_APPLICABLE = "NA";
	
	public static final String NOT_INTERESTING = "NID";
	public static final String NOT_INTERESTED = "NIU";
	
	/* Used to weed out traceroutes lacking a certain amount of useful info. */
	private static byte maximumTRUnknowns = 127;
	private static final byte minimumInterestingLeadingHops = 2;

	public static class BRPPeer extends GenericPeer {
		/** downloads with which peer is associated **/
		public HashSet<Download> dls;

		/** BRP data **/
		HashMap<Byte, HashMap<String, Double>> branchPoints;		// via remote BRPTrie.getBranchPoints()
		public Double cossim;
		public long lastUpdate = -1;

		public BRPPeer(String ip, int port) {
			super(ip, port);
			this.peer_type = BRP_PEER;
			this.source = SOURCE_UNKNOWN;
			this.dls = new HashSet<Download>();
			this.branchPoints = new HashMap<Byte, HashMap<String, Double>>();
			this.cossim = 0.0;
		}

		@Override
		public int hashCode() {
			return ip.hashCode();
		}

		public void updatePort(int port) {
			if (port > 0 && this.port <= 0) {
				this.port = port;
			}
		}

		public void registerPeerSource(byte source) {
			this.source = source;
		}

		public void registerPeerDownload(Download dl) {
			this.dls.add(dl);
		}

		public String getIp() {
			return ip;
		}

		public int getPort() {
			if (!MainGeneric.isAzureus()) {
				return MainGeneric.getPeerPort(ip);
			}
			return port;
		}

		public short getProtocol() {
			return protocol;
		}

		public HashMap<String, Double> getBranchPoints(byte version) {
			return this.branchPoints.get(version);
		}

		public void setBranchPoints(byte version, HashMap<String, Double> bps) {
			this.branchPoints.put(version, bps);
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

			sb.append("Branch points: ");
			sb.append(this.branchPoints.toString());
			sb.append("\n");

			return sb.toString();
		}

		/**
		 * Copies the argument's data into this data
		 * 
		 * @param peer
		 *            the data to copy
		 */
		public void copy(BRPPeer peer) {
			super.copy(peer);
			this.branchPoints = peer.branchPoints;
		}

		public String getRelativeDownloadPerformance() {
			String toReturn = null;
			Download toAdd = null;
			boolean applicable = false;
			for (byte b: supportedVersions)
				if (this.getCosSim(b) >= BRPPeerManager.COS_SIM_THRESHOLD)
					applicable = true;
			
			if (!applicable) {
				toReturn = NOT_APPLICABLE;
			} else {
				for (Download o : this.dls) {
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
							if (BRPPeerManager.getInstance().getBRPPeer(
									p.getIp()) == null)
								continue;
							if (p.getStats().getDownloadAverage() > 1000) {
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
					this.dls.add(toAdd);
				}

			if (toReturn == null)
				toReturn = CANT_COMPARE;
			return toReturn;
		}

		public String getRelativeUploadPerformance() {
			String toReturn = null;
			Download toAdd = null;
			boolean applicable = false;
			for (byte b: supportedVersions)
				if (this.getCosSim(b) >= BRPPeerManager.COS_SIM_THRESHOLD)
					applicable = true;
			
			if (!applicable) {
				toReturn = NOT_APPLICABLE;
			} else {
				for (Download o : this.dls) {
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
							if (BRPPeerManager.getInstance().getBRPPeer(
									p.getIp()) == null)
								continue;
							if (p.getStats().getUploadAverage() > 1000) {
								sum += p.getStats().getUploadAverage();
								count++;
							}

						}

						Peer ps[] = ((Download) o).getPeerManager().getPeers(
								getIp());
						if (ps.length > 0) {
							if (ps[0].getStats().getUploadAverage() < 1000) {
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
									.getUploadAverage() / otherAverage)) + "%");
						}
						toReturn = NOT_CONNECTED;
					}
				}
				if (toAdd != null) {
					this.dls.add(toAdd);
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
		
		public String printCosSim(boolean StrongOnly) {
			
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			
			int count = 0;
			for (byte version : supportedVersions){
			
			double cossim = BRPPeerManager.getInstance().cosineSimilarity(this, version);
			
			if (StrongOnly && cossim < BRPPeerManager.COS_SIM_THRESHOLD) {
				continue;
			}
				if (count>0) sb.append(", ");
				sb.append(cossim);
				count++;
			}
			
			sb.append("}");
			return sb.toString();
		}

		public double getCosSim(byte version) {
			return BRPPeerManager.getInstance().cosineSimilarity(this, version);
		}

		public double getMaxCosSim() {
			double max = 0;
			for (byte b : supportedVersions){
				double d = BRPPeerManager.getInstance().cosineSimilarity(this, b);
				if (d > max) max = d;
			}
			return max;
		}
	}
 
	private static BRPPeerManager self;
	
	public static final byte BRP_DIST_WT = 1;
	public static final byte BRP_HIER_WT = 2;
	private static final boolean TESTING = false;
	private static final long COS_SIM_UPDATE_INTERVAL = 10*60*1000L;
	private static final long BP_UPDATE_INTERVAL = 5*60*1000L;
	public static HashSet<Byte> supportedVersions = new HashSet<Byte>();
	private HashMapCache<String, BRPPeer> peers;
	@SuppressWarnings("unused")
	private String myIp;
	private BRPTrie myTrie;
	private HashMap<Byte,HashMap<String, Double>> myBranchPoints;
	private ArrayList<NeighborhoodListener> myListeners;
	private static String BRPDataFileName;
	private static String BRPLogFileName;
	private PrintWriter BRPLogFileWriter;
	private long bpLastUpdate;

	public BRPPeerManager() {
//		this.BRPVersion = (byte)OnoConfiguration.getInstance().BRPVersion;
		this.peers = new HashMapCache<String, BRPPeer>(500);
		this.myIp = MainGeneric.getPublicIpAddress();
		this.myTrie = new BRPTrie();
		this.myBranchPoints = new HashMap<Byte, HashMap<String, Double>>();
		this.myListeners = new ArrayList<NeighborhoodListener>();
		supportedVersions.add(BRP_DIST_WT);
		supportedVersions.add(BRP_HIER_WT);
		if (MainGeneric.isAzureus()) {
			try {
			String[] snpcs = MainGeneric.getPublicIpAddress().split("\\.");
			BRPPeerManager.BRPDataFileName = 
				MainGeneric.getPluginInterface().getPluginDirectoryName() +
				File.separatorChar +
				"brp_" + snpcs[0] + "_" + snpcs[1] + "_" + snpcs[2] + ".dat"; 
			} catch (Exception e){
				e.printStackTrace();
			}
		} else {
			BRPPeerManager.BRPDataFileName = null;
		}
		
		if (BRPPeerManager.BRPDataFileName != null) {
			try {
				File BRPDataFile = new File(BRPPeerManager.BRPDataFileName);
				if (BRPDataFile.exists()) {
					FileInputStream BRPDataFIS = new FileInputStream(
							BRPPeerManager.BRPDataFileName);
					FileChannel BRPDataFC = BRPDataFIS.getChannel();
					// TODO(Ted): Temporarily hard-coded.
					ByteBuffer BRPData = ByteBuffer.allocate(1024 * 1024);
					BRPDataFC.read(BRPData);
					
					BRPData.flip();
					myTrie.deserialize(BRPData);
					
					
				}
			} catch (FileNotFoundException fnfe) {
				this.BRPLog(fnfe.getMessage());
			} catch (IOException ioe) {
				this.BRPLog(ioe.getMessage());
			}
			
			BRPPeerManager.BRPLogFileName = null;
			this.BRPLogFileWriter = null;
		}
		
		
		// TODO(Ted): Add a timer to recalculate BRP weights occasionally?
		//timeoutTimer = MainGeneric.createTimer("PeerManagerTimeout");
		this.BRPLog("Initialized!");
	}

	public static synchronized BRPPeerManager getInstance() {
		if (self == null) {
			self = new BRPPeerManager();
			
			
			TraceRouteRunner.getInstance().addListener(self);
		}
		return self;
	}

	public void stop() {
		try {
			if (BRPPeerManager.BRPDataFileName != null) {
				this.BRPLog("Writing trie to file...");			
				ByteArrayOutputStream trieBaos = new ByteArrayOutputStream();
				FileOutputStream brpDatFile = new FileOutputStream(
						new File(BRPPeerManager.BRPDataFileName));
				this.myTrie.serialize(trieBaos);
				trieBaos.writeTo(brpDatFile);
				this.BRPLog("Done.");
			}
		} catch (IOException ioe) {
			this.BRPLog(ioe.getMessage());
		}
		
		self = null;
	}
	
//	public byte getVersion() {
//		return this.BRPVersion;
//	}

	public void BRPLog(String text) {
		if (OnoConfiguration.getInstance().logBRP) {
			if (this.BRPLogFileWriter == null) {
				try {
					if (MainGeneric.isAzureus()) {
						BRPPeerManager.BRPLogFileName =
							MainGeneric.getPluginInterface().getPluginDirectoryName() +
							File.separatorChar +
							"brplog.txt";
					} else {
						/* MainGeneric.pi, the alternative, is protected, so no go. */ 
						return;
					}
					this.BRPLogFileWriter = new PrintWriter(
							new File(BRPPeerManager.BRPLogFileName));
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (this.BRPLogFileWriter != null) {
				DateFormat timestamp = new SimpleDateFormat("yyyy/M/d HH:mm:ss.SSS");
				this.BRPLogFileWriter.println("[" + timestamp.format(new Date()) + "]: " + text);
				this.BRPLogFileWriter.flush();
			}
		}
		
		if (OnoConfiguration.getInstance().printBRP) {
			//TODO(Ted): Switch to Ono output handle.
			System.out.println("BRP: " + text);
		}
	}

	public HashMap<String, Double> getMyBranchPoints(float B, byte version) {
		if (myBranchPoints.get(version)!=null && bpLastUpdate < BP_UPDATE_INTERVAL){
			return myBranchPoints.get(version);
		}
		myBranchPoints.put(version, myTrie.getBranchPoints(B, version));
		bpLastUpdate = System.currentTimeMillis();
		return myBranchPoints.get(version);
	}

	/*
	 * All of these methods need to be synchronized on this.peers.
	 * PeerFinderGlobal.run() is serial code, but annouceResult is a callback.
	 */
	public void addPeer(String ip, int port) {
		synchronized (this.peers) {
			if (this.peers.get(ip) != null) {
				this.peers.get(ip).updatePort(port);
			} else {
				this.peers.put(ip, new BRPPeer(ip, port));
				TraceRouteRunner.getInstance().addIp(ip);
			}
		}
	}
	
	public void addPeer(BRPPeer p) {
		this.peers.put(p.getIp(), p);
	}

	public void remove(String ip) {
		synchronized (this.peers) {
			this.peers.remove(ip);
		}
	}

	public void registerPeerSource(String peerIp, byte source) {
		synchronized(this.peers) {
			if (this.peers.get(peerIp) != null) {
				this.peers.get(peerIp).registerPeerSource(source);
			} else {
				throw new RuntimeException("Need to register peer ip before "
						+ "registering source!");
			}
		}
	}

	public void setPeerProtocol(String peerIp, short protocol) {
		synchronized(this.peers) {
			BRPPeer found = this.peers.get(peerIp);
			if (found != null) {
				found.protocol = protocol;
			} else {
				throw new RuntimeException("Need to register peer ip before setting protocol!");
			}
		}
	}
	
	public void setPeerCosSim(String peerIp, double cossim) {
		synchronized(this.peers) {
			BRPPeer found = this.peers.get(peerIp);
			if (found != null) {
				found.cossim = cossim;
				found.lastUpdate = System.currentTimeMillis();
			} else {
				throw new RuntimeException("Need to register peer ip before setting cossim!");
			}
		}
	}

	public void registerPeerDownload(String peerIp, Download dl) {
		synchronized(this.peers) {
			BRPPeer found = this.peers.get(peerIp);
			if (found != null) {
				found.dls.add(dl);
			} else {
				throw new RuntimeException(
				"Need to register peer ip before associating with a download!");
			}
		}
	}

	public BRPPeer getBRPPeer(String peerIp) {
		synchronized (this.peers) {
			BRPPeer found = peers.get(peerIp);
			if (found == null) {
				return null;
			}
			return found;
		}
	}

	public double cosineSimilarity(BRPPeer p, byte version) {
		double num = 0.0;
		double mag1 = 0.0;
		double mag2 = 0.0;
		double toReturn = 0.0;

		HashMap<String, Double> myBP = this.getMyBranchPoints(0.0F, version);
		if (p.branchPoints.get(version)==null) return 0;
		for (Entry<String, Double> e : p.branchPoints.get(version).entrySet()) {
			if (myBP.containsKey(e.getKey())) {
				num += e.getValue() * myBP.get(e.getKey());
			}
			mag1 += e.getValue() * e.getValue();
		}

		for(Entry<String, Double> e : myBP.entrySet()) {
			mag2 += e.getValue() * e.getValue();
		}
		if (mag1 == 0){
			return 0;
		}
		else if (mag2 == 0){
			return 0;
		}

		double denom = Math.sqrt(mag1 * mag2);
		toReturn = num/denom;

		return toReturn;
	}

	/**
	 * 
	 * @param dl
	 * @return
	 */
	public Set<BRPPeer> getPreferredPeers(Download dl) {
		
		
		synchronized (this.peers) {
			Set<BRPPeer> toReturn = new HashSet<BRPPeer>();
			if (this.peers.size() < 1) {
				return toReturn;
			}
			/*
			 * PriorityQueues in Java are minheaps, so we use an anonymous Comparator that gives
			 * BRPPeers a natural order of decreasing cossim.
			 */
			PriorityQueue<BRPPeer> peers = new PriorityQueue<BRPPeer>(this.peers.size(),
					new Comparator<BRPPeer>() {
				public int compare(BRPPeer p1, BRPPeer p2) {
					if (p1.cossim == p2.cossim) {
						return 0;
					}
					if (p1.cossim < p2.cossim) {
						return 1;
					}
					else {
						return -1;
					}
				}
			});		

			int numRemaining = 1;
			for (BRPPeer p : this.peers.values()) {
				if (p.dls.contains(dl)) {
					if (System.currentTimeMillis()-p.lastUpdate>COS_SIM_UPDATE_INTERVAL){ 
						if (TESTING && Math.random()<0.01 && numRemaining>0){
							numRemaining--;
							for (byte b : supportedVersions)
								this.setPeerCosSim(p.ip, Math.random()+COS_SIM_THRESHOLD);
						} else {
						for (byte b : supportedVersions)
							this.setPeerCosSim(p.ip, cosineSimilarity(p, b));
						}
					}
					if(p.cossim > 0.0) {
						peers.add(p);
					}
				}
			}

			while (peers.peek() != null && peers.peek().cossim > COS_SIM_THRESHOLD) {
				TraceRouteRunner.getInstance().addIp(peers.peek().getIp(), true, true);
				toReturn.add(peers.poll());
			}

			return toReturn;
		}
	}

	/**
	 * This function is identical to getPreferredPeers(), except for the check against a particular
	 * {@link org.gudy.azureus2.plugins.download.Download Download} object.
	 */
	public Set<BRPPeer> getAllPreferredPeers() {
		synchronized (this.peers) {
			Set<BRPPeer> toReturn = new HashSet<BRPPeer>();
			if (this.peers.size() < 1) {
				return toReturn;
			}
			/*
			 * PriorityQueues in Java are minheaps, so we use an anonymous Comparator that gives
			 * BRPPeers a natural order of decreasing cossim.
			 */
			PriorityQueue<BRPPeer> peers = new PriorityQueue<BRPPeer>(this.peers.size(),
					new Comparator<BRPPeer>() {
				public int compare(BRPPeer p1, BRPPeer p2) {
					if (p1.cossim == p2.cossim) {
						return 0;
					}
					if (p1.cossim < p2.cossim) {
						return 1;
					}
					else {
						return -1;
					}
				}
			});		

			// TODO cache cossim
			int numRemaining = 1;
			for (BRPPeer p : this.peers.values()) {
				if (System.currentTimeMillis()-p.lastUpdate>COS_SIM_UPDATE_INTERVAL){ 
					if (TESTING && Math.random()<0.01 && numRemaining>0){
						numRemaining--;
						for (byte b : supportedVersions)
							this.setPeerCosSim(p.ip, Math.random()+COS_SIM_THRESHOLD);
					} else {
					for (byte b : supportedVersions)
						this.setPeerCosSim(p.ip, cosineSimilarity(p, b));
					}
				}
				if(p.cossim > 0.0) {
					peers.add(p);
				}
			}

			while (peers.peek() != null && peers.peek().cossim > COS_SIM_THRESHOLD) {
				toReturn.add(peers.poll());
			}
			
			for (NeighborhoodListener nl : this.myListeners) {
				for (BRPPeer p : toReturn) {
					nl.foundNewNeighborhoodPeer(p);
				}
			}

			return toReturn;
		}
	}

	
	/**
	 * 
	 * @param te
	 * @return int - -1 means no interesting values in the te; nonzero means the index of the first
	 * interesting one.
	 */
	private int isHopInteresting(TraceEntry te) {
		for (int i = 0; i < 3; ++i) {
			String hop = te.router[i];
			if (hop == null) continue;
			if (hop == "null") continue;
			// 'unknown' is treated as though there were a wire there instead of a router.
			// Since we have no info about that machine, it might as well be.
			if (hop.equals("unknown")) continue;
			if (!hop.matches("(:?\\d{1,3}\\.){3}(:?\\d{1,3})")) continue;
			// see RFC 1597 - Allocation for Private Internets
			if (hop.startsWith("10.")) continue;
			if (hop.startsWith("192.168.")) continue;
			if (hop.startsWith("172.") &&
					Integer.parseInt(hop.split("\\.")[1]) >= 16 &&
					Integer.parseInt(hop.split("\\.")[1]) <= 31) continue;
			return i;
		}
		return -1;
	}

	public synchronized void receiveTraceResult(String ip, TraceResult tr) {
		if (tr != null && tr.entries != null && tr.entries.size() > 1) {
			byte numLeadingUnknowns = 0;
			byte numLeadingInterestingHops = 0;
			boolean hitFirstInterestingHop = false;
			boolean hitSecondUninterestingHop = false;
			
			ArrayList<String> hops = new ArrayList<String>();
			HashSet<String> distinctHops = new HashSet<String>();
			 
			for (TraceEntry te : tr.entries) {
				int interesting_index = this.isHopInteresting(te);
				
				if (interesting_index >= 0) {
					// This hop is interesting.
					hitFirstInterestingHop = true;
					if (hitSecondUninterestingHop == false) {
						numLeadingInterestingHops++;
					}
					
					if (distinctHops.contains(te.router[interesting_index])) {
						distinctHops.clear();
						hops = (ArrayList<String>) hops.subList(0,
								hops.indexOf(te.router[interesting_index]));
						distinctHops.addAll(hops);
					}
					
					distinctHops.add(te.router[interesting_index]);
					hops.add(te.router[interesting_index]);
				} else {
					// This hop is not interesting.
					if (hitFirstInterestingHop == false) {
						numLeadingUnknowns++;
					} else {
						hitSecondUninterestingHop = true;
						hops.add("something");
					}
				}
				
				if (hops.size() > 15) {
					break;
				}
			}

			if (numLeadingUnknowns < BRPPeerManager.maximumTRUnknowns) {
				BRPPeerManager.maximumTRUnknowns = numLeadingUnknowns;
			}
			
			if (hops.size() > 0 &&
					numLeadingUnknowns <= BRPPeerManager.maximumTRUnknowns &&
					numLeadingInterestingHops >= BRPPeerManager.minimumInterestingLeadingHops) {
				this.myTrie.addRoute(hops);	
			}
			
			this.BRPLog("\n" + this.myTrie.getBranchTree());
			this.BRPLog("Size: " + Integer.toString(this.myTrie.getTrieSize()));
			this.BRPLog("Depth: " + Integer.toString(this.myTrie.getTrieDepth()));
			
			for (byte b : supportedVersions){
				this.BRPLog(this.getMyBranchPoints(BP_WEIGHT_THRESHOLD, b).toString());
			
				for (String bp : getMyBranchPoints(BP_WEIGHT_THRESHOLD, b).keySet()){
					Statistics.getInstance().addBranchPointForLookup(bp);
				}
			}
		
		}
	}

	public void processMessage(OnoRatioMessage orm, String peerIp) {
		synchronized(this.peers) {
//			this.BRPLog("Received message from ip " + peerIp + " with branch points "
//					+ orm.getBranchPoints());
			
			if (this.peers.get(peerIp) == null) {
				this.addPeer(peerIp, 0);
			}
			
			for (byte b : supportedVersions){
			if (orm.getBranchPoints(b)!=null && orm.getBranchPoints(b).size() > 0) {
				this.peers.get(peerIp).setBranchPoints(b, orm.getBranchPoints(b));
				
				this.BRPLog("BRPPeerManager: Peer has cossim " +
						this.cosineSimilarity(this.peers.get(peerIp), b));
				
			}
			}
		}
	}

	public void addListener(NeighborhoodListener newListener) {
		myListeners.add(newListener);
	}
	
	public void removeListener(NeighborhoodListener newListener) {
		myListeners.remove(newListener);
	}

	public double getMaxCosineSimilarity(BRPPeer peer) {
		double max = 0;
		for (byte b : supportedVersions){
			double temp = cosineSimilarity(peer, b);
			if (temp>max) max = temp;
		}
		return max;
	}
}
