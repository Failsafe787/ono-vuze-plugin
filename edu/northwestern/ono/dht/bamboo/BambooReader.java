/**
 * Ono Project
 *
 * File:         BambooReader.java
 * RCS:          $Id: BambooReader.java,v 1.5 2010/03/29 16:48:04 drc915 Exp $
 * Description:  BambooReader class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Feb 13, 2007 at 10:46:50 AM
 * Language:     Java
 * Package:      edu.northwestern.ono.dht.bamboo
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
package edu.northwestern.ono.dht.bamboo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import edu.northwestern.ono.dht.IDDBReadAction;
import edu.northwestern.ono.dht.IDistributedDatabase;
import edu.northwestern.ono.dht.IDistributedDatabaseEvent;
import edu.northwestern.ono.dns.EdgeServerMapManager;
import edu.northwestern.ono.stats.EdgeServerRatio;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The BambooReader class ...
 */
public class BambooReader {
	
	static boolean done = false;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Thread t = new Thread (new BambooDHTManager(true));
			t.setDaemon(true);
			t.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		
		IDistributedDatabase ddb = BambooDHTManager.getInstance();
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		String ip = "206.166.49";

		ddb.read(ip, ip, 30, new IDDBReadAction(){

			public void handleComplete(IDistributedDatabaseEvent event) {
				System.out.println("Read value: complete!");
				done = true;
				
			}

			public void handleRead(byte[] b, IDistributedDatabaseEvent event) {
				if (b.length>0){
		    		ByteBuffer bBuffer = ByteBuffer.wrap(b);
		    		bBuffer.order(ByteOrder.LITTLE_ENDIAN);
		    		byte type = bBuffer.get();
		    		if (type == EdgeServerMapManager.RATIO_ENTRY){
			    		long timestamp = bBuffer.getLong();
			    		int customerIndex = bBuffer.getShort();
			    		EdgeServerRatio esr = new EdgeServerRatio();
			    		esr.deserialize(bBuffer);
			    		esr.setLastTimestamp(timestamp);
			    		System.out.println(esr.toString());
//			    		Digger.getInstance().addRatio(customerIndex, 
//			    				event.getKey().getDescription(), esr);
		    		}
		    		else if (type== EdgeServerMapManager.PEER_RATIO_MAP){
		    			
		    			byte ip[] = new byte[4];
		    			bBuffer.get(ip);
		    			String ipText;
						try {
							ipText = InetAddress.getByAddress(ip).getHostAddress();
						    			
//							Digger.getInstance().getRatios(ipText);
							System.out.println("Found peer "+ ipText);
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		    			
		    		}
		    		
		    	}

			}

			public void handleTimeout(IDistributedDatabaseEvent event) {
				System.out.println("Read value: timeout!");
				done = true;
			}}, 0);
		
		while (!done){
			try {
				Thread.sleep(15*1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

    private static String getEdgeServerCluster(byte buffer[]) {
    	byte sVal[] = new byte[2];
    	sVal[1] = 0;
    	ByteBuffer bBuffer;
    	StringBuffer sb = new StringBuffer();
    	for (int i = 0; i < buffer.length; i++){
	    	sVal[0] = buffer[i];
	    	bBuffer = ByteBuffer.wrap(sVal);
	    	bBuffer.order(ByteOrder.LITTLE_ENDIAN);
	    	sb.append(bBuffer.getShort());
	    	if (i!=2) sb.append('.');
    	}
    	
		return sb.toString();
	}
}
