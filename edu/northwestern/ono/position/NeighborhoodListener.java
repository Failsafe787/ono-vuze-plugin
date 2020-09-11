/**
 * Ono Project
 *
 * File:         NeighborhoodListener.java
 * RCS:          $Id: NeighborhoodListener.java,v 1.6 2010/03/29 16:48:03 drc915 Exp $
 * Description:  NeighborhoodListener class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Apr 16, 2007 at 4:08:34 PM
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

import java.util.Map;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The NeighborhoodListener class ...
 */
public interface NeighborhoodListener {

	public void foundNewNeighborhoodPeer(GenericPeer p);
	
	/**
	 * This is called when any new IP is found--not known yet if 
	 * this peer is in the neighborhood.
	 * @param allIpsSeen
	 */
	public void foundNewIpAddress(Map<String, String> allIpsSeen);
	
}
