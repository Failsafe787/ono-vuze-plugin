/**
 * Ono Project
 *
 * File:         PhpReporter.java
 * RCS:          $Id: PhpReporter.java,v 1.56 2010/06/23 20:45:03 drc915 Exp $
 * Description:  PhpReporter class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Oct 30, 2007 at 12:53:21 PM
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.OnoConfiguration;
import edu.northwestern.ono.experiment.TraceRouteRunner.TraceEntry;
import edu.northwestern.ono.timer.ITimer;
import edu.northwestern.ono.timer.ITimerEvent;
import edu.northwestern.ono.timer.ITimerEventPerformer;
import edu.northwestern.ono.util.Pair;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 * 
 * The PhpReporter class reports data to a web service based on PHP.
 */
public class PhpReporter implements DatabaseReporter {

	// TODO deal with failed inserts
	// TODO make sure buffers are cleared appropriately

	public TreeMap<Double, String> urls = new TreeMap<Double, String>();

	public static final boolean TESTING = false;
	public static final boolean TESTING_GZIP = false;

	public static final String DEFAULT_ENCODING = "US-ASCII";
	public static final String BYTE_ENCODING = "ISO-8859-1";
	public static Charset BYTE_CHARSET;
	public static Charset DEFAULT_CHARSET;
	/** the largest post that will be sent */
	private static int POST_THRESHOLD = 30 * 1024; // 30K for now

	private static final String SERVER_LIST_LOC = "http://www.aqua-lab.org/ono/ws/ws-list.dat";

	private static final String DEFAULT_SERVER = "http://haleakala.cs.northwestern.edu/ono/ws/reportStats.php";
	
	private static final boolean SKIP_GET_URLS = false;
	private static final String TEST_GZIP_SERVER = DEFAULT_SERVER;
	NumberFormat nf = NumberFormat.getInstance();

	private static PhpReporter currentInstance;

	static {
		try {
			BYTE_CHARSET = Charset.forName(BYTE_ENCODING);
			DEFAULT_CHARSET = Charset.forName(DEFAULT_ENCODING);

		} catch (Throwable e) {

			e.printStackTrace();
		}
	}

	ByteArrayOutputStream bpBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream bpeBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream digBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream dsdBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream dssBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream eventBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream lpsdBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream lpssBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream pingBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream pingOessBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream ratioBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream reBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream ratioOessBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream reOessBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream rpsdBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream rpssBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream teBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream tedOessBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream tedBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream vrBaos = new ByteArrayOutputStream();
	ByteArrayOutputStream timeoutOessBaos = new ByteArrayOutputStream();

	ArrayList<Pair<String, ByteArrayOutputStream>> buffers;

	private String currentUrl;

	/** if true, data can be sent; otherwise, no data sent to server */
	private boolean canSendData;

	private long lastFailTime = -1;

	private boolean tedInProgress = false;

	private boolean hasConfig = false;
	
	boolean useGzip = false; // will be set to true only if server returns a gzip stream

	private static final boolean REPORT_ERROR = false;

	static final String DIG_HEADER = "type=dig&data=";
	static final String DSD_HEADER = "type=dsd&data=";
	static final String DSS_HEADER = "type=dss&data=";
	static final String EVENT_HEADER = "type=event&data=";
	static final String LPSD_HEADER = "type=lpsd&data=";
	static final String LPSS_HEADER = "type=lpss&data=";
	static final String PING_HEADER = "type=ping&data=";
	static final String PING_HEADER_OESS = "type=pingoess&data=";
	static final String TIMEOUT_HEADER_OESS = "type=timeoutoess&data=";
	static final String RATIO_HEADER = "";// "type=ratio&data=";
	static final String RE_HEADER = "type=re&data=";
	static final String RE_OESS_HEADER = "type=reoess&data=";
	static final String RPSD_HEADER = "type=rpsd&data=";
	static final String RPSS_HEADER = "type=rpss&data=";
	static final String TE_HEADER = "type=te&data=";
	static final String TE_OESS_HEADER = "type=teoess&data=";
	static final String TED_HEADER = "";// "type=ted&data=";
	static final String VR_HEADER = "type=vr&data=";
	static final String BP_HEADER = "type=brpe&data=";




	/**
	 * @return
	 */
	public synchronized static DatabaseReporter getInstance() {
		if (currentInstance == null
				|| (!(currentInstance instanceof PhpReporter) && !MainGeneric
						.isShuttingDown())) {
			currentInstance = new PhpReporter();
		}

		return currentInstance;
	}

	public PhpReporter() {
		canSendData = true;
		nf.setMaximumFractionDigits(4);

		urls.put(1.0, DEFAULT_SERVER);
		currentUrl = TESTING_GZIP ? TEST_GZIP_SERVER : DEFAULT_SERVER;

		buffers = new ArrayList<Pair<String, ByteArrayOutputStream>>();
		buffers.add(new Pair<String, ByteArrayOutputStream>(DIG_HEADER,
						digBaos));
		buffers.add(new Pair<String, ByteArrayOutputStream>(DSD_HEADER,
						dsdBaos));
		buffers.add(new Pair<String, ByteArrayOutputStream>(DSS_HEADER,
						dssBaos));
		buffers.add(new Pair<String, ByteArrayOutputStream>(EVENT_HEADER,
				eventBaos));
		buffers.add(new Pair<String, ByteArrayOutputStream>(LPSD_HEADER,
				lpsdBaos));
		buffers.add(new Pair<String, ByteArrayOutputStream>(LPSS_HEADER,
				lpssBaos));
		buffers.add(new Pair<String, ByteArrayOutputStream>(PING_HEADER,
				pingBaos));
		buffers.add(new Pair<String, ByteArrayOutputStream>(PING_HEADER_OESS,
				pingOessBaos));
		buffers.add(new Pair<String, ByteArrayOutputStream>(TIMEOUT_HEADER_OESS,
				timeoutOessBaos));
		buffers.add(new Pair<String, ByteArrayOutputStream>(RATIO_HEADER,
				ratioBaos));
		buffers.add(new Pair<String, ByteArrayOutputStream>(RE_HEADER, 
				reBaos));
		buffers.add(new Pair<String, ByteArrayOutputStream>(RE_OESS_HEADER, 
				reOessBaos));
		buffers.add(new Pair<String, ByteArrayOutputStream>(RPSD_HEADER,
				rpsdBaos));
		buffers.add(new Pair<String, ByteArrayOutputStream>(RPSS_HEADER,
				rpssBaos));
		buffers.add(new Pair<String, ByteArrayOutputStream>(TE_HEADER, 
				teBaos));
		buffers.add(new Pair<String, ByteArrayOutputStream>(TE_OESS_HEADER, 
				tedOessBaos));
		buffers
				.add(new Pair<String, ByteArrayOutputStream>(TED_HEADER,
						tedBaos));
		buffers.add(new Pair<String, ByteArrayOutputStream>(VR_HEADER, vrBaos));
		buffers.add(new Pair<String, ByteArrayOutputStream>(BP_HEADER, bpBaos));

		
		
		MainGeneric.createThread("WebServiceFinder", new Runnable() {

			public void run() {
				getUrls();
			}
		});

		ITimer t = MainGeneric.createTimer("OnoWebServiceTimer");
		t.addPeriodicEvent(60 * 60 * 1000, new ITimerEventPerformer() {

			public void perform(ITimerEvent event) {
				getUrls();

			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#canConnect()
	 */
	public boolean canConnect() {
		return lastFailTime < 0
				|| (System.currentTimeMillis() - lastFailTime) > OnoConfiguration
						.getInstance().getDbReconnectInterval() * 1000;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#connect()
	 */
	public boolean connect() {
		if (!hasConfig) {
			hasConfig = true;
			sendData("type=config", null);
		}

		return true;
	}
	
	public void updateConfig(){
		sendData("type=config", null);
	}

	public void parseConfig(String data) {
		String items[] = data.split("\\;");
		OnoConfiguration oc = OnoConfiguration.getInstance();
		try {
			oc.setPeerUpdateIntervalSec(Integer.parseInt(items[0]));
			oc.setSendToDatabase(Boolean.parseBoolean(items[1]));
//			oc.setRecordStats(Boolean.parseBoolean(items[2]));
			oc.setDigFrequencySec(Integer.parseInt(items[3]));
			oc.setVivaldiFrequencySec(Integer.parseInt(items[4]));
			oc.setDoTraceroute(Boolean.parseBoolean(items[5]));
			oc.setCosSimThreshold(Double.parseDouble(items[6]));
			oc.setRatioUpdateFrequencySec(Integer.parseInt(items[7]));
			oc.setDbUpdateFreqSec(Integer.parseInt(items[8]));
			oc.setUpdateCheckFreqSec(Integer.parseInt(items[9]));
			if (items.length>10) POST_THRESHOLD = Integer.parseInt(items[10]);
			if (items.length>11) oc.setDropVivaldiPct(Integer.parseInt(items[11]));
			int i = 12;
			if (items.length>i) oc.setReportDigs(Boolean.parseBoolean(items[i])); i++;
			if (items.length>i) oc.setReportTraceRoutes(Boolean.parseBoolean(items[i])); i++;
			if (items.length>i) oc.setReportPings(Boolean.parseBoolean(items[i])); i++;
			if (items.length>i) oc.setReportRpss(Boolean.parseBoolean(items[i])); i++;
			if (items.length>i) oc.setReportRpsd(Boolean.parseBoolean(items[i])); i++;
			if (items.length>i) oc.setReportLpsd(Boolean.parseBoolean(items[i])); i++;
			if (items.length>i) oc.setReportLpss(Boolean.parseBoolean(items[i])); i++;
			if (items.length>i) oc.setReportDsd(Boolean.parseBoolean(items[i])); i++;
			if (items.length>i) oc.setReportDss(Boolean.parseBoolean(items[i])); i++;
			if (items.length>i) oc.setReportRatios(Boolean.parseBoolean(items[i])); i++;
			if (items.length>i) oc.setReportVivaldi(Boolean.parseBoolean(items[i])); i++;
			if (items.length>i) oc.setOessEnableDigger(Boolean.parseBoolean(items[i])); i++;
			if (items.length>i) oc.setOessEnableExperiment(Boolean.parseBoolean(items[i])); i++;
			if (items.length>i) oc.setOessUseDelegation(Boolean.parseBoolean(items[i])); i++;
			if (items.length>i) oc.setOessDnsTimeout(Integer.parseInt(items[i])); i++;
			if (items.length>i) oc.setBrpCosSimThresh(Double.parseDouble(items[i])); i++;
			if (items.length>i) oc.setBrpWeightThresh(Double.parseDouble(items[i])); i++;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#disconnect()
	 */
	public void disconnect() {
		// for (Pair<String, ByteArrayOutputStream> p : buffers){
		// if (p.getValue().size()>0){
		// sendData(p.getKey(), p.getValue());
		// }
		// }

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#doCreateTable(java.lang.String)
	 */
	public void doCreateTable(String s) throws SQLException, IOException,
			ClassNotFoundException {
		throw new RuntimeException("Not supported!");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#doInsert(java.lang.String,
	 *      java.lang.String)
	 */
	public int doInsert(String insert, String select) throws IOException,
			ClassNotFoundException, SQLException {
		throw new RuntimeException("Not supported!");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#getNextPosition()
	 */
	public int getNextPosition() {
		throw new RuntimeException("Not supported!");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#getRecordingInterval(java.lang.String)
	 */
	public int getRecordingInterval(String columnName) {
		throw new RuntimeException("Not supported!");
		// return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertDataTransferResult(long,
	 *      int, byte, int, int, int, int, double, double)
	 */
	public synchronized boolean insertDataTransferResult(long time,
			int experimentId, byte type, int size, int sourceId, int destId,
			int currentId, double raceResultPrimary, double raceResultSecondary) {
		throw new RuntimeException("Not supported!");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertDigResult(int,
	 *      int, int, int, long)
	 */
	public synchronized boolean insertDigResult(int customerId, int peerId,
			int edge1Id, int edge2Id, long time) {
		if (!OnoConfiguration.getInstance().isSendToDatabase()) return true;
		if (!OnoConfiguration.getInstance().isReportDigs()) return true;
		
		String out = customerId + ";" + peerId + ";" + edge1Id + ";" + edge2Id
				+ ";" + time + "\n";
		try {
			digBaos.write(DEFAULT_CHARSET.encode(out).array());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (digBaos.size() > POST_THRESHOLD) {
			long response = sendData(DIG_HEADER, digBaos);
			return response >= 0;
		} else
			return true;
	}

	private long sendData(String header, ByteArrayOutputStream data) {
		try {
			if (!canSendData)
				return -1;
			if (data == null)
				data = new ByteArrayOutputStream();
			String response = getResponse(header, data);

			if (response == null || response.length()==0){
				
			if (TESTING)
				System.out.println(response);
			

			if (response == null)
				return -1;
			}
			if (response.contains("Error")) {
				data.reset();
				return -1;
			} else if (response.contains("Fail")) {
				data.reset();
				updateLastFailTime();
				return -1;
			} else if (response.contains("Success")) {
				data.reset();
				return 0;
			} else if (response.length() > 0) {
				if (header.equals("type=config")) {
					parseConfig(response);
					return 0;
				} else {
					try {
						if (data != null && data.size() > 0)
							data.reset();
						// must be encoded as <length>:<number>
						String pair[] = response.split("\\:");
						if (pair.length != 2)
							return -1;
						int size = Integer.parseInt(pair[0]);
						if (pair[1].length() == size)
							return Integer.parseInt(pair[1]);
						else
							return -1;
					} catch (NumberFormatException e) {
						return -1;
					}
				}
			} else {
				return -1;
			}

		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if (TESTING)
				e.printStackTrace();
			
			updateLastFailTime();
		} finally {
			if (data!=null) data.reset();
		}
		return -1;
	}

	/**
	 * Update lastFailTime in a way that prevents a thundering herd by using randomness
	 */
	private void updateLastFailTime() {
		lastFailTime = System.currentTimeMillis() + 
			(long)(OnoConfiguration.getInstance().getDbReconnectInterval()*
					1000*(0.5-MainGeneric.getRandom().nextDouble()));

		hasConfig = false;
	}

	private String getResponse(String header, ByteArrayOutputStream data)
			throws IOException {

		if (!canConnect()) return null;
		URL url = null;
		URLConnection urlConn = null;
		InputStream is = null;

		int count = 0;
		while (true) {
			try {
				url = new URL(currentUrl);
				if (TESTING) {
					String h = header;
					if (header.contains("\n"))
						h = header.split("\\n")[0];
					if (h == null || h.length() == 0) {
						h = data.toString();
						if (h.contains("\n"))
							h = h.split("\\n")[0];
					}
					System.out.print("["
							+ DateFormat.getDateTimeInstance().format(
									new Date()) + "]" + " Connecting to: "
							+ url + "(" + h + ")");
				}
			} catch (MalformedURLException e2) {
				currentUrl = getNewRandomUrl(currentUrl);
				count++;
				if (count >= urls.size()) {
					updateLastFailTime();
					return null;
				}
				continue;
			}

			// URL connection channel.
			try {
				urlConn = url.openConnection();
				break;
			} catch (IOException e2) {
				currentUrl = getNewRandomUrl(currentUrl);
				count++;
				if (count >= urls.size()) {
					updateLastFailTime();
					return null;
				}
			}
		}

		lastFailTime = -1; // reset failure time since we are successful
		try {
			byte headerB[] = DEFAULT_CHARSET.encode(header).array();
			byte dataB[] = data.toByteArray();
			int rawLength = headerB.length+dataB.length;
			// Let the run-time system (RTS) know that we want input.
			urlConn.setDoInput(true);
			// Let the RTS know that we want to do output.
			urlConn.setDoOutput(true);
			// No caching, we want the real thing.
			urlConn.setUseCaches(false);
			// Specify the content type.
			urlConn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			urlConn.setRequestProperty("accept-encoding",
                    "gzip");
			// java gzip implementation wastes more space than it saves if size 
			// is less than 150
			if (rawLength>=150 && useGzip)urlConn.setRequestProperty("Content-Encoding",
            "gzip");
			
			// Send POST output.
			OutputStream os = urlConn.getOutputStream();
			
			if (useGzip){
				//os.write(DEFAULT_CHARSET.encode("enc=gzip&zd=").array());
//				int level = -1;
				if (rawLength<150){
//					os = new OnoGZIPOutputStream(os, 512, Deflater.BEST_COMPRESSION);
				}
				else os = new GZIPOutputStream(os);
			}
			
			if (TESTING) System.out.print("[Sending "+(headerB.length+dataB.length)+" bytes] ");
			os.write(headerB);
			if (data.size() > 0)
				os.write(dataB);

			if (useGzip && os instanceof GZIPOutputStream) ((GZIPOutputStream)os).finish();
			os.flush();		
			os.close();
			
			// Get response data.
			
			//System.out.println("Header: \n" + urlConn.getHeaderFields());
			for (Entry<String, List<String>> ent : urlConn.getHeaderFields().entrySet() ){
				for (String s : ent.getValue()){
					if (s.equals("gzip")) {
						useGzip = true;
						break;
					}
					if (useGzip) break;
				}
			}
			is = urlConn.getInputStream();
			if (useGzip) is = new GZIPInputStream(is);
			StringBuffer sb = new StringBuffer();
			byte b[] = new byte[1];
			boolean eof = false;
			int readCount = 0;
			while (!eof) {
				int c = is.read(b);
				if (c < 0)
					break;
				sb.append((char) b[0]);
				readCount++;
				//System.out.println(readCount);
				if (readCount > 15000) {
					System.out.println("Something wrong!?");
					break;
				}
			}
			is.close();
			return sb.toString();
		} finally {
			if (TESTING) System.out.println(" [Done]");
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private String getNewRandomUrl(String oldUrl) {
		if (TESTING_GZIP) return TEST_GZIP_SERVER;
		if (SKIP_GET_URLS) return DEFAULT_SERVER;
		useGzip = false;
		String thisUrl = oldUrl;
		if (urls.size() <= 1)
			return thisUrl;
		else {
			do {
				double val = MainGeneric.getRandom().nextDouble();
				for (Entry<Double, String> ent : urls.entrySet()) {
					if (val <= ent.getKey())
						thisUrl = ent.getValue();
				}
			} while (!thisUrl.equals(oldUrl));
		}
		return thisUrl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertDownloadStatDynamic(int,
	 *      int, long, int, float, int, int, int, int, int, int, int, int, int)
	 */
	public synchronized boolean insertDownloadStatDynamic(int selfId,
			int downloadId, long timeRecorded, int numPeers,
			float availability, int completed, int discarded, int downloadRate,
			int uploadRate, int shareRatio, int numKnownNonSeeds,
			int numKnownSeeds, int state, int priority,
			long downloaded, long uploaded, int position, int numOnoPeers,
			int totalNumKnownNonSeeds, int totalNumKnownSeeds, long hashFails) {
		
		if (!OnoConfiguration.getInstance().isSendToDatabase()) return true;
		if (!OnoConfiguration.getInstance().isReportDsd()) return true;
		
		ByteArrayOutputStream baos = dsdBaos;
		String out = selfId + ";" + downloadId + ";" + timeRecorded + ";"
				+ numPeers + ";" + formatDouble(availability) + ";" + completed
				+ ";" + discarded + ";" + downloadRate + ";" + uploadRate + ";"
				+ shareRatio + ";" + numKnownNonSeeds + ";" + numKnownSeeds
				+ ";" + state + ";" + priority + ";" + position 
				+ ";" + downloaded + ";" + uploaded + ";" + numOnoPeers 
				+ ";" + totalNumKnownNonSeeds + ";" + totalNumKnownSeeds 
				+ ";" + hashFails + "\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(out).array());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (baos.size() > POST_THRESHOLD) {
			long response = sendData(DSD_HEADER, baos);
			return response >= 0;
		} else
			return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertDownloadStatSummary(int,
	 *      int, long, long, long, int, int, long, long, long, long)
	 */
	public synchronized void insertDownloadStatSummary(int selfId,
			int downloadId, long currentTime, long downloadingTime,
			long seedingTime, int maxDownloadRateLimit, int maxUploadLimit,
			long totalUploaded, long totalDownloaded, long size, long pieceSize) {
		if (!OnoConfiguration.getInstance().isSendToDatabase()) return;
		if (!OnoConfiguration.getInstance().isReportDss()) return;
		
		ByteArrayOutputStream baos = dssBaos;
		String out = selfId + ";" + downloadId + ";" + currentTime + ";"
				+ downloadingTime + ";" + seedingTime + ";"
				+ maxDownloadRateLimit + ";" + maxUploadLimit + ";"
				+ totalUploaded + ";" + totalDownloaded + ";" + size + ";"
				+ pieceSize + "\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(out).array());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (baos.size() > POST_THRESHOLD) {
			sendData(DSS_HEADER, baos);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertEvent(int, int,
	 *      int, int, long, int)
	 */
	public synchronized boolean insertEvent(int customerId, int edgeId,
			int peerId, int eventType, long time, int timeZoneId) {
		
		if (!OnoConfiguration.getInstance().isSendToDatabase()) return true;
		if (!OnoConfiguration.getInstance().isReportDigs()) return true;
		
		ByteArrayOutputStream baos = eventBaos;
		String out = customerId + ";" + edgeId + ";" + peerId + ";" + eventType
				+ ";" + time + ";" + timeZoneId + "\n";

		try {
			baos.write(DEFAULT_CHARSET.encode(out).array());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (baos.size() > POST_THRESHOLD) {
			long response = sendData(EVENT_HEADER, baos);
			return response >= 0;
		} else
			return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertLocalPeerStatDynamic(int,
	 *      long, int, int)
	 */
	public synchronized boolean insertLocalPeerStatDynamic(int selfId,
			long currentTime, int uploadRate, int downloadRate, 
			int maxDownloadRate, int maxUploadRate, boolean isAutoSpeed, int rst, 
			int numConnections, int passiveOpens, int activeOpens, int failedConns) {

		if (!OnoConfiguration.getInstance().isSendToDatabase()) return true;
		if (!OnoConfiguration.getInstance().isReportLpsd()) return true;
		
		ByteArrayOutputStream baos = lpsdBaos;
		String out = selfId + ";" + currentTime + ";" + uploadRate + ";"
				+ downloadRate + ";"+ 
				maxDownloadRate + ";" + maxUploadRate + ";" + 
				(isAutoSpeed?1:0) + ";" + rst  + ";" + numConnections 
				+ ";" + passiveOpens+ ";" + activeOpens+ ";" + failedConns+ "\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(out).array());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (baos.size() > POST_THRESHOLD) {
			long response = sendData(LPSD_HEADER, baos);
			return response >= 0;
		} else
			return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertLocalPeerStatStatic(int,
	 *      long, int, int)
	 */
	public synchronized void insertLocalPeerStatStatic(int selfId, long uptime,
			int maxUploadRate, int maxDownloadRate) {
		
		if (!OnoConfiguration.getInstance().isSendToDatabase()) return;
		if (!OnoConfiguration.getInstance().isReportLpss()) return;
		
		ByteArrayOutputStream baos = lpssBaos;
		String out = selfId
				+ ";"
				+ uptime
				+ ";"
				+ maxUploadRate
				+ ";"
				+ maxDownloadRate
				+ ";"
				+ Util.currentGMTTime() + "\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(out).array());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (baos.size() > POST_THRESHOLD) {
			sendData(LPSS_HEADER, baos);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertPingResult(int,
	 *      int, int, int, float, long, int)
	 */
	public synchronized boolean insertPingResult(int edgeId, int myId,
			int destId, int middleId, float rtt, long time, int experimentId) {
		if (!OnoConfiguration.getInstance().isSendToDatabase()) return true;
		if (!OnoConfiguration.getInstance().isReportPings()) return true;
		
		ByteArrayOutputStream baos = pingBaos;
		String out = edgeId + ";" + myId + ";" + destId + ";" + middleId + ";"
				+ formatDouble(rtt) + ";" + time + ";" + experimentId + "\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(out).array());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (baos.size() > POST_THRESHOLD) {
			long response = sendData(PING_HEADER, baos);
			return response >= 0;
		} else
			return true;
	}

	public synchronized boolean insertPingResultOess(int sourceId, int destId,float rtt,  
			long time, int destType) {
		if (!OnoConfiguration.getInstance().isSendToDatabase()) return true;
		if (!OnoConfiguration.getInstance().isReportPings()) return true;
	
//		System.out.println("INSERTAR PINGOESS " + " sourceId: " + sourceId + " destId: " + destId +  
//				" rtt: " + rtt + " time: " + time + ";" + " destType: " + destType);

		ByteArrayOutputStream baos = pingOessBaos;
		String out = sourceId + ";" + destId + ";" 
				+ formatDouble(rtt) + ";" + time + ";" + destType + "\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(out).array());
//		System.out.println(out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		if (baos.size() > POST_THRESHOLD) {
			long response = sendData(PING_HEADER_OESS, baos);
			return response >= 0;
		} else
			return true;
		
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertRatio(long,
	 *      int, double)
	 */
	public synchronized void insertRatio(long entryId, int edgeClusterId,
			double value) {
		if (!OnoConfiguration.getInstance().isSendToDatabase()) return;
		if (!OnoConfiguration.getInstance().isReportRatios()) return;
		
		ByteArrayOutputStream baos = ratioBaos;
		String out = edgeClusterId + ";" + formatDouble(value) + "\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(out).array());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// if (baos.size()>POST_THRESHOLD){
		// sendData(RATIO_HEADER, baos);
		// }
	}
	
	
	public synchronized void insertBranchPoint(long entryId, int branchPointId,
			double cossim) {
		if (!OnoConfiguration.getInstance().isSendToDatabase()) return;
		if (!OnoConfiguration.getInstance().isReportRatios()) return;
		
		ByteArrayOutputStream baos = bpBaos;
		String out = branchPointId + ";" + formatDouble(cossim) + "\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(out).array());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized long insertBranchPointEntry(int myId, byte version, int numEntries) {
		if (!OnoConfiguration.getInstance().isSendToDatabase()) return 0;
		if (!OnoConfiguration.getInstance().isReportRatios()) return 0;

		ByteArrayOutputStream baos = bpBaos;
		
		if (baos.size() > POST_THRESHOLD) {
			long response = sendData("", baos);
		}
		
		String s;
		if (baos.size() == 0)
			s = "type=brpe&data=" + myId + ";"+version + ";"+numEntries+"\n";
		else
			s = myId + ";"+version + ";"+numEntries+"\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(s).array());
//			System.out.println(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public synchronized void insertRatioOess(long entryId, int dnsId, int edgeClusterId,
			double value) {
		if (!OnoConfiguration.getInstance().isSendToDatabase()) return;
		if (!OnoConfiguration.getInstance().isReportRatios()) return;
//		System.out.println("INSERTAR RATIOOES " + "id: " + entryId + " dns: " + dnsId + " cluster: " + edgeClusterId + " value: " + value);
		
		ByteArrayOutputStream baos = reOessBaos;
		String out = edgeClusterId + ";" + formatDouble(value) + "\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(out).array());
//			System.out.println(out);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized void insertTimeoutOess(int myDnsId, int myId, long timeout) {
		
//		System.out.println("INSERTAR TIMEOUTOESS " + " myDnsId: " + myDnsId + " myId: " + myId +  
//				" timeout: " + timeout);

		ByteArrayOutputStream baos = timeoutOessBaos;
		String out = myDnsId + ";" + myId + ";" + timeout + ";" + System.currentTimeMillis() + "\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(out).array());
//		System.out.println(out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		if (baos.size() > POST_THRESHOLD) {
			long response = sendData(TIMEOUT_HEADER_OESS, baos);
		}		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertRatioEntry(int,
	 *      int)
	 */
	public synchronized long insertRatioEntry(int myId, int customerId,
			int entries) {
		if (!OnoConfiguration.getInstance().isSendToDatabase()) return 0;
		if (!OnoConfiguration.getInstance().isReportRatios()) return 0;
		
		ByteArrayOutputStream baos = ratioBaos;

		if (baos.size() > POST_THRESHOLD) {
			long response = sendData("", baos);
		}

		String s;
		if (baos.size() == 0)
			s = "type=re&data=" + myId + ";" + customerId + ";" + entries
					+ "\n";
		else
			s = myId + ";" + customerId + ";" + entries + "\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(s).array());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public synchronized long insertRatioOessEntry(int myId, int myDns, int customerId,
			int entries) {
		if (!OnoConfiguration.getInstance().isSendToDatabase()) return 0;
		if (!OnoConfiguration.getInstance().isReportRatios()) return 0;
//		System.out.println("INSERTAR OESSENTRY " + "id: " + myId + " dns: " + myDns + " customer: " + customerId + " entries: " + entries);
		
		ByteArrayOutputStream baos = reOessBaos;

		if (baos.size() > POST_THRESHOLD) {
			long response = sendData("", baos);
		}

		String s;
		if (baos.size() == 0)
			s = "type=reoess&data=" + myId + ";" + myDns + ";" + customerId + ";" + entries
					+ "\n";
		else
			s = myId + ";" + myDns + ";" + customerId + ";" + entries + "\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(s).array());
//			System.out.println(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertRemotePeerStatDynamic(int,
	 *      int, long, int, int, int, int, boolean, double, boolean, boolean)
	 */
	public synchronized boolean insertRemotePeerStatDynamic(int selfId,
			int peerId, long currentTime, int bytesReceivedFromPeer,
			int bytesSentToPeer, int currentReceiveRate, int currentSendRate,
			boolean isSeed, double ping, boolean isInterested,
			boolean isInteresting, int discarded) {

		if (!OnoConfiguration.getInstance().isSendToDatabase()) return true;
		if (!OnoConfiguration.getInstance().isReportRpsd()) return true;
		
		ByteArrayOutputStream baos = rpsdBaos;
		String out = selfId + ";" + peerId + ";" + currentTime + ";"
				+ bytesReceivedFromPeer + ";" + bytesSentToPeer + ";"
				+ currentReceiveRate + ";" + currentSendRate + ";" + isSeed
				+ ";" + formatDouble(ping) + ";" + isInterested + ";"
				+ isInteresting + ";" + discarded + "\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(out).array());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (baos.size() > POST_THRESHOLD) {
			long response = sendData(RPSD_HEADER, baos);
			return response >= 0;
		} else
			return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertRemotePeerStatStatic(int,
	 *      int, int, long, int, int, boolean)
	 */
	public synchronized void insertRemotePeerStatStatic(int selfId, int peerId,
			int peerClient, long uptime, int tcpPort, int udpPort,
			boolean sameCluster, boolean bpSameCluster) {

		if (!OnoConfiguration.getInstance().isSendToDatabase()) return;
		if (!OnoConfiguration.getInstance().isReportRpss()) return;
		
		ByteArrayOutputStream baos = rpssBaos;
		String out = selfId
				+ ";"
				+ peerId
				+ ";"
				+ peerClient
				+ ";"
				+ uptime
				+ ";"
				+ tcpPort
				+ ";"
				+ udpPort
				+ ";"
				+ (sameCluster ? "1" : "0")
				+ ";"
				+ Util.currentGMTTime() 
				+ ";"
				+ (bpSameCluster ? "1" : "0")
				+ "\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(out).array());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (baos.size() > POST_THRESHOLD) {
			long response = sendData(RPSS_HEADER, baos);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertTraceEntry(int,
	 *      int, long)
	 */
	public synchronized long insertTraceEntry(int sourceId, int destId,
			long time, int entries) {

		if (!OnoConfiguration.getInstance().isSendToDatabase()) return 0;
		if (!OnoConfiguration.getInstance().isReportDigs()) return 0;
		
		ByteArrayOutputStream baos = tedBaos;

		if (baos.size() > POST_THRESHOLD) {
			long response = sendData("", baos);
		}

		String s;
		entries += 1;
		if (tedBaos.size() == 0)
			s = "type=te&data=" + sourceId + ";" + destId + ";" + time + ";"
					+ entries + "\n";
		else
			s = sourceId + ";" + destId + ";" + time + ";" + entries + "\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(s).array());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertTraceEntryData(long,
	 *      int[], edu.northwestern.ono.experiment.TraceRouteRunner.TraceEntry)
	 */
	public synchronized void insertTraceEntryData(long entryId,
			int[] myRouterIds, TraceEntry te) {

		if (!OnoConfiguration.getInstance().isSendToDatabase()) return;
		if (!OnoConfiguration.getInstance().isReportTraceRoutes()) return;
		
		ByteArrayOutputStream baos = tedBaos;
		StringBuffer sb = new StringBuffer();
		// sb.append(entryId);
		// sb.append(";");
		for (int i = 0; i < te.rtt.length; i++) {
			sb.append(myRouterIds[i]);
			sb.append(";");
			sb.append(formatDouble(te.rtt[i]));
			if (i == te.rtt.length - 1)
				sb.append("\n");
			else
				sb.append(";");
		}

		try {
			baos.write(DEFAULT_CHARSET.encode(sb.toString()).array());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// if (baos.size()>POST_THRESHOLD){
		// long response = sendData("", baos);
		// }
	}

	
	public synchronized long insertTraceEntryOess(int sourceId, int destId,
			long time, int entries, int destType) {

		if (!OnoConfiguration.getInstance().isSendToDatabase()) return 0;
		if (!OnoConfiguration.getInstance().isReportDigs()) return 0;
		
		ByteArrayOutputStream baos = tedOessBaos;
//		System.out.println("INSERTAR TRACEENTRYOESS " + "sourceId: " + sourceId + " destId: " + destId + " time: " + time + " entry: " + entries);
		
		if (baos.size() > POST_THRESHOLD) {
			long response = sendData("", baos);
		}

		String s;
		entries += 1;
		if (tedOessBaos.size() == 0)
			s = "type=teoess&data=" + sourceId + ";" + destId + ";" + time + ";"
					+ entries + ";" + destType + "\n";
		else
			s = sourceId + ";" + destId + ";" + time + ";" + entries + ";" + destType + "\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(s).array());
//			System.out.println(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public synchronized void insertTraceEntryDataOess(long entryId,
			int[] myRouterIds, TraceEntry te) {

		if (!OnoConfiguration.getInstance().isSendToDatabase()) return;
		if (!OnoConfiguration.getInstance().isReportTraceRoutes()) return;
		
//		System.out.println("INSERTAR TRACEENTRYDATAOESS " + "entryId: " + entryId + " myRouterIds: " + myRouterIds);
		ByteArrayOutputStream baos = tedOessBaos;
		StringBuffer sb = new StringBuffer();
		// sb.append(entryId);
		// sb.append(";");
		for (int i = 0; i < te.rtt.length; i++) {
			sb.append(myRouterIds[i]);
			sb.append(";");
			sb.append(formatDouble(te.rtt[i]));
			if (i == te.rtt.length - 1)
				sb.append("\n");
			else
				sb.append(";");
		}

		try {
			baos.write(DEFAULT_CHARSET.encode(sb.toString()).array());
//			System.out.println(sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// if (baos.size()>POST_THRESHOLD){
		// long response = sendData("", baos);
		// }

	}

	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertVivaldiResult(long,
	 *      int, int, float, float, float, float, float, float, float, byte)
	 */
	public synchronized boolean insertVivaldiResult(long timestamp, int peerId,
			int observedBy, float x, float y, float h, float estErr,
			float estRTT, float pingResult, float estRTTv2, float[] coordsV2, byte type) {

		
		if (!OnoConfiguration.getInstance().isSendToDatabase()) return true;
		if (!OnoConfiguration.getInstance().isReportVivaldi()) return true;
		
		ByteArrayOutputStream baos = vrBaos;
		String out = timestamp + ";" + peerId + ";" + observedBy + ";"
				+ formatDouble(x) + ";" + formatDouble(y) + ";"
				+ formatDouble(h) + ";" + formatDouble(estErr) + ";"
				+ formatDouble(estRTT) + ";" + formatDouble(pingResult) + ";"
				+ formatDouble(estRTTv2) + ";" + type + ";";
		if (coordsV2!=null){
			for (int i = 0; i < coordsV2.length-1; i++) out+= formatDouble(coordsV2[i])+";";
			 out+= formatDouble(coordsV2[coordsV2.length-1]);
		}
		out += "\n";
		try {
			baos.write(DEFAULT_CHARSET.encode(out).array());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (baos.size() > POST_THRESHOLD) {
			long response = sendData(VR_HEADER, baos);
			baos.reset();
			return response >= 0;
		} else
			return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#makeId(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	public int makeId(String tableName, String colName, String value) {
		String data = "type=makeId&data=" + tableName + ";" + colName + ";"
				+ value + "\n";
		return (int) sendData(data, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#insertDnsMapping(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	public boolean insertDnsMapping(int myId, int publicDns, int privateDns, String type, int order) {
		
		long timeRecorded = Util.currentGMTTime();
		String data = "type=dnsMapping2&data=" + myId + ";" + publicDns + ";"
				+ privateDns + ";" + type + ";" + order + ";" + timeRecorded + "\n";
		
		long response = sendData(data, null);
		return response >= 0;		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#registerUsage(int,
	 *      long, java.lang.String)
	 */
	public void registerUsage(int myId, long currentTime, String uuid) {
		String data = "type=registerUsage&data=" + myId + ";" + currentTime
				+ ";" + uuid + "&os="+getOsString()+"\n";
		sendData(data, null);

	}
	
	public String getOsString(){
		String name = System.getProperty("os.name");
		String version = System.getProperty("os.version");
		String arch = System.getProperty("os.arch");
		if (name != null){
			return name + ";" + version + ";"+arch;
		}
		else return "Unknown";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#reportError(java.lang.Exception)
	 */
	public void reportError(Exception e) {
		if (!REPORT_ERROR ) return;
		if (!OnoConfiguration.getInstance().isSendToDatabase()) return;
		String error = e.toString() + "\n";
		error += e.getCause() != null ? e.getCause().toString() + "\n" : e
				.getMessage()
				+ "\n";
		for (StackTraceElement ste : e.getStackTrace()) {
			error += ste.toString() + "\n";
		}

		String data = "type=error&data=" + error + "\n";
		sendData(data, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#setExtendedConnection(boolean)
	 */
	public void setExtendedConnection(boolean extend) {
		// this has no meaning in a web service

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.northwestern.ono.database.DatabaseReporter#stopReporting()
	 */
	public void stopReporting() {
		canSendData = false;

	}

	public synchronized void flush() {
		if (!OnoConfiguration.getInstance().isSendToDatabase()) return;
		for (Pair<String, ByteArrayOutputStream> p : buffers) {
			if (p.getValue().size() > 0) {
				sendData(p.getKey(), p.getValue());
			}
		}

	}

	public Map<String, Integer> makeIdBatch(String tableName,
			String columnName, List<String> values) {
		if (!canSendData)
			return null;
		if (values.size()==0) return null;
		long start = System.currentTimeMillis();
		HashMap<String, Integer> toReturn = new HashMap<String, Integer>();
		try {
			String data = "type=makeIds&data=" + tableName + ";" + columnName
					+ "\n";
			if (TESTING)
				System.out.println(tableName + ": " + values.size());
			int i = 0;
			for (String s : values) {
				data += s + "\n";
			}
			LinkedList<Integer> keys = sendDataBatch(data);
			if (keys == null)
				return null;

			Iterator<String> sIt = values.iterator();
			Iterator<Integer> iIt = keys.iterator();
			
			while (sIt.hasNext() && iIt.hasNext()) {
				int id = iIt.next();
				if (id == -1)
					sIt.next();
				else
					toReturn.put(sIt.next(), id);
			}
			return toReturn;
		} finally {
			long time = System.currentTimeMillis() - start;
			if (TESTING)
				System.out.println("Took " + time / 1000.0 + " seconds, found " 
						+ toReturn.size() + " entries...");
		}
	}

	private LinkedList<Integer> sendDataBatch(String data) {
		LinkedList<Integer> toReturn = new LinkedList<Integer>();
		int numFound = 0;
		String s="";
		try {

			s = getResponse(data, new ByteArrayOutputStream());
			if (s == null) return toReturn;
			String lines[] = s.split("\\n");
			
			for (String response : lines) {
				try {

					// must be encoded as <length>:<number>
					String pair[] = response.split("\\:");
					if (pair.length != 2)
						toReturn.add(-1);
					int size = Integer.parseInt(pair[0]);
					if (pair[1].length() == size){
						toReturn.add(Integer.parseInt(pair[1]));
						numFound++;
					}
					else
						toReturn.add(-1);
				} catch (NumberFormatException e) {
					toReturn.add(-1);
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			if (TESTING)
				e.printStackTrace();
		}
		if (TESTING && numFound==0){
			System.err.println(s);
		}
		return toReturn;
	}

	/**
	 * 
	 */
	private void getUrls() {

		if (SKIP_GET_URLS) return;
		Properties servers = MainGeneric.downloadFromURL(SERVER_LIST_LOC,
				30 * 1000);
		for (Entry<Object, Object> ent : servers.entrySet()) {
			try {
				String pair[] = ((String) ent.getValue()).split(";");
				urls.put(Double.parseDouble(pair[1]), pair[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		// remove default if there's more than one server and its weight wasn't
		// changed
		if (SKIP_GET_URLS) return;
		if (urls.size() > 1 && urls.get(1.0).equals(DEFAULT_SERVER))
			urls.remove(1.0);
		// if (!TESTING){
		double val = MainGeneric.getRandom().nextDouble();
		for (Entry<Double, String> ent : urls.entrySet()) {
			if (val <= ent.getKey())
				if (!TESTING_GZIP){
					useGzip = false;
					currentUrl = ent.getValue();
					if (TESTING){
						System.out.println("New URL: "+currentUrl);
						System.out.println("Val: "+val+" Ent:"+ent);
					}
					break;
				}
		}
		// }
	}

	private String formatDouble(double d) {
		String s = Double.toString(d);
		if (s.length() > 10) {
			if (s.contains("E")) {
				return s.substring(0, 7) + s.substring(s.indexOf('E'));
			} else
				return s.substring(0, 9);
		}
		return s;
	}

	public void reportIpChange(int oldIp, int newIp, int localIp,
			long currentGMTTime) {

		String data = "type=ipchange&data=" + oldIp +";" 
		+ newIp +";"
		+ localIp +";" +
		currentGMTTime+ "\n";
		sendData(data, null);
		
	}

}
