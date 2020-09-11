/**
 * Ono Project
 *
 * File:         PingResult.java
 * RCS:          $Id: PingResult.java,v 1.5 2006/11/27 13:26:05 drc915 Exp $
 * Description:  PingResult class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 16, 2006 at 8:14:21 AM
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
 * The PingResult class ...
 */
public class PingResult {
    /** maps ids to strings */
    public static HashMap<String, Integer> edges = new HashMap<String, Integer>();
    public static HashMap<String, Integer> peers = Statistics.peerMap;
    public int edge; // -1 means direct path
    public int source;
    public int dest;
    public int middle; // -1 means the edge server was pinged
    public float rtt;
    public long time;
    public int myIp;
    public int experimentId;

    /**
     *
     */
    public PingResult() {
        super();

        // TODO Auto-generated constructor stub
    }

    public PingResult(String e, String m, String d, String myIp, double rtt,
        long time, int experimentId) {
        if (e.equals("-1")) {
            this.edge = -1;
        } else if (edges.containsKey(e)) {
            this.edge = edges.get(e);
        } else {
            edges.put(e, edges.size());
            this.edge = edges.size() - 1;
        }

        synchronized (peers) {
            if (m.equals(e)) {
                this.middle = -1; // pinged the edge        
            } else if (peers.containsKey(m)) {
                this.middle = peers.get(m);
            } else {
                peers.put(m, peers.size());
                this.middle = peers.size() - 1;
            }

            if (peers.containsKey(d)) {
                this.dest = peers.get(d);
            } else {
                peers.put(d, peers.size());
                this.dest = peers.size() - 1;
            }

            if (peers.containsKey(myIp)) {
                this.myIp = peers.get(myIp);
            } else {
                peers.put(myIp, peers.size());
                this.myIp = peers.size() - 1;
            }
        }

        this.rtt = (float) rtt;
        this.time = time;
        this.experimentId = experimentId;
    }
}
