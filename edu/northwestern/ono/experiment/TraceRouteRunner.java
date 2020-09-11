/**
 * Ono Project
 *
 * File:         TraceRouteRunner.java
 * RCS:          $Id: TraceRouteRunner.java,v 1.39 2010/03/29 16:48:04 drc915 Exp $
 * Description:  TraceRouteRunner class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Feb 25, 2007 at 1:22:39 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.experiment
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
package edu.northwestern.ono.experiment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TimeZone;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.stats.Statistics;
import edu.northwestern.ono.util.HashSetCache;
import edu.northwestern.ono.util.PluginInterface;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The TraceRouteRunner class handles asynchronous recording of 
 * traceroute data.
 */
public class TraceRouteRunner implements Runnable {
	
	public class TraceEntry {
		public float rtt[] = new float[]{-1f,-1f,-1f};
		public String router[] = new String[3];
		public int numRouters = 0;
	}
	
	public class TraceResult {
		public String source;
		public String dest;
		public ArrayList<TraceEntry> entries;
		public long timestamp = Util.currentGMTTime();
		public int destType;
	}
	
	private class TraceRouteRun{
		public String ip;
		public boolean recordInDB = true;
		public int destType;
		
		public TraceRouteRun(String ip){
			this.ip = ip;
		}
		
		public TraceRouteRun(String ip, boolean record){
			this.ip = ip;
			recordInDB = record;
		} 
		
		public TraceRouteRun(String ip, int type) {
			this.ip = ip;
			this.destType = type;
		}
		
		@Override
		public boolean equals(Object obj) {

			return obj!=null && obj instanceof TraceRouteRun && ip.equals(((TraceRouteRun)obj).ip);
		}
		@Override
		public int hashCode() {

			return ip.hashCode();
		}
		
		
	}



	private static final boolean DEBUG = false;
	

	
    static BufferedReader in;
    static Calendar myCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    static PluginInterface pi = MainGeneric.getPluginInterface();

	private static TraceRouteRunner self;
	private ArrayList<TraceRouteReceiver> listeners;
	private static Process p;

	private static boolean active = true;
    TraceResult tr;
    HashSetCache<TraceRouteRun> pendingRuns;
    HashSetCache<TraceRouteRun> pendingRunsPrio;
    public static ArrayList<String> OessList = new ArrayList<String>();
    
    private TraceRouteRunner(){
    	pendingRuns = new HashSetCache<TraceRouteRun>(200);
    	pendingRunsPrio = new HashSetCache<TraceRouteRun>(50);
    	MainGeneric.createThread("TraceRouteRunner", this);
    	
    }
    
    public static TraceRouteRunner getInstance(){
    	if (self==null && !MainGeneric.isShuttingDown()){
    		active = true;
    		self = new TraceRouteRunner();
    		self.listeners = new ArrayList<TraceRouteReceiver>();
    	}
    	return self;
    }
    
	public void addIp( String dest ){
		synchronized(pendingRuns){
			pendingRuns.add(new TraceRouteRun(dest));
			pendingRuns.notifyAll();
		}
	}
	
	public void addIp( String dest, boolean recordInDB ){
		synchronized(pendingRuns){
			pendingRuns.add(new TraceRouteRun(dest, recordInDB));
			pendingRuns.notifyAll();
		}
	}
	
	public void addIpOess(String dest, int type) {
		OessList.add(dest);

		synchronized (pendingRuns) {
			pendingRuns.add(new TraceRouteRun(dest, type));
			pendingRuns.notifyAll();
		}
	}
	
	public static void stop(){
		active = false;
		if (self==null || 
				(self.pendingRuns==null && self.pendingRunsPrio==null)) return;
		if (p!=null){
			p.destroy();
		}
		if (self.pendingRuns!=null){
			synchronized(self.pendingRuns){
				self.pendingRuns.notifyAll();
			}
		}

//		if (in!=null){
//			try {
//				in.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		self = null;
		//in = null; // will be closed already
		myCalendar = null;
		p = null;
		pi = null;
		
		
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		/** the ip address to trace routes on */
		String dest;
		
		TraceRouteRun trr = null;
		while (!MainGeneric.isShuttingDown()){
			synchronized(pendingRuns){
				while (pendingRuns.size()==0 && pendingRunsPrio.size()==0 
						&& !MainGeneric.isShuttingDown()){
					try {
						pendingRuns.wait(30*1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				Iterator<TraceRouteRun> it;
				if (pendingRunsPrio.size()>0){
					
					it = pendingRunsPrio.iterator();
					trr = it.next();
					it.remove();
					if (DEBUG) System.out.println("High priority traceroute to "+trr.ip);
				} else {
					it = pendingRuns.iterator();
					if (!it.hasNext()){
						continue;
					}
					trr = it.next();
					it.remove();
					if (DEBUG) System.out.println("Normal priority traceroute to "+trr.ip);
				}
			}
			if (!active) return;
			tr = new TraceResult();
			dest = trr.ip;
			
		ArrayList<TraceEntry> data = new ArrayList<TraceEntry>();
		 String line = null;
		try {
            
			if (pi.getUtilities().isWindows()) {
				boolean isV6 = Util.isV6Address(dest);
				String v6Text = "";
				if (isV6) v6Text = "-6 ";
                p = Runtime.getRuntime().exec("tracert -d -w 3000 " + v6Text + dest);
                in = new BufferedReader(new InputStreamReader(
                            p.getInputStream()));

               

                while ((line = in.readLine()) != null) {                	
                    // parse data
                	String[] entries = line.split("[\\s]+");
                	if (entries.length<=1) continue;
                	try {
                		Integer.parseInt(entries[1]);
                	} catch (NumberFormatException e){
                		continue;
                	}
                	TraceEntry te = new TraceEntry();
                	int i = 2;
                	int numEntries = 0;
                	while (i < entries.length-1){
                        try{
                		te.rtt[numEntries] = getValue(entries[i]);
                        } catch (NumberFormatException e){
                            // failure during traceroute
                            te.router[te.numRouters++] = getRouterIp(entries[i]);
                            break;
                        }
                		if (te.rtt[numEntries++]<0) i++;
                		else i+=2;
                		if (numEntries==te.rtt.length) break;
                		
                	}
                	
                	te.router[te.numRouters++] = getRouterIp(entries[entries.length-1]);  
                	data.add(te);
                	if (data.size()>=30) break;
                }

                // then destroy
                if (p!=null) p.destroy();         
                
            } else if (pi.getUtilities().isLinux() || pi.getUtilities().isOSX()) {
            	boolean isV6 = Util.isV6Address(dest); 
            	String v6Text = "";
            	String commandText = "";
				if (isV6){
					//v6Text = "-A inet6 ";
					commandText = "traceroute6";
				}else{
					commandText = "traceroute";
				}
				
            	p = Runtime.getRuntime().exec(commandText+" -n -w 3 " + v6Text + dest);

                in = new BufferedReader(new InputStreamReader(
                            p.getInputStream()));
                

                while (in!=null && (line = in.readLine()) != null) {                	
                    // parse data
                	
                	String[] entries = line.split("[\\s]+");
                	//System.out.println(Arrays.toString(entries));
                	if (entries.length<=1) continue;
                	int index = 0;
                	while (entries[index].equals("")) index++;
                	try {
                		Integer.parseInt(entries[index]);
                	} catch (NumberFormatException e){
                		// not an entry we care about
                		continue;
                	}
                	TraceEntry te = new TraceEntry();
                	int rttCount = 0;
                	for (int i = index+1; i < entries.length; i++){
//                		System.out.println("i:"+i);
                		if (entries[i].contains("*")){
                			te.router[te.numRouters++] = getRouterIp(entries[i]);
                			te.rtt[rttCount++] = -1;
                			//System.out.println("Found asterisk!");
                		}
                		else {
                			if(isValidIP(entries[i])){
                				// if code reaches this point, this is an ip
                				te.router[te.numRouters++] = getRouterIp(entries[i]);
                				//System.out.println("Found router!");
                			}
                			else{
                				// not an ip, so it must be a value
                				try {
	                				te.rtt[rttCount] = Float.parseFloat(entries[i++]);
	                				rttCount++;
                				} catch (NumberFormatException e ){
                					
                					continue; // ignore the garbage
                				}
                				
                				//System.out.println("Found rtt!");
                				// i++ skips the "ms" entry
                				
                			}
                		}
                	}
                 
                	data.add(te);
                	if (data.size()>30) break;
                }

                // then destroy
                if (p!=null) p.destroy();
            }
			
			 tr.dest = dest;
	            tr.source = MainGeneric.getPublicIpAddress();
	            tr.entries = data;
	            tr.destType = trr.destType;
	            
	            
			for (TraceRouteReceiver list : listeners) {
				list.receiveTraceResult(trr.ip, tr);
			}
            
            if (trr.recordInDB){
	           
				if (OessList.contains(dest)) {
					Statistics.getInstance().addTraceRouteResultOess(tr);
					OessList.remove(dest);
				}
					
				else
					Statistics.getInstance().addTraceRouteResult(tr);

            }
            if (in!=null) in.close();
            if (p!=null) p.destroy();            
        } catch (IOException e) {
        	if (p!=null)p.destroy();
            //e.printStackTrace();
        } catch (Exception e){
        	try {
				if (in !=null ) in.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	
        	if (p!=null) p.destroy();
            if (active){
	        	Statistics.getInstance().reportError(e);
	            Statistics.getInstance().reportError(new 
	            		RuntimeException("Error input was: \n"+line));
            }
        }
        
		} // end while active

	}

	private String getRouterIp(String maybeIp) {
		if(maybeIp.matches("([0-9]{1,3}\\.){3}[0-9]{1,3}")){
			// if code reaches this point, this is an ip	
			Statistics.getInstance().addRouterForLookup(maybeIp);
			return maybeIp;
			//System.out.println("Found router!");
		} else if (maybeIp.matches("\\s*((([0-9A-Fa-f]{1,4}:){7}(([0-9A-Fa-f]{1,4})|:))" +
				"|(([0-9A-Fa-f]{1,4}:){6}(:|((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2" +
				"[0-4]\\d|[01]?\\d{1,2})){3})|(:[0-9A-Fa-f]{1,4})))|(([0-9A-Fa-f]{1,4}:){5}(" +
				"(:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3}" +
				")?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){4}(:[0-9A-Fa-f]{1,4})" +
				"{0,1}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2}" +
				")){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){3}(:[0-9A-Fa-f]{1,4}" +
				"){0,2}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})" +
				"?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){2}(:[0-9A-Fa-f]{1,4}){0,3}" +
				"((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((" +
				":[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:)(:[0-9A-Fa-f]{1,4}){0,4}" +
				"((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)" +
				"|((:[0-9A-Fa-f]{1,4}){1,2})))|(:(:[0-9A-Fa-f]{1,4}){0,5}((:((25[0-5]|2[0-4]\\d|" +
				"[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4})" +
				"{1,2})))|(((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2}))" +
				"{3})))(%.+)?\\s*")){
			Statistics.getInstance().addRouterForLookup(maybeIp);
			return maybeIp;
		}
		Statistics.getInstance().addRouterForLookup("unknown");
		return "unknown";
	}
	
	public boolean isValidIP(String maybeIp){
		if(maybeIp.matches("([0-9]{1,3}\\.){3}[0-9]{1,3}")){
			// if code reaches this point, this is an ip	
			return true;
			//System.out.println("Found router!");
		} else if (maybeIp.matches("\\s*((([0-9A-Fa-f]{1,4}:){7}(([0-9A-Fa-f]{1,4})|:))" +
				"|(([0-9A-Fa-f]{1,4}:){6}(:|((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2" +
				"[0-4]\\d|[01]?\\d{1,2})){3})|(:[0-9A-Fa-f]{1,4})))|(([0-9A-Fa-f]{1,4}:){5}(" +
				"(:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3}" +
				")?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){4}(:[0-9A-Fa-f]{1,4})" +
				"{0,1}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2}" +
				")){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){3}(:[0-9A-Fa-f]{1,4}" +
				"){0,2}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})" +
				"?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){2}(:[0-9A-Fa-f]{1,4}){0,3}" +
				"((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((" +
				":[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:)(:[0-9A-Fa-f]{1,4}){0,4}" +
				"((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)" +
				"|((:[0-9A-Fa-f]{1,4}){1,2})))|(:(:[0-9A-Fa-f]{1,4}){0,5}((:((25[0-5]|2[0-4]\\d|" +
				"[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4})" +
				"{1,2})))|(((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2}))" +
				"{3})))(%.+)?\\s*")){
			return true;
		}
		return false;
	}

	private float getValue(String val) {
		if (val.contains("<"))return 0;
		else if (val.contains("*")) return -1;
    	else return Float.parseFloat(val);
	}

	public void addIp(String dest, boolean recordInDB, boolean sameCluster) {
		if (!sameCluster){
			synchronized(pendingRuns){
				pendingRuns.add(new TraceRouteRun(dest, recordInDB));
				pendingRuns.notifyAll();
			}
		} else {
			synchronized(pendingRuns){
				pendingRunsPrio.add(new TraceRouteRun(dest, recordInDB));

				pendingRuns.notifyAll();
			}
		}
		
	}
	
	public void addListener(TraceRouteReceiver list){
		
		synchronized (listeners){
			if (!listeners.contains(list)) listeners.add(list);
		}
	}
}
