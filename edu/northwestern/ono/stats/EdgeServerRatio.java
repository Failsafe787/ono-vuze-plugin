/**
 * Ono Project
 *
 * File:         EdgeServerRatio.java
 * RCS:          $Id: EdgeServerRatio.java,v 1.23 2010/03/29 16:48:04 drc915 Exp $
 * Description:  EdgeServerRatio class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 16, 2007 at 11:01:35 AM
 * Language:     Java
 * Package:      edu.northwestern.ono.stats
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
package edu.northwestern.ono.stats;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Map.Entry;

import edu.northwestern.ono.OnoConfiguration;
import edu.northwestern.ono.position.CDNClusterFinder;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The EdgeServerRatio class maintains the edge-server ratios for a given 
 * peer.
 */
public class EdgeServerRatio implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1221642255540144145L;

	private static final boolean DEBUG = false;

	/** map of edge-server cluster to number of times seen */
	private HashMap<String, Double> ratios;
	
	/** yields a once-seen value expiring after ~ 1 day */
	private double exponent = 0.998;
	
	public static final long EXPIRE_TIME = 24*60*60*1000;

	private static final int MIN_VALID_LOOKUPS = 4;
	
	/** the value at which to flush a cluster from recently seen ratios */
	private double threshold = 0.0001;
	
	private double baseTime = 30*1000; // number of seconds
	
	/** the last time a value was reported */
	private long lastTimestamp = -1;
	
	/** the maximum time to wait between digs */
	private int digInterval = 30;
	
	private static HashSet<String> blackList = new HashSet<String>();
	private static HashSet<String> blackList2 = new HashSet<String>();
	
    public static HashMap<String, Integer> edgeClusters = new HashMap<String, Integer>();
    
    public int numLookups = 0;
	
	public EdgeServerRatio(){
		ratios = new HashMap<String, Double>();
		lastTimestamp =  Util.currentGMTTime();
//		blackList.add("72.247.29"); // known akamai-central servers
//		blackList.add("72.247.28");
		blackList2.add("72.247");
		blackList2.add("72.246");
		blackList2.add("68.142"); // limewire
		blackList2.add("208.111"); // limewire
	}
	
	/**
	 * Reports that an edge was seen.
	 * @param ips the ip addresses of the edges
	 * @return true if ratio changed significantly (or not enough ratio 
	 * information is present)
	 */
	public boolean reportSeen(Set<String> ips){
		EdgeServerRatio temp = null;
		// create copy for comparison
		if (OnoConfiguration.getInstance().isAdaptiveDig()){
			temp = new EdgeServerRatio();
			for (Entry<String, Double> ent: ratios.entrySet()){
				temp.addRatio(ent.getKey(), ent.getValue());
			}
		}
		if (numLookups==Integer.MAX_VALUE) numLookups = MIN_VALID_LOOKUPS;
		numLookups++;
		long timeDiff = Util.currentGMTTime() - lastTimestamp; 
		int factor = getFactor(timeDiff);
		if (factor==0) factor = 1;
		double sum = age(factor);
		double weight = (1-sum)/ips.size();
		HashMap<String, Double> weights = new HashMap<String, Double>();
		for (String ip : ips){
			String edgeCluster = Util.getClassCSubnet(ip);
			if (blackList.contains(edgeCluster) || 
					blackList2.contains(edgeCluster.substring(0, 
							edgeCluster.lastIndexOf('.')))){
				edgeCluster = ip;
			}
			
			if (weights.get(edgeCluster)==null){
				weights.put(edgeCluster, weight);
			} else {
				weights.put(edgeCluster, weights.get(edgeCluster)+weight);
			}
			
			// add mapping for DB reporting
	        if (!edgeClusters.containsKey(edgeCluster)) {            
	        	edgeClusters.put(edgeCluster, edgeClusters.size()); 
	        }
		}
		
		for (String edgeCluster : weights.keySet()){
		if (ratios.get(edgeCluster)==null){			
			ratios.put(edgeCluster, weights.get(edgeCluster)); // use sum to ensure unit-length vector
		}
		else {			
			ratios.put(edgeCluster, ratios.get(edgeCluster) + weights.get(edgeCluster));
		}
		}
        lastTimestamp = Util.currentGMTTime();
	
        if (OnoConfiguration.getInstance().isAdaptiveDig()){
        	//double t = cosineSimilarity(temp);
        	if (numLookups < MIN_VALID_LOOKUPS){
        		adjustInterval(true);
        		return true;
        	}
        	else {
        		double threshold = 0.05;
        		for (Entry<String, Double> ent : ratios.entrySet()){
        			if (temp.ratios.get(ent.getKey()) == null){
        				if ( ent.getValue()>threshold){
        					adjustInterval(true);
        					return true;
        				}
        				else continue;
        			}        					        			
        			if (Math.abs(ent.getValue()-temp.ratios.get(ent.getKey()))
        					> threshold) {
        				adjustInterval(true);
        				return true;
        			}
        		}
        		adjustInterval(false);
        		return false;
        	}
//        	else if (t<0.97){
//        		System.out.println("Reducing lookup interval (cossim="+t+") @" +System.currentTimeMillis());
//        		return true;
//        	}
//        	else return false;
        }
        adjustInterval(false);
        return false;
	}
	
	private void adjustInterval(boolean shouldDecrease){
		if (OnoConfiguration.getInstance().isAdaptiveDig()){ 
        	if (shouldDecrease){
        		digInterval /= 2;
        		if (digInterval < OnoConfiguration.getInstance().getDigStart())
        			digInterval = (int)OnoConfiguration.getInstance().getDigStart();
        	} else {
        		digInterval += (int)OnoConfiguration.getInstance().getDigIncrement();
            	if (digInterval > OnoConfiguration.getInstance().getDigMaxInterval()){
            		digInterval = (int) OnoConfiguration.getInstance().getDigMaxInterval();
            	}
        	}
        }
	}
	
	/**
	 * Gets the dig interval (in seconds) for this ratio
	 * @return
	 */
	public int getSleepInterval(){
		return digInterval;
	}

	private int getFactor(long timeDiff) {
		return (int)Math.floor(timeDiff/baseTime);
	}

	/**
	 * Ages the ratios and reports their sum.
	 * @param factor 
	 * @return the sum of all ratios.
	 */
	private double age(int factor) {
		LinkedList<String> toRemove = new LinkedList<String>();
		double ratioToUse = exponent;
		for (int i = 1; i < factor; i++) ratioToUse *= exponent;
		double sum = 0;
		for(String key : ratios.keySet()){
			double value = ratios.get(key)*ratioToUse;
			if(value<threshold) toRemove.add(key);
			else{
				ratios.put(key, value);
				sum+=value;
			}
		}
		return sum;
	}

	@Override
	public String toString() {
		StringBuffer sb =  new StringBuffer();
		sb.append("[\n");
		for (Entry<String, Double> ent : ratios.entrySet()){
			sb.append("\t[");
			sb.append(ent.getKey());
			sb.append(", ");
			sb.append(ent.getValue());
			sb.append("]\n");
		}
		sb.append("]");
		return sb.toString();
	}

	public Set<Entry<String, Double>> getEntries() {
		return ratios.entrySet();
	}

	public void addRatio(String edgeCluster, double ratio) {
		ratios.put(edgeCluster, ratio);		
		// add mapping for DB reporting
        if (!edgeClusters.containsKey(edgeCluster)) {            
        	edgeClusters.put(edgeCluster, edgeClusters.size()); 
        }
	}

	public long getLastUpdate() {
		// TODO Auto-generated method stub
		return lastTimestamp;
	}
	
	public void setLastTimestamp(long ts) {
		lastTimestamp = ts;
	}

	public double cosineSimilarity(EdgeServerRatio value) {
		   double num = 0.0;
		   double mag1 = 0.0;
		   double mag2 = 0.0;
		   double toReturn = 0.0;
		   
		   for (Entry<String, Double> e : ratios.entrySet()){
		   
				if (value.ratios.containsKey(e.getKey())){
					num += e.getValue() * value.ratios.get(e.getKey());
				}
				mag1 += e.getValue() * e.getValue();
		   }
		   
		   for(Entry<String, Double> e : value.ratios.entrySet()) {
				mag2 += e.getValue() * e.getValue();
		   }
		   if (mag1 == 0){
				if (DEBUG) System.err.println("Node $n1 has zero magnitude!");
				return 0;
		   }
		   else if (mag2 == 0){
			   if (DEBUG) System.err.println("Node $n2 has zero magnitude!");
				return 0;
		   }
		   double denom = Math.sqrt(mag1 * mag2);

		   toReturn = num/denom;
		   
		   if (DEBUG && toReturn > 0.1){
			   System.out.println("Match!");
		   }
		   
		   return toReturn;
	}

	public Set<String> getTightlyBoundEdgeClusters() {
		Set<String> toReturn = new HashSet<String>();
		for (Entry<String, Double> e : getEntries()){
			if (e.getValue()>CDNClusterFinder.COSINE_THRESHOLD){
				toReturn.add(e.getKey());
			}
		}
		return toReturn;
	}

	public void serialize(ByteArrayOutputStream baos) throws IOException {
		int entryCount = 10;
		for (Entry<String, Double> e2 : getEntries()){
			if (e2.getValue()<CDNClusterFinder.COSINE_THRESHOLD) continue;
			baos.write(convertEdgeCluster(e2.getKey()));
			baos.write(Util.convertFloat(e2.getValue().floatValue()));
			entryCount--;
			if (entryCount==0) break;
		}
		byte ip[] = new byte[]{0,0,0};
		byte value[] = new byte[]{0,0,0,0};
		for (int i = entryCount; i > 0 ; i--){
			baos.write(ip);
			baos.write(value);
		}
		
	}
	
	public void deserialize(ByteBuffer bBuffer) {
		byte cluster[] = new byte[3];
		String sCluster;
		for (int i = 0; i < 10; i++){
			
			bBuffer.get(cluster);
			if ((cluster[0]|cluster[1]|cluster[2])==0) break;
			sCluster = getEdgeServerCluster(cluster);
			// if "blacklisted", it's a whole ip
			if (blackList.contains(sCluster) || 
					blackList2.contains(sCluster.substring(0, 
							sCluster.lastIndexOf('.')))){
				byte cluster2[] = new byte[4];
				for (int j = 0; j < cluster.length; j++){
					cluster2[j] = cluster[j];
				}
				cluster2[3] = bBuffer.get();
				sCluster = getEdgeServerCluster(cluster2);
			}
			addRatio(sCluster, bBuffer.getFloat());
		}
		
	}
	
	private byte[] convertEdgeCluster(String key) {
		String vals[] = key.split("\\.");
		byte bArray[] = new byte[vals.length];		
		for (int i = 0; i < vals.length; i++){
			short val = Short.parseShort(vals[i]);
			bArray[i] = Util.convertShort(val)[0];
		}
		return bArray;
		                         
		
	}


    private String getEdgeServerCluster(byte buffer[]) {
    	byte sVal[] = new byte[2];
    	sVal[1] = 0;
    	ByteBuffer bBuffer;
    	StringBuffer sb = new StringBuffer();
    	for (int i = 0; i < buffer.length; i++){
	    	sVal[0] = buffer[i];
	    	bBuffer = ByteBuffer.wrap(sVal);
	    	bBuffer.order(ByteOrder.LITTLE_ENDIAN);
	    	sb.append(bBuffer.getShort());
	    	if (i!=buffer.length-1) sb.append('.');
    	}
    	
		return sb.toString();
	}

	public void ageAll(long timeDiff) {
		age(getFactor(timeDiff));
		
	}

	public boolean valid() {
		return numLookups >= MIN_VALID_LOOKUPS;
	}

	public boolean shouldLookup() {
		return Util.currentGMTTime()-lastTimestamp > digInterval*1000;
	}
	
}
