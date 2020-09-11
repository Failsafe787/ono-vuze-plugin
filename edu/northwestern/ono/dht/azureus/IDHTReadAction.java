/**
 * Ono Project
 *
 * File:         IDHTReadAction.java
 * RCS:          $Id: IDHTReadAction.java,v 1.1 2007/02/01 16:52:19 drc915 Exp $
 * Description:  IDHTReadAction class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 2, 2006 at 5:10:10 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.dht
 * Status:       Experimental (Do Not Distribute)
 *
 * (C) Copyright 2006, Northwestern University, all rights reserved.
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
package edu.northwestern.ono.dht.azureus;

import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The IDHTReadAction class ...
 */
public interface IDHTReadAction {
    /**
     * Handles a successful DHT read operation.
     * @param s the value read from the DHT.
     */
    public void handleRead(byte[] b, DistributedDatabaseEvent event);

    /**
     * @param event
     */
    public void handleTimeout(DistributedDatabaseEvent event);

    /**
     *
     * @param event
     */
    public void handleComplete(DistributedDatabaseEvent event);
}
