/**
 * Ono Project
 *
 * File:         MainPlanetLab.java
 * RCS:          $Id: MainPlanetLab.java,v 1.27 2011/07/06 15:45:53 mas939 Exp $
 * Description:  MainPlanetLab class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 29, 2007 at 1:00:29 PM
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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.Map.Entry;

import edu.northwestern.ono.api.TransferManager;
import edu.northwestern.ono.connection.IOnoConnectionManager;
import edu.northwestern.ono.connection.standard.StandardConnectionManager;
import edu.northwestern.ono.dht.IDistributedDatabase;
import edu.northwestern.ono.dht.bamboo.BambooDHTManager;
import edu.northwestern.ono.experiment.OnoExperiment;
import edu.northwestern.ono.net.SideStepTransferManager;
import edu.northwestern.ono.position.OnoPeerManager;
import edu.northwestern.ono.stats.StatManager;
import edu.northwestern.ono.timer.ITimer;
import edu.northwestern.ono.timer.StandardTimerManager;
import edu.northwestern.ono.ui.SideStep;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The MainPlanetLab class ...
 */
public class MainPlanetLab extends MainGeneric {
	
	static Thread t = null;

	public static ITimer createTimer(String name) {
		return StandardTimerManager.getInstance().createTimer(name);
	}
	
	public static URL getURLInput(String url, int timeout){
		URL u;
	      InputStream is = null;
	      try {
		u = new URL(url);
	       
        URLConnection c = u.openConnection();
        c.setConnectTimeout(timeout);
        c.connect();
        return u;
        
	 } catch (MalformedURLException mue) {

         System.out.println("Ouch - a MalformedURLException happened.");
         mue.printStackTrace();

      } catch (IOException ioe) {

         System.out.println("Oops- an IOException happened.");
         ioe.printStackTrace();

      } finally {


	         try {
	        	 if (is!=null)
	            is.close();
	         } catch (IOException ioe) {
	            // just going to ignore this one
	         }

	      } // end of 'finally' clause
      return null;
	}

	public static Properties downloadFromURL(String url, int timeout) {
		URL u;
	      InputStream is = null;          // throws an IOException

	      try {
	    	  is = getURLInput(url, timeout).openStream();
	      	if (is!=null){


	         Properties props = new Properties();
	         props.load(is);
	         return props;
	      	}
	      	else return null;

	        

	      } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}finally {


	         try {
	        	 if (is!=null)
	            is.close();
	         } catch (IOException ioe) {
	            // just going to ignore this one
	         }

	      } // end of 'finally' clause
		return null;
	}

	public static IOnoConnectionManager getConnectionManager() {
		return StandardConnectionManager.getInstance();
	}

	public synchronized static IDistributedDatabase getDistributedDatabase() {
		if (t==null){
		try {
			t = new Thread (new BambooDHTManager(true));
			t.setDaemon(true);
			t.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		}
		
		return BambooDHTManager.getInstance();
	}
	
	public static void main(String args[]){
		initialize();
		
		Thread shutdownHook= new Thread(){

			@Override
			public void run() {
				
				MainGeneric.startShutdown();
				
				try {
			        // catch all exceptions so it is sure to unload
			    		MainGeneric.getReporter().setExtendedConnection(true);
			    	
			    	try {StatManager.stop();} catch (Exception e){
			    		e.printStackTrace();
			    	}
			    
			    	
			    	try {self.onoUnload();} catch (Exception e){
			    		e.printStackTrace();
			    	}
			    	
			    	OnoPeerManager.getInstance().stop();
			        
			        MainGeneric.getReporter().setExtendedConnection(false);
			        MainGeneric.getReporter().disconnect();
			        
			        
			        
			    	} catch (Exception e){
			    		e.printStackTrace();
			    		MainGeneric.getReporter().disconnect();
			    	}
			}
			
			
		};
		shutdownHook.setName("Shutdown hook");
		Runtime.getRuntime().addShutdownHook(shutdownHook);
	}

	private static MainPlanetLab self;
	
	public MainPlanetLab(){
		super();
	}

	static public void initialize() {
		if (self!=null) return;
		
		self = new MainPlanetLab();
		self.loadProperties();
        // load properties 
		self.initializeValues();
		
		// loads proerties from db
		MainGeneric.getReporter().connect();
		MainGeneric.getReporter().disconnect();
		
		if (OnoConfiguration.getInstance().isVisualize()){
			SideStep.getInstance();
		}
        
//      create and initialize digger
		self.startDigger();

        // create and start experiment manager
		if (OnoConfiguration.getInstance().isRunExperiments())
			self.startExperimentManager();
		
		
		
	}
	
	public static MainPlanetLab getInstance(){
		if (self==null){
			self = new MainPlanetLab();
			self.loadProperties();
	        // load properties 
			self.initializeValues();
        
	//      create and initialize differ
			self.startDigger();
		}
		return self;
	}

	public static void createThread(String string, Runnable oem) {
		Thread t = new Thread(oem);
		t.setName(string);
		t.setDaemon(true);
		t.start();
		
	}


	public static int getDdbTTL(){
		return BambooDHTManager.DEFAULT_TTL;
	}
	
	public static TransferManager getTransferManager(){
      SideStepTransferManager dtr = SideStepTransferManager.getInstance();
        dtr.setManager(self.getConnectionManager());
        return dtr;
        
        // TODO for client: connection.addListener(dtr);
	}
	
	public static void getHardCodedMappings(OnoExperiment onoExp, 
			HashMap<String, HashSet<String>> hardCodedMappings, 
			Vector<InetAddress> otherPeers) {
	    String hostName;
	    InputStream is = null;
	    boolean DO_HARDCODE = false;
	
	    try {
	        hostName = java.net.InetAddress.getLocalHost().getHostName();
	
	        // download the experiment list 
	        Properties details = new Properties();
	        if (DO_HARDCODE){
	        	is = getURLInput("http://165.124.180.20/~aqualab/azplugin/hardcode/" +
	        			"hardCodedMappings-" + hostName + ".txt", 60*1000).openStream();
	        	details.load(is);
	        	is.close();

	        	Iterator it = details.entrySet().iterator();

	        	while (it.hasNext()) {
	        		Entry ent = ((Entry) it.next());
	        		String peerIp = (String) ent.getKey();
	        		String edges = (String) ent.getValue();
	        		String[] edgeArray = edges.split(";");

	        		for (String edge : edgeArray) {
	        			if (hardCodedMappings.get(Util.getClassCSubnet(edge)) == null) {
	        				hardCodedMappings.put(Util.getClassCSubnet(edge),
	        						new HashSet<String>());
	        			}

	        			hardCodedMappings.get(Util.getClassCSubnet(edge)).add(peerIp);
	        		}
	        	}
	        }
	        // now get active nodes 
	       
	        is = getURLInput("http://aqua-lab.org/sidestep/hardcode/" +
                    "allNodes.txt", 60*1000).openStream();
	        
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
			}
			is.close();
			
	
	        String[] lines = sb.toString().split("\n");
	
	        for (String line : lines) {
	            otherPeers.add(InetAddress.getByName(line.trim()));
	        }
	       
	    } catch (UnknownHostException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    } catch (MalformedURLException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    } catch (NullPointerException e) {
	    	
	    }finally {
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

}
