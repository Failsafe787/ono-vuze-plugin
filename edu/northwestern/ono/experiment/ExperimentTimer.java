/**
 * Ono Project
 *
 * File:         ExperimentTimer.java
 * RCS:          $Id: ExperimentTimer.java,v 1.4 2007/02/21 03:57:11 drc915 Exp $
 * Description:  ExperimentTimer class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 19, 2006 at 3:19:33 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.experiment
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
package edu.northwestern.ono.experiment;

import edu.northwestern.ono.timer.ITimer;
import edu.northwestern.ono.timer.ITimerEvent;
import edu.northwestern.ono.timer.ITimerEventPerformer;


public class ExperimentTimer implements ITimerEventPerformer {
    OnoExperiment onoExp;
    IExperimentRunner runner;
	private ITimer timer;

    /**
     * @param timer 
     * @param result
     */
    public ExperimentTimer(IExperimentRunner runner, OnoExperiment exp, ITimer timer) {
        // TODO Auto-generated constructor stub
        onoExp = exp;
        this.runner = runner;
        this.timer = timer;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.utils.UTTimerEventPerformer#perform(org.gudy.azureus2.plugins.utils.UTTimerEvent)
     */
    public void perform(ITimerEvent event) {
        // make connection to other peer
        runner.beginExperiment(onoExp); // this sets a chain of events that leads to the experiment being performed        
        timer.destroy();
    }
}
