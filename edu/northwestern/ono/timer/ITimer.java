/**
 * Ono Project
 *
 * File:         ITimer.java
 * RCS:          $Id: ITimer.java,v 1.1 2007/02/01 16:52:18 drc915 Exp $
 * Description:  ITimer class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 27, 2007 at 2:21:02 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.util
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


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The ITimer class ...
 */
public interface ITimer {
	/**
	 * Create a single-shot event with delay
	 * @param when			when it is to occur (absolute time, not relative)
	 * @param performer
	 * @return
	 */

public ITimerEvent addEvent( long when, ITimerEventPerformer	performer );

	/**
	 * Create a periodic event that will fire every specified number of milliseconds until cancelled
	 * or the timer is destroyed
	 * @param periodic_millis
	 * @param performer
	 * @return
	 */

public ITimerEvent
addPeriodicEvent(
	long					periodic_millis,
	ITimerEventPerformer	performer );

	/**
	 * Releases resources associated with this timer and renders it unusable
	 */

public void
destroy();
}
