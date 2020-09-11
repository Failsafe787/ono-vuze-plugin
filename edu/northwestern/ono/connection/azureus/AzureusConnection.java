/**
 * Ono Project
 *
 * File:         AzureusConnection.java
 * RCS:          $Id: AzureusConnection.java,v 1.4 2010/03/29 16:48:04 drc915 Exp $
 * Description:  AzureusConnection class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 26, 2007 at 10:28:20 AM
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

import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnection;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;

import edu.northwestern.ono.connection.IConnectionListener;
import edu.northwestern.ono.connection.IEndpoint;
import edu.northwestern.ono.connection.IMessageException;
import edu.northwestern.ono.connection.IOnoConnection;
import edu.northwestern.ono.util.NewTagByteBuffer;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The AzureusConnection class ...
 */
public class AzureusConnection implements IOnoConnection {
	
	GenericMessageConnection connection;
	AzureusOnoConnectionManager manager;
	boolean isSenderInitiated = false;

	public AzureusConnection(GenericMessageConnection connection, 
			AzureusOnoConnectionManager manager, boolean isSenderInitiated) {
		this.connection = connection;
		this.manager = manager;
		this.isSenderInitiated = isSenderInitiated;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnection#addListener(edu.northwestern.ono.connection.IConnectionListener)
	 */
	public void addListener(IConnectionListener listener) {
		connection.addListener(new AzureusConnectionListener(listener, manager));

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnection#close()
	 */
	public void close() throws IMessageException {
		try {
			manager.removeConnection(this);
			connection.close();
		} catch (MessageException e) {
			// TODO Auto-generated catch block
			throw new IMessageException(e);
		}

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnection#connect()
	 */
	public void connect() throws IMessageException {
		try {
			connection.connect();
		} catch (MessageException e) {
			// TODO Auto-generated catch block
			throw new IMessageException(e);
		}

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnection#getEndpoint()
	 */
	public IEndpoint getEndpoint() {
		// TODO Auto-generated method stub
		return new AzureusEndpoint(connection.getEndpoint());
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnection#getMaximumMessageSize()
	 */
	public int getMaximumMessageSize() {
		// TODO Auto-generated method stub
		return connection.getMaximumMessageSize();
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnection#removeListener(edu.northwestern.ono.connection.IConnectionListener)
	 */
	public void removeListener(IConnectionListener listener) {
		connection.removeListener(AzureusConnectionListener.listeners.get(listener));

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnection#send(byte[])
	 */
	public void send(NewTagByteBuffer message) throws IMessageException {
		
		try {
			PooledByteBuffer pbb = manager.getPooledByteBuffer(message.getBuffer());
			if (pbb.toByteBuffer()==null){
				System.err.println("eek");
			}
			connection.send(pbb);
		} catch (MessageException e) {
			// TODO Auto-generated catch block
			throw new IMessageException(e);
		}

	}

	public boolean isLocallyIniated() {
		// TODO Auto-generated method stub
		return isSenderInitiated;
	}

}
