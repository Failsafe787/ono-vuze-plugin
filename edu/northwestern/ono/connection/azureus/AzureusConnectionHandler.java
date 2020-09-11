/**
 * Ono Project
 *
 * File:         AzureusConnectionHandler.java
 * RCS:          $Id: AzureusConnectionHandler.java,v 1.1 2007/02/01 19:31:49 drc915 Exp $
 * Description:  AzureusConnectionHandler class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 26, 2007 at 10:42:12 AM
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
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageHandler;

import edu.northwestern.ono.connection.IConnectionHandler;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The AzureusConnectionHandler class ...
 */
public class AzureusConnectionHandler implements GenericMessageHandler {

	private IConnectionHandler handler;
	private AzureusOnoConnectionManager manager;
	
	public AzureusConnectionHandler(IConnectionHandler handler, 
			AzureusOnoConnectionManager manager){
		this.handler = handler;
		this.manager = manager;
	}
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.messaging.generic.GenericMessageHandler#accept(org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnection)
	 */
	public boolean accept(GenericMessageConnection connection)
			throws MessageException {
		AzureusConnection ac = new AzureusConnection(connection, manager, false);
		manager.addDirectConnection(connection.getEndpoint().getNotionalAddress(),
	            connection, ac);
		
		return handler.accept(ac);
	}

}
