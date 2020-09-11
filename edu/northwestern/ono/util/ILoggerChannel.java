/**
 * Ono Project
 *
 * File:         ILoggerChannel.java
 * RCS:          $Id: ILoggerChannel.java,v 1.1 2007/02/01 16:52:19 drc915 Exp $
 * Description:  ILoggerChannel class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 26, 2007 at 9:00:02 AM
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


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The ILoggerChannel class ...
 */
public interface ILoggerChannel {
	/**
	 * Returns the name of the Logger Channel
	 * 
	 * @return Logger channel name
	 * @since 2.0.7.0
	 */
	public String getName();

	/**
	 * Indicates whether or not logging is enabled - use to optimise calls to 
	 * the log methods that require resources to construct the message to be 
	 * logged.
	 * 
	 * Note that this doesn't apply to alerts - these will always be handled
	 * 
	 * @return Enabled state of logging
	 * 
	 * @since 2.3.0.2
	 */
	public boolean isEnabled();

	/**
	 * This causes the channel to also write to logs/<i>name</i> files in a cyclic 
	 * fashion (c.f. the debug_1/2._log files)
	 * 
	 * @since 2.4.0.2
	 */
	public void setDiagnostic();

	/**
	 * Log a message of a specific type to this channel's logger
	 * 
	 * @param log_type LT_* constant
	 * @param data text to log
	 * 
	 * @since 2.0.7.0
	 */
	public void log(int log_type, String data);

	/**
	 * log text with implicit type {@link #LT_INFORMATION}
	 * 
	 * @param data text to log
	 * 
	 * @since 2.1.0.0
	 */
	public void log(String data);

	/**
	 * log an error with implicit type of {@link #LT_ERROR}
	 * 
	 * @param error Throwable object to log
	 * 
	 * @since 2.0.7.0
	 */
	public void log(Throwable error);

	/**
	 * log an error with implicit type of {@link #LT_ERROR}
	 * 
	 * @param data text to log
	 * @param error Throwable object to log
	 * 
	 * @since 2.0.7.0
	 */
	public void log(String data, Throwable error);

	/**
	 * Log a string against a list of objects
	 * 
	 * @param relatedTo a list of what this log is related to (ex. Peer, Torrent,
	 *                   Download, Object)
	 * @param log_type LT_* constant
	 * @param data text to log
	 * 
	 * @since 2.3.0.7
	 */
	public void log(Object[] relatedTo, int log_type, String data);

	/**
	 * Log an error against an object.
	 * 
	 * @param relatedTo What this log is related to (ex. Peer, Torrent,
	 *         Download, Object, etc)
	 * @param log_type LT_* constant
	 * @param data text to log
	 * 
	 * @since 2.3.0.7
	 */
	public void log(Object relatedTo, int log_type, String data);

	/**
	 * Log an error against an object.
	 * 
	 * @param relatedTo What this log is related to (ex. Peer, Torrent,
	 *         Download, Object, etc)
	 * @param data text to log
	 * @param error Error that will be appended to the log entry
	 * 
	 * @since 2.3.0.7
	 */

	public void log(Object relatedTo, String data, Throwable error);

	/**
	 * Log an error against a list of objects
	 * 
	 * @param relatedTo a list of what this log is related to (ex. Peer, Torrent,
	 *                   Download, Object)
	 * @param data text to log
	 * @param error Error that will be appended to the log entry
	 * 
	 * @since 2.3.0.7
	 */
	public void log(Object[] relatedTo, String data, Throwable error);

	/**
	 * raise an alert to the user, if UI present
	 * Note that messages shown to the user are filtered on unique message content
	 * So if you raise an identical alert the second + subsequent messages will not be
	 * shown. Thus, if you want "identical" messages to be shown, prefix them with something
	 * unique like a timestamp.
	 * 
	 * @param alert_type LT_* constant
	 * @param message text to alert user with
	 * 
	 * @since 2.0.8.0
	 */
	public void logAlert(int alert_type, String message);

	/**
	 * Alert the user of an error
	 * 
	 * @param message text to alert user with
	 * @param e Error that will be attached to the alert
	 * 
	 * @since 2.1.0.2
	 */
	public void logAlert(String message, Throwable e);

	/**
	 * Raise an alert to the user, if UI present. Subsequent, identical messages
	 * will always generate an alert (i.e. duplicates won't be filtered)
	 * 
	 * @param alert_type LT_* constant
	 * @param message text to alert user with
	 * 
	 * @since 2.1.0.2
	 */
	public void logAlertRepeatable(int alert_type, String message);

	/**
	 * Raise an alert to the user, if UI present. Subsequent, identical messages
	 * will always generate an alert (i.e. duplicates won't be filtered)
	 * 
	 * @param message text to alert user with
	 * @param e Error that will be attached to the alert
	 * 
	 * @since 2.1.0.2
	 */
	public void logAlertRepeatable(String message, Throwable e);


}
