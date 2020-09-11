/**
 * Ono Project
 *
 * File:         AzureusLoggerChannel.java
 * RCS:          $Id: AzureusLoggerChannel.java,v 1.1 2007/02/01 16:52:19 drc915 Exp $
 * Description:  AzureusLoggerChannel class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 26, 2007 at 9:02:47 AM
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
package edu.northwestern.ono.util;

import org.gudy.azureus2.plugins.logging.LoggerChannel;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The AzureusLoggerChannel class ...
 */
public class AzureusLoggerChannel implements ILoggerChannel {

	LoggerChannel log;
	
	public AzureusLoggerChannel(LoggerChannel log){
		this.log = log;
	}
	
	/* (non-Javadoc)
	 * @see edu.northwestern.ono.util.ILoggerChannel#getName()
	 */
	public String getName() {
		// TODO Auto-generated method stub
		return log.getName();
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.util.ILoggerChannel#isEnabled()
	 */
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return log.isEnabled();
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.util.ILoggerChannel#log(int, java.lang.String)
	 */
	public void log(int log_type, String data) {
		log.log(log_type, data);

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.util.ILoggerChannel#log(java.lang.String)
	 */
	public void log(String data) {
		if (log!=null);
		log.log(data);

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.util.ILoggerChannel#log(java.lang.Throwable)
	 */
	public void log(Throwable error) {
		log.log(error);

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.util.ILoggerChannel#log(java.lang.String, java.lang.Throwable)
	 */
	public void log(String data, Throwable error) {
		log.log(data, error);

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.util.ILoggerChannel#log(java.lang.Object[], int, java.lang.String)
	 */
	public void log(Object[] relatedTo, int log_type, String data) {
		log.log(relatedTo, log_type, data);

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.util.ILoggerChannel#log(java.lang.Object, int, java.lang.String)
	 */
	public void log(Object relatedTo, int log_type, String data) {
		log.log(relatedTo, log_type, data);

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.util.ILoggerChannel#log(java.lang.Object, java.lang.String, java.lang.Throwable)
	 */
	public void log(Object relatedTo, String data, Throwable error) {
		log.log(relatedTo, data, error);

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.util.ILoggerChannel#log(java.lang.Object[], java.lang.String, java.lang.Throwable)
	 */
	public void log(Object[] relatedTo, String data, Throwable error) {
		log.log(relatedTo, data, error);

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.util.ILoggerChannel#logAlert(int, java.lang.String)
	 */
	public void logAlert(int alert_type, String message) {
		log.logAlert(alert_type, message);

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.util.ILoggerChannel#logAlert(java.lang.String, java.lang.Throwable)
	 */
	public void logAlert(String message, Throwable e) {
		log.logAlert(message, e);

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.util.ILoggerChannel#logAlertRepeatable(int, java.lang.String)
	 */
	public void logAlertRepeatable(int alert_type, String message) {
		log.logAlertRepeatable(alert_type, message);

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.util.ILoggerChannel#logAlertRepeatable(java.lang.String, java.lang.Throwable)
	 */
	public void logAlertRepeatable(String message, Throwable e) {
		log.logAlertRepeatable(message, e);

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.util.ILoggerChannel#setDiagnostic()
	 */
	public void setDiagnostic() {
		log.setDiagnostic();

	}

}
