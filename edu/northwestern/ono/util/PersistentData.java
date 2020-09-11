/**
 * Dasu Project
 *
 * File:         PersistentData.java
 * RCS:          $Id: PersistentData.java,v 1.1 2011/07/06 15:45:53 mas939 Exp $
 * Description:  PersistentData class (see below)
 * Author:       Mario Sanchez
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      May 24, 2010 at 2:27:04 PM
 * Language:     Java
 * Package:      edu.northwestern.dasu
 * Status:       Experimental (Do Not Distribute)
 *
 * (C) Copyright 2010, Northwestern University, all rights reserved.
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

package edu.northwestern.ono.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

import edu.northwestern.ono.OnoConfiguration;

/**
 * @author Mario Sanchez &lt;msanchez@eecs.northwestern.edu&gt;
 *
 * The PersistentData class ...
 */
public class PersistentData implements Serializable{

	private static final long serialVersionUID = 1221642255540144449L;

	private int maxLogEntries = 
		OnoConfiguration.getInstance().getMaxLogEntries();
	
	/* message log */
	private LinkedHashMap<String, Long> messageLog = new LinkedHashMap<String, Long>();
	
	public PersistentData() {
	}
	
	public synchronized LinkedHashMap<String, Long> getMessageLog() {
		return messageLog;
	}
	
}