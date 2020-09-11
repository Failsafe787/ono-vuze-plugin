/**
 * Ono Project
 *
 * File:         PeerObserver.java
 * RCS:          $Id: PeerObserver.java,v 1.1 2007/04/12 21:24:35 drc915 Exp $
 * Description:  PeerObserver class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Mar 27, 2007 at 4:41:28 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.position
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
package edu.northwestern.ono.position;

import java.util.HashSet;

import org.gudy.azureus2.plugins.peers.Peer;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The PeerObserver class ...
 */
public class PeerObserver implements Runnable {
	
	private static PeerObserver self;
	private HashSet<Peer> peers = new HashSet<Peer>();
	/** time to wait between cycles */
	private int sleepTime = 5 * 1000;

	/**
	 * 
	 * @param sleepTime the time to sleep between run operations
	 */
	private PeerObserver(int sleepTime){
		this.sleepTime = sleepTime;
	}
	
	private PeerObserver() {
		// TODO Auto-generated constructor stub
	}

	public static PeerObserver getInstance(){
		if (self == null){
			self = new PeerObserver();
		}
		return self;
	}
	
	public static void addPeer(Peer p){
		synchronized (self){
			self.peers.add(p);
		}
	
	}
	
	public void run() {
		long currentTime, timeDiff;
		
		while (true){
			currentTime = System.currentTimeMillis();
			
			
			// insert functionality
			
			timeDiff = System.currentTimeMillis()-currentTime;
			if (sleepTime*1000 < timeDiff) continue;
			else{
				try {
					Thread.sleep(sleepTime*1000 - timeDiff);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
	}

}
