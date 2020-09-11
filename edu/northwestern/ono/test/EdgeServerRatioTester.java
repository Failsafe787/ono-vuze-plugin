/**
 * Ono Project
 *
 * File:         EdgeServerRatioTester.java
 * RCS:          $Id: EdgeServerRatioTester.java,v 1.4 2010/03/29 16:48:04 drc915 Exp $
 * Description:  EdgeServerRatioTester class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 17, 2007 at 6:02:01 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.test
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
package edu.northwestern.ono.test;

import java.util.ArrayList;
import java.util.Map;

import edu.northwestern.ono.position.OnoPeerManager;
import edu.northwestern.ono.stats.EdgeServerRatio;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The EdgeServerRatioTester class tests to see if the edge-server 
 * ratio stuff is really working in the DHT.
 */
public class EdgeServerRatioTester extends Thread {

	ArrayList<String> ips = new ArrayList<String>();
	
	public EdgeServerRatioTester(){
		ips.add("165.124.182.135");
	}
	
	@Override
	public void run() {
		
		while (true){
			Map<String, EdgeServerRatio> ratio;
			for (String peerIp : ips){
				ratio = OnoPeerManager.getInstance().getRatios(peerIp, true);
				if (ratio!=null){
					System.out.println("Got ratio!");
				}
			}
			
			try {
				Thread.sleep(30*1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	
}
