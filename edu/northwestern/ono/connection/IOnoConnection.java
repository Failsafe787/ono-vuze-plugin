/**
 * Ono Project
 *
 * File:         IOnoConnection.java
 * RCS:          $Id: IOnoConnection.java,v 1.3 2007/12/26 18:13:58 drc915 Exp $
 * Description:  IOnoConnection class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 26, 2007 at 10:13:20 AM
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
 * The IOnoConnection class ...
 */
public interface IOnoConnection {
	
	/**
	 * Returns the endpoint for this connection
	 * @return
	 */
	public IEndpoint
	getEndpoint();
	
	/**
	 * Connects to the endpoint.
	 * @throws IMessageException
	 */
	public void
	connect()
	
		throws IMessageException;
	
	/**
	 * Sends data to the endpiont
	 * @param message the data to send
	 * @throws IMessageException
	 */
	public void
	send(
		NewTagByteBuffer message )
	
		throws IMessageException;
	
	/**
	 * Closes the connection
	 * @throws IMessageException
	 */
	public void
	close()
	
		throws IMessageException;
	
	/**
	 * Returns the largest possible message over this connection
	 * @return
	 */
	public int
	getMaximumMessageSize();
	
	/**
	 * Adds a listener to this connection.
	 * @param listener
	 */
	public void
	addListener(
		IConnectionListener		listener );
	
	/**
	 * Removes the specified listener from this connection.
	 * @param listener
	 */
	public void
	removeListener(
		IConnectionListener		listener );
	
	/**
	 * Returns true if the local host initiated the connection
	 * @return
	 */
	public boolean isLocallyIniated();
}
