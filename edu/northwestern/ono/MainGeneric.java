/**
 * Ono Project
 *
 * File:         MainGeneric.java
 * RCS:          $Id: MainGeneric.java,v 1.51 2011/07/06 15:45:53 mas939 Exp $
 * Description:  MainGeneric class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 22, 2007 at 5:11:14 PM
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;
import java.util.Map.Entry;

import edu.northwestern.ono.api.TransferManager;
import edu.northwestern.ono.connection.IOnoConnectionManager;
import edu.northwestern.ono.connection.standard.StandardConnectionManager;
import edu.northwestern.ono.database.DatabaseReporter;
import edu.northwestern.ono.database.MySQLReporter;
import edu.northwestern.ono.database.NullReporter;
import edu.northwestern.ono.database.PhpReporter;
import edu.northwestern.ono.dht.IDistributedDatabase;
import edu.northwestern.ono.dns.Digger;
import edu.northwestern.ono.experiment.OessExperimentManager;
import edu.northwestern.ono.experiment.OnoExperiment;
import edu.northwestern.ono.experiment.OnoExperimentManager;
import edu.northwestern.ono.experiment.TraceRouteRunner;
import edu.northwestern.ono.position.CDNClusterFinder;
import edu.northwestern.ono.position.OnoPeerManager;
import edu.northwestern.ono.position.PeerFinder;
import edu.northwestern.ono.position.PeerFinderGlobal;
import edu.northwestern.ono.stats.EdgeServerRatio;
import edu.northwestern.ono.stats.StatManager;
import edu.northwestern.ono.stats.Statistics;
import edu.northwestern.ono.timer.ITimer;
import edu.northwestern.ono.timer.ITimerEvent;
import edu.northwestern.ono.timer.ITimerEventPerformer;
import edu.northwestern.ono.util.ILoggerChannel;
import edu.northwestern.ono.util.OnoPluginInterface;
import edu.northwestern.ono.util.PingManager;
import edu.northwestern.ono.util.PluginInterface;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The MainGeneric class ...
 */
public class MainGeneric {
	 public static final int RD_SIZE_RETRIES = 3;
	    public static final int RD_SIZE_TIMEOUT = 10000;

	    protected static boolean isServer;

	    protected Properties props;

	    /** determines whether active network positioning is performed */
	    protected static final boolean DO_NET_POS = true;
	    protected static final boolean TEST_EDGE_RATIO = false;
	    /** time in seconds before azureus stops reporting stats */
		private static final long MAX_SHUTDOWNTIME = 30;
		protected static final boolean DEBUG = false;
	    public static int EXPERIMENT_SLEEP_TIME = 15;
	    public static boolean RUN_EXPERIMENT_MANAGER = true;

	    private ArrayList<URL> urls = new ArrayList<URL>();
	   
	    Digger dig;
	    
	    protected static PluginInterface pi = new OnoPluginInterface();
		private static String type = "NotAzurues";
		private static String publicIp;
		private static Long nextIpUpdate = 0L;
		private static long ipUpdateInterval = 30*60*1000; // 30 minutes
		private static boolean isShuttingDown = false;
		private static ITimer shutdownTimer;
		private static Random rand;
	    protected HashMap<Object, PeerFinder> peerFinders = new HashMap<Object, PeerFinder>();
	    protected PeerFinderGlobal globalPeerFinder;


	    public void loadProperties(){
	    	props = new Properties();

	        //try {

	        // here's the name of the file
	        File properties_file = new File("ono.properties");

	   
            // if properties file exists on its own then override any properties file
            // potentially held within a jar
            if (properties_file.exists()) {
                FileInputStream fis = null;

                try {
                    fis = new FileInputStream(properties_file);

                    props.load(fis);
                } catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}finally {
                    if (fis != null) {
                        try {
							fis.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                    }
                }
            }

	    }
	    
	    protected void initializeValues() {
	    	
	    	OnoConfiguration oc = OnoConfiguration.getInstance();
	    	oc.loadProperties(props);
	    	
		    	isServer = props.getProperty("ono.digger.isServer").equals("true")
	            ? true : false;
		    
		    
//	        oc.setDoRemoteDig(digForOthers);
	        if (props.containsKey("ono.experiment.sleepTime")){
	        	EXPERIMENT_SLEEP_TIME = Integer.valueOf(props.getProperty("ono.experiment.sleepTime"));
	        }
	        
	        if (props.containsKey("ono.enableExperimentManager")){
	        	RUN_EXPERIMENT_MANAGER = props.getProperty(
	        			"ono.enableExperimentManager").equals("true")	        
		        ? true : false;
	        }
	    }
	    
	   

		protected void startExperimentManager() {
			OnoExperimentManager oem = new OnoExperimentManager();
			if (isAzureus()){
				Main.getPluginInterface().getUtilities().createThread("OnoExperimentManager", oem);
			}
			else {
				oem.setConnectionManager(getConnectionManager());
				MainPlanetLab.createThread("OnoExperimentManager", oem);
			}
		}

		protected void startDigger() {
			dig = Digger.getInstance();
	        //dig.setEdgeServerRatios(getEdgeServerRatios());
	        if (!dig.isAlive()) dig.start();
		}

		

	   
	    /* (non-Javadoc)
	     * @see java.lang.Object#finalize()
	     */
	    @Override
	    protected void finalize() throws Throwable {
	    	onoUnload();

	        super.finalize();
	    }

	    public void onoUnload(){
	    	if (DEBUG) System.out.println("Stopping ping manager");
	    	PingManager.stop();
	    	if (DEBUG) System.out.println("Stopping traceroutes");
	    	TraceRouteRunner.stop();
	    	
	    	
	    	if (dig != null) {
	            saveEdgeServerRatios(OnoPeerManager.getInstance().getMyRatios());
	            dig.setActive(false);
	            
	        }
	    	
            if (DEBUG) System.out.println("Stopping oess experiment manager");
	    	OessExperimentManager.stop();

	    	
	    	if (DEBUG) System.out.println("Flushing stat manager");
	    	StatManager.flush();
	    	try {
	    		if (DEBUG) System.out.println("Stopping stat manager");
	    		StatManager.stop();
	    	} catch (Exception e){
	    		e.printStackTrace();
	    	}
	    	if (DEBUG) System.out.println("Flushing statistics");
            Statistics.getInstance().flush();


	        if (peerFinders != null) {
	            for (PeerFinder pf : peerFinders.values()) {
	                pf.setActive(false);
	            }
	        }
	        
	        if (globalPeerFinder!=null){
	        	globalPeerFinder.setActive(false);
	        }
	        
	        if (DEBUG) System.out.println("Stopping statistics");
	        Statistics.getInstance().stop();
	        
	        CDNClusterFinder.getInstance().setActive(false);
	        	        
	    }



	    public static Map<String, EdgeServerRatio> getEdgeServerRatios(){
	    	HashMap<String, EdgeServerRatio> toReturn = new HashMap<String, EdgeServerRatio>();
	    	File outputFile = new File(getPluginInterface().getPluginDirectoryName() +
	                File.separatorChar +
	                "OnoCDNRatios.dat");
	    	
	    	
	        if (outputFile.exists()) {
	        	// get age of file
	        	long age = System.currentTimeMillis() - 
	        		outputFile.lastModified();
	        	
	        	if (age > EdgeServerRatio.EXPIRE_TIME) return null;
	        	
	           Properties prop = new Properties();
	           try {
				prop.load(new FileInputStream(outputFile));
				int i = 0;
				while (true){
					String cust = prop.getProperty("customer.name."+i);
					if (cust !=null){
						toReturn.put(cust, new EdgeServerRatio());
					}
					else {
						break;
					}
					i++;
				}
				for (String s : toReturn.keySet()){
					String edgeCluster;
					double ratio;
					for (Entry<Object, Object> e : prop.entrySet()){
						String key = (String)e.getKey();
						String value = (String)e.getValue();
						if (key.contains(s)){
							edgeCluster = key.substring(s.length()+1);
							ratio = Double.parseDouble(value);
							toReturn.get(s).addRatio(edgeCluster, ratio);
						}					
					}
				}
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        	
				// age the entries
				for (String s : toReturn.keySet()){
					toReturn.get(s).ageAll(age);
				}
	        }
	        
	        
	        
	        
	        return toReturn;
	    }
	    
	    private static void saveEdgeServerRatios(Map<String, EdgeServerRatio> edgeServerRatio) {
	    	if (edgeServerRatio==null) return;
	    	File outputFile = new File(getPluginInterface().getPluginDirectoryName() +
	                File.separatorChar +
	                "OnoCDNRatios.dat");

	        if (outputFile.exists()) {
	            outputFile.delete();
	        }

	        Properties props = new Properties();
	        FileOutputStream out;

	        try {
	            out = new FileOutputStream(outputFile);

	            int c = 0;
	            
	            for (String customer : edgeServerRatio.keySet()){
	            	props.put("customer.name."+c, customer);
	            	for (Entry<String, Double> e : edgeServerRatio.get(customer).getEntries()){
	            		props.put(customer+"."+e.getKey(), e.getValue().toString());
	            	}
	            	c++;
	            }

	            	props.store(out, "");
	            out.flush();
	            out.close();
	            
	        } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
			
		}

		/**
	     * @return Returns the dbUpdate.
	     */
	    public static int getDbUpdate() {
	        return OnoConfiguration.getInstance().getDbUpdateFreqSec();
	    }

	    /**
	     * @return Returns the isServer.
	     */
	    public static boolean isServer() {
	        return isServer;
	    }

	    /**
	     * @return Returns the updateCheck.
	     */
	    public static int getUpdateCheck() {
	        return OnoConfiguration.getInstance().getUpdateCheckFreqSec();
	    }

	    public static OnoPluginInterface getOnoPluginInterface() {
	        // TODO Auto-generated method stub
	        return (OnoPluginInterface) pi;
	    }
	    
	    public static PluginInterface getPluginInterface() {
	        // TODO Auto-generated method stub
	        if (isAzureus()) return Main.getPluginInterface();
	    	return pi;
	    }
	    
	    public static String getDatabaseName(){
	    	if (isAzureus()) return "ono";
	    	else return "sidestep";
	    	
	    }
	    
	    public static String getDatabaseHost(){
	    	if (isAzureus()) return "merlot.cs.northwestern.edu";
	    	else return "janus.cs.northwestern.edu";
	    	
	    }

	    /**
	     * @return Returns the digSleepTime.
	     */
	    public static int getDigSleepTime() {
	        return OnoConfiguration.getInstance().getDigFrequencySec();
	    }

	    /**
	     * @return
	     */
	    public static int getMaxPeers() {
	        // TODO Auto-generated method stub
	        return OnoConfiguration.getInstance().getMaxPeers();
	    }

	    /**
	     * @deprecated
	     * @return
	     */
		public static boolean getStatsEnabled() {
			// TODO Auto-generated method stub
			return OnoConfiguration.getInstance().isRecordStats();
		}
		
		public static DatabaseReporter getReporter() {
			// TODO Auto-generated method stub
			if (!OnoConfiguration.getInstance().isRecordStats()) 
				return NullReporter.getInstance();
			
			if (isAzureus()) return PhpReporter.getInstance();
			else return MySQLReporter.getInstance();
		}

		public static IDistributedDatabase getDistributedDatabase() {
			if (isAzureus()) return Main.getDistributedDatabase();
	    	return MainPlanetLab.getDistributedDatabase();
			
		}

		public static void checkForTorrentToDownload() {
			if (isAzureus()) Main.checkForTorrentToDownload();
			
		}

		public static HashMap<String, Integer> getOnoPeers(int limit) {
			if(isAzureus()) return Main.getOnoPeers(limit);
			return new HashMap<String, Integer>();
		}
		
		public static ILoggerChannel getLoggerChannel(final String name){
			if(isAzureus()) return Main.getLoggerChannel(name);
			return new ILoggerChannel(){

				public String getName() {
					// TODO Auto-generated method stub
					return name;
				}

				public boolean isEnabled() {
					// TODO Auto-generated method stub
					return false;
				}

				public void log(int log_type, String data) {
					// TODO Auto-generated method stub
					
				}

				public void log(String data) {
					// TODO Auto-generated method stub
					
				}

				public void log(Throwable error) {
					// TODO Auto-generated method stub
					
				}

				public void log(String data, Throwable error) {
					// TODO Auto-generated method stub
					
				}

				public void log(Object[] relatedTo, int log_type, String data) {
					// TODO Auto-generated method stub
					
				}

				public void log(Object relatedTo, int log_type, String data) {
					// TODO Auto-generated method stub
					
				}

				public void log(Object relatedTo, String data, Throwable error) {
					// TODO Auto-generated method stub
					
				}

				public void log(Object[] relatedTo, String data, Throwable error) {
					// TODO Auto-generated method stub
					
				}

				public void logAlert(int alert_type, String message) {
					// TODO Auto-generated method stub
					
				}

				public void logAlert(String message, Throwable e) {
					// TODO Auto-generated method stub
					
				}

				public void logAlertRepeatable(int alert_type, String message) {
					// TODO Auto-generated method stub
					
				}

				public void logAlertRepeatable(String message, Throwable e) {
					// TODO Auto-generated method stub
					
				}

				public void setDiagnostic() {
					// TODO Auto-generated method stub
					
				}};
		}
		
		public static void getHardCodedMappings(OnoExperiment onoExp, 
				HashMap<String, HashSet<String>> hardCodedMappings, 
				Vector<InetAddress> otherPeers){
			if(isAzureus()) Main.getHardCodedMappings(onoExp, 
					hardCodedMappings, otherPeers);
			else MainPlanetLab.getHardCodedMappings(onoExp, hardCodedMappings, otherPeers);
			
		}
		
		public static IOnoConnectionManager getConnectionManager(){
			if(isAzureus()) return Main.getConnectionManager();
			return MainPlanetLab.getConnectionManager();
		}

		public static int getPeerPort(String dest) {
			if (isAzureus()) return Main.getPeerPort(dest);
			else return StandardConnectionManager.STANDARD_PORT;
		}

		public static ITimer createTimer(String name) {
			if (isAzureus()) return Main.createTimer(name);
			return MainPlanetLab.createTimer(name);
		}
		
		public static void createThread(String name, Runnable action) {
			
			if (isAzureus()) Main.getPluginInterface().getUtilities().createThread(name, action);
			else MainPlanetLab.createThread(name, action);
		}

		public static Properties downloadFromURL(String url, int timeout) {
			if (isAzureus()) return Main.downloadFromURL( url, timeout);
			return MainPlanetLab.downloadFromURL(url, timeout);
			
		}
		
		/**
		 * Returns the TTL value for entries in the DDB, in seconds.
		 * @return
		 */
		public static int getDdbTTL(){
			if (isAzureus()){
				return 30;
			}
			else {
				return MainPlanetLab.getDdbTTL();
			}
		}

		public static String getPublicIpAddress() {
			/*if (isAzureus()){
				if (Main.getPluginInterface().getUtilities().getPublicAddress() != null) {
		            return Main.getPluginInterface().getUtilities().getPublicAddress().toString().substring(1);
		        } else {
		            return null;
		        }
			}
			else{*/
			synchronized (nextIpUpdate){
				if (publicIp!=null && nextIpUpdate  > System.currentTimeMillis() ){
					return publicIp;
				}
				InputStream is = null;
				String s;
				try {
					URL url = MainPlanetLab.getURLInput(
							"http://checkip.dyndns.com/", 10*1000);
					if (url!=null) is  = url.openStream();
					if (is!=null){
						InputStreamReader isr = new InputStreamReader(is);
						BufferedReader br = new BufferedReader(isr);
						s = br.readLine();
						if (s.length()>=0 && s.contains(": ") && s.contains("<")){
							String ip = s.substring(s.indexOf(": ")+2, s.indexOf('<', s.indexOf(":"))).trim();						
							if (publicIp!=null && !ip.equals(publicIp)){
								Statistics.getInstance().reportIpChange(publicIp, ip, getLocalAddress(ip), Util.currentGMTTime());
							} else if (publicIp==null && ip!=null && ip.length()>0){
								Statistics.getInstance().reportIpChange(ip, ip, getLocalAddress(ip), Util.currentGMTTime());
							}
							publicIp = ip;
							nextIpUpdate = System.currentTimeMillis()+ipUpdateInterval;
							return ip;
						}
					}
					// otherwise, try to get the local address
//					String s = InetAddress.getLocalHost().toString();
					s = getLocalPublicAddress();
					if (s.startsWith("192.168")) return null;
					publicIp = s;
					nextIpUpdate = System.currentTimeMillis()+ipUpdateInterval;
					return s;
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// sometimes the site can't resolve, in which case something 
					// like string index out of bounds happens...

				}finally {
					if (is!=null){
						try {
							is.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				if (isAzureus()){
					if ( Main.getPluginInterface().getUtilities().getPublicAddress()!=null)
							return Main.getPluginInterface().getUtilities().getPublicAddress().getHostAddress();
				}
				return null;
			}
			//}
		}

		public static String getLocalAddress(String ip) throws UnknownHostException {
			String s;
			InetAddress addresses[] = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
			// TODO verify that this always picks the "right" IP address
			//s = addresses[addresses.length-1].getHostAddress();
			if (ip!=null){
				for (InetAddress addr : addresses){
					if (addr.getHostAddress().equals(ip)) return addr.getHostAddress();
				}
			}
			
			for (InetAddress addr : addresses){
				if (addr.isSiteLocalAddress()) return addr.getHostAddress();
			}
			return "";
		}
		
		protected static String getLocalPublicAddress() throws UnknownHostException {
			String s;
			InetAddress addresses[] = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
			// TODO verify that this always picks the "right" IP address
			//s = addresses[addresses.length-1].getHostAddress();
			for (InetAddress addr : addresses){
				if (!addr.isSiteLocalAddress()) return addr.getHostAddress();
			}
			return "";
		}
		
		public static boolean isAzureus(){
			return type.equals("Azureus");
		}

		public static void setType(String string) {
			type = string;
			
		}
		
		public static void saveEdgeRatios(){
			saveEdgeServerRatios(OnoPeerManager.getInstance().getMyRatios());
		}

		public static Object getClusterFinderObject(String peerIp) {
			if (isAzureus()) return Main.getClusterFinderObject( peerIp );
			else return Digger.getInstance(); // TODO why not?
			}
		
		public static TransferManager getTransferManager(){
			if (isAzureus()) throw new RuntimeException("TransferManager " +
					"not implemented for Azureus!");
			else return MainPlanetLab.getTransferManager(); 
		}

		public static void initialize() {
			if (isAzureus()) Main.initialize();
			else MainPlanetLab.initialize();
			
			
		}

		public static void setStatsEnabled(boolean b) {
			OnoConfiguration.getInstance().setRecordStats(b);
			
		}
		
		public static boolean isShuttingDown(){
			return isShuttingDown;
		}
		
		public static void startShutdown(){
			shutdownTimer = createTimer("OnoShutdownTimer");
			shutdownTimer.addEvent(System.currentTimeMillis()+OnoConfiguration.getInstance().getReportingTimeout()*1000, 
					new ITimerEventPerformer(){

						public void perform(ITimerEvent event) {
							isShuttingDown = true;
							MainGeneric.getReporter().stopReporting();
							
						}}
			);
		}
		
		public static void startUp(){
			isShuttingDown = false;
			PingManager.activate();
		}

		public static boolean getDhtEnabled() {
			// TODO Auto-generated method stub
			if (isAzureus()) return false;
			else return true;
		}

		public static Random getRandom() {
			if (rand==null){
				rand = new Random();
			}
			return rand;
		}

		public static ArrayList<String> getPropertiesLocations() {
			if (isAzureus()) return Main.getPropertiesLocations();
			ArrayList<String> locs = new ArrayList<String>();
			locs.add(getPluginInterface().getPluginDirectoryName() +
						java.io.File.separator + "ono.properties");
			return locs;
		}
		
		
}
