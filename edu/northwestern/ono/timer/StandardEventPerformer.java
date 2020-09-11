/**
 * Ono Project
 *
 * File:         StandardEventPerformer.java
 * RCS:          $Id: StandardEventPerformer.java,v 1.3 2007/10/01 13:00:18 drc915 Exp $
 * Description:  StandardEventPerformer class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 29, 2007 at 1:11:54 PM
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

import java.util.TimerTask;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The StandardEventPerformer class ...
 */
public class StandardEventPerformer extends TimerTask implements ITimerEventPerformer {

	private ITimerEventPerformer performer;
	private StandardTimerEvent ste;

	public StandardEventPerformer(ITimerEventPerformer performer, StandardTimerEvent ste) {
		this.performer = performer;
		this.ste = ste;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.timer.ITimerEventPerformer#perform(edu.northwestern.ono.timer.ITimerEvent)
	 */
	public void perform(ITimerEvent event) {
		performer.perform(event);		

	}

	public void run() {
		try {
		performer.perform(ste);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public boolean cancel(){
		return super.cancel();
	}

}
