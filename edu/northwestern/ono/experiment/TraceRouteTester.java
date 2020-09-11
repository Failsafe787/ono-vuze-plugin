/**
 * Ono Project
 *
 * File:         TraceRouteTester.java
 * RCS:          $Id: TraceRouteTester.java,v 1.6 2010/03/29 16:48:04 drc915 Exp $
 * Description:  TraceRouteTester class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Feb 25, 2007 at 3:40:20 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.experiment
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
package edu.northwestern.ono.experiment;

import edu.northwestern.ono.stats.Statistics;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The TraceRouteTester class tests traceroute functionality
 */
public class TraceRouteTester {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TraceRouteRunner.getInstance().addIp("66.156.76.142");
		
		try {
			Thread.sleep(60*1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Statistics.getInstance().reportTraces();
		
		System.exit(0);
		
		
		

	}

}
