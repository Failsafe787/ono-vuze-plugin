/**
 * Ono Project
 *
 * File:         OptimalEdgeServerExperimentManager.java
 * RCS:          $Id: OessExperimentManager.java,v 1.3 2008/11/16 03:30:07 mas939 Exp $
 * Description:  OptimalEdge class (see below)
 * Author:       Mario Sanchez
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Oct 15, 2008 at 07:38:30 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.experiment
 * Status:       Experimental (Do Not Distribute)
 *
 * (C) Copyright 2008, Northwestern University, all rights reserved.
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

import edu.northwestern.ono.OnoConfiguration;
import edu.northwestern.ono.dns.OessDigger;

/**
 * @author Mario Sanchez &lt;msanchez@u.northwestern.edu&gt;
 * 
 *         The OptimalEdgeManager class manages the Optimal Edge Server
 *         Selection experiment.
 */

public class OessExperimentManager implements Runnable {	
	
	private static boolean isActive = true;
	private static OessExperimentManager self;
	private boolean DEBUG = false;

	//sleepInterval in minutes
	int sleepInterval = 30; 
	static OessDigger oessDig;
	static OessExperiment oessExperiment;
	public boolean oessExperimentFinished = false; 
	
	
	/** map of edge ip to ping value */

	public OessExperimentManager() {
		super();
		self = this;
	}

	public static OessExperimentManager getInstance() {
		return self;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */

	public void run() {
	
		try {
			Thread.sleep(10*1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		while (isActive) {
			
			if (DEBUG) 
				System.out.println("OESSEXPERIMENTMANAGER: " +
						"start oessdigger?: " 
						+ OnoConfiguration.getInstance().isOessEnableExperiment() +
						", start oessExperiment? " 
						+ OnoConfiguration.getInstance().isOessEnableExperiment());
					

			if (OnoConfiguration.getInstance().isOessEnableDigger())
				startOessDigger();
			else
				if (oessDig != null)
					oessDig.setActive(false);
			
			if (OnoConfiguration.getInstance().isOessEnableExperiment() &&
					!oessExperimentFinished ) 
				startOessExperiment();			
			else 
				if (oessExperiment != null)
				oessExperiment.setActive(false);
			
			try {
				
				if (DEBUG)
					System.out.println("OESSEXPERIMENTMANAGER: sleeping for " + 
							sleepInterval + " minutes...");
				
				Thread.sleep(sleepInterval*60*1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
	}
	
	protected void startOessDigger() {
		if (oessDig == null) 
			oessDig = OessDigger.getInstance();
		
        if (!oessDig.isAlive()) 
        	oessDig.start();
	}
	
	protected void startOessExperiment() {
		if (oessExperiment == null) 
			oessExperiment = OessExperiment.getInstance();
		
		if (!oessExperiment.isAlive()) 
			oessExperiment.start(); 
	}
	
	public static void stop() {
		isActive = false;
		if (oessDig != null) oessDig.setActive(false);
		if (oessExperiment != null) oessExperiment.setActive(false);
		self = null;	
		
	}

}
