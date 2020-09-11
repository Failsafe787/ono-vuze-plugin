/**
 * Ono Project
 *
 * File:         EdgeMapTable.java
 * RCS:          $Id: EdgeMapTable.java,v 1.5 2007/05/24 22:39:12 drc915 Exp $
 * Description:  EdgeMapTable class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Apr 24, 2007 at 3:28:12 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.ui
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
package edu.northwestern.ono.ui;


import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;

import edu.northwestern.ono.tables.TableColumnCore;
import edu.northwestern.ono.tables.TableView;
import edu.northwestern.ono.ui.OnoView.EdgeData;
import edu.northwestern.ono.ui.items.CustomerItem;
import edu.northwestern.ono.ui.items.IpItem;
import edu.northwestern.ono.ui.items.PctOfTimeSeenItem;
import edu.northwestern.ono.ui.items.RttItem;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The EdgeMapTable class ...
 */
public class EdgeMapTable 
       extends TableView
{
  private static final TableColumnCore[] basicItems = {
    new CustomerItem(),
    new IpItem(),
    new RttItem(),
    new PctOfTimeSeenItem()    
  };
private OnoView onoView;
private ArrayList<EdgeData> edges;
private Object[] edgesToRemove;
private Object[] edgesToAdd;


  /**
   * Initialize
   *
   */
  public EdgeMapTable(OnoView onoView) {	  
	    super(OnoView.EDGE_MAPPINGS, "EdgeMappings", 
	          basicItems, "cust", SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
    setRowDefaultHeight(16);
    this.onoView = onoView;
    bEnableTabViews = false;
    edges = new ArrayList<EdgeData>();
    edgesToRemove = new Object[0];
    edgesToAdd = new Object[0];

  }
  
	public void dataSourceChanged(Object newDataSource) {

  	if (getTable() != null) {
    	addExistingDatasources();
    }

	}

  
	public void initializeTable(Table table) {
		super.initializeTable(table);

  	addExistingDatasources();
	}

	public void tableStructureChanged() {
    //1. Unregister for item creation
		super.tableStructureChanged();
  }

	public void fillMenu(final Menu menu) {

//		final MenuItem block_item = new MenuItem(menu, SWT.CHECK);
//
//		PEPeer peer = (PEPeer) getFirstSelectedDataSource();
//
//		if ( peer == null || peer.getManager().getDiskManager().getRemainingExcludingDND() > 0 ){
//			// disallow peer upload blocking when downloading
//			block_item.setSelection(false);
//			block_item.setEnabled(false);
//		}
//		else {
//			block_item.setEnabled(true);
//			block_item.setSelection(peer.isSnubbed());
//		}
//
//		Messages.setLanguageText(block_item, "PeersView.menu.blockupload");
//		block_item.addListener(SWT.Selection, new SelectedTableRowsListener() {
//			public void run(TableRowCore row) {
//				((PEPeer) row.getDataSource(true))
//						.setSnubbed(block_item.getSelection());
//			}
//		});
//
//		final MenuItem ban_item = new MenuItem(menu, SWT.PUSH);
//
//		Messages.setLanguageText(ban_item, "PeersView.menu.kickandban");
//		ban_item.addListener(SWT.Selection, new SelectedTableRowsListener() {
//			public void run(TableRowCore row) {
//				PEPeer peer = (PEPeer) row.getDataSource(true);
//				String msg = MessageText.getString("PeersView.menu.kickandban.reason");
//				IpFilterManagerFactory.getSingleton().getIPFilter().ban(peer.getIp(),
//						msg);
//				peer.getManager().removePeer(peer);
//			}
//		});
//
//		new MenuItem(menu, SWT.SEPARATOR);

		super.fillMenu(menu);
	}


  public void delete() {

    super.delete();
  }
  


	/**
	 * Add datasources already in existance before we called addListener.
	 * Faster than allowing addListener to call us one datasource at a time. 
	 */
	/**
	 * Add datasources already in existance before we called addListener.
	 * Faster than allowing addListener to call us one datasource at a time. 
	 */
	private void addExistingDatasources() {
		
		EdgeData[] dataSources = onoView.getEdgeDataInArray();
		
		if (dataSources == null || dataSources.length == 0)
			return;
		
		updateExistingData(dataSources);
		
		if (edgesToRemove.length>0)
			removeDataSources(edgesToRemove);
		if (edgesToAdd.length>0)
			addDataSources(edgesToAdd);		
		processDataSourceQueue();
		refreshTable(true);
	}

	private void updateExistingData(EdgeData[] dataSources) {
		ArrayList<EdgeData> toRemove = new ArrayList<EdgeData>();
		toRemove.addAll(edges);
		ArrayList<EdgeData> toAdd = new ArrayList<EdgeData>();
		for (int i = 0; i < dataSources.length; i++){
			boolean found = false;
			for (int j = 0; j < edges.size(); j++){
				if (edges.get(j).equals(dataSources[i])){
					edges.get(j).copy(dataSources[i]);
					found = true;
					break;
				}
			}
			if (!found){
				toAdd.add(dataSources[i]);
				edges.add(dataSources[i]);
			}
			else if (!toRemove.remove(dataSources[i])){
				System.err.println("Tried to remove live entry that didn't exist!");			
			}
		}
		
		edgesToAdd = new Object[toAdd.size()];
		for (int i = 0; i < toAdd.size(); i++) edgesToAdd[i] = toAdd.get(i);
		edgesToRemove = new Object[toRemove.size()];
		for (int i = 0; i < toRemove.size(); i++){
			if (!edges.remove(toRemove.get(i))){
				System.err.println("Tried to remove stale entry that didn't exist!");
			}
			edgesToRemove[i] = toRemove.get(i);
		}
		
	}
}
