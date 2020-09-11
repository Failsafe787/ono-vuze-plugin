/**
 * Ono Project
 *
 * File:         CustomerItem.java
 * RCS:          $Id: CustomerItem.java,v 1.2 2010/03/29 16:48:04 drc915 Exp $
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
package edu.northwestern.ono.vuze.ui.items;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;

import edu.northwestern.ono.vuze.tables.utils.CoreTableColumn;
import edu.northwestern.ono.vuze.ui.OnoView;
import edu.northwestern.ono.vuze.ui.OnoView.EdgeData;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The CustomerItem class ...
 */
public class CustomerItem extends CoreTableColumn implements
		TableCellRefreshListener, ObfusticateCellText {

	
	/** Default Constructor */
	  public CustomerItem() {
	    super("cust", POSITION_LAST, 100, OnoView.EDGE_MAPPINGS);
	    setObfustication(true);
	   }
	  
	  public void refresh(TableCell cell) {
		  EdgeData cust = (EdgeData)cell.getDataSource();
		    String sText = (cust == null) ? "" : cust.custName;

		    if (cell.setText(sText) || !cell.isValid()) {
		      
		          cell.setSortValue(sText);
		      
		    }
		  }

		  public String getObfusticatedText(TableCell cell) {
		  	return cell.getText().substring(0, cell.getText().indexOf("."));
		  }

}
