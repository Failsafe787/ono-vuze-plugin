/**
 * Ono Project
 *
 *  File:         DNSEvent.java
 * RCS:          $Id: DNSEvent.java,v 1.8 2010/06/23 20:45:03 drc915 Exp $
 * Description:  DNSEvent class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jun 14, 2006 at 8:03:10 AM
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

import java.util.HashMap;



/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The DNSEvent class represents a single DNS event.
 */
public class DNSEvent {
    /** maps ids to strings */
    public static HashMap<String, Integer> customers = new HashMap<String, Integer>();
    public static HashMap<String, Integer> edges = new HashMap<String, Integer>();
    public static HashMap<String, Integer> peers = Statistics.peerMap;
    public int customer;
    public int edgeServer;
    public int peer;
    public EventType event;
    public long time; 

    /**
     * @param customer
     * @param edgeServer
     * @param event
     */
    public DNSEvent(String peer, String customer, String edgeServer,
        EventType event) {
        if (customers.containsKey(customer)) {
            this.customer = customers.get(customer);
        } else {
            customers.put(customer, customers.size());
            this.customer = customers.size() - 1;
        }

        synchronized (edges){
	        if (edges.containsKey(edgeServer)) {
	            this.edgeServer = edges.get(edgeServer);
	        } else {
	            edges.put(edgeServer, edges.size());
	            this.edgeServer = edges.size() - 1;
	        } 
        }

        synchronized (peers) {
            if (peers.containsKey(peer)) {
                this.peer = peers.get(peer);
            } else {
                peers.put(peer, peers.size());
                this.peer = peers.size() - 1;
            }
        }

        this.event = event;
        this.time = System.currentTimeMillis();
    }
    public enum EventType {NEW_ENTRY,
        REORDER;
    }
}
