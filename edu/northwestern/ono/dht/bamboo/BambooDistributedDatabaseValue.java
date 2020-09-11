/**
 * Ono Project
 *
 * File:         BambooDistributedDatabaseValue.java
 * RCS:          $Id: BambooDistributedDatabaseValue.java,v 1.2 2010/03/29 16:48:04 drc915 Exp $
 * Description:  BambooDistributedDatabaseValue class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 31, 2007 at 3:42:55 PM
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

import edu.northwestern.ono.dht.IDistributedDatabaseValue;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The BambooDistributedDatabaseValue class ...
 */
public class BambooDistributedDatabaseValue implements
		IDistributedDatabaseValue {
	
	Object value;

	public BambooDistributedDatabaseValue(byte[] value2) {
		if (value2!=null) {
			value = Util.convertByteToString(value2);
		}
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.dht.IDistributedDatabaseValue#getValue(java.lang.Class)
	 */
	public Object getValue(Class type) {
		// TODO Auto-generated method stub
		return value;
	}

}
