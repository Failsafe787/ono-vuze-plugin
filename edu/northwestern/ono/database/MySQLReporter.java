/**
 * Ono Project
 *
 * File:         MySQLReporter.java
 * RCS:          $Id: MySQLReporter.java,v 1.69 2010/03/29 16:48:03 drc915 Exp $
 * Description:  MySQLReporter class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jun 14, 2006 at 8:28:41 AM
 * Language:     Java
 * Package:      edu.northwestern.ono.database
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
package edu.northwestern.ono.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.OnoConfiguration;
import edu.northwestern.ono.experiment.TraceRouteRunner.TraceEntry;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 * 
 * The MySQLReporter class sends data to a mysql database.
 */
public class MySQLReporter implements DatabaseReporter {
	static final String DRIVER_CLASS_NAME = "org.gjt.mm.mysql.Driver";

	static final String DRIVER_ID_STRING = "mysql";

	static final String PORT = "";

//	static final String HOSTNAME = "merlot.cs.northwestern.edu";

	static final String USER_NAME = "azplugin";

	static final String PASSWD = "Paf:FTqvpUDaUXNf";

//	static final String DATABASE_NAME = "ono";

	private static final boolean DEBUG = false;

	private static final int MAX_RECONNECT_ATTEMPTS = 3;

	private static final boolean OUTPUT_TIMING = true;

	private static boolean fresh = true;

	static Connection connection = null;

	private static DatabaseReporter currentInstance;

	PreparedStatement insertDNSEvent;

	String insertDNSEventS = "insert into dnsevent values("
			+ "?, ?, ?, ?, ?, ?)";

	PreparedStatement insertDigResult;

	String insertDigResultS = "insert into mappings values("
			+ "0, ?, ?, ?, ?, ?)";

	PreparedStatement selectEdge;

	String selectEdgeS = "select id from edgeservers where ipaddress like ?";

	PreparedStatement insertEdge;

	String insertEdgeS = "insert into edgeservers values(?,?)";

	PreparedStatement lastEdge;

	String lastEdgeS = "SELECT LAST_INSERT_ID()";

	PreparedStatement insertPingResult;

	String insertPingResultS = "insert into pingdata values("
			+ "?, ?, ?, ?, ?, ?, ?)";

	PreparedStatement insertDTEResult;

	String insertDTEResultS = "insert into datatransfer values("
			+ "?, ?, ?, ?, ?, ?, ?, ?, ?)";

	PreparedStatement insertVivaldiResult;

	String insertVivaldiResultS = "insert into vivaldi2 values("
			+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	PreparedStatement insertClusterRatios;

	String insertClusterRatiosS = "insert into ratios values(" + "?, ?, ?)";

	PreparedStatement insertRatioEntry;

	String insertRatioEntryS = "insert into ratio_mappings values("
			+ "0, ?, ?, CURRENT_TIMESTAMP())";

	PreparedStatement insertTraceResult;

	String insertTraceResultS = "insert into traceEntry values("
			+ "0, ?, ?, ?)";

	PreparedStatement insertTraceEntryData;

	String insertTraceEntryDataS = "insert into traceEntryData values("
			+ "?, ?, ?, ?, ?, ?, ?)";

	PreparedStatement insertlocalPeerStatDynamic;

	String insertlocalPeerStatDynamicS = "insert into localPeerStatDynamic values("
			+ "?, ?, ?, ?)";

	PreparedStatement insertlocalPeerStatStatic;

	String insertlocalPeerStatStaticS = "insert into localPeerStatStatic values("
			+ "?, ?, ?, ?, ?)";

	PreparedStatement insertremotePeerStatStatic;

	String insertremotePeerStatStaticS = "insert into remotePeerStatStatic values("
			+ "?, ?, ?, ?, ?,?,?,?)";

	PreparedStatement insertremotePeerStatDynamic;

	String insertremotePeerStatDynamicS = "insert into remotePeerStatDynamic2 values("
			+ "?, ?, ?, ?, ?,?,?,?,?,?,?)";

	PreparedStatement insertDownloadStatSummary;

	String insertDownloadStatSummaryS = "insert into downloadStatSummary2 values("
			+ "?, ?, ?, ?, ?,?,?,?,?,?,?)";

	PreparedStatement insertDownloadStatDynamic;

	String insertDownloadStatDynamicS = "insert into downloadStatDynamic2 values("
			+ "?, ?, ?, ?, ?,?,?,?,?,?,?,?,?,?)";

	private long lastFailTime = -1;

	private int failCount = 0;

	static Boolean connectionObject = new Boolean(true);
	static Boolean extendedObject = new Boolean(true);

	/** if greater than zero, indicates that connection should stay open */
	private static int extendedOperation = 0;

	private static boolean forceClose = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		super.finalize();

		if (connection != null) {
			connection.close();
		}
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#setExtendedConnection(boolean)
	 */
	public void setExtendedConnection(boolean extend) {
		synchronized(extendedObject){
			if (extend)
				extendedOperation++;
			else
				extendedOperation--;
		}
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#connect()
	 */
	public synchronized boolean connect() {
		try {
		
				if ((connection != null) && !connection.isClosed()) {
					return true;
				}
			
			testDriver();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// don't reconnect if failure happened recently
		if (lastFailTime > 0
				&& (System.currentTimeMillis() - lastFailTime) < OnoConfiguration
						.getInstance().getDbReconnectInterval() * 1000) {
			return false;
		}

		String url = "";

		try {
			url = "jdbc:mysql://" + MainGeneric.getDatabaseHost() 
				+ "/" + MainGeneric.getDatabaseName();
			connection = DriverManager.getConnection(url, USER_NAME, PASSWD);
			insertDigResult = null;
			insertDNSEvent = null;
			insertPingResult = null;
			lastEdge = null;
			selectEdge = null;
			insertEdge = null;

			if (fresh) {
				getConfiguration();
			}

			fresh = false;
			lastFailTime = -1;
			failCount = 0;
			if (OUTPUT_TIMING){
			 System.out.println("Connection established to " + url + 
					 " @ t="+System.currentTimeMillis());
			}
			return true;
		} catch (java.sql.SQLException e) {
			System.out.println("Connection couldn't be established to " + url);
			if (e.getErrorCode() == 1203) {
				// too many connections, don't record data
				if (fresh) {
					OnoConfiguration.getInstance().setSendToDatabase(false);
					// OnoConfiguration.getInstance().setRecordStats(true);
				}

			} else {
				// some other failure mode
				if (fresh) {
					OnoConfiguration.getInstance().setSendToDatabase(false);
					// OnoConfiguration.getInstance().setRecordStats(true);
				}
			}

			if (failCount < MAX_RECONNECT_ATTEMPTS) {
				failCount++;
			} else {
				failCount = 0;
				lastFailTime = System.currentTimeMillis();
			}

			return false;
		}
	}

	/**
	 * Gets configuration information from the database
	 * 
	 */
	private void getConfiguration() {
		Statement statement = null;

		String select = "select * from intervals";

		if (DEBUG) {
			System.out.println("::doSelect: sending: " + select);
		}

		try {
			statement = connection.createStatement();

			ResultSet r = statement.executeQuery(select);

			if (r.first()) {
				OnoConfiguration oc = OnoConfiguration.getInstance();
				oc.setPeerUpdateIntervalSec(r.getInt(1));
				oc.setSendToDatabase(r.getBoolean(2));
				oc.setRecordStats(r.getBoolean(2));
				oc.setDigFrequencySec(r.getInt(3));
				oc.setVivaldiFrequencySec(r.getInt(4));
				oc.setDoTraceroute(r.getBoolean(5));
				oc.setCosSimThreshold(r.getFloat(6));
				oc.setRatioUpdateFrequencySec(r.getInt(7));
				oc.setDbUpdateFreqSec(r.getInt(8));
				oc.setUpdateCheckFreqSec(r.getInt(9));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#doInsert(java.lang.String, java.lang.String)
	 */
	public int doInsert(String insert, String select) throws IOException,
			ClassNotFoundException, SQLException {
		Statement statement = null;

		// now get the row number
		if (select != null) {
			if (DEBUG) {
				System.out.println("::doInsert: sending: " + select);
			}

			statement = connection.createStatement();

			ResultSet r = statement.executeQuery(select);

			if (r.first()) {
				return r.getInt(1);
			}
		}

		if (DEBUG) {
			System.out.println("::doInsert: sending: " + insert);
		}

		statement = connection.createStatement();
		statement.executeUpdate(insert);

		statement = connection.createStatement();

		ResultSet r = statement.executeQuery("SELECT LAST_INSERT_ID()");

		if (r.first()) {
			return r.getInt(1);
		}

		return -1;
	}

	public static String sqlEncode(String txt) {
		// escape char is '_' in conformance with table and col naming
		// constraints
		char escape_char = '_';
		StringBuffer e = new StringBuffer();

		for (int i = 0; i < txt.length(); i++) {
			char c = txt.charAt(i);

			if (c < 16) {
				e.append(escape_char + "0" + Integer.toString(c, 16));
			} else if ((c < 32) || (c > 127) || (".%^_'\"".indexOf(c) >= 0)) {
				e.append(escape_char + Integer.toString(c, 16));
			} else {
				e.append(c);
			}
		}

		return e.toString();
	}

	protected static String sqlDecode(String txt) {
		// escape char is '_' in conformance with table and col naming
		// constraints
		char escape_char = '_';
		StringBuffer d = new StringBuffer();

		for (int i = 0; i < txt.length(); i++) {
			char c = txt.charAt(i);

			if (c == escape_char) {
				String hex = txt.substring(i + 1, i + 3);
				char dec = (char) Integer.parseInt(hex, 16);
				d.append(dec);
				i += 2;
			} else {
				d.append(c);
			}
		}

		return d.toString();
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#doCreateTable(java.lang.String)
	 */
	public void doCreateTable(String s) throws SQLException, IOException,
			ClassNotFoundException {
		// CREATE TABLE TABLE_NAME (COL1_NAME COL1_TYPE, COL2_NAME COL2_TYPE,
		// ...);
		Statement statement = null;

		try {
			if (DEBUG) {
				System.out.println("::createTable: sending: " + s);
			}

			statement = connection.createStatement();
			statement.executeUpdate(s);
		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException ignored) {
				}
			}
		}
	}

	/**
	 * Checks whether the MySQL JDBC Driver is installed
	 */
	protected void testDriver() throws Exception {
		try {
			Class.forName("org.gjt.mm.mysql.Driver");
			// System.out.println("MySQL Driver Found");
		} catch (java.lang.ClassNotFoundException e) {
			System.out.println("MySQL JDBC Driver not found ... ");
			throw (e);
		}
	}

	/**
	 * @return
	 */
	public synchronized static DatabaseReporter getInstance() {
		if (currentInstance == null || 
				(!(currentInstance instanceof MySQLReporter) /*&& 
						!MainGeneric.isShuttingDown()*/)) {
			currentInstance = new MySQLReporter();
			currentInstance.connect();
		}

		return currentInstance;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#canConnect()
	 */
	public boolean canConnect() {
		return connection != null
				|| lastFailTime < 0
				|| (System.currentTimeMillis() - lastFailTime) > OnoConfiguration
						.getInstance().getDbReconnectInterval() * 1000;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#getNextPosition()
	 */
	public int getNextPosition() {
		try {
			int iso = connection.getTransactionIsolation();
			connection
					.setTransactionIsolation(connection.TRANSACTION_SERIALIZABLE);

			int retVal = -1;
			Statement statement = connection.createStatement();
			ResultSet r = statement
					.executeQuery("Select count from coordinator where id=0");

			if (r.first()) {
				retVal = r.getInt(1) + 1;
			}

			statement = connection.createStatement();
			statement.executeUpdate("Update coordinator set count=" + retVal
					+ " where id=0");
			connection.setTransactionIsolation(iso);

			return retVal;
		} catch (Exception e) {
			e.printStackTrace();
		}

		// TODO Auto-generated method stub
		return -1;
	}

	/**
	 * @return Returns the connection.
	 */
	public static Connection getConnection() {
		return connection;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertEvent(int, int, int, int, long, int)
	 */
	public boolean insertEvent(int customerId, int edgeId, int peerId,
			int eventType, long time, int timeZoneId) {
		try {
			synchronized (connectionObject) {
				if (connection.isClosed()) {
					if (!connect()) {
						return false;
					}
				}

				if (insertDNSEvent == null) {

					insertDNSEvent = connection
							.prepareStatement(insertDNSEventS);

				}

				insertDNSEvent.setInt(1, customerId);
				insertDNSEvent.setInt(2, edgeId);
				insertDNSEvent.setInt(3, peerId);
				insertDNSEvent.setInt(4, eventType);
				insertDNSEvent.setLong(5, time);
				insertDNSEvent.setInt(6, timeZoneId);

				insertDNSEvent.executeUpdate();
			}

			return true;
		} catch (SQLException e) {
			insertDNSEvent = null;
			// TODO Auto-generated catch block
			e.printStackTrace();

			return false;
		} catch (NullPointerException e) {
			insertDNSEvent = null;
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#makeId(java.lang.String, java.lang.String, java.lang.String)
	 */
	public int makeId(String tableName, String colName, String value) {
		// select from table
		Statement statement = null;
		String select = "Select * from " + tableName + " where " + colName
				+ " like '" + value + "'";
		String insert = "Insert into " + tableName + " values(0, '" + value
				+ "')";
		int iso = -1;

		try {
			synchronized (connectionObject) {
				if ((connection == null) || connection.isClosed()) {
					if (!connect()) {
						return -1;
					}
				}

				// now get the row number
				if (select != null) {
					if (DEBUG) {
						System.out.println("::doInsert: sending: " + select);
					}

					statement = connection.createStatement();

					ResultSet r = statement.executeQuery(select);

					if (r.first()) {
						int id = r.getInt(1);
						r.close();
						statement.close();

						return id;
					}
				}

				// iso = connection.getTransactionIsolation();
				// connection.setTransactionIsolation(connection.TRANSACTION_SERIALIZABLE);

				if (DEBUG) {
					System.out.println("::doInsert: sending: " + insert);
				}

				statement = connection.createStatement();
				statement.executeUpdate(insert);

				statement = connection.createStatement();

				ResultSet r = statement.executeQuery("SELECT LAST_INSERT_ID()");
				// connection.setTransactionIsolation(iso);

				if (r.first()) {
					int id = r.getInt(1);
					r.close();
					statement.close();

					return id;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();

			// if (iso != -1) {
			// try {
			// connection.setTransactionIsolation(iso);
			// statement.close();
			// } catch (SQLException e1) {
			// // TODO Auto-generated catch block
			// e1.printStackTrace();
			// }
			// }
		}

		return -1;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertDigResult(int, int, int, int, long)
	 */
	public boolean insertDigResult(int customerId, int peerId, int edge1Id,
			int edge2Id, long time) {
		try {
			synchronized (connectionObject) {
				if (connection.isClosed()) {
					if (!connect()) {
						return false;
					}
				}

				if (insertDigResult == null) {
					insertDigResult = connection
							.prepareStatement(insertDigResultS);
				}

				insertDigResult.setInt(1, peerId);
				insertDigResult.setInt(2, customerId);
				insertDigResult.setInt(3, edge1Id);
				insertDigResult.setInt(4, edge2Id);
				insertDigResult.setLong(5, time);

				insertDigResult.executeUpdate();
			}
			return true;
		} catch (SQLException e) {
			insertDigResult = null;
			// TODO Auto-generated catch block
			e.printStackTrace();

			return false;
		} catch (NullPointerException e) {
			insertDigResult = null;
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#disconnect()
	 */
	public synchronized void disconnect() {
		synchronized(extendedObject){
			if (!forceClose && extendedOperation > 0)
				return;
		}
		try {

			synchronized (connectionObject) {
				if (connection != null && !connection.isClosed()) {
					connection.close();
					if (OUTPUT_TIMING) System.out.println("Disconnected from DB!");
				}
				insertVivaldiResult = null;
				insertPingResult = null;
				insertDNSEvent = null;
				insertDigResult = null;
				selectEdge = null;
				insertEdge = null;
				lastEdge = null;
				insertDTEResult = null;
				insertRatioEntry = null;
				insertClusterRatios = null;
				insertTraceResult = null;
				insertTraceEntryData = null;
				insertlocalPeerStatDynamic = null;
				insertlocalPeerStatStatic = null;
				insertremotePeerStatStatic = null;
				insertremotePeerStatDynamic = null;
				insertDownloadStatDynamic = null;
				insertDownloadStatSummary = null;
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertPingResult(int, int, int, int, float, long, int)
	 */
	public synchronized boolean insertPingResult(int edgeId, int myId, int destId,
			int middleId, float rtt, long time, int experimentId) {
		try {
			synchronized (connectionObject) {
				if (connection.isClosed()) {
					if (!connect()) {
						return false;
					}
				}

				;

				if (insertPingResult == null) {
					insertPingResult = connection
							.prepareStatement(insertPingResultS);
				}

				insertPingResult.setInt(1, edgeId);
				insertPingResult.setInt(2, myId);
				insertPingResult.setInt(3, destId);
				insertPingResult.setInt(4, middleId);
				insertPingResult.setFloat(5, rtt);
				insertPingResult.setLong(6, time);
				insertPingResult.setInt(7, experimentId);

				insertPingResult.executeUpdate();
			}

			return true;
		} catch (SQLException e) {
			insertPingResult = null;
			// TODO Auto-generated catch block
			e.printStackTrace();

			return false;
		} catch (NullPointerException e) {
			insertPingResult = null;
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertDataTransferResult(long, int, byte, int, int, int, int, double, double)
	 */
	public boolean insertDataTransferResult(long time, int experimentId,
			byte type, int size, int sourceId, int destId, int currentId,
			double raceResultPrimary, double raceResultSecondary) {
		try {
			synchronized (connectionObject) {
				if (connection.isClosed()) {
					if (!connect()) {
						return false;
					}
				}

				if (insertDTEResult == null) {
					insertDTEResult = connection
							.prepareStatement(insertDTEResultS);
				}

				insertDTEResult.setLong(1, time);
				insertDTEResult.setInt(2, experimentId);
				insertDTEResult.setByte(3, type);
				insertDTEResult.setInt(4, size);
				insertDTEResult.setInt(5, sourceId);
				insertDTEResult.setInt(6, destId);
				insertDTEResult.setInt(7, currentId);
				insertDTEResult.setDouble(8, raceResultPrimary);
				insertDTEResult.setDouble(9, raceResultSecondary);

				insertDTEResult.executeUpdate();
				insertDTEResult.clearParameters();

				return true;
			}
		} catch (SQLException e) {
			insertDTEResult = null;
			// TODO Auto-generated catch block
			e.printStackTrace();

			return false;
		} catch (NullPointerException e) {
			insertDTEResult = null;
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertVivaldiResult(long, int, int, float, float, float, float, float, float, float, byte)
	 */
	public boolean insertVivaldiResult(long timestamp, int peerId,
			int observedBy, float x, float y, float h, float estErr,
			float estRTT, float pingResult, float estRTTv2, float[] coordsV2, byte type) {
		try {
			synchronized (connectionObject) {
				if (connection.isClosed()) {
					if (!connect()) {
						return false;
					}
				}

				if (insertVivaldiResult == null) {
					insertVivaldiResult = connection
							.prepareStatement(insertVivaldiResultS);
				}

				insertVivaldiResult.setLong(1, timestamp);
				insertVivaldiResult.setInt(2, peerId);
				insertVivaldiResult.setInt(3, observedBy);
				insertVivaldiResult.setFloat(4, x);
				insertVivaldiResult.setFloat(5, y);
				insertVivaldiResult.setFloat(6, h);
				insertVivaldiResult.setFloat(7, estErr);
				insertVivaldiResult.setFloat(8, estRTT);
				insertVivaldiResult.setFloat(9, pingResult);
				insertVivaldiResult.setFloat(10, estRTTv2);
				insertVivaldiResult.setByte(11, type);

				insertVivaldiResult.executeUpdate();
				insertVivaldiResult.clearParameters();
			}

			return true;
		} catch (SQLException e) {
			insertVivaldiResult = null;
			// TODO Auto-generated catch block
			e.printStackTrace();

			return false;
		} catch (NullPointerException e) {
			insertVivaldiResult = null;
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertRatioEntry(int, int)
	 */
	public long insertRatioEntry(int myId, int customerId, int entries) {
		int isolation = 0;
		// try{

		try {
			synchronized (connectionObject) {
				if (connection.isClosed()) {
					if (!connect()) {
						return -1;
					}
				}

				// isolation = connection.getTransactionIsolation();
				// connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

				if (insertRatioEntry == null) {
					insertRatioEntry = connection
							.prepareStatement(insertRatioEntryS);
				}

				insertRatioEntry.setInt(1, myId);
				insertRatioEntry.setInt(2, customerId);

				insertRatioEntry.executeUpdate();
				insertRatioEntry.clearParameters();

				Statement statement = connection.createStatement();

				ResultSet r = statement.executeQuery("SELECT LAST_INSERT_ID()");

				if (r.first()) {
					return r.getLong(1);
				}
			}

			return -1;
		} catch (SQLException e) {
			insertRatioEntry = null;
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			insertRatioEntry = null;

		}
		// finally{
		// // connection.setTransactionIsolation(isolation);
		// }
		// }catch (SQLException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// return -1;
		// }
		return -1;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertRatio(long, int, double)
	 */
	public void insertRatio(long entryId, int edgeClusterId, double value) {
		try {
			synchronized (connectionObject) {
				if (connection.isClosed()) {
					if (!connect()) {
						return;
					}
				}

				if (insertClusterRatios == null) {
					insertClusterRatios = connection
							.prepareStatement(insertClusterRatiosS);
				}

				insertClusterRatios.setLong(1, entryId);
				insertClusterRatios.setInt(2, edgeClusterId);
				insertClusterRatios.setDouble(3, value);

				insertClusterRatios.executeUpdate();
				insertClusterRatios.clearParameters();
			}

		} catch (SQLException e) {
			insertClusterRatios = null;
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			insertClusterRatios = null;

		}

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#reportError(java.lang.Exception)
	 */
	public void reportError(Exception e) {
		try {
			synchronized (connectionObject) {
				if (connection.isClosed()) {
					if (!connect()) {
						return;
					}
				}
				String error = e.toString() + "\n";
				error += e.getCause() != null ? e.getCause().toString() + "\n"
						: e.getMessage() + "\n";
				for (StackTraceElement ste : e.getStackTrace()) {
					error += ste.toString() + "\n";
				}

				Statement s = connection.createStatement();
				s.execute("Insert into exceptions values('"
						+ MainGeneric.getPublicIpAddress() + "', '" + error
						+ "')");

			}

		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		disconnect();

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertTraceEntry(int, int, long)
	 */
	public long insertTraceEntry(int sourceId, int destId, long time, int entries) {
		int isolation = 0;
		// try{

		try {
			synchronized (connectionObject) {
				if (connection.isClosed()) {
					if (!connect()) {
						return -1;
					}
				}

				// isolation = connection.getTransactionIsolation();
				// connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

				if (insertTraceResult == null) {
					insertTraceResult = connection
							.prepareStatement(insertTraceResultS);
				}

				insertTraceResult.setInt(1, sourceId);
				insertTraceResult.setInt(2, destId);
				insertTraceResult.setLong(3, time);

				insertTraceResult.executeUpdate();
				insertTraceResult.clearParameters();

				Statement statement = connection.createStatement();

				ResultSet r = statement.executeQuery("SELECT LAST_INSERT_ID()");

				if (r.first()) {
					return r.getLong(1);
				}
			}

			return -1;
		} catch (SQLException e) {
			insertTraceResult = null;
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			insertTraceResult = null;

		}
		// finally{
		// connection.setTransactionIsolation(isolation);
		// }
		// }catch (SQLException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// return -1;
		// }
		return -1;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertTraceEntryData(long, int[], edu.northwestern.ono.experiment.TraceRouteRunner.TraceEntry)
	 */
	public void insertTraceEntryData(long entryId, int[] myRouterIds,
			TraceEntry te) {
		try {
			synchronized (connectionObject) {
				if (connection.isClosed()) {
					if (!connect()) {
						return;
					}
				}

				if (insertTraceEntryData == null) {
					insertTraceEntryData = connection
							.prepareStatement(insertTraceEntryDataS);
				}

				insertTraceEntryData.setLong(1, entryId);
				for (int i = 0; i < te.rtt.length; i++) {
					int j = i * 2;
					insertTraceEntryData.setInt(2 + j, myRouterIds[i]);
					insertTraceEntryData.setFloat(3 + j, te.rtt[i]);
				}

				insertTraceEntryData.executeUpdate();
				insertTraceEntryData.clearParameters();
			}

		} catch (SQLException e) {
			insertTraceEntryData = null;
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			insertTraceEntryData = null;

		}

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertLocalPeerStatDynamic(int, long, int, int)
	 */
	public boolean insertLocalPeerStatDynamic(int selfId, long currentTime,
			int uploadRate, int downloadRate, 
			int maxDownloadRate, int maxUploadRate, boolean isAutoSpeed, int rst, 
			int numConnections, int passiveOpens, int activeOpens, int failedConns) {
		try {
			if (selfId == -1) {
				System.err.println("Invalid id!");
				return false;
			}
			synchronized (connectionObject) {
				if (connection == null || connection.isClosed()) {
					if (!connect()) {
						return false;
					}
				}

				if (insertlocalPeerStatDynamic == null) {
					insertlocalPeerStatDynamic = connection
							.prepareStatement(insertlocalPeerStatDynamicS);
				}

				insertlocalPeerStatDynamic.setInt(1, selfId);
				insertlocalPeerStatDynamic.setLong(2, currentTime);
				insertlocalPeerStatDynamic.setInt(3, uploadRate);
				insertlocalPeerStatDynamic.setInt(4, downloadRate);

				insertlocalPeerStatDynamic.executeUpdate();
				insertlocalPeerStatDynamic.clearParameters();
			}

		} catch (SQLException e) {
			insertlocalPeerStatDynamic = null;
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (NullPointerException e) {
			insertlocalPeerStatDynamic = null;
			return false;

		}
		return true;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertLocalPeerStatStatic(int, long, int, int)
	 */
	public void insertLocalPeerStatStatic(int selfId, long uptime,
			int maxUploadRate, int maxDownloadRate) {
		try {
			synchronized (connectionObject) {
				if (connection == null || connection.isClosed()) {
					if (!connect()) {
						return;
					}
				}

				if (insertlocalPeerStatStatic == null) {
					insertlocalPeerStatStatic = connection
							.prepareStatement(insertlocalPeerStatStaticS);
				}

				insertlocalPeerStatStatic.setInt(1, selfId);
				insertlocalPeerStatStatic.setLong(2, uptime);
				insertlocalPeerStatStatic.setInt(3, maxUploadRate);
				insertlocalPeerStatStatic.setInt(4, maxDownloadRate);
				insertlocalPeerStatStatic.setLong(5, Calendar.getInstance(
						TimeZone.getTimeZone("GMT")).getTimeInMillis());

				insertlocalPeerStatStatic.executeUpdate();
				insertlocalPeerStatStatic.clearParameters();
			}

		} catch (SQLException e) {
			insertlocalPeerStatStatic = null;
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			insertlocalPeerStatStatic = null;
		}

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertRemotePeerStatStatic(int, int, int, long, int, int, boolean)
	 */
	public void insertRemotePeerStatStatic(int selfId, int peerId,
			int peerClient, long uptime, int tcpPort, int udpPort,
			boolean sameCluster, boolean bpSameCluster) {

		try {
			if (selfId == -1 || peerId == -1) {
				System.err.println("Invalid id!");
				return;
			}
			synchronized (connectionObject) {
				if (connection == null || connection.isClosed()) {
					if (!connect()) {
						return;
					}
				}

				if (insertremotePeerStatStatic == null) {
					insertremotePeerStatStatic = connection
							.prepareStatement(insertremotePeerStatStaticS);
				}

				insertremotePeerStatStatic.setInt(1, selfId);
				insertremotePeerStatStatic.setInt(2, peerId);
				insertremotePeerStatStatic.setInt(3, peerClient);
				insertremotePeerStatStatic.setLong(4, uptime);
				insertremotePeerStatStatic.setInt(5, tcpPort);
				insertremotePeerStatStatic.setInt(6, udpPort);
				insertremotePeerStatStatic.setBoolean(7, sameCluster);
				insertremotePeerStatStatic.setLong(8, Util.currentGMTTime());

				insertremotePeerStatStatic.executeUpdate();
				insertremotePeerStatStatic.clearParameters();
			}

		} catch (SQLException e) {
			insertremotePeerStatStatic = null;
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			try {
				insertremotePeerStatStatic.close();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			insertremotePeerStatStatic = null;
		}
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertRemotePeerStatDynamic(int, int, long, int, int, int, int, boolean, double, boolean, boolean)
	 */
	public boolean insertRemotePeerStatDynamic(int selfId, int peerId,
			long currentTime, int bytesReceivedFromPeer, int bytesSentToPeer,
			int currentReceiveRate, int currentSendRate, boolean isSeed,
			double ping, boolean isInterested, boolean isInteresting, int discarded) {
		try {
			if (selfId == -1 || peerId == -1) {
				System.err.println("Invalid id!");
				return false;
			}
			synchronized (connectionObject) {
				if (connection == null || connection.isClosed()) {
					if (!connect()) {
						return false;
					}
				}

				if (insertremotePeerStatDynamic == null) {
					insertremotePeerStatDynamic = connection
							.prepareStatement(insertremotePeerStatDynamicS);
				}

				insertremotePeerStatDynamic.setInt(1, selfId);
				insertremotePeerStatDynamic.setInt(2, peerId);
				insertremotePeerStatDynamic.setLong(3, currentTime);
				insertremotePeerStatDynamic.setInt(4, bytesReceivedFromPeer);
				insertremotePeerStatDynamic.setInt(5, bytesSentToPeer);
				insertremotePeerStatDynamic.setInt(6, currentReceiveRate);
				insertremotePeerStatDynamic.setInt(7, currentSendRate);
				insertremotePeerStatDynamic.setBoolean(8, isSeed);
				insertremotePeerStatDynamic.setDouble(9, ping);
				insertremotePeerStatDynamic.setBoolean(10, isInterested);
				insertremotePeerStatDynamic.setBoolean(11, isInteresting);

				insertremotePeerStatDynamic.executeUpdate();
				insertremotePeerStatDynamic.clearParameters();
			}

		} catch (SQLException e) {
			insertremotePeerStatDynamic = null;
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			insertremotePeerStatDynamic = null;
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertDownloadStatSummary(int, int, long, long, long, int, int, long, long, long, long)
	 */
	public void insertDownloadStatSummary(int selfId, int downloadId,
			long currentTime, long downloadingTime, long seedingTime,
			int maxDownloadRateLimit, int maxUploadLimit, long totalUploaded,
			long totalDownloaded, long size, long pieceSize) {
		try {
			synchronized (connectionObject) {
				if (connection.isClosed()) {
					if (!connect()) {
						return;
					}
				}

				if (insertDownloadStatSummary == null) {
					insertDownloadStatSummary = connection
							.prepareStatement(insertDownloadStatSummaryS);
				}

				insertDownloadStatSummary.setInt(1, selfId);
				insertDownloadStatSummary.setInt(2, downloadId);
				insertDownloadStatSummary.setLong(3, currentTime);
				insertDownloadStatSummary.setLong(4, downloadingTime);
				insertDownloadStatSummary.setLong(5, seedingTime);
				insertDownloadStatSummary.setInt(6, maxDownloadRateLimit);
				insertDownloadStatSummary.setInt(7, maxUploadLimit);
				insertDownloadStatSummary.setLong(8, totalUploaded);
				insertDownloadStatSummary.setLong(9, totalDownloaded);
				insertDownloadStatSummary.setLong(10, size);
				insertDownloadStatSummary.setLong(11, pieceSize);

				insertDownloadStatSummary.executeUpdate();
				insertDownloadStatSummary.clearParameters();
			}

		} catch (SQLException e) {
			insertDownloadStatSummary = null;
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			insertDownloadStatSummary = null;
		}

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
		try {
			synchronized (connectionObject) {
				if (connection.isClosed()) {
					if (!connect()) {
						return false;
					}
				}

				if (insertDownloadStatDynamic == null) {
					insertDownloadStatDynamic = connection
							.prepareStatement(insertDownloadStatDynamicS);
				}

				insertDownloadStatDynamic.setInt(1, selfId);
				insertDownloadStatDynamic.setInt(2, downloadId);
				insertDownloadStatDynamic.setLong(3, timeRecorded);
				insertDownloadStatDynamic.setInt(4, numPeers);
				insertDownloadStatDynamic.setFloat(5, availability);
				insertDownloadStatDynamic.setInt(6, completed);
				insertDownloadStatDynamic.setInt(7, discarded);
				insertDownloadStatDynamic.setInt(8, downloadRate);
				insertDownloadStatDynamic.setInt(9, uploadRate);
				insertDownloadStatDynamic.setInt(10, shareRatio);
				insertDownloadStatDynamic.setInt(11, numKnownNonSeeds);
				insertDownloadStatDynamic.setInt(12, numKnownSeeds);
				insertDownloadStatDynamic.setInt(13, state);
				insertDownloadStatDynamic.setInt(14, priority);

				insertDownloadStatDynamic.executeUpdate();
				insertDownloadStatDynamic.clearParameters();

			}

		} catch (SQLException e) {
			insertDownloadStatDynamic = null;
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (NullPointerException e) {
			insertDownloadStatDynamic = null;
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#registerUsage(int, long)
	 */
	public void registerUsage(int myId, long currentTime, String uuid) {
		try {
			synchronized (connectionObject) {
				if (connection == null || connection.isClosed()) {
					if (!connect()) {
						return;
					}
				}

				Statement statement = null;

				statement = connection.createStatement();
				statement.executeUpdate("Insert into `usage2` values (" + myId
						+ ", " + currentTime + ", current_timestamp(), '"+uuid+"')");

			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#getRecordingInterval(java.lang.String)
	 */
	public int getRecordingInterval(String columnName) {
		try {
			synchronized (connectionObject) {
				if (connection.isClosed()) {
					if (!connect()) {
						return -1;
					}
				}

				Statement statement = null;

				statement = connection.createStatement();

				ResultSet r = statement.executeQuery("Select " + columnName
						+ " from " + "intervals");

				if (r.first()) {
					return r.getInt(1);
				}

			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.database.DatabaseReporter#stopReporting()
	 */
	public void stopReporting() {
		currentInstance = new NullReporter();
		forceClose = true;
		disconnect();
		
	}

	public void flush() {
		// this has no meaning w/ synchronous reporting
		
	}

	public Map<String, Integer> makeIdBatch(String tableName,
			String columnName, List<String> values) {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		for (String value : values){
			map.put(value, makeId(tableName, columnName, value));
		}
		return map;
	}
	
	public void updateConfig(){
		return;
	}

	
	public void reportIpChange(int oldIp, int newIp, int localIp,
			long currentGMTTime) {
		// TODO Auto-generated method stub
		System.err.println("report ip change unimplemented!");
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
