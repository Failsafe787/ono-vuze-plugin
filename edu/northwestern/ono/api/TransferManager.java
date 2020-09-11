/**
 * Ono Project
 *
 * File:         TransferManager.java
 * RCS:          $Id: TransferManager.java,v 1.3 2010/03/29 16:48:04 drc915 Exp $
 * Description:  TransferManager class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Mar 2, 2007 at 12:27:24 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.net
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
package edu.northwestern.ono.api;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import edu.northwestern.ono.net.DataTransferConfig;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The TransferManager class ...
 */
public interface TransferManager {
	public void beginTransfer(DataTransferConfig config);

	public void closeConnections(Object transferContext);

	public OutputStream getOutputStream(Object transferContext);
	
	public InputStream getInputStream(Object transferContext);

	public void createDataConnection(InetSocketAddress socket, 
			Object transferContext);
	
	public void listenForConnection(InetSocketAddress socket, 
			Object transferContext);

	public void waitForDone(Object transferContext);
}
