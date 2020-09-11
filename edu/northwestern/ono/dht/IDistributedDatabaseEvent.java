/**
 * Ono Project
 *
 * File:         IDistributedDatabaseEvent.java
 * RCS:          $Id: IDistributedDatabaseEvent.java,v 1.1 2007/02/01 16:52:18 drc915 Exp $
 * Description:  IDistributedDatabaseEvent class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 25, 2007 at 4:59:12 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.dht
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
package edu.northwestern.ono.dht;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The IDistributedDatabaseEvent class ...
 */
public interface IDistributedDatabaseEvent {
	public static final int	ET_VALUE_WRITTEN		= 1;
	public static final int	ET_VALUE_READ			= 2;
	public static final int	ET_VALUE_DELETED		= 3;
	
	public static final int	ET_OPERATION_COMPLETE	= 4;
	public static final int	ET_OPERATION_TIMEOUT	= 5;

	public static final int	ET_KEY_STATS_READ		= 6;

	public int
	getType();
	
	public IDistributedDatabaseKey
	getKey();

	
	public IDistributedDatabaseValue
	getValue();
	
}
