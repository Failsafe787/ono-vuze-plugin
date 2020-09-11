/**
 * Ono Project
 *
 * File:         AzureusDDBEvent.java
 * RCS:          $Id: AzureusDDBEvent.java,v 1.1 2007/02/01 16:52:19 drc915 Exp $
 * Description:  AzureusDDBEvent class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 25, 2007 at 9:22:59 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.dht.azureus
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
package edu.northwestern.ono.dht.azureus;

import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;

import edu.northwestern.ono.dht.IDistributedDatabaseEvent;
import edu.northwestern.ono.dht.IDistributedDatabaseKey;
import edu.northwestern.ono.dht.IDistributedDatabaseValue;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The AzureusDDBEvent class ...
 */
public class AzureusDDBEvent implements IDistributedDatabaseEvent {

	DistributedDatabaseEvent dde;
	
	public AzureusDDBEvent(DistributedDatabaseEvent dde){
		this.dde = dde;
	}
	
	/* (non-Javadoc)
	 * @see edu.northwestern.ono.dht.IDistributedDatabaseEvent#getKey()
	 */
	public IDistributedDatabaseKey getKey() {
		// TODO Auto-generated method stub
		return new AzureusDDBKey(dde.getKey());
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.dht.IDistributedDatabaseEvent#getType()
	 */
	public int getType() {
		// TODO Auto-generated method stub
		return dde.getType();
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.dht.IDistributedDatabaseEvent#getValue()
	 */
	public IDistributedDatabaseValue getValue() {
		// TODO Auto-generated method stub
		return new AzureusDDBValue(dde.getValue());
	}

}
