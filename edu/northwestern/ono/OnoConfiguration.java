/**
 * Ono Project
 *
 * File:         OnoConfiguration.java
 * RCS:          $Id: OnoConfiguration.java,v 1.28 2011/07/06 15:45:53 mas939 Exp $
 * Description:  OnoConfiguration class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Apr 26, 2007 at 9:02:05 AM
 * Language:     Java
 * Package:      edu.northwestern.ono
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
package edu.northwestern.ono;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;
import java.util.Map.Entry;

import edu.northwestern.ono.brp.BRPPeerManager;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The OnoConfiguration class ...
 */
public class OnoConfiguration {
	private static final boolean DEBUG = false;

	private static OnoConfiguration self;
	
	boolean recordStats = true;
	boolean sendToDatabase = true;
	int peerUpdateIntervalSec = 60;
	int digFrequencySec = 60;
	int vivaldiFrequencySec = 60;
	boolean doTraceroute = true;
	double cosSimThreshold = 0.2;
	int ratioUpdateFrequencySec = 600;
	int dbUpdateFreqSec = 3600;
	int updateCheckFreqSec = 43200;
	boolean doRemoteDig = true;
	int maxPeers = 100;
	String uuid = null;
	
	private int dbReconnectInterval = 15*60; // 10 minutes to retry

	private int reportingTimeout = 60;

	private int maxPings = 10;

	private boolean streamDebug = false;

	private boolean runExperiments;

	private boolean visualize=false;
	
	/** if true, will increase dig rate incrementally */
	private boolean adaptiveDig = false;
	
	/** initial dig interval (s) */
	private long digStart = 30;
	
	/** longest period between successive digs */
	private long digMaxInterval = 30*60;
	
	/** increment for dig interval */
	private long digIncrement = 60;

	private int dropVivaldiPct = 0;
	
	boolean reportDigs = true;
	boolean reportTraceRoutes = true;
	boolean reportPings = true;
	boolean reportRpss = true;
	boolean reportRpsd = true;
	boolean reportLpsd = true;
	boolean reportLpss = true;
	boolean reportDsd = true;
	boolean reportDss = true;
	boolean reportRatios = true;
	boolean reportVivaldi = true;
	
	/** the path to the file where whitelists for biased peers are located */
	String cidrFile = null;
	

	/** ana **/
	boolean oessEnableDigger = false;
	boolean oessEnableExperiment = false;
	boolean oessUseDelegation = false;
	/** Dns interval Timeout in seconds */
	int oessDnsTimeout = 0;
	
	/** BRP **/
	public boolean enableBRP = true;
	public int BRPVersion = 1;
	public boolean logBRP = false;
	public boolean printBRP = false;

	private double brpCosSimThresh = BRPPeerManager.COS_SIM_THRESHOLD_DEFAULT;

	private double brpWeightThresh = BRPPeerManager.BP_WEIGHT_THRESHOLD_DEFAULT;
	
	private int messageManagerInterval = 60*60; //60*60 seconds = 1 hour
	private boolean enablePopups = true;
	private int maxLogEntries = 200;
	
	public int getDbUpdateFreqSec() {
		return dbUpdateFreqSec;
	}


	public void setDbUpdateFreqSec(int dbUpdateFreqSec) {
		this.dbUpdateFreqSec = dbUpdateFreqSec;
	}


	public boolean isDoRemoteDig() {
		return doRemoteDig;
	}


	public void setDoRemoteDig(boolean doRemoteDig) {
		this.doRemoteDig = doRemoteDig;
	}


	public int getMaxPeers() {
		return maxPeers;
	}


	public void setMaxPeers(int maxPeers) {
		this.maxPeers = maxPeers;
	}


	public int getUpdateCheckFreqSec() {
		return updateCheckFreqSec;
	}


	public void setUpdateCheckFreqSec(int updateCheckFreqSec) {
		this.updateCheckFreqSec = updateCheckFreqSec;
	}


	public synchronized static OnoConfiguration getInstance(){
		if (self==null){
			self = new OnoConfiguration();
		}
		return self;
	}


	public double getCosSimThreshold() {
		return cosSimThreshold;
	}


	public void setCosSimThreshold(double cosSimThreshold) {
		this.cosSimThreshold = cosSimThreshold;
	}


	public int getDigFrequencySec() {
		return adaptiveDig ? (int)digStart : digFrequencySec;
	}


	public void setDigFrequencySec(int digFrequencySec) {
		this.digFrequencySec = digFrequencySec;
	}


	public boolean isDoTraceroute() {
		return doTraceroute;
	}


	public void setDoTraceroute(boolean doTraceroute) {
		this.doTraceroute = doTraceroute;
	}


	public int getPeerUpdateIntervalSec() {
		return peerUpdateIntervalSec;
	}


	public void setPeerUpdateIntervalSec(int peerUpdateIntervalSec) {
		this.peerUpdateIntervalSec = peerUpdateIntervalSec;
	}


	public int getRatioUpdateFrequencySec() {
		return ratioUpdateFrequencySec;
	}


	public void setRatioUpdateFrequencySec(int ratioUpdateFrequencySec) {
		this.ratioUpdateFrequencySec = ratioUpdateFrequencySec;
	}


	public boolean isRecordStats() {
		return recordStats;
	}


	public void setRecordStats(boolean recordStats) {
		this.recordStats = recordStats;
	}


	public boolean isSendToDatabase() {
		return sendToDatabase;
	}


	public void setSendToDatabase(boolean sendToDatabase) {
		this.sendToDatabase = sendToDatabase;
	}


	public int getVivaldiFrequencySec() {
		return vivaldiFrequencySec;
	}


	public void setVivaldiFrequencySec(int vivaldiFrequencySec) {
		this.vivaldiFrequencySec = vivaldiFrequencySec;
	}


	public void writeProperties(Properties props, ArrayList<String> locations) {
		props.put("ono.enableStats", recordStats ? "true" : "false");
		props.put("ono.stats.dbUpdate", dbUpdateFreqSec+"" );
		props.put("ono.update.updateCheck", updateCheckFreqSec+"");
		props.put("ono.digger.sleepTime", digFrequencySec+"");
		props.put("ono.digger.maxPeers", maxPeers+"");
		props.put("ono.digger.digForOthers", doRemoteDig+"");
		props.put("ono.cossim", cosSimThreshold+"");
		props.put("ono.doTraceroute", doTraceroute ? "true" : "false");
		props.put("ono.stats.peerUpdate", peerUpdateIntervalSec+"");
		props.put("ono.vivaldiUpdate", vivaldiFrequencySec+"");
		props.put("ono.position.ratioUpdate", ratioUpdateFrequencySec+"");
		props.put("ono.reportingTimeout", reportingTimeout+"");
		props.put("ono.maxPings", maxPings+"");
		props.put("ono.uuid", uuid);
		props.put("ono.streamDebug", streamDebug ? "true" : "false");
		props.put("ono.runExperiments", runExperiments ? "true" : "false");
		props.put("ono.visualize", visualize ? "true" : "false");
		props.put("ono.digger.adaptive", adaptiveDig ? "true" : "false");
		props.put("ono.digger.digStartInterval", digStart+"");
		props.put("ono.digger.digMaxInterval", digMaxInterval+"");
		props.put("ono.digger.digIncrement", digIncrement+"");
		props.put("ono.whitelist.path", cidrFile+"");
		props.put("ono.brp.enable", enableBRP+"");
		props.put("ono.brp.version", BRPVersion+"");
		props.put("ono.brp.log", logBRP+"");
		props.put("ono.brp.print", printBRP+"");
		props.put("ana.oessdigger.enableDigger", oessEnableDigger ? "true" : "false");
		props.put("ana.oessexperiment.enableExperiment", oessEnableExperiment ? "true" : "false");
		props.put("ana.oessdigger.useDelegation", oessUseDelegation ? "true" : "false");
		props.put("ana.oessdigger.dnsTimeout", oessDnsTimeout+"");
		props.put("brp.cosSimThresh", brpCosSimThresh+"");
		props.put("brp.weightThresh", brpWeightThresh+"");
		props.put("ono.messageManagerInterval", messageManagerInterval+"");
		props.put("ono.enablePopups", enablePopups ? "true" : "false");
		props.put("ono.maxLogEntries", maxLogEntries+"");
		

		if (cidrFile!=null && !cidrFile.equals("null") && cidrFile.length()>0) 
			props.put("ono.whitelist.path", cidrFile+"");
		else props.remove("ono.whitelist.path");

		for (String filename : locations){
			try {
				FileOutputStream fos = new FileOutputStream(filename);
				props.store(fos, "");
				break;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}


	public void loadProperties(Properties props) {

		for (Entry<Object, Object> ent : props.entrySet()){
			try {
			String key = (String)ent.getKey();
			String value = (String)ent.getValue();
			if (key.equals("ono.enableStats")){
				recordStats= value.equals("true") ? true : false;
			} else if (key.equals("ono.stats.dbUpdate")){
				dbUpdateFreqSec = Integer.valueOf(value);
			} else if (key.equals("ono.update.updateCheck")){
				updateCheckFreqSec = Integer.valueOf(value);
			}else if (key.equals("ono.digger.sleepTime")){
				digFrequencySec= Integer.valueOf(value);
			}else if (key.equals("ono.digger.maxPeers")){
				maxPeers = Integer.valueOf(value);
			}else if (key.equals("ono.digger.digForOthers")){
				doRemoteDig= value.equals("true") ? true : false;
			}else if (key.equals("ono.cossim")){
				cosSimThreshold = Double.parseDouble(value);
			}else if (key.equals("ono.doTraceroute")){
				doTraceroute= value.equals("true") ? true : false;
			}else if (key.equals("ono.stats.peerUpdate")){
				peerUpdateIntervalSec = Integer.valueOf(value);
			}else if (key.equals("ono.vivaldiUpdate")){
				vivaldiFrequencySec = Integer.valueOf(value);
			}else if (key.equals("ono.position.ratioUpdate")){
				ratioUpdateFrequencySec = Integer.valueOf(value);
			}else if (key.equals("ono.reportingTimeout")){
				reportingTimeout = Integer.valueOf(value);
			} else if (key.equals("ono.maxPings")){
				maxPings = Integer.valueOf(value);
			} else if (key.equals("ono.uuid")){
				uuid = value;
			} else if (key.equals("ono.whitelist.path") && !value.equals("null")){
				cidrFile = value;
			} else if (key.equals("ono.streamDebug")){
				streamDebug = value.equals("true") ? true : false;;
			} else if (key.equals("ono.runExperiments")){
				runExperiments = value.equals("true") ? true : false;
			} else if (key.equals("ono.visualize")){
				visualize = value.equals("true") ? true : false;
			} else if (key.equals("ono.digger.adaptive")){
				adaptiveDig = value.equals("true") ? true : false;
			} else if (key.equals("ono.digger.digStartInterval")){
				digStart = Integer.valueOf(value);
			} else if (key.equals("ono.digMaxInterval")){
				digMaxInterval = Integer.valueOf(value);
			} else if (key.equals("ono.digIncrement")){
				digIncrement = Integer.valueOf(value);
			} else if (key.equals("ana.oessdigger.useDelegation")){
				oessUseDelegation = value.equals("true") ? true : false;
			} else if (key.equals("ana.oessmanager.enableManager")){
				oessEnableExperiment = value.equals("true") ? true : false;
			} else if (key.equals("ana.oessdigger.enableDigger")){
				oessEnableDigger = value.equals("true") ? true : false;
			} else if (key.equals("ono.oessdigger.dnsTimeout")){
				oessDnsTimeout = Integer.valueOf(value);
			} else if (key.equals("ono.brp.enable")){
				enableBRP = value.equals("true") ? true : false;
			} else if (key.equals("ono.brp.log")) {
				logBRP = value.equals("true") ? true : false;
			} else if (key.equals("ono.brp.print")) {
				printBRP = value.equals("true") ? true : false;
			} else if (key.equals("ono.brp.version")) {
				BRPVersion = Integer.valueOf(value);
			} else if (key.equals("brp.cosSimThresh")){
				brpCosSimThresh = Double.parseDouble(value);
				BRPPeerManager.COS_SIM_THRESHOLD = (float)brpCosSimThresh;				
			} else if (key.equals("brp.weightThresh")){
				brpWeightThresh = Double.parseDouble(value);
				BRPPeerManager.BP_WEIGHT_THRESHOLD = (float)brpWeightThresh;
			} else if (key.equals("ono.enablePopups")){
				enablePopups= value.equals("true") ? true : false;
			}else {
				if (DEBUG) System.err.println("Unknown key: "+key);
			}
			} catch (Exception e){
				System.err.println("Error processing property...");
				e.printStackTrace();
			}
		} // end for
		
		// if no uuid, generate it now
		if (uuid==null){
			uuid = UUID.randomUUID().toString();
			props.put("ono.uuid", uuid);
			
			writeProperties(props, MainGeneric.getPropertiesLocations());
		}
		
		
		
	}


	public int getDbReconnectInterval() {
		// TODO Auto-generated method stub
		return dbReconnectInterval;
	}

	public int getOessDnsTimeout() {
		// TODO Auto-generated method stub
		return oessDnsTimeout;
	}
	
	public void setDbReconnectInterval(int dbReconnectInterval) {
		this.dbReconnectInterval = dbReconnectInterval;
	}


	public void setReportingTimeout(int timeout) {
		this.reportingTimeout = timeout;
		
	}

	public int getReportingTimeout() {
		return reportingTimeout;
	}

	public void setOessDnsTimeout(int value) {
		this.oessDnsTimeout = value;
		
	}

	public void setMaxPings(int value) {
		this.maxPings = value;
		
	}
	
	public int getMaxPings(){
		return maxPings;
	}


	public String getUUID() {
		return uuid;
	}
	
	public void setUUID(String uuid){
		this.uuid = uuid;
	}


	public boolean isStreamDebug() {
		// TODO Auto-generated method stub
		return this.streamDebug;
	}
	
	public void setStreamDebug(boolean streamDebug){
		this.streamDebug = streamDebug;
	}


	public boolean isRunExperiments() {
		// TODO Auto-generated method stub
		return runExperiments;
	}
	
	public void setRunExperiments(boolean runExperiments){
		this.runExperiments = runExperiments;
	}


	public boolean isVisualize() {
		// TODO Auto-generated method stub
		return visualize;
	}


	public boolean isAdaptiveDig() {
		return adaptiveDig;
	}


	public void setAdaptiveDig(boolean adaptiveDig) {
		this.adaptiveDig = adaptiveDig;
	}


	public long getDigStart() {
		return digStart;
	}


	public void setDigStart(long digStart) {
		this.digStart = digStart;
	}


	public long getDigMaxInterval() {
		return digMaxInterval;
	}


	public void setDigMaxInterval(long digMaxInterval) {
		this.digMaxInterval = digMaxInterval;
	}


	public long getDigIncrement() {
		return digIncrement;
	}


	public void setDigIncrement(long digIncrement) {
		this.digIncrement = digIncrement;
	}


	public void setDropVivaldiPct(int dropVivaldiPct) {
		this.dropVivaldiPct = dropVivaldiPct;
		
	}
	
	public int getDropVivaldiPct(){
		return dropVivaldiPct;
	}


	public boolean isReportDigs() {
		return reportDigs;
	}


	public void setReportDigs(boolean reportDigs) {
		this.reportDigs = reportDigs;
	}


	public boolean isReportTraceRoutes() {
		return reportTraceRoutes;
	}


	public void setReportTraceRoutes(boolean reportTraceRoutes) {
		this.reportTraceRoutes = reportTraceRoutes;
	}


	public boolean isReportPings() {
		return reportPings;
	}


	public void setReportPings(boolean reportPings) {
		this.reportPings = reportPings;
	}


	public boolean isReportRpss() {
		return reportRpss;
	}


	public void setReportRpss(boolean reportRpss) {
		this.reportRpss = reportRpss;
	}


	public boolean isReportRpsd() {
		return reportRpsd;
	}


	public void setReportRpsd(boolean reportRpsd) {
		this.reportRpsd = reportRpsd;
	}


	public boolean isReportLpsd() {
		return reportLpsd;
	}


	public void setReportLpsd(boolean reportLpsd) {
		this.reportLpsd = reportLpsd;
	}


	public boolean isReportLpss() {
		return reportLpss;
	}


	public void setReportLpss(boolean reportLpss) {
		this.reportLpss = reportLpss;
	}


	public boolean isReportDsd() {
		return reportDsd;
	}


	public void setReportDsd(boolean reportDsd) {
		this.reportDsd = reportDsd;
	}


	public boolean isReportDss() {
		return reportDss;
	}


	public void setReportDss(boolean reportDss) {
		this.reportDss = reportDss;
	}


	public boolean isReportRatios() {
		return reportRatios;
	}


	public void setReportRatios(boolean reportRatios) {
		this.reportRatios = reportRatios;
	}


	public boolean isReportVivaldi() {
		return reportVivaldi;
	}


	public void setReportVivaldi(boolean reportVivaldi) {
		this.reportVivaldi = reportVivaldi;
	}


	public String getCidrFile() {
		return cidrFile;
	}


	public void setCidrFile(String cidrFile) {
		this.cidrFile = cidrFile;
	}
	
	public void setOessEnableDigger(boolean oessEnableDigger) {
		this.oessEnableDigger = oessEnableDigger;
	}

	public void setOessEnableExperiment(boolean oessEnableExperiment) {
		this.oessEnableExperiment = oessEnableExperiment;
	}

	public void setOessUseDelegation(boolean oessUseDelegation) {
		this.oessUseDelegation = oessUseDelegation;
	}

	public boolean isOessEnableDigger() {
		return oessEnableDigger;
	}

	public boolean isOessEnableExperiment() {
		return oessEnableExperiment;
	}

	public boolean isOessUseDelegation() {
		return oessUseDelegation;
	}


	public void setBrpCosSimThresh(double brpCosSimThresh) {
		this.brpCosSimThresh  = brpCosSimThresh;
		BRPPeerManager.COS_SIM_THRESHOLD = (float)brpCosSimThresh;
	}


	public void setBrpWeightThresh(double brpWeightThresh) {
		this.brpWeightThresh = brpWeightThresh;
		BRPPeerManager.BP_WEIGHT_THRESHOLD = (float)brpWeightThresh;
	}
	
	public int getMessageManagerInterval() {
		return messageManagerInterval ;
	}
	
	public void setMessageManagerInterval(int interval){
		this.messageManagerInterval = interval;
	}
	
	public boolean isEnablePopups() {
		return enablePopups;
	}

	public void setEnablePopups(boolean enablePopups) {
		this.enablePopups = enablePopups;
	}

	public int getMaxLogEntries() {
		return maxLogEntries ;
	}
	
	public void setMaxLogEntries(int entries){
		this.maxLogEntries = entries;
	}

	
}
