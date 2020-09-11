/**
 * Ono Project
 *
 * File:         BambooDeleteHandler.java
 * RCS:          $Id: BambooDeleteHandler.java,v 1.6 2010/03/29 16:48:04 drc915 Exp $
 * Description:  BambooDeleteHandler class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 31, 2007 at 4:50:18 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.dht.bamboo
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
package edu.northwestern.ono.dht.bamboo;

import bamboo.util.Curry.Thunk1;
import edu.northwestern.ono.dht.IDDBDeleteAction;
import edu.northwestern.ono.dht.IDistributedDatabaseKey;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The BambooDeleteHandler class ...
 */
public class BambooDeleteHandler implements Thunk1<Integer> {

	private IDDBDeleteAction action;
	private IDistributedDatabaseKey key;

	public BambooDeleteHandler(IDDBDeleteAction action, IDistributedDatabaseKey key) {
		this.action = action;
		this.key = key;
	}

	/* (non-Javadoc)
	 * @see bamboo.util.Curry.Thunk1#run(java.lang.Object)
	 */
	public void run(Integer result) {
		BambooDHTManager.getInstance().doneWithDelete((String) key.getKey());
		if (action==null) return;
        if (result.intValue() != 0) {
        	System.out.println("Delete failed on " + key.getDescription()+"!");
            action.handleTimeout(new BambooDistriubtedDatabaseEvent(key, false));
        }
        else {
        	System.out.println("Delete complete on " + key.getDescription()+"!");
        	action.handleComplete(new BambooDistriubtedDatabaseEvent(key, true));
        }
       

	}

}
