/**
 * Ono Project
 *
 * File:         PingResult.java
 * RCS:          $Id: PingResultOess.java,v 1.2 2010/03/29 16:48:04 drc915 Exp $
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


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 * 
 *         The PingResult class ...
 */
public class PingResultOess {

	/** maps ids to strings */
	public String sourceIp;
	public int sourceId;
	public String destIp;
	public int destId;
	public float rtt;
	public long time;
	//destType = 1 (DNS), 2 (EDGE)
	public int destType;
	
	/**
     *
     */
	public PingResultOess() {
		super();

		// TODO Auto-generated constructor stub
	}
	
	
	public PingResultOess(String sIp, String dIp, double rtt, long time, int type)
	{
		this.destType = type;
		this.rtt = (float) rtt;
		this.time = time;
		this.sourceIp = sIp;
		this.destIp = dIp;
		this.destId = -1;
		this.sourceId = -1;
		
	}

}
