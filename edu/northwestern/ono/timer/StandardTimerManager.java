/**
 * Ono Project
 *
 * File:         StandardTimerManager.java
 * RCS:          $Id: StandardTimerManager.java,v 1.1 2007/02/01 16:52:18 drc915 Exp $
 * Description:  StandardTimerManager class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 29, 2007 at 1:04:10 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.timer
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
package edu.northwestern.ono.timer;

import java.util.HashMap;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The StandardTimerManager class ...
 */
public class StandardTimerManager {

	private static StandardTimerManager self;
	
	private HashMap<String, ITimer> timers;
	
	public StandardTimerManager(){
		timers = new HashMap<String, ITimer>();
	}
	
	public static StandardTimerManager getInstance(){
		if (self==null){
			self = new StandardTimerManager();
		}
		return self;
	}

	public void destroy(StandardTimer timer) {
		timers.remove(timer.getName());
		
	}
	
	public ITimer createTimer(String name){
		StandardTimer st = new StandardTimer(name);
		ITimer old = timers.get(name);
		if (old!=null && old instanceof StandardTimer){
			((StandardTimer)old).destroy();
		}
		timers.put(name, st);
		return st;
		
	}
	
	
	
}
