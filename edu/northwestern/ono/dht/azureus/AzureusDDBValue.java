/**
 * Ono Project
 *
 * File:         AzuruesDDBValue.java
 * RCS:          $Id: AzureusDDBValue.java,v 1.1 2007/02/01 16:52:19 drc915 Exp $
 * Description:  AzuruesDDBValue class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 25, 2007 at 9:26:17 PM
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

import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;

import edu.northwestern.ono.dht.IDistributedDatabaseValue;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The AzuruesDDBValue class ...
 */
public class AzureusDDBValue implements IDistributedDatabaseValue {
	
	DistributedDatabaseValue ddbValue;
	
	public AzureusDDBValue(DistributedDatabaseValue ddbValue){
		this.ddbValue = ddbValue;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.dht.IDistributedDatabaseValue#getValue(java.lang.Class)
	 */
	public Object getValue(Class type) {
		// TODO Auto-generated method stub
		try {
			return ddbValue.getValue(type);
		} catch (DistributedDatabaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
