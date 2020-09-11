/**
 * Ono Project
 *
 * File:         CustomerItem.java
 * RCS:          $Id: NPIpItem.java,v 1.3 2010/03/29 16:48:03 drc915 Exp $
 * Description:  CustomerItem class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Apr 24, 2007 at 3:34:16 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.ui.items
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
package edu.northwestern.ono.ui.items;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;

import edu.northwestern.ono.position.OnoPeerManager.OnoPeer;
import edu.northwestern.ono.tables.utils.CoreTableColumn;
import edu.northwestern.ono.ui.OnoView;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The CustomerItem class ...
 */
public class NPIpItem extends CoreTableColumn implements
		TableCellRefreshListener, ObfusticateCellText {

	
	/** Default Constructor */
	  public NPIpItem() {
	    super("ip", POSITION_LAST, 100, OnoView.NEARBY_PEERS);
	    setObfustication(true);
	   }
	  
	  public void refresh(TableCell cell) {
		  OnoPeer op = (OnoPeer)cell.getDataSource();
		    String sText = (op == null) ? "" : op.getIp();

		    if (cell.setText(sText) || !cell.isValid()) {
		        String[] sBlocks = sText.split("\\.");
		        if (sBlocks.length == 4) {
		          try {
		            long l = (Long.parseLong(sBlocks[0]) << 24) +
		                     (Long.parseLong(sBlocks[1]) << 16) +
		                     (Long.parseLong(sBlocks[2]) << 8) +
		                     Long.parseLong(sBlocks[3]);
		            cell.setSortValue(l);
		          } catch (Exception e) { e.printStackTrace(); /* ignore */ }
		        }
		      }
		    }

		    public String getObfusticatedText(TableCell cell) {
		    	return cell.getText().substring(0, 3);
		    }
		  }
