/**
 * Ono Project
 *
 * File:         BambooDHTWriteHandler.java
 * RCS:          $Id: BambooDHTWriteHandler.java,v 1.8 2010/03/29 16:48:04 drc915 Exp $
 * Description:  BambooDHTWriteHandler class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 31, 2007 at 3:33:30 PM
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

import bamboo.dht.bamboo_put_arguments;
import bamboo.util.Curry.Thunk1;
import edu.northwestern.ono.dht.IDDBWriteAction;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The BambooDHTWriteHandler class ...
 */
public class BambooDHTWriteHandler implements Thunk1<Integer> {

	 private IDDBWriteAction action;
	private bamboo_put_arguments put;
	private String description;

	public BambooDHTWriteHandler(IDDBWriteAction action, bamboo_put_arguments put, 
			String description) {
		this.action = action;
		this.put = put;
		this.description = description;
	}

	public void run(final Integer result) {
		 BambooDHTManager.getInstance().doneWithWrite(Util.convertByteToString(put.key.value));
         if (result.intValue () != 0) {
        	 if (BambooDHTManager.DEBUG) System.err.println("Write failed on " + description + "! Error code: "+result.intValue());
//             outstanding--;
//             BambooDHTManager.getInstance().reWrite(this, put, put.key.value);
         }
         else {
        	 if (BambooDHTManager.DEBUG) System.out.println("Write complete on " + description + "!");
        	 BambooDistriubtedDatabaseEvent event = new BambooDistriubtedDatabaseEvent(put);
        	 action.handleWrite(event);
        	 action.handleComplete(event);
        	
//             StringBuffer sb = new StringBuffer (100);
//             bytes_to_sbuf (put.key.value, 0, 
//                     put.key.value.length, false, sb);
//             final String key = sb.toString ();
//             logger.debug ("got response " + result.intValue () + 
//                     " for 0x" + key);
//             acore.register_timer (1000, new Runnable () {
//                     public void run() {
//                         logger.debug ("reputting block 0x" + key);
//                         client.put (put, curry(put_done_cb, put));
//                     }
//                 });
         }
     }
}
