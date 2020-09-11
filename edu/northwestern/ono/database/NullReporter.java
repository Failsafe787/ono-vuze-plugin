/**
 * Ono Project
 *
 * File:         NullReporter.java
 * RCS:          $Id: NullReporter.java,v 1.24 2010/03/27 20:08:39 mas939 Exp $
 * Description:  NullReporter class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jul 8, 2007 at 10:00:02 AM
 * Language:     Java
 * Package:      edu.northwestern.ono.database
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
package edu.northwestern.ono.database;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import edu.northwestern.ono.experiment.TraceRouteRunner.TraceEntry;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The NullReporter class has all the methods of 
 * a database reporter, but does nothing. Useful for 
 * shutting down. 
 */
public class NullReporter implements DatabaseReporter {

	private static NullReporter instance = new NullReporter();
	
	public static DatabaseReporter getInstance(){
		return instance;
	}
	
	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#canConnect()
	 */
	public boolean canConnect() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#connect()
	 */
	public boolean connect() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#disconnect()
	 */
	public void disconnect() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#doCreateTable(java.lang.String)
	 */
	public void doCreateTable(String s) throws SQLException, IOException,
			ClassNotFoundException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#doInsert(java.lang.String, java.lang.String)
	 */
	public int doInsert(String insert, String select) throws IOException,
			ClassNotFoundException, SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#getNextPosition()
	 */
	public int getNextPosition() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#getRecordingInterval(java.lang.String)
	 */
	public int getRecordingInterval(String columnName) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertDataTransferResult(long, int, byte, int, int, int, int, double, double)
	 */
	public boolean insertDataTransferResult(long time, int experimentId,
			byte type, int size, int sourceId, int destId, int currentId,
			double raceResultPrimary, double raceResultSecondary) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertDigResult(int, int, int, int, long)
	 */
	
	public boolean insertDigResult(int customerId, int peerId, int edge1Id,
			int edge2Id, long time) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertDownloadStatDynamic(int, int, long, int, float, int, int, int, int, int, int, int, int, int)
	 */
	public boolean insertDownloadStatDynamic(int selfId, int downloadId,
			long timeRecorded, int numPeers, float availability, int completed,
			int discarded, int downloadRate, int uploadRate, int shareRatio,
			int numKnownNonSeeds, int numKnownSeeds, int state, int priority,
			long downloaded, long uploaded, int position, int numOnoPeers,
			int totalNumKnownNonSeeds, int totalNumKnownSeeds, long hashFails) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertDownloadStatSummary(int, int, long, long, long, int, int, long, long, long, long)
	 */
	public void insertDownloadStatSummary(int selfId, int downloadId,
			long currentTime, long downloadingTime, long seedingTime,
			int maxDownloadRateLimit, int maxUploadLimit, long totalUploaded,
			long totalDownloaded, long size, long pieceSize) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertEvent(int, int, int, int, long, int)
	 */
	public boolean insertEvent(int customerId, int edgeId, int peerId,
			int eventType, long time, int timeZoneId) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertLocalPeerStatDynamic(int, long, int, int)
	 */
	public boolean insertLocalPeerStatDynamic(int selfId, long currentTime,
			int uploadRate, int downloadRate, 
			int maxDownloadRate, int maxUploadRate, boolean isAutoSpeed, int rst, 
			int numConnections, int passiveOpens, int activeOpens, int failedConns) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertLocalPeerStatStatic(int, long, int, int)
	 */
	public void insertLocalPeerStatStatic(int selfId, long uptime,
			int maxUploadRate, int maxDownloadRate) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertPingResult(int, int, int, int, float, long, int)
	 */
	public boolean insertPingResult(int edgeId, int myId, int destId,
			int middleId, float rtt, long time, int experimentId) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertRatio(long, int, double)
	 */
	public void insertRatio(long entryId, int edgeClusterId, double value) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertRatioEntry(int, int)
	 */
	public long insertRatioEntry(int myId, int customerId, int entries) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertRemotePeerStatDynamic(int, int, long, int, int, int, int, boolean, double, boolean, boolean)
	 */
	public boolean insertRemotePeerStatDynamic(int selfId, int peerId,
			long currentTime, int bytesReceivedFromPeer, int bytesSentToPeer,
			int currentReceiveRate, int currentSendRate, boolean isSeed,
			double ping, boolean isInterested, boolean isInteresting, int discarded) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertRemotePeerStatStatic(int, int, int, long, int, int, boolean)
	 */
	public void insertRemotePeerStatStatic(int selfId, int peerId,
			int peerClient, long uptime, int tcpPort, int udpPort,
			boolean sameCluster, boolean bpSameCluster) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertTraceEntry(int, int, long)
	 */
	public long insertTraceEntry(int sourceId, int destId, long time, int entries) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertTraceEntryData(long, int[], edu.northwestern.ono.experiment.TraceRouteRunner.TraceEntry)
	 */
	public void insertTraceEntryData(long entryId, int[] myRouterIds,
			TraceEntry te) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertVivaldiResult(long, int, int, float, float, float, float, float, float, float, byte)
	 */
	public boolean insertVivaldiResult(long timestamp, int peerId,
			int observedBy, float x, float y, float h, float estErr,
			float estRTT, float pingResult, float estRTTv2, float[] coordsV2, byte type) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#makeId(java.lang.String, java.lang.String, java.lang.String)
	 */
	public int makeId(String tableName, String colName, String value) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#registerUsage(int, long)
	 */
	public void registerUsage(int myId, long currentTime, String uuid) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#reportError(java.lang.Exception)
	 */
	public void reportError(Exception e) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#setExtendedConnection(boolean)
	 */
	public void setExtendedConnection(boolean extend) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#stopReporting()
	 */
	public void stopReporting() {
		// TODO Auto-generated method stub

	}

	public void flush() {
		// TODO Auto-generated method stub
		
	}

	public Map<String, Integer> makeIdBatch(String tableName,
			String columnName, List<String> values) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void updateConfig(){
		
	}

	
	public void reportIpChange(int oldIp, int newIp, int localIp,
			long currentGMTTime) {
		// TODO Auto-generated method stub
		
	}

	public boolean insertPingResultOess(int destId, int sourceId, float rtt,
			long time, int destType) {
		// TODO Auto-generated method stub
		return false;
	}

	public void insertRatioOess(long entryId, int dnsId, int edgeClusterId,
			double value) {
		// TODO Auto-generated method stub
		
	}

	public long insertRatioOessEntry(int myId, int dnsId, int customerId,
			int entries) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void insertTraceEntryDataOess(long entryId, int[] myRouterIds,
			TraceEntry te) {
		// TODO Auto-generated method stub
		
	}

	public long insertTraceEntryOess(int sourceId, int destId, long time,
			int entries, int destType) {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean insertDnsMapping(int myId, int publicDns, int privateDns, String type, int order) {
		// TODO Auto-generated method stub
		return false;
	}

	public void insertTimeoutOess(int myDnsId, int myId, long timeout) {
		// TODO Auto-generated method stub
		
	}

	
	public void insertBranchPoint(long entryId, int branchPointId, double cossim) {
		// TODO Auto-generated method stub
		
	}

	
	public long insertBranchPointEntry(int myId, byte version, int numEntries) {
		// TODO Auto-generated method stub
		return 0;
	}

}
