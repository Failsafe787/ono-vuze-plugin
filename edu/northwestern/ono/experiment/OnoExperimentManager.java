/**
 * Ono Project
 *
 * File:         Pinger.java
 * RCS:          $Id: OnoExperimentManager.java,v 1.38 2008/04/17 02:19:07 npb853 Exp $
 * Description:  Pinger class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 8, 2006 at 10:50:54 AM
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

import java.util.HashSet;
import java.util.Properties;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.connection.IConnectionHandler;
import edu.northwestern.ono.connection.IMessageException;
import edu.northwestern.ono.connection.IOnoConnection;
import edu.northwestern.ono.connection.IOnoConnectionManager;
import edu.northwestern.ono.net.SideStepTransferManager;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 * 
 * The Pinger class manages a ping experiemnt.
 */
public class OnoExperimentManager implements Runnable, IConnectionHandler {
	private HashSet<String> experimentSeen;

	private boolean isActive = true;
	// private Digger dig;
	protected boolean compatiblePeerFound = false;
	private IOnoConnectionManager manager;
	private static OnoExperimentManager self;
	private String url;

	/**
	 * 
	 */
	public OnoExperimentManager() {
		super();
		experimentSeen = new HashSet<String>();
		self = this;
	}

	public static OnoExperimentManager getInstance() {
		return self;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		try {
			MainGeneric.getConnectionManager().registerManagerListener(
					"ONO_DATA_MSG", "Ono data message", this);
		} catch (IMessageException e) {
			e.printStackTrace();
		}

		// periodically check for an experiment
		while (isActive) {
			synchronized (SideStepTransferManager.getInstance()) {
				if (!SideStepTransferManager.getInstance().isRunning())
					checkExperiments(false, false);
			}
			try {
				Thread.sleep(MainGeneric.EXPERIMENT_SLEEP_TIME * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} // end while
	}

	public void checkExperiments(boolean failed, boolean finished) {
		url = "http://bandito.cs.northwestern.edu/~drc915/azplugin/experiments.php";
		if (failed) {
			System.out.println("Experiment timed out, starting new one...");
			url = url + "?failed=true";
		}
		if (finished)
			url = failed ? url + "&finished=true" : url + "?finished=true";

		Properties exp = new Properties();

		System.out.println("Downloading properties...");
		exp = MainGeneric.downloadFromURL(url, 60 * 1000);

		if (exp == null)
			return;
		System.out.println(exp.toString());

		OnoExperiment result = OnoExperiment.fromProperties(exp, null);

		// skip if not a ping experiment
		IExperimentRunner expRunner = null;

		if (result.experimentType == OnoExperiment.EXP_PING) {
			expRunner = PingRunner.getInstance();
			experimentSeen.add(String.valueOf(result.experimentId));
		} else if (result.experimentType == OnoExperiment.EXP_DL) {
			expRunner = SideStepTransferManager.getInstance();
			((SideStepTransferManager) expRunner).setManager(manager);
			experimentSeen.add(String.valueOf(result.experimentId));
		} else {
			// continue;
		}

		if (expRunner != null) {
			// downloaded experiment config, now deal with it
			expRunner.processExperiment(result);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gudy.azureus2.plugins.messaging.generic.GenericMessageHandler#accept(org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnection)
	 */
	public boolean accept(IOnoConnection connection) throws IMessageException {
		System.out.println("Incoming connection from "
				+ connection.getEndpoint().getNotionalAddress() + "!");

		synchronized (SideStepTransferManager.getInstance()) {
			if (!SideStepTransferManager.getInstance().isRunning()
					&& !SideStepTransferManager.getInstance().hasExperiment(
							connection))
				checkExperiments(false, false);
		}

		SideStepTransferManager dtr = SideStepTransferManager.getInstance();
		dtr.setManager(manager);
		connection.addListener(dtr);

		return true;
	}

	/**
	 * @param gmr
	 */
	public void setConnectionManager(IOnoConnectionManager manager) {
		this.manager = manager;
	}
}
