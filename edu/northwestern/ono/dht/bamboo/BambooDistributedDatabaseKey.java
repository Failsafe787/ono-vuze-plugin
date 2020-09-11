/**
 * Ono Project
 *
 * File:         BambooDistributedDatabaseKey.java
 * RCS:          $Id: BambooDistributedDatabaseKey.java,v 1.4 2010/03/29 16:48:04 drc915 Exp $
 * Description:  BambooDistributedDatabaseKey class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 31, 2007 at 2:09:48 PM
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
import bamboo.dht.bamboo_key;
import bamboo.dht.bamboo_put_arguments;
import edu.northwestern.ono.dht.IDistributedDatabaseKey;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The BambooDistributedDatabaseKey class ...
 */
public class BambooDistributedDatabaseKey implements IDistributedDatabaseKey {

	String description;
	Object key;
	String secret;

	public BambooDistributedDatabaseKey(bamboo_put_arguments put) {
		description = Util.convertByteToString(put.key.value);
		key = description;
		
	}

	public BambooDistributedDatabaseKey(bamboo_get_args get, String description) {
		key = Util.convertByteToString(get.key.value);
		this.description = description;
	}

	public BambooDistributedDatabaseKey(bamboo_key key2, String description) {
		this.description = description;
		key = Util.convertByteToString(key2.value);
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.dht.IDistributedDatabaseKey#getDescription()
	 */
	public String getDescription() {
		// TODO Auto-generated method stub
		return description;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.dht.IDistributedDatabaseKey#getKey()
	 */
	public Object getKey() {
		// TODO Auto-generated method stub
		return key;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.dht.IDistributedDatabaseKey#getSecret()
	 */
	public String getSecret() {
		// TODO Auto-generated method stub
		return null;
	}

}
