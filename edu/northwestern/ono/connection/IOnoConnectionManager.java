/**
 * Ono Project
 *
 * File:         IOnoConnection.java
 * RCS:          $Id: IOnoConnectionManager.java,v 1.5 2007/12/26 18:13:58 drc915 Exp $
 * Description:  IOnoConnection class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 26, 2007 at 9:48:51 AM
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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;

import edu.northwestern.ono.util.NewTagByteBuffer;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The IOnoConnection interface defines methods for communicating
 * with other Ono peers. 
 */
public interface IOnoConnectionManager {

	/**
	 * Shortcut to sent some bytes over an existing connection.
	 * 
	 * @param bytes the bytes to send
	 * @param ipAddress the string representation of the ip address to send to
	 */
	public void send(byte bytes[], String ipAddress);

	/**
	 * Registers a connection listener with this manager. Required for accepting 
	 * incoming connections. 
	 * 
	 * @param name the name of the type of message to listen for
	 * @param description the description of this message type
	 * @param manager the object that will receive accept calls
	 */
	public void registerManagerListener(String name, String description, 
			IConnectionHandler manager);

	/**
	 * Establishes a connection to the endpoint and registers the specified listener 
	 * for that connection.
	 * 
	 * @param endPoint the endpoint to connect to
	 * @param runner the listener for the connection
	 * @param useTCP if true, establishes a TCP connection; UDP otherwise
	 */
	public void connect(InetSocketAddress endPoint, IConnectionListener runner, boolean useTCP);
	
	
	/**
	 * Returns a byte buffer to the pool.
	 * @param bb the byte buffer to return to the pool.
	 */
	public void returnToPool(NewTagByteBuffer bb);
	
	/**
	 * Allows implementations to use pools.
	 * @param size the size of the buffer to get
	 */
	public NewTagByteBuffer getByteBuffer(int size);
	
	/**
	 * Returns true if the connection has already been established and is cached.
	 * @param sock the endpoint to test for a connection
	 * @return
	 */
	public boolean hasConnection(InetSocketAddress sock);
	
	/**
	 * Returns the set of active connections.
	 * @return
	 */
	public Map<InetSocketAddress, IOnoConnection> getActiveConnections();

	/**
	 * Get a reference to an existing connection on the specified socket
	 * @param dest the endpoint to retrieve
	 * @return
	 */
	public IOnoConnection getConnection(InetSocketAddress dest);
	
    /**
     * Get a reference to an existing connection on the specified address
     * @param dest the endpoint to retrieve
     * @return
     */
    public IOnoConnection getConnection(InetAddress dest);
	
}
