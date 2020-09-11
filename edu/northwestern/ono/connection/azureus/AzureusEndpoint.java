/**
 * Ono Project
 *
 * File:         AzureusEndpoint.java
 * RCS:          $Id: AzureusEndpoint.java,v 1.1 2007/02/01 19:31:49 drc915 Exp $
 * Description:  AzureusEndpoint class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 26, 2007 at 3:39:15 PM
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
package edu.northwestern.ono.connection.azureus;

import java.net.InetSocketAddress;

import org.gudy.azureus2.plugins.messaging.generic.GenericMessageEndpoint;

import edu.northwestern.ono.connection.IEndpoint;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The AzureusEndpoint class ...
 */
public class AzureusEndpoint implements IEndpoint {
	
	private GenericMessageEndpoint ep;
	
	public AzureusEndpoint(GenericMessageEndpoint ep){
		this.ep=ep;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IEndpoint#addTCP(java.net.InetSocketAddress)
	 */
	public void addTCP(InetSocketAddress target) {
		ep.addTCP(target);

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IEndpoint#addUDP(java.net.InetSocketAddress)
	 */
	public void addUDP(InetSocketAddress target) {
		ep.addUDP(target);

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IEndpoint#getNotionalAddress()
	 */
	public InetSocketAddress getNotionalAddress() {
		// TODO Auto-generated method stub
		return ep.getNotionalAddress();
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IEndpoint#getTCP()
	 */
	public InetSocketAddress getTCP() {
		// TODO Auto-generated method stub
		return ep.getTCP();
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IEndpoint#getUDP()
	 */
	public InetSocketAddress getUDP() {
		// TODO Auto-generated method stub
		return ep.getUDP();
	}

}
