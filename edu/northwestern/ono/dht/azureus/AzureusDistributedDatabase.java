/**
 * Ono Project
 *
 * File:         AzuruesDistriubutedDatabase.java
 * RCS:          $Id: AzureusDistributedDatabase.java,v 1.6 2010/03/29 16:48:04 drc915 Exp $
 * Description:  AzuruesDistriubutedDatabase class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 25, 2007 at 5:06:56 PM
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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;

import edu.northwestern.ono.dht.IDDBDeleteAction;
import edu.northwestern.ono.dht.IDDBReadAction;
import edu.northwestern.ono.dht.IDDBWriteAction;
import edu.northwestern.ono.dht.IDHTOption;
import edu.northwestern.ono.dht.IDistributedDatabase;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The AzuruesDistriubutedDatabase class ...
 */
public class AzureusDistributedDatabase implements IDistributedDatabase {
	
	DistributedDatabase ddb;
	DHTManager myManager;
	AzureusDHTOption options;
	private static final boolean ENABLED = false;
	
	public AzureusDistributedDatabase(DistributedDatabase ddb){
		this.ddb = ddb;
		myManager = new DHTManager(ddb);
		options = new AzureusDHTOption();
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.dht.IDistributedDatabase#delete(edu.northwestern.ono.dht.IDistributedDatabaseKey, int, edu.northwestern.ono.dht.IDDBDeleteAction)
	 */
	public void delete(String key, int timeout,
			IDDBDeleteAction action, ArrayList<ByteArrayOutputStream> values) {
		if (!ENABLED) return;
		// TODO add delete action
		try {
			myManager.doDelete(ddb.createKey(key, "Refreshing position for IP " + key));
		} catch (DistributedDatabaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.dht.IDistributedDatabase#read(edu.northwestern.ono.dht.IDistributedDatabaseKey, int, edu.northwestern.ono.dht.IDDBReadAction)
	 */
	public void read(String key, String description, int timeout,
			IDDBReadAction action, int option) {
		if (!ENABLED) return;
		myManager.doRead(key, description, new AzureusReadAction(action), 
				option, timeout*1000);

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.dht.IDistributedDatabase#write(edu.northwestern.ono.dht.IDistributedDatabaseKey, edu.northwestern.ono.dht.IDistributedDatabaseValue, int, edu.northwestern.ono.dht.IDDBWriteAction)
	 */
	public void write(String key,
			ArrayList<ByteArrayOutputStream> values, int timeout, IDDBWriteAction action) {
		if (!ENABLED) return;
		DistributedDatabaseKey ddbKey;
		try {
//			GregorianCalendar now = new GregorianCalendar();
//		      Date d = now.getTime();
//		      DateFormat df2 = DateFormat.getTimeInstance(DateFormat.SHORT);
//		      String time = df2.format(d);
			ddbKey = ddb.createKey(key, "Writing Ono position for IP " + key + " @ "+System.currentTimeMillis());
		
			myManager.doWrite(ddbKey, values, new AzureusWriteAction(action));
		} catch (DistributedDatabaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int maximumValueSize(){
		return 253; // TODO check
	}

	public IDHTOption getOptions() {
		// TODO Auto-generated method stub
		return options;
	}

}
