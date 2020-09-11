/**
 * Ono Project
 *
 * File:         DigResult.java
 * RCS:          $Id: DigResult.java,v 1.5 2006/12/14 18:50:33 drc915 Exp $
 * Description:  DigResult class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jun 26, 2006 at 10:12:50 AM
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
 * The DigResult class ...
 */
public class DigResult {
    /** maps ids to strings */
    public static HashMap<String, Integer> customers = new HashMap<String, Integer>();
    public static HashMap<String, Integer> edges = new HashMap<String, Integer>();
    public final static HashMap<String, Integer> peers = Statistics.peerMap;
    int customer;
    int peer;
    int edge1;
    int edge2;
    long time;

    /**
     *
     */
    public DigResult(String customer, String peer, String edge1, String edge2) {
        if (customers.containsKey(customer)) {
            this.customer = customers.get(customer);
        } else {
            customers.put(customer, customers.size());
            this.customer = customers.size() - 1;
        }

        mapEdge(edge1, true);
        mapEdge(edge2, false);

        synchronized (peers) {
            if (peers.containsKey(peer)) {
                this.peer = peers.get(peer);
            } else {
                peers.put(peer, peers.size());
                this.peer = peers.size() - 1;
            }
        }

        this.time = System.currentTimeMillis();
    }

    /**
     *
     */
    private void mapEdge(String edgeServer, boolean whichVar) {
        if (edges.containsKey(edgeServer)) {
            if (whichVar) {
                edge1 = edges.get(edgeServer);
            } else {
                edge2 = edges.get(edgeServer);
            }
        } else {
            edges.put(edgeServer, edges.size());

            if (whichVar) {
                this.edge1 = edges.size() - 1;
            } else {
                this.edge2 = edges.size() - 1;
            }
        }
    }
}
