/**
 * Ono Project
 *
 * File:         BambooDHTReadHandler.java
 * RCS:          $Id: BambooDHTReadHandler.java,v 1.12 2010/03/29 16:48:04 drc915 Exp $
 * Description:  BambooDHTReadHandler class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 31, 2007 at 1:53:03 PM
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

import java.nio.ByteBuffer;

import bamboo.dht.bamboo_get_args;
import bamboo.dht.bamboo_get_result;
import bamboo.dht.bamboo_get_value;
import bamboo.util.Curry.Thunk3;
import edu.northwestern.ono.dht.IDDBReadAction;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The BambooDHTReadHandler class ...
 */

public class BambooDHTReadHandler implements 
	Thunk3<bamboo_get_args,Long,bamboo_get_result> {
		
		private static final boolean VERBOSE = false;
		private IDDBReadAction action;
		private int option;
		private String description;
		
		
		public BambooDHTReadHandler(IDDBReadAction handler, int option, 
				String description){
			this.action = handler;
			this.option = option;
			this.description = description;
		}
	/* (non-Javadoc)
	 * @see bamboo.util.Curry.Thunk3#run(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	public void run(final bamboo_get_args get, final Long pos, 
			bamboo_get_result result) { 
		
		if (result.values==null || result.values.length == 0) { 
			if (VERBOSE) System.err.println("Read found zero values on " + description + "!");
			action.handleTimeout(new BambooDistriubtedDatabaseEvent(get, null));
//            StringBuffer sb = new StringBuffer (100);
//            bytes_to_sbuf (get.key.value, 0, 
//                    get.key.value.length, false, sb);
//            final String key = sb.toString ();
////            logger.debug ("got empty response for 0x" + key);
//            BambooDHTManager.getInstance().reReadKey(get, pos, key, this);
            BambooDHTManager.getInstance().doneWithRead(Util.convertByteToString(get.key.value));
        }
        else {
            for (bamboo_get_value value : result.values){
//            logger.debug ("got 0x" + bytes_to_str(get.key.value));
//            outstanding--;
	            ByteBuffer bb = ByteBuffer.wrap (value.value.value);
	            action.handleRead(bb.array(), new BambooDistriubtedDatabaseEvent(get, value.value.value, description));
            }
            if (result.placemark.value == null || result.placemark.value.length==0){
	            action.handleComplete(new BambooDistriubtedDatabaseEvent(get, null, description));
	            BambooDHTManager.getInstance().doneWithRead(Util.convertByteToString(get.key.value));
            }
            else {
            	get.placemark.value = result.placemark.value;
            	BambooDHTManager.getInstance().doDirectRead(description, get, this);
            }
//            int l = bb.getInt ();
//            if ((rblocks.size () < l + 1) || 
//                (rblocks.elementAt (l) == null)) {
//                rblocks.setSize (max (rblocks.size (), l + 1));
//                rblocks.setElementAt (new PriorityQueue (BRANCHING), l);
//                logger.debug ("setting level " + l + " need to 0");
//                rnext.setSize (max (rnext.size (), l + 1));
//                rnext.setElementAt (new Long (0), l);
//            }
//            if (l == 0) {
//                rblocks.elementAt (l).add (bb, pos.longValue ());
//            }
//            else {
//                long p = BRANCHING * pos.longValue ();
//                while (bb.position () < bb.limit ()) {
//                    byte [] k = new byte [20];
//                    bb.get (k);
//                    logger.debug ("need to get lev=" + l + " pos=" + p + 
//                            " key=0x" + bytes_to_str (k));
//                    rblocks.elementAt (l).add (ByteBuffer.wrap (k), p++);
//                }
//            }
        }

        // Write any blocks we can to disk, then call read again.
//        while ((! rblocks.elementAt (0).isEmpty ()) && 
//               (rnext.elementAt (0).longValue () ==
//                rblocks.elementAt (0).getFirstPriority ())) {
//            long p = rblocks.elementAt (0).getFirstPriority ();
//            ByteBuffer bb = (ByteBuffer) 
//                rblocks.elementAt (0).removeFirst ();
//            logger.debug ("wrote block; updating level 0 need to " + (p+1));
//            rnext.setElementAt (new Long (p + 1), 0);
//            try {
//                logger.debug ("position=" + bb.position () + " writing " +
//                              (bb.limit () - bb.position ()) + " bytes");
//                os.write (bb.array (), bb.arrayOffset () + bb.position (), 
//                          bb.limit () - bb.position ());
//            }
//            catch (IOException e) {
//                logger.error ("could not write to output file");
//                System.exit (1);
//            }
//        }

//        read ();
//    }

	}

}
