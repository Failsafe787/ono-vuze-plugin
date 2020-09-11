/**
 * Ono Project
 *
 * File:         EdgeMapTable.java
 * RCS:          $Id: NearbyPeersTable.java,v 1.5 2010/03/29 16:48:04 drc915 Exp $
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
package edu.northwestern.ono.vuze.ui;


import java.util.ArrayList;

import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;

import edu.northwestern.ono.brp.BRPPeerManager;
import edu.northwestern.ono.brp.BRPPeerManager.BRPPeer;
import edu.northwestern.ono.position.GenericPeer;
import edu.northwestern.ono.position.OnoPeerManager.OnoPeer;
import edu.northwestern.ono.vuze.tables.common.TableColumnCore;
import edu.northwestern.ono.vuze.tables.imp.TableViewSWTImpl;
import edu.northwestern.ono.vuze.ui.items.CDNNameItem;
import edu.northwestern.ono.vuze.ui.items.CosineSimilarityItem;
import edu.northwestern.ono.vuze.ui.items.DownloadSeenFromItem;
import edu.northwestern.ono.vuze.ui.items.NPIpItem;
import edu.northwestern.ono.vuze.ui.items.PctDownloadImprovementItem;
import edu.northwestern.ono.vuze.ui.items.PctUploadImprovementItem;
import edu.northwestern.ono.vuze.ui.items.PortItem;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The EdgeMapTable class ...
 */
public class NearbyPeersTable 
       extends TableViewSWTImpl<GenericPeer>
{
  private static final TableColumnCore[] basicItems = {
    new NPIpItem(),
    new PortItem(),
    new CosineSimilarityItem(),
    new CDNNameItem(),
    new DownloadSeenFromItem(), 
    new PctDownloadImprovementItem(),
    new PctUploadImprovementItem()
  };
private OnoView onoView;
private ArrayList<GenericPeer> peers;
private GenericPeer[] peersToRemove;
private GenericPeer[] peersToAdd;


  /**
   * Initialize
   *
   */
  public NearbyPeersTable(OnoView onoView) {	  
	    super(GenericPeer.class, OnoView.NEARBY_PEERS, "NearbyPeers", 
	          basicItems, "cossim");
    setRowDefaultHeight(16);
    this.onoView = onoView;
    bEnableTabViews = false;
    peers = new ArrayList<GenericPeer>();
    peersToRemove = new GenericPeer[0];
    peersToAdd = new GenericPeer[0];

  }
  
	public void dataSourceChanged(Object newDataSource) {

  	if (super.getTableComposite() != null) {
    	addExistingDatasources();
    }

	}

  
	public void initializeTable(Table table) {
		super.initializeTable(table);

  	addExistingDatasources();
	}

	public void tableStructureChanged() {
    //1. Unregister for item creation
		super.tableStructureChanged(bEnableTabViews, OnoPeer.class);
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

		super.fillMenu(menu, null);
	}


  public void delete() {

    super.delete();
  }
  


	/**
	 * Add datasources already in existance before we called addListener.
	 * Faster than allowing addListener to call us one datasource at a time. 
	 */
	private void addExistingDatasources() {
		
		GenericPeer[] dataSources = onoView.getAllDataInArray();
		
		if (dataSources == null || dataSources.length == 0)
			return;
		
		updateExistingData(dataSources);
		
		if (peersToRemove.length>0)
			removeDataSources(peersToRemove);
		if (peersToAdd.length>0)
			addDataSources(peersToAdd);		
		processDataSourceQueue();
//		refresh();
	}

	private void updateExistingData(GenericPeer[] dataSources) {
		ArrayList<GenericPeer> toRemove = new ArrayList<GenericPeer>();
		toRemove.addAll(peers);
		for (GenericPeer p : dataSources) if (!toRemove.contains(p)) toRemove.add(p);
		ArrayList<GenericPeer> toAdd = new ArrayList<GenericPeer>();
		for (int i = 0; i < dataSources.length; i++){
			if (dataSources[i].isOnoPeer()) {
				if (((OnoPeer)dataSources[i]).getMaxCosSim()==null &&
						!((OnoPeer)dataSources[i]).isCIDRMatch()) 
					continue;
			} else {
				assert(dataSources[i].isBRPPeer()); //remove this if a third peer type is added
				if (BRPPeerManager.getInstance().getMaxCosineSimilarity(
						((BRPPeer) dataSources[i])) >
				BRPPeerManager.COS_SIM_THRESHOLD) {
					toRemove.add(dataSources[i]);
				}
			}
			boolean found = false;
			for (int j = 0; j < peers.size(); j++){
				if (peers.get(j).equals(dataSources[i])){
					peers.get(j).copy(dataSources[i]);
					found = true;
					break;
				}
			}

			if (!found){
				
				toAdd.add(dataSources[i]);
				peers.add(dataSources[i]);
				toRemove.remove(dataSources[i]);
			}
			else if (!toRemove.remove(dataSources[i])){
				System.err.println("Tried to remove live entry that didn't exist!");			
			}
		}
		
		peersToAdd = new GenericPeer[toAdd.size()];
		for (int i = 0; i < toAdd.size(); i++) peersToAdd[i] = toAdd.get(i);
		peersToRemove = new GenericPeer[toRemove.size()];
		for (int i = 0; i < toRemove.size(); i++){
			if (!peers.remove(toRemove.get(i))){
				System.err.println("Tried to remove stale entry that didn't exist!");
			}
			peersToRemove[i] = toRemove.get(i);
		}
		
	}
}
