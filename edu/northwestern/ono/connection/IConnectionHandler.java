/**
 * Ono Project
 *
 * File:         IConnectionHandler.java
 * RCS:          $Id: IConnectionHandler.java,v 1.2 2007/02/06 19:47:22 drc915 Exp $
 * Description:  IConnectionHandler class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 26, 2007 at 10:23:48 AM
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

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The IConnectionHandler interface specifies how to handle incoming connections.
 */
public interface IConnectionHandler {
	
	/**
	 * Signifies an incoming connection. Subclasses must return true to establish the 
	 * connection.
	 * @param connection the incoming connection
	 * @return true if accepted; false otherwise
	 * @throws IMessageException
	 */
	public boolean
	accept(
		IOnoConnection	connection )
	
		throws IMessageException;
}
