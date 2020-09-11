/**
 * Ono Project
 *
 * File:         AzureusConnectionListener.java
 * RCS:          $Id: AzureusConnectionListener.java,v 1.4 2007/12/26 18:13:58 drc915 Exp $
 * Description:  AzureusConnectionListener class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 26, 2007 at 10:18:59 AM
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

import java.nio.ByteBuffer;
import java.util.HashMap;

import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnection;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnectionListener;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;

import edu.northwestern.ono.connection.IConnectionListener;
import edu.northwestern.ono.connection.IOnoConnection;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The AzureusConnectionListener class ...
 */
public class AzureusConnectionListener implements GenericMessageConnectionListener {

	public static HashMap<IConnectionListener, AzureusConnectionListener> listeners = new 
		HashMap<IConnectionListener, AzureusConnectionListener>();
	
	IConnectionListener listener;
	AzureusOnoConnectionManager manager;
	
	public AzureusConnectionListener(IConnectionListener listener, AzureusOnoConnectionManager manager){
		this.listener = listener;
		this.manager = manager;
		listeners.put(listener, this);
	}
	

	public void connected(GenericMessageConnection connection) {
	            		
		listener.connected(manager.cacheConnection(connection));
		 
		
	}

	public void failed(GenericMessageConnection connection, Throwable error) throws MessageException {
		IOnoConnection con = manager.getOnoConnection(connection);
		manager.failed(connection, error);
		listener.failed(con, error);
		
		
		
	}

	public void receive(GenericMessageConnection connection, PooledByteBuffer message) throws MessageException {
		ByteBuffer bb = message.toByteBuffer();
		manager.addPooledByteBuffer(bb, message);
		throw new RuntimeException("Not implemented!");
//		listener.receive(manager.getOnoConnection(connection), bb);
		
		
		
	}
	
	

}
