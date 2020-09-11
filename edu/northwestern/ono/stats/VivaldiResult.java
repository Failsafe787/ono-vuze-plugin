/**
 * Ono Project
 *
 * File:         VivaldiResult.java
 * RCS:          $Id: VivaldiResult.java,v 1.3 2008/04/09 03:17:59 drc915 Exp $
 * Description:  VivaldiResult class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Nov 20, 2006 at 1:22:22 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.stats
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
package edu.northwestern.ono.stats;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;
import com.aelitis.azureus.core.dht.netcoords.vivaldi.ver1.VivaldiPosition;
import com.aelitis.azureus.core.dht.netcoords.vivaldi.ver1.impl.HeightCoordinatesImpl;

import edu.northwestern.ono.dns.Digger;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The VivaldiResult class ...
 */
public class VivaldiResult {
	public static HashMap<String, Integer> peers = Statistics.peerMap;
	public static VivaldiPosition current = null;
	public static DHTNetworkPosition currentV2 = null;
	public static int observedBy = -1;
	
	 public DHTNetworkPosition position;
	 public DHTNetworkPosition position2;
	 public HeightCoordinatesImpl coord;
	 public float pingResult;
	 public int peerIp;
	 public long timestamp;
	public float errorEstimate;
	public float rttEstimate;
	public float rttEstimateV2;
	public byte type;
	 
	public VivaldiResult(VivaldiPosition position, DHTNetworkPosition positionV2,
			HeightCoordinatesImpl coord, double pingResult, String peerIp) {
		super();
		type = position.getPositionType();
		this.position = position;
		this.position2 = positionV2;
		errorEstimate = position.getErrorEstimate();
		if (position==null || current==null)
			rttEstimate = -1;
		else rttEstimate= position.estimateRTT(
				VivaldiResult.current.getCoordinates());
		if (positionV2==null || currentV2==null)
			rttEstimateV2 = -1;
		else rttEstimateV2 = positionV2.estimateRTT(currentV2);
		
		this.coord = coord;
		this.pingResult = (float)pingResult;
		timestamp = System.currentTimeMillis();
		
		synchronized (peers) {
			if (observedBy==-1){
				String ip = Digger.getInstance().getIp();
				if (peers.containsKey(ip)){
					observedBy = peers.get(ip);
	            } else {
	                peers.put(ip, peers.size());
	                observedBy = peers.size() - 1;
	            }
			}
			
			
			if (peers.containsKey(peerIp)) {
                this.peerIp = peers.get(peerIp);
            } else {
                peers.put(peerIp, peers.size());
                this.peerIp = peers.size() - 1;
            }
		}
		
		
	}

	

	public float getX() {
		return coord==null?0:coord.getX();
	}

	public float getY() {
		return coord==null?0:coord.getY();
	}

	public float getH() {
		return coord==null?0:coord.getH();
	}
	
	public float[] getV2Coords(){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		if (position2!=null){
			try {
				position2.serialise(dos);
				DataInputStream dis = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
				byte version = dis.readByte();
				if (version != 4) return null;
				float coords[] = new float[5];
				int i = 0;
				while (dis.available()>0 && i < coords.length){
					coords[i] = dis.readFloat();
					i++;
				}
				return coords;
				//coords[i] = -1;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		return null;
	}
}
