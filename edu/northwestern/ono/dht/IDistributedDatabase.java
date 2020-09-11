/**
 * Ono Project
 *
 * File:         IDistributedDatabase.java
 * RCS:          $Id: IDistributedDatabase.java,v 1.3 2007/02/10 19:59:40 drc915 Exp $
 * Description:  IDistributedDatabase class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 25, 2007 at 4:55:43 PM
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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The IDistributedDatabase class ...
 */
public interface IDistributedDatabase {

	public void read(String key, String description, int timeout, IDDBReadAction action, int option);
	
	public void write(String key, ArrayList<ByteArrayOutputStream> values, 
			int timeout, IDDBWriteAction action); 
	
	public void delete(String key, int timeout, IDDBDeleteAction action, ArrayList<ByteArrayOutputStream> values);
	
	public int maximumValueSize();
	
	public IDHTOption getOptions();
}
