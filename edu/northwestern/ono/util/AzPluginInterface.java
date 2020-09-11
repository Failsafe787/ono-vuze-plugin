/**
 * Ono Project
 *
 * File:         AzPluginInterface.java
 * RCS:          $Id: AzPluginInterface.java,v 1.7 2011/07/06 15:45:53 mas939 Exp $
 * Description:  AzPluginInterface class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 22, 2007 at 9:38:21 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.util
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
package edu.northwestern.ono.util;

import java.util.Properties;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.plugins.PluginEventListener;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.PluginState;
import org.gudy.azureus2.plugins.PluginView;
import org.gudy.azureus2.plugins.clientid.ClientIDManager;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.dht.mainline.MainlineDHTManager;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.plugins.ipfilter.IPFilter;
import org.gudy.azureus2.plugins.logging.Logger;
import org.gudy.azureus2.plugins.messaging.MessageManager;
import org.gudy.azureus2.plugins.network.ConnectionManager;
import org.gudy.azureus2.plugins.platform.PlatformManager;
import org.gudy.azureus2.plugins.sharing.ShareException;
import org.gudy.azureus2.plugins.sharing.ShareManager;
import org.gudy.azureus2.plugins.torrent.TorrentManager;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;
import org.gudy.azureus2.plugins.update.UpdateManager;
import org.gudy.azureus2.plugins.utils.ShortCuts;
import org.gudy.azureus2.plugins.utils.Utilities;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The AzPluginInterface class ...
 */
public class AzPluginInterface implements PluginInterface {

	private org.gudy.azureus2.plugins.PluginInterface self;
	
	public void setPluginInterface(org.gudy.azureus2.plugins.PluginInterface pi){
		self = pi;
	}
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#addColumnToMyTorrentsTable(java.lang.String, org.gudy.azureus2.plugins.ui.tables.mytorrents.PluginMyTorrentsItemFactory)
	 */
//	public void addColumnToMyTorrentsTable(String columnName,
//			PluginMyTorrentsItemFactory factory) {
//		self.addColumnToMyTorrentsTable(columnName, factory);
//
//	}
//
//	/* (non-Javadoc)
//	 * @see org.gudy.azureus2.plugins.PluginInterface#addColumnToPeersTable(java.lang.String, org.gudy.azureus2.plugins.ui.tables.peers.PluginPeerItemFactory)
//	 */
//	public void addColumnToPeersTable(String columnName,
//			PluginPeerItemFactory factory) {
//		self.addColumnToPeersTable(columnName, factory);
//
//	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#addConfigSection(org.gudy.azureus2.plugins.ui.config.ConfigSection)
	 */
	public void addConfigSection(ConfigSection section) {
		self.addConfigSection(section);

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#addConfigUIParameters(org.gudy.azureus2.plugins.ui.config.Parameter[], java.lang.String)
	 */
	public void addConfigUIParameters(Parameter[] parameters, String displayName) {
		self.addConfigUIParameters(parameters, displayName);

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#addEventListener(org.gudy.azureus2.plugins.PluginEventListener)
	 */
	public void addEventListener(PluginEventListener l) {
		self.addEventListener(l);

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#addListener(org.gudy.azureus2.plugins.PluginListener)
	 */
	public void addListener(PluginListener l) {
		self.addListener(l);

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#addView(org.gudy.azureus2.plugins.PluginView)
	 */
	public void addView(PluginView view) {
		self.addView(view);

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#firePluginEvent(org.gudy.azureus2.plugins.PluginEvent)
	 */
	public void firePluginEvent(PluginEvent event) {
		self.firePluginEvent(event);

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getAzureusName()
	 */
	public String getAzureusName() {
		// TODO Auto-generated method stub
		return self.getAzureusName();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getAzureusVersion()
	 */
	public String getAzureusVersion() {
		// TODO Auto-generated method stub
		return self.getAzureusVersion();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getClientIDManager()
	 */
	public ClientIDManager getClientIDManager() {
		// TODO Auto-generated method stub
		return self.getClientIDManager();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getConnectionManager()
	 */
	public ConnectionManager getConnectionManager() {
		// TODO Auto-generated method stub
		return self.getConnectionManager();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getDistributedDatabase()
	 */
	public DistributedDatabase getDistributedDatabase() {
		// TODO Auto-generated method stub
		return self.getDistributedDatabase();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getDownloadManager()
	 */
	public DownloadManager getDownloadManager() {
		// TODO Auto-generated method stub
		return self.getDownloadManager();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getIPC()
	 */
	public IPCInterface getIPC() {
		// TODO Auto-generated method stub
		return self.getIPC();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getIPFilter()
	 */
	public IPFilter getIPFilter() {
		// TODO Auto-generated method stub
		return self.getIPFilter();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getLocalPluginInterface(java.lang.Class, java.lang.String)
	 */
	public org.gudy.azureus2.plugins.PluginInterface getLocalPluginInterface(
			Class plugin, String id) throws PluginException {
		// TODO Auto-generated method stub
		return self.getLocalPluginInterface(plugin, id);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getLogger()
	 */
	public Logger getLogger() {
		// TODO Auto-generated method stub
		return self.getLogger();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getMessageManager()
	 */
	public MessageManager getMessageManager() {
		// TODO Auto-generated method stub
		return self.getMessageManager();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPeerProtocolManager()
	 */
//	public PeerProtocolManager getPeerProtocolManager() {
//		// TODO Auto-generated method stub
//		return self.getPeerProtocolManager();
//	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPlatformManager()
	 */
	public PlatformManager getPlatformManager() {
		// TODO Auto-generated method stub
		return self.getPlatformManager();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPlugin()
	 */
	public Plugin getPlugin() {
		// TODO Auto-generated method stub
		return self.getPlugin();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginClassLoader()
	 */
	public ClassLoader getPluginClassLoader() {
		// TODO Auto-generated method stub
		return self.getPluginClassLoader();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginConfigUIFactory()
	 */
	public PluginConfigUIFactory getPluginConfigUIFactory() {
		// TODO Auto-generated method stub
		return self.getPluginConfigUIFactory();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginDirectoryName()
	 */
	public String getPluginDirectoryName() {
		// TODO Auto-generated method stub
		return self.getPluginDirectoryName();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginID()
	 */
	public String getPluginID() {
		// TODO Auto-generated method stub
		return self.getPluginID();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginManager()
	 */
	public PluginManager getPluginManager() {
		// TODO Auto-generated method stub
		return self.getPluginManager();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginName()
	 */
	public String getPluginName() {
		// TODO Auto-generated method stub
		return self.getPluginName();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginProperties()
	 */
	public Properties getPluginProperties() {
		// TODO Auto-generated method stub
		return self.getPluginProperties();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginVersion()
	 */
	public String getPluginVersion() {
		// TODO Auto-generated method stub
		return self.getPluginVersion();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginconfig()
	 */
	public PluginConfig getPluginconfig() {
		// TODO Auto-generated method stub
		return self.getPluginconfig();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getShareManager()
	 */
	public ShareManager getShareManager() throws ShareException {
		// TODO Auto-generated method stub
		return self.getShareManager();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getShortCuts()
	 */
	public ShortCuts getShortCuts() {
		// TODO Auto-generated method stub
		return self.getShortCuts();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getTorrentManager()
	 */
	public TorrentManager getTorrentManager() {
		// TODO Auto-generated method stub
		return self.getTorrentManager();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getTracker()
	 */
	public Tracker getTracker() {
		// TODO Auto-generated method stub
		return self.getTracker();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getUIManager()
	 */
	public UIManager getUIManager() {
		// TODO Auto-generated method stub
		return self.getUIManager();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getUpdateManager()
	 */
	public UpdateManager getUpdateManager() {
		// TODO Auto-generated method stub
		return self.getUpdateManager();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getUtilities()
	 */
	public Utilities getUtilities() {
		// TODO Auto-generated method stub
		return self.getUtilities();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#isBuiltIn()
	 */
	public boolean isBuiltIn() {
		// TODO Auto-generated method stub
		return self.isBuiltIn();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#isDisabled()
	 */
	public boolean isDisabled() {
		// TODO Auto-generated method stub
		return self.isDisabled();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#isMandatory()
	 */
	public boolean isMandatory() {
		// TODO Auto-generated method stub
		return self.isMandatory();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#isOperational()
	 */
	public boolean isOperational() {
		// TODO Auto-generated method stub
		return self.isOperational();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#isUnloadable()
	 */
	public boolean isUnloadable() {
		// TODO Auto-generated method stub
		return self.isUnloadable();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#openTorrentFile(java.lang.String)
	 */
	public void openTorrentFile(String fileName) {
		self.openTorrentFile(fileName);

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#openTorrentURL(java.lang.String)
	 */
	public void openTorrentURL(String url) {
		self.openTorrentURL(url);

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#reload()
	 */
	public void reload() throws PluginException {
		self.reload();

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#removeConfigSection(org.gudy.azureus2.plugins.ui.config.ConfigSection)
	 */
	public void removeConfigSection(ConfigSection section) {
		self.removeConfigSection(section);

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#removeEventListener(org.gudy.azureus2.plugins.PluginEventListener)
	 */
	public void removeEventListener(PluginEventListener l) {
		self.removeEventListener(l);

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#removeListener(org.gudy.azureus2.plugins.PluginListener)
	 */
	public void removeListener(PluginListener l) {
		self.removeListener(l);

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#setDisabled(boolean)
	 */
	public void setDisabled(boolean disabled) {
		self.setDisabled(disabled);

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#uninstall()
	 */
	public void uninstall() throws PluginException {
		self.uninstall();

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#unload()
	 */
	public void unload() throws PluginException {
		self.unload();

	}
	
	public ConfigSection[] getConfigSections() {
		// TODO Auto-generated method stub
		return self.getConfigSections();
	}
	
	public boolean isShared() {
		// TODO Auto-generated method stub
		return self.isShared();
	}

	//@Override
	public String getApplicationName() {
		// TODO Auto-generated method stub
		return self.getApplicationName();
	}

	//@Override
	public MainlineDHTManager getMainlineDHTManager() {
		// TODO Auto-generated method stub
		return self.getMainlineDHTManager();
	}

	//@Override
	public PluginState getPluginState() {
		// TODO Auto-generated method stub
		return self.getPluginState();
	}

	//@Override
	public boolean isInitialisationThread() {
		// TODO Auto-generated method stub
		return self.isInitialisationThread();
	}

	public String getPerUserPluginDirectoryName() {
		// TODO Auto-generated method stub
		return null;
	}

//	@Override
//	public String getPerUserPluginDirectoryName() {
//		// TODO Auto-generated method stub
//		return null;
//	}

}
