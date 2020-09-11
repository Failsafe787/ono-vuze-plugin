/**
 * Ono Project
 *
 * File:         BambooDistriubtedDatabaseEvent.java
 * RCS:          $Id: BambooDistriubtedDatabaseEvent.java,v 1.3 2010/03/29 16:48:04 drc915 Exp $
 * Description:  BambooDistriubtedDatabaseEvent class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 31, 2007 at 2:07:11 PM
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

import bamboo.dht.bamboo_get_args;
import bamboo.dht.bamboo_put_arguments;
import edu.northwestern.ono.dht.IDistributedDatabaseEvent;
import edu.northwestern.ono.dht.IDistributedDatabaseKey;
import edu.northwestern.ono.dht.IDistributedDatabaseValue;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The BambooDistriubtedDatabaseEvent class ...
 */
public class BambooDistriubtedDatabaseEvent implements
		IDistributedDatabaseEvent {

	private bamboo_get_args get;
	int type;
	private bamboo_put_arguments put;
	IDistributedDatabaseKey key;
	IDistributedDatabaseValue value;

	public BambooDistriubtedDatabaseEvent(bamboo_get_args get, byte value[]) {
		this.get = get;
		type = ET_VALUE_READ;
		this.key = new BambooDistributedDatabaseKey(get, null);
		this.value = new BambooDistributedDatabaseValue(value);
	}
	
	public BambooDistriubtedDatabaseEvent(bamboo_get_args get, byte value[], String description) {
		this.get = get;
		type = ET_VALUE_READ;
		this.key = new BambooDistributedDatabaseKey(get, description);
		this.value = new BambooDistributedDatabaseValue(value);
	}

	public BambooDistriubtedDatabaseEvent(bamboo_put_arguments put) {
		this.put = put;
		type = ET_VALUE_WRITTEN;
		this.key = new BambooDistributedDatabaseKey(put);
		this.value = new BambooDistributedDatabaseValue(put.value.value);
		
	}

	/**
	 * Called for deletion handlers
	 * @param key
	 * @param deleteSuccessful
	 */
	public BambooDistriubtedDatabaseEvent(IDistributedDatabaseKey key, boolean deleteSuccessful) {
		this.key = key;
		if (deleteSuccessful){
			type = ET_VALUE_DELETED;
		}
		else {
			type = ET_OPERATION_TIMEOUT;		
		}
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.dht.IDistributedDatabaseEvent#getKey()
	 */
	public IDistributedDatabaseKey getKey() {
		// TODO Auto-generated method stub
		return key;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.dht.IDistributedDatabaseEvent#getType()
	 */
	public int getType() {
		// TODO Auto-generated method stub
		return type;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.dht.IDistributedDatabaseEvent#getValue()
	 */
	public IDistributedDatabaseValue getValue() {
		// TODO Auto-generated method stub
		return value;
	}

}
