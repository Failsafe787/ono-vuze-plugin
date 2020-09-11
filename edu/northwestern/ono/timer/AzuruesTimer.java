/**
 * Ono Project
 *
 * File:         AzuruesTimer.java
 * RCS:          $Id: AzuruesTimer.java,v 1.3 2009/02/09 13:47:07 drc915 Exp $
 * Description:  AzuruesTimer class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 27, 2007 at 2:34:14 PM
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

import org.gudy.azureus2.plugins.utils.UTTimer;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The AzuruesTimer class ...
 */
public class AzuruesTimer implements ITimer {

	private UTTimer timer;

	public AzuruesTimer(UTTimer timer) {
		this.timer = timer;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.timer.ITimer#addEvent(long, edu.northwestern.ono.timer.ITimerEventPerformer)
	 */
	public ITimerEvent addEvent(long when, ITimerEventPerformer performer) {
		// TODO Auto-generated method stub
		return new AzureusTimerEvent(timer.addEvent(when, new AzureusTimerEventPerformer(performer)));
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.timer.ITimer#addPeriodicEvent(long, edu.northwestern.ono.timer.ITimerEventPerformer)
	 */
	public ITimerEvent addPeriodicEvent(long periodic_millis,
			ITimerEventPerformer performer) {
		// TODO Auto-generated method stub
		return new AzureusTimerEvent(timer.addPeriodicEvent(periodic_millis, new AzureusTimerEventPerformer(performer)));
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.timer.ITimer#destroy()
	 */
	public void destroy() {
		timer.destroy();

	}

}
