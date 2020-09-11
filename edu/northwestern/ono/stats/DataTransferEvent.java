/**
 * Ono Project
 *
 * File:         DataTransferEvent.java
 * RCS:          $Id: DataTransferEvent.java,v 1.7 2008/10/17 12:08:20 drc915 Exp $
 * Description:  DataTransferEvent class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Sep 2, 2006 at 9:06:25 AM
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
 * The DataTransferEvent class ...
 */
public class DataTransferEvent {
    public final static HashMap<String, Integer> peers = Statistics.peerMap;
    public long time; // time of event in nanoseconds
    public int experimentId; // the experiment it belongs to, -1 means unknown
    public byte type; // the message type received
    public int size; // the size of the message received
    public int source; // the source id
    public int dest; // the dest id
    public int current; // the id of the node where we are at
    public double raceResultPrimary = -1; // bps of primary node
    public double raceResultSecondary = -1; // bps of secondary node

    public DataTransferEvent(String source, String current, String dest,
        int id, int size, long time, byte type) {
        if ((source == null) || current.equals("-1") || dest.equals("-1")) {
            throw new RuntimeException("Null values!");
        }

        this.current = mapPeer(current);
        this.dest = mapPeer(dest);
        experimentId = id;
        this.size = size;
        this.source = mapPeer(source);
        this.time = time;
        this.type = type;
    }

    public void setRaceResults(double primary, double secondary) {
        raceResultPrimary = primary;
        raceResultSecondary = secondary;
    }

    public int mapPeer(String ip) {
        synchronized (peers) {
            if (peers.containsKey(ip)) {
                return peers.get(ip);
            } else {
                peers.put(ip, peers.size());

                return peers.size() - 1;
            }
        }
    }
}
