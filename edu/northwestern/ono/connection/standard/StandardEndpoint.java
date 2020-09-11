/**
 * Ono Project
 *
 * File:         StandardEndpoint.java
 * RCS:          $Id: StandardEndpoint.java,v 1.2 2007/02/20 15:24:59 drc915 Exp $
 * Description:  StandardEndpoint class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 30, 2007 at 3:03:58 PM
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
package edu.northwestern.ono.connection.standard;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import edu.northwestern.ono.connection.IEndpoint;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The StandardEndpoint class ...
 */
public class StandardEndpoint implements IEndpoint {

	private SocketChannel channel;
	private InetSocketAddress connectSocket;

	public StandardEndpoint(SocketChannel channel, InetSocketAddress connectSocket) {
		this.channel = channel;
		this.connectSocket = connectSocket;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IEndpoint#addTCP(java.net.InetSocketAddress)
	 */
	public void addTCP(InetSocketAddress target) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IEndpoint#addUDP(java.net.InetSocketAddress)
	 */
	public void addUDP(InetSocketAddress target) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IEndpoint#getNotionalAddress()
	 */
	public InetSocketAddress getNotionalAddress() {
		if (channel.socket().isConnected()){
		return new InetSocketAddress(channel.socket().getInetAddress(), channel.socket().getLocalPort());
		} else {
			return connectSocket;
		}
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IEndpoint#getTCP()
	 */
	public InetSocketAddress getTCP() {
	
		return new InetSocketAddress(channel.socket().getInetAddress(), channel.socket().getLocalPort());
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IEndpoint#getUDP()
	 */
	public InetSocketAddress getUDP() {
		// TODO Auto-generated method stub
		return null;
	}

}
