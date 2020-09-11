/**
 * Ono Project
 *
 * File:         BambooDHTOptions.java
 * RCS:          $Id: BambooDHTOptions.java,v 1.1 2007/02/01 16:52:18 drc915 Exp $
 * Description:  BambooDHTOptions class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 31, 2007 at 4:34:37 PM
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

import edu.northwestern.ono.dht.IDHTOption;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The BambooDHTOptions class ...
 */
public class BambooDHTOptions implements IDHTOption {

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.dht.IDHTOption#getExhaustiveRead()
	 */
	public int getExhaustiveRead() {
		// TODO Auto-generated method stub
		return 1;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.dht.IDHTOption#getHighPriority()
	 */
	public int getHighPriority() {
		// TODO Auto-generated method stub
		return 2;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.dht.IDHTOption#getNone()
	 */
	public int getNone() {
		// TODO Auto-generated method stub
		return 0;
	}

}
