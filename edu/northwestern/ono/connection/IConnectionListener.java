/**
 * Ono Project
 *
 * File:         IConnectionListener.java
 * RCS:          $Id: IConnectionListener.java,v 1.4 2010/03/29 16:48:04 drc915 Exp $
 * Description:  IConnectionListener class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 26, 2007 at 10:10:50 AM
 * Language:     Java
 * Package:      edu.northwestern.ono.connection
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
package edu.northwestern.ono.connection;

import edu.northwestern.ono.util.NewTagByteBuffer;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The IConnectionListener interface specifies the events that a listener 
 * can registere for on a connection.
 */
public interface IConnectionListener {

	/**
	 * Called when a connection is established
	 * @param connection the connection
	 */
	public void
	connected(
		IOnoConnection	connection );
	
	/**
	 * Called when a message is received
	 * @param connection the connection
	 * @param message the message received over the connection
	 * @throws IMessageException
	 */
	public void
	receive(
		IOnoConnection	connection,
		NewTagByteBuffer			message )
	
		throws IMessageException;
	
	/**
	 * Called when the connection fails (e.g., is closed unexpectedly)
	 * @param connection the connection the failed
	 * @param error the error
	 * @throws IMessageException
	 */
	public void
	failed(
		IOnoConnection	connection,
		Throwable 					error )
	
		throws IMessageException;
}
