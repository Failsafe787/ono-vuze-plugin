package edu.northwestern.ono.database;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import edu.northwestern.ono.experiment.TraceRouteRunner.TraceEntry;

public interface DatabaseReporter {

	public abstract void setExtendedConnection(boolean extend);

	/**
	 * Establishes a connection to the databse.
	 * 
	 * @return false if the connection was not made; true otherwise
	 */
	public abstract boolean connect();

	/**
	 * @param config
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	public abstract int doInsert(String insert, String select)
			throws IOException, ClassNotFoundException, SQLException;

	public abstract void doCreateTable(String s) throws SQLException,
			IOException, ClassNotFoundException;

	public abstract boolean canConnect();

	/**
	 * @return
	 */
	public abstract int getNextPosition();

	/**
	 * @param customerId
	 * @param edgeId
	 * @param peerId
	 * @param i
	 * @param time
	 * @param timeZoneId
	 */
	public abstract boolean insertEvent(int customerId, int edgeId, int peerId,
			int eventType, long time, int timeZoneId);

	/**
	 * @param edgeServer
	 * @return
	 * @throws SQLException
	 */
	public abstract int makeId(String tableName, String colName, String value);

	/**
	 * @param customerId
	 * @param peerId
	 * @param edge1Id
	 * @param edge2Id
	 * @param time
	 */
	public abstract boolean insertDigResult(int customerId, int peerId,
			int edge1Id, int edge2Id, long time);

	/**
	 * 
	 */
	public abstract void disconnect();

	/**
	 * @param edgeId
	 * @param myId
	 * @param destId
	 * @param middleId
	 * @param rtt
	 * @param time
	 */
	public abstract boolean insertPingResult(int edgeId, int myId, int destId,
			int middleId, float rtt, long time, int experimentId);

	public abstract boolean insertPingResultOess(int destId, int sourceId,float rtt,  
			long time, int destType);
	
	public abstract boolean insertDataTransferResult(long time,
			int experimentId, byte type, int size, int sourceId, int destId,
			int currentId, double raceResultPrimary, double raceResultSecondary);

	public abstract boolean insertVivaldiResult(long timestamp, int peerId,
			int observedBy, float x, float y, float h, float estErr,
			float estRTT, float pingResult, float estRTTv2, float[] coordsV2,
			byte type);

	public boolean insertDnsMapping(int myIp, int publicDns, int privateDns, String type, int order);
	
		
	/**
	 * returns the id for the entry description
	 * 
	 * @param myId
	 * @param customerId
	 * @return
	 */
	
	public abstract long insertRatioEntry(int myId, int customerId, int entries);

	public abstract void insertRatio(long entryId, int edgeClusterId,
			double value);
	
	public abstract long insertBranchPointEntry(int myId, byte version, int numEntries);
	
	public abstract void insertBranchPoint(long entryId, int branchPointId, double cossim);

	public abstract long insertRatioOessEntry(int myId, int dnsId, int customerId, int entries);
	
	public abstract void insertRatioOess(long entryId, int dnsId, int edgeClusterId,
			double value);
	
	public abstract void insertTimeoutOess(int myDnsId, int myId, long timeout);

	public abstract void reportError(Exception e);

	public abstract long insertTraceEntry(int sourceId, int destId, long time,
			int entries);

	public abstract void insertTraceEntryData(long entryId, int[] myRouterIds,
			TraceEntry te);
	
	public abstract long insertTraceEntryOess(int sourceId, int destId, long time,
			int entries, int destType);
	
	public abstract void insertTraceEntryDataOess(long entryId, int[] myRouterIds,
			TraceEntry te);
	/**
	 * Inserts local peer stats.
	 * 
	 * @param selfId
	 *            local peer id
	 * @param currentTime
	 *            the time of the stat being reported
	 * @param uploadRate
	 *            the upload rate at that time
	 * @param downloadRate
	 *            the download rate at that time
	 * @param rst 
	 * @param b
	 * @param j
	 * @param i
	 * @return
	 */
	public abstract boolean insertLocalPeerStatDynamic(int selfId,
			long currentTime, int uploadRate, int downloadRate,
			int maxDownloadRate, int maxUploadRate, boolean isAutoSpeed, int rst, 
			int numConnections, int passiveOpens, int activeOpens, int failedConns);

	/**
	 * Inserts once-per-session peer stats
	 * 
	 * @param selfId
	 *            the peer id
	 * @param uptime
	 *            the peer session lifetime
	 * @param maxUploadRate
	 *            the max upload rate
	 * @param maxDownloadRate
	 *            the max download rate
	 */
	public abstract void insertLocalPeerStatStatic(int selfId, long uptime,
			int maxUploadRate, int maxDownloadRate);

	/**
	 * Reports once-per-session peer stats.
	 * 
	 * @param selfId
	 *            the id of the local peer
	 * @param peerId
	 *            the id of the peer being reported on
	 * @param peerClient
	 *            the id of the client software being used
	 * @param uptime
	 *            the duration of the connection
	 * @param tcpPort
	 *            the tcp port being used
	 * @param udpPort
	 *            the udp port being used
	 * @param sameCluster
	 *            true if node is in same cluster
	 */
	public abstract void insertRemotePeerStatStatic(int selfId, int peerId,
			int peerClient, long uptime, int tcpPort, int udpPort,
			boolean sameCluster, boolean bpSameCluster);

	/**
	 * Reports periodic data from remote peers.
	 * 
	 * @param selfId
	 *            local peer
	 * @param peerId
	 *            remote peer (being measured)
	 * @param currentTime
	 *            time of measurement
	 * @param bytesReceivedFromPeer
	 *            bytes received at that point
	 * @param bytesSentToPeer
	 *            bytes sent at that point
	 * @param currentReceiveRate
	 *            current download rate
	 * @param currentSendRate
	 *            current upload rate
	 * @param isSeed
	 *            whether it is a seed
	 * @param ping
	 *            ping latency
	 * @param isInteresting
	 * @param isInterested
	 * @param discarded 
	 * @return
	 */
	public abstract boolean insertRemotePeerStatDynamic(int selfId, int peerId,
			long currentTime, int bytesReceivedFromPeer, int bytesSentToPeer,
			int currentReceiveRate, int currentSendRate, boolean isSeed,
			double ping, boolean isInterested, boolean isInteresting, int discarded);

	/**
	 * 
	 * @param selfId
	 *            peer id
	 * @param downloadId
	 *            the id for this download for this session
	 * @param currentTime
	 *            current time in ms
	 * @param downloadingTime
	 *            time spent downloading (s)
	 * @param seedingTime
	 *            time spent seeding (s)
	 * @param maxDownloadRateLimit
	 *            dl rate limit
	 * @param maxUploadLimit
	 *            upload rate limit
	 * @param totalUploaded
	 *            total bytes uploaded for this download
	 * @param totalDownloaded
	 *            total bytes downloaded for this download
	 * @param size
	 *            size of the download
	 * @param pieceSize
	 *            piece size for dl
	 */
	public abstract void insertDownloadStatSummary(int selfId, int downloadId,
			long currentTime, long downloadingTime, long seedingTime,
			int maxDownloadRateLimit, int maxUploadLimit, long totalUploaded,
			long totalDownloaded, long size, long pieceSize);

	/**
	 * 
	 * @param selfId
	 *            peer id
	 * @param downloadId
	 *            download id
	 * @param timeRecorded
	 *            time recorded
	 * @param numPeers
	 *            num peers connected
	 * @param availability
	 *            data availability
	 * @param completed
	 *            completion in thousands
	 * @param discarded
	 *            num bytes discarded
	 * @param downloadRate
	 *            download rate
	 * @param uploadRate
	 *            upload rate
	 * @param shareRatio
	 *            share ratio in thousandths
	 * @param numKnownNonSeeds
	 *            total non seeds known about
	 * @param numKnownSeeds
	 *            total seeds known about
	 * @param state
	 *            download state
	 * @param priority
	 * @param uploaded 
	 * @param downloaded 
	 * @param position 
	 * @param numOnoPeers 
	 * @param totalNumKnownSeeds 
	 * @param totalNumKnownNonSeeds 
	 * @param hashFails 
	 * @return success
	 */
	public abstract boolean insertDownloadStatDynamic(int selfId,
			int downloadId, long timeRecorded, int numPeers,
			float availability, int completed, int discarded, int downloadRate,
			int uploadRate, int shareRatio, int numKnownNonSeeds,
			int numKnownSeeds, int state, int priority, long downloaded,
			long uploaded, int position, int numOnoPeers,
			int totalNumKnownNonSeeds, int totalNumKnownSeeds, long hashFails);

	public abstract void registerUsage(int myId, long currentTime, String uuid);

	/**
	 * Gets the update interval, in seconds
	 * 
	 * @return
	 */
	public abstract int getRecordingInterval(String columnName);

	public abstract void stopReporting();

	public abstract void flush();

	public abstract Map<String, Integer> makeIdBatch(String tableName,
			String columnName, List<String> values);

	public abstract void updateConfig();

	public abstract void reportIpChange(int oldIp, int newIp, int localIp,
			long currentGMTTime);

}
