/**
 * Ono Project
 *
 * File:         OnoPluginInterface.java
 * RCS:          $Id: OnoPluginInterface.java,v 1.12 2011/07/06 15:45:53 mas939 Exp $
 * Description:  OnoPluginInterface class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 22, 2007 at 5:19:16 PM
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Map;
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
import org.gudy.azureus2.plugins.utils.AggregatedDispatcher;
import org.gudy.azureus2.plugins.utils.AggregatedList;
import org.gudy.azureus2.plugins.utils.AggregatedListAcceptor;
import org.gudy.azureus2.plugins.utils.ByteArrayWrapper;
import org.gudy.azureus2.plugins.utils.DelayedTask;
import org.gudy.azureus2.plugins.utils.FeatureManager;
import org.gudy.azureus2.plugins.utils.Formatters;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.plugins.utils.Monitor;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.plugins.utils.Semaphore;
import org.gudy.azureus2.plugins.utils.ShortCuts;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.plugins.utils.resourceuploader.ResourceUploaderFactory;
import org.gudy.azureus2.plugins.utils.search.SearchException;
import org.gudy.azureus2.plugins.utils.search.SearchInitiator;
import org.gudy.azureus2.plugins.utils.search.SearchProvider;
import org.gudy.azureus2.plugins.utils.security.SESecurityManager;
import org.gudy.azureus2.plugins.utils.subscriptions.SubscriptionException;
import org.gudy.azureus2.plugins.utils.subscriptions.SubscriptionManager;
import org.gudy.azureus2.plugins.utils.xml.rss.RSSFeed;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentException;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentFactory;

import edu.northwestern.ono.MainPlanetLab;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The OnoPluginInterface class ...
 */
public class OnoPluginInterface implements PluginInterface {

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#addColumnToMyTorrentsTable(java.lang.String, org.gudy.azureus2.plugins.ui.tables.mytorrents.PluginMyTorrentsItemFactory)
	 */
//	public void addColumnToMyTorrentsTable(String columnName,
//			PluginMyTorrentsItemFactory factory) {
//		// TODO Auto-generated method stub
//
//	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#addColumnToPeersTable(java.lang.String, org.gudy.azureus2.plugins.ui.tables.peers.PluginPeerItemFactory)
	 */
//	public void addColumnToPeersTable(String columnName,
//			PluginPeerItemFactory factory) {
//		// TODO Auto-generated method stub
//
//	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#addConfigSection(org.gudy.azureus2.plugins.ui.config.ConfigSection)
	 */
	public void addConfigSection(ConfigSection section) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#addConfigUIParameters(org.gudy.azureus2.plugins.ui.config.Parameter[], java.lang.String)
	 */
	public void addConfigUIParameters(Parameter[] parameters, String displayName) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#addEventListener(org.gudy.azureus2.plugins.PluginEventListener)
	 */
	public void addEventListener(PluginEventListener l) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#addListener(org.gudy.azureus2.plugins.PluginListener)
	 */
	public void addListener(PluginListener l) {
		// TODO Auto-generated method stub

	}


	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#firePluginEvent(org.gudy.azureus2.plugins.PluginEvent)
	 */
	public void firePluginEvent(PluginEvent event) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getAzureusName()
	 */
	public String getAzureusName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getAzureusVersion()
	 */
	public String getAzureusVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getClientIDManager()
	 */
	public ClientIDManager getClientIDManager() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getConnectionManager()
	 */
	public ConnectionManager getConnectionManager() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getDistributedDatabase()
	 */
	public DistributedDatabase getDistributedDatabase() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getDownloadManager()
	 */
	public DownloadManager getDownloadManager() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getIPC()
	 */
	public IPCInterface getIPC() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getIPFilter()
	 */
	public IPFilter getIPFilter() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getLocalPluginInterface(java.lang.Class, java.lang.String)
	 */
	public PluginInterface getLocalPluginInterface(Class plugin, String id)
			throws PluginException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getLogger()
	 */
	public Logger getLogger() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getMessageManager()
	 */
	public MessageManager getMessageManager() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPeerProtocolManager()
	 */
//	public PeerProtocolManager getPeerProtocolManager() {
//		// TODO Auto-generated method stub
//		return null;
//	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPlatformManager()
	 */
	public PlatformManager getPlatformManager() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPlugin()
	 */
	public Plugin getPlugin() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginClassLoader()
	 */
	public ClassLoader getPluginClassLoader() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginConfigUIFactory()
	 */
	public PluginConfigUIFactory getPluginConfigUIFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginDirectoryName()
	 */
	public String getPluginDirectoryName() {
		File dir1 = new File (".");
	     try {
	       return dir1.getCanonicalPath();
	       	       }
	     catch(Exception e) {
	       e.printStackTrace();
	       }
	     return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginID()
	 */
	public String getPluginID() {
		// TODO Auto-generated method stub
		return "Ono";
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginManager()
	 */
	public PluginManager getPluginManager() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginName()
	 */
	public String getPluginName() {
		// TODO Auto-generated method stub
		return "Ono";
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginProperties()
	 */
	public Properties getPluginProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginVersion()
	 */
	public String getPluginVersion() {
		// TODO Auto-generated method stub
		return "0.4";
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getPluginconfig()
	 */
	public PluginConfig getPluginconfig() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getShareManager()
	 */
	public ShareManager getShareManager() throws ShareException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getShortCuts()
	 */
	public ShortCuts getShortCuts() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getTorrentManager()
	 */
	public TorrentManager getTorrentManager() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getTracker()
	 */
	public Tracker getTracker() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getUIManager()
	 */
	public UIManager getUIManager() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getUpdateManager()
	 */
	public UpdateManager getUpdateManager() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#getUtilities()
	 */
	public Utilities getUtilities() {
		// TODO Auto-generated method stub
		return new Utilities(){

			public ByteBuffer allocateDirectByteBuffer(int size) {
				// TODO Auto-generated method stub
				return null;
			}

			public PooledByteBuffer allocatePooledByteBuffer(int size) {
				// TODO Auto-generated method stub
				return null;
			}

			public PooledByteBuffer allocatePooledByteBuffer(byte[] data) {
				// TODO Auto-generated method stub
				return null;
			}

			public PooledByteBuffer allocatePooledByteBuffer(Map data) throws IOException {
				// TODO Auto-generated method stub
				return null;
			}

			public int compareVersions(String v1, String v2) {
				// TODO Auto-generated method stub
				return v1.compareTo(v2);
			}

			public AggregatedDispatcher createAggregatedDispatcher(long idle_dispatch_time, long max_queue_size) {
				// TODO Auto-generated method stub
				return null;
			}

			public AggregatedList createAggregatedList(AggregatedListAcceptor acceptor, long idle_dispatch_time, long max_queue_size) {
				// TODO Auto-generated method stub
				return null;
			}

			public void createProcess(String command_line) throws PluginException {
				// TODO Auto-generated method stub
				
			}

			public void createThread(String name, Runnable target) {
				MainPlanetLab.createThread(name, target);
				
			}

			public UTTimer createTimer(String name) {
				// TODO Auto-generated method stub
				return null;
			}

			public UTTimer createTimer(String name, boolean lightweight) {
				// TODO Auto-generated method stub
				return null;
			}

			public ByteArrayWrapper createWrapper(byte[] data) {
				// TODO Auto-generated method stub
				return null;
			}

			public void freeDirectByteBuffer(ByteBuffer buffer) {
				// TODO Auto-generated method stub
				
			}

			public String getAzureusProgramDir() {
				// TODO Auto-generated method stub
				return null;
			}

			public String getAzureusUserDir() {
				// TODO Auto-generated method stub
				return null;
			}

			public long getCurrentSystemTime() {
				// TODO Auto-generated method stub
				return System.currentTimeMillis();
			}

			public Formatters getFormatters() {
				// TODO Auto-generated method stub
				return null;
			}

			public InputStream getImageAsStream(String image_name) {
				// TODO Auto-generated method stub
				return null;
			}

			public LocaleUtilities getLocaleUtilities() {
				// TODO Auto-generated method stub
				return null;
			}

			public Monitor getMonitor() {
				// TODO Auto-generated method stub
				return null;
			}

			public InetAddress getPublicAddress() {
				// TODO Auto-generated method stub
				try {
					return InetAddress.getLocalHost();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}

			public RSSFeed getRSSFeed(URL feed_location) throws ResourceDownloaderException, SimpleXMLParserDocumentException {
				// TODO Auto-generated method stub
				return null;
			}

			public RSSFeed getRSSFeed(ResourceDownloader feed_location) throws ResourceDownloaderException, SimpleXMLParserDocumentException {
				// TODO Auto-generated method stub
				return null;
			}

			public ResourceDownloaderFactory getResourceDownloaderFactory() {
				// TODO Auto-generated method stub
				return null;
			}

			public ResourceUploaderFactory getResourceUploaderFactory() {
				// TODO Auto-generated method stub
				return null;
			}

			public SESecurityManager getSecurityManager() {
				// TODO Auto-generated method stub
				return null;
			}

			public Semaphore getSemaphore() {
				// TODO Auto-generated method stub
				return null;
			}

			public SimpleXMLParserDocumentFactory getSimpleXMLParserDocumentFactory() {
				// TODO Auto-generated method stub
				return null;
			}

			public boolean isCVSVersion() {
				// TODO Auto-generated method stub
				return false;
			}

			public boolean isFreeBSD() {
				// TODO Auto-generated method stub
				return System.getProperty("os.name").toUpperCase().contains("FREEBSD");
			}

			public boolean isLinux() {
				// TODO Auto-generated method stub
				return System.getProperty("os.name").toUpperCase().contains("LINUX");
			}

			public boolean isOSX() {
				// TODO Auto-generated method stub
				return System.getProperty("os.name").toUpperCase().contains("OSX");
			}

			public boolean isSolaris() {
				// TODO Auto-generated method stub
				return System.getProperty("os.name").toUpperCase().contains("SOLARIS");
			}

			public boolean isUnix() {
				// TODO Auto-generated method stub
				return System.getProperty("os.name").toUpperCase().contains("UNIX");
			}

			public boolean isWindows() {
				// TODO Auto-generated method stub
				return System.getProperty("os.name").toUpperCase().contains("WINDOWS");
			}

			public Map readResilientBEncodedFile(File parent_dir, String file_name, boolean use_backup) {
				// TODO Auto-generated method stub
				return null;
			}

			public String reverseDNSLookup(InetAddress address) {
				// TODO Auto-generated method stub
				return null;
			}

			public void writeResilientBEncodedFile(File parent_dir, String file_name, Map data, boolean use_backup) {
				// TODO Auto-generated method stub
				
			}

	
			public UTTimer createTimer(String name, int priority) {
				// TODO Auto-generated method stub
				return null;
			}

		
			public String normaliseFileName(String f_name) {
				// TODO Auto-generated method stub
				return null;
			}

			//@Override
			public DelayedTask createDelayedTask(Runnable r) {
				// TODO Auto-generated method stub
				return null;
			}

			//@Override
			public InetAddress getPublicAddress(boolean ipv6) {
				// TODO Auto-generated method stub
				return null;
			}

			//@Override
			public RSSFeed getRSSFeed(InputStream is)
					throws SimpleXMLParserDocumentException {
				// TODO Auto-generated method stub
				return null;
			}

			//@Override
			public void registerSearchProvider(SearchProvider provider)
					throws SearchException {
				// TODO Auto-generated method stub
				
			}

			public void deleteResilientBEncodedFile(File parent_dir,
					String file_name, boolean use_backup) {
				// TODO Auto-generated method stub
				
			}

			public SearchInitiator getSearchInitiator() throws SearchException {
				// TODO Auto-generated method stub
				return null;
			}
//
//			public SubscriptionManager getSubscriptionManager()
//					throws SubscriptionException {
//				// TODO Auto-generated method stub
//				return null;
//			}

			public boolean isFeatureEnabled(String feature_id,
					Map<String, Object> feature_properties) {
				// TODO Auto-generated method stub
				return false;
			}

			public SubscriptionManager getSubscriptionManager()
					throws SubscriptionException {
				// TODO Auto-generated method stub
				return null;
			}

			public FeatureManager getFeatureManager() {
				// TODO Auto-generated method stub
				return null;
			}

//			@Override
//			public FeatureManager getFeatureManager() {
//				// TODO Auto-generated method stub
//				return null;
//			}

//			public void registerFeatureEnabler(FeatureEnabler enabler) {
//
//				// TODO Auto-generated method stub
//				
//			}
//
//			public void unregisterFeatureEnabler(FeatureEnabler enabler) {
//				// TODO Auto-generated method stub
//				
//
//			}

//			public void registerFeatureEnabler(FeatureEnabler enabler) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			public void unregisterFeatureEnabler(FeatureEnabler enabler) {
//				// TODO Auto-generated method stub
//				
//
//			}
			};

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#isBuiltIn()
	 */
	public boolean isBuiltIn() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#isDisabled()
	 */
	public boolean isDisabled() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#isMandatory()
	 */
	public boolean isMandatory() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#isOperational()
	 */
	public boolean isOperational() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#isUnloadable()
	 */
	public boolean isUnloadable() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#openTorrentFile(java.lang.String)
	 */
	public void openTorrentFile(String fileName) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#openTorrentURL(java.lang.String)
	 */
	public void openTorrentURL(String url) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#reload()
	 */
	public void reload() throws PluginException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#removeConfigSection(org.gudy.azureus2.plugins.ui.config.ConfigSection)
	 */
	public void removeConfigSection(ConfigSection section) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#removeEventListener(org.gudy.azureus2.plugins.PluginEventListener)
	 */
	public void removeEventListener(PluginEventListener l) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#removeListener(org.gudy.azureus2.plugins.PluginListener)
	 */
	public void removeListener(PluginListener l) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#setDisabled(boolean)
	 */
	public void setDisabled(boolean disabled) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#uninstall()
	 */
	public void uninstall() throws PluginException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginInterface#unload()
	 */
	public void unload() throws PluginException {
		// TODO Auto-generated method stub

	}

	
	public ConfigSection[] getConfigSections() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public boolean isShared() {
		// TODO Auto-generated method stub
		return false;
	}

	//@Override
	public String getApplicationName() {
		// TODO Auto-generated method stub
		return null;
	}

	//@Override
	public MainlineDHTManager getMainlineDHTManager() {
		// TODO Auto-generated method stub
		return null;
	}

	//@Override
	public PluginState getPluginState() {
		// TODO Auto-generated method stub
		return null;
	}

	//@Override
	public boolean isInitialisationThread() {
		// TODO Auto-generated method stub
		return false;
	}

	public String getPerUserPluginDirectoryName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addView(PluginView view) {
		// TODO Auto-generated method stub
		
	}

//	@Override
//	public String getPerUserPluginDirectoryName() {
//		// TODO Auto-generated method stub
//		return null;
//	}

}
