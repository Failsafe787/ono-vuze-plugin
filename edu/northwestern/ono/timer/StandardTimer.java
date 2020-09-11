/**
 * Ono Project
 *
 * File:         StandardTimer.java
 * RCS:          $Id: StandardTimer.java,v 1.7 2010/03/29 16:48:04 drc915 Exp $
 * Description:  StandardTimer class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 29, 2007 at 1:02:50 PM
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

import java.util.Date;
import java.util.Timer;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The StandardTimer class ...
 */
public class StandardTimer implements ITimer {
	
	Timer timer;
	private String myName;
//	Set<TimerTask> tasks = new HashSet<TimerTask>();

	public StandardTimer(String name) {
		myName = name;
		timer = new Timer(name);
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.timer.ITimer#addEvent(long, edu.northwestern.ono.timer.ITimerEventPerformer)
	 */
	public ITimerEvent addEvent(long when, ITimerEventPerformer performer) {
		// TODO Auto-generated method stub
		StandardTimerEvent ste = new StandardTimerEvent();
		StandardEventPerformer sep = new StandardEventPerformer(performer, ste);
//		tasks.add(sep);
		ste.setTimer(this);
		ste.setEventPerformer(sep);
		Date d = new Date(when);
		timer.schedule(sep, d);
		
		return ste;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.timer.ITimer#addPeriodicEvent(long, edu.northwestern.ono.timer.ITimerEventPerformer)
	 */
	public ITimerEvent addPeriodicEvent(long periodic_millis,
			ITimerEventPerformer performer) {
		StandardTimerEvent ste = new StandardTimerEvent();
		StandardEventPerformer sep = new StandardEventPerformer(performer, ste);
//		tasks.add(sep);
		ste.setTimer(this);
		ste.setEventPerformer(sep);
		try {
			timer.schedule(sep, periodic_millis, periodic_millis);
		} catch (Exception e ){
			e.printStackTrace();
		}
		
		return ste;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.timer.ITimer#destroy()
	 */
	public void destroy() {
		timer.cancel();
		timer.purge();
		StandardTimerManager.getInstance().destroy(this);

	}

	public void cancel() {
		timer.cancel();
//		tasks.remove(event);
		
	}

	public String getName() {
		// TODO Auto-generated method stub
		return myName;
	}

}
