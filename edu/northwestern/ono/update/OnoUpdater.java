/**
 * Ono Project
 *
 * File:         OnoUpdater.java
 * RCS:          $Id: OnoUpdater.java,v 1.16 2010/03/29 16:48:04 drc915 Exp $
 * Description:  OnoUpdater class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 3, 2006 at 1:00:44 PM
 * Language:     Java
 * Package:      edu.northwestern.ono
 * Status:       Experimental (Do Not Distribute)
 *
 * (C) Copyright 2006, Northwestern University, all rights reserved.
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
package edu.northwestern.ono.update;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.gudy.azureus2.core3.html.HTMLPage;
import org.gudy.azureus2.core3.html.HTMLPageFactory;
import org.gudy.azureus2.core3.html.HTMLTable;
import org.gudy.azureus2.core3.html.HTMLTableCell;
import org.gudy.azureus2.core3.html.HTMLTableRow;
import org.gudy.azureus2.core3.html.HTMLUtils;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.update.UpdatableComponent;
import org.gudy.azureus2.plugins.update.Update;
import org.gudy.azureus2.plugins.update.UpdateCheckInstance;
import org.gudy.azureus2.plugins.update.UpdateChecker;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderAdapter;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;
import org.gudy.azureus2.pluginsimpl.update.PluginUpdatePlugin;
import org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetails;
import org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetailsException;
import org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetailsLoaderListener;

import edu.northwestern.ono.Main;
import edu.northwestern.ono.util.HTTPPoller;

public final class OnoUpdater implements UpdatableComponent,
		ResourceDownloaderListener {
	private static final String site_prefix = "http://merlot.cs.northwestern.edu/azplugin/";

	private static final String TORRENT_URL = "http://www.aqua-lab.org/torrents/";

	/** time between checking */
	private static int CHECK_TIME = 30 * 60;

	/**
	 * 
	 */
	private final Main main;

	private final int rd_size_timeout;

	protected List listeners = new ArrayList();

	private final int rd_size_retries;

	protected ResourceDownloaderFactory rd_factory = ResourceDownloaderFactoryImpl
			.getSingleton();

	private String page_url = "http://merlot.cs.northwestern.edu/azplugin/pluginList.dat";

	protected boolean updated = false;

	private boolean updating = false;

	LoggerChannel log;

	Random r;

	PluginInterface pi;

	UpdateCheckInstance uci;

	public OnoUpdater(Main main, int rd_size_timeout, int rd_size_retries,
			LoggerChannel log, PluginInterface pi, UpdateCheckInstance uci) {
		super();
		this.main = main;
		this.rd_size_timeout = 600 * 1000;// rd_size_timeout;
		this.rd_size_retries = rd_size_retries;
		this.log = log;
		this.r = new Random();
		this.pi = pi;
		this.uci = uci;
		CHECK_TIME = Main.getUpdateCheck();
	}

	public String getName() {
		return "Ono Update";
	}

	public int getMaximumCheckTime() {
		return CHECK_TIME + (int) (r.nextDouble() * (CHECK_TIME / 4));
	}

	public void checkForUpdate(final UpdateChecker checker) {
		log.log("Checking for update!");

		if (updating) {
			uci.cancel();

			try {
				this.main.unload();
			} catch (PluginException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return;
		}

		if (updated) {
			try {
				checker.completed();
				this.main.unload();

				return;
			} catch (PluginException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			// TODO: update this
			SFPluginDetails sf_details = loadPluginList();

			if (sf_details == null) {
				// System.err.println("Error getting plugin details!");

				return;
			}

			String current_version = pi.getPluginVersion();

			// if (Logger.isEnabled())
			// Logger.log(new LogEvent(LOGID,
			// "PlatformManager:Win32 update check starts: current = "
			// + current_version));
			//                                
			boolean current_az_is_cvs = false; // = Constants.isCVSVersion();

			String sf_plugin_version = sf_details.getVersion();

			String sf_comp_version = sf_plugin_version;

			if (current_az_is_cvs) {
				String sf_cvs_version = sf_details.getCVSVersion();

				if (sf_cvs_version.length() > 0) {
					// sf cvs version ALWAYS entry in _CVS
					sf_plugin_version = sf_cvs_version;

					sf_comp_version = sf_plugin_version.substring(0,
							sf_plugin_version.length() - 4);
				}
			}

			String target_version = null;

			if ((sf_comp_version.length() == 0)
					|| !Character.isDigit(sf_comp_version.charAt(0))) {
				if (log.isEnabled()) {
					log.log(LoggerChannel.LT_WARNING,
							"PlatformManager:Win32 no valid version to check against ("
									+ sf_comp_version + ")");
				}
			} else if (Constants.compareVersions(current_version,
					sf_comp_version) < 0) {
				target_version = sf_comp_version;
			}

			checker.reportProgress("Win32: current = " + current_version
					+ ", latest = " + sf_comp_version);

			if (log.isEnabled()) {
				log.log("PlatformManager:Win32 update required = "
						+ (target_version != null));
			}

			if (target_version != null) {
				String target_download = sf_details.getDownloadURL();
				//"http://www.choffnes.com/ono_"
				//		+ sf_details.getVersion() + ".jar";
				// sf_details.getDownloadURL();

				if (current_az_is_cvs) {
					String sf_cvs_version = sf_details.getCVSVersion();

					if (sf_cvs_version.length() > 0) {
						target_download = sf_details.getCVSDownloadURL();
					}
				}

				updating = true;

				ResourceDownloaderFactory rdf = ResourceDownloaderFactoryImpl
						.getSingleton();

				ResourceDownloader direct_rdl = rdf.create(new URL(
						target_download));

				// TODO set up torrent download
				String torrent_download = TORRENT_URL;

				int slash_pos = target_download.lastIndexOf("/");

				if (slash_pos == -1) {
					torrent_download += target_download;
				} else {
					torrent_download += target_download
							.substring(slash_pos + 1);
				}

				torrent_download += ".torrent";

				 ResourceDownloader torrent_rdl = rdf.create( new URL(
				 torrent_download ));
				        
				 torrent_rdl = rdf.getSuffixBasedDownloader( torrent_rdl );
				                                
				 // create an alternate downloader with torrent attempt first
				                                
				 ResourceDownloader alternate_rdl =
				 rdf.getAlternateDownloader( new ResourceDownloader[]{
				 torrent_rdl, direct_rdl });
				        
				 // get size here so it is cached
				                                
				 rdf.getTimeoutDownloader(rdf.getRetryDownloader(alternate_rdl,rd_size_retries),rd_size_timeout).getSize();
				List update_desc = new ArrayList();

				update_desc.add("This is a maintenance release -- version "
						+ sf_details.getVersion() + ".");

				List desc_lines = HTMLUtils.convertHTMLToText("", sf_details
						.getDescription());

				update_desc.addAll(desc_lines);

				List comment_lines = HTMLUtils.convertHTMLToText("    ",
						sf_details.getComment());

				update_desc.addAll(comment_lines);

				String[] update_d = new String[update_desc.size()];

				update_desc.toArray(update_d);

				rdf.getTimeoutDownloader(
						rdf.getRetryDownloader(direct_rdl, rd_size_retries),
						rd_size_timeout).getSize();

				log.log("Update: Trying to get plugin update interface...");

				PluginUpdatePlugin pup = null;
				boolean working = false;

				while (!working) {
					try {
						pup = (PluginUpdatePlugin) pi.getPluginManager()
								.getPluginInterfaceByClass(
										PluginUpdatePlugin.class).getPlugin();
						working = true;
					} catch (NullPointerException e) {
					}
				}

				log.log("Update: Got interface, trying to add update from "
						+ target_download);

				pup
						.addUpdate(
								pi,
								checker,
								"Ono",
								update_d,
								/*
								 * new String[]{"Installation from file: " +
								 * target_download.toString() },
								 */
								sf_details.getVersion(),
								alternate_rdl,
								true,
								/* pi.isUnloadable()?Update.RESTART_REQUIRED_NO: */Update.RESTART_REQUIRED_YES,
								false);
				
				pi.getUIManager().showTextMessage("Update.title", null, "An Ono update " +
						"is downloading. Please click yes \nwhen prompted about " +
						"installing the new version of the plugin.");

				log.log("After update added...");

				direct_rdl.addListener(new ResourceDownloaderAdapter() {
					public boolean completed(
							final ResourceDownloader downloader,
							java.io.InputStream data) {
						// installUpdate( checker, pup, downloader, data );
						log.log("Update complete!");
						updated = true;
						try {
							data.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						// checker.completed();
						// unload();
						return (true);
					}
				});

				log.log("Update in progress...");
				updating = true;

				alternate_rdl.download();
				// direct_rdl.cancel();

				// alternate_rdl.addListener(
			}
		} catch (Throwable e) {
			Debug.printStackTrace(e);

			checker.failed();
		} finally {
			checker.completed();
		}

		// checker.completed();
	}

	protected void installUpdate(UpdateChecker checker, PluginUpdatePlugin pup,
			ResourceDownloader rd, InputStream data) {
		ZipInputStream zip = null;

		try {
			/*
			 * UpdateInstaller installer = checker.createInstaller();
			 * 
			 * installer.addResource("ono", data, false);
			 * 
			 * installer.addMoveAction("ono", pi.getPluginDirectoryName() +
			 * File.pathSeparator + "ono");
			 */
			zip = new ZipInputStream(data);

			ZipEntry entry = null;

			while ((entry = zip.getNextEntry()) != null) {
				String name = entry.getName();

				if (name.toLowerCase().startsWith("libs/")) {
					// get lib files
					name = name.substring(5);

					// skip the directory entry
					if (name.length() > 0) {
						rd.reportActivity("Adding update action for '" + name
								+ "'");

						if (log.isEnabled()) {
							log.log("PlatformManager:Win32 adding action for '"
									+ name + "'");
						}

						// pup.addResource(name, zip, false);
						//        
						// installer.addMoveAction(name,
						// pi.getPluginDirectoryName()
						// + File.separator + name);
					}
				}
			}
		} catch (Throwable e) {
			rd.reportActivity("Update install failed:" + e.getMessage());
		} finally {
			if (zip != null) {
				try {
					zip.close();

					if (rd != null) {
						rd.cancel();
						rd = null;
					}
				} catch (Throwable e) {
				}
			}
		}

		if (rd != null) {
			rd.cancel();
		}
	}

	protected SFPluginDetails loadPluginList() throws SFPluginDetailsException {
		InputStream is = null;

		if (HTTPPoller.hasBeenModified(page_url)) {
			ResourceDownloader dl = null;
			try {
				dl = rd_factory.create(new URL(page_url));

				dl = rd_factory.getTimeoutDownloader(dl, 60 * 1000);

				dl.addListener(this);

				Properties details = new Properties();

				is = dl.download();

				details.load(is);

				Iterator it = details.keySet().iterator();

				String version = null;
				String cvs_version = null;
				String name = null;
				String category = "";
				String plugin_id = null, download_url = null, author = null, desc = null, comment = null;
				while (it.hasNext()) {
					String propname = (String) it.next();

					String data = (String) details.get(propname);

					if (propname.equals("plugin.name")) {
						name = data;
					} else if (propname.equals("plugin.version")) {
						version = data;
					} else if (propname.equals("plugin.cvsversion")) {
						cvs_version = data;
					} else if (propname.equals("plugin.category")) {
						category = data;
					} else if (propname.equals("plugin.id")) {
						plugin_id = data;
					} else if (propname.equals("plugin.location")) {
						download_url = data;
					} else if (propname.equals("plugin.author")) {
						author = data;
					} else if (propname.equals("plugin.description")) {
						desc = data;
					} else if (propname.equals("plugin.comment")) {
						comment = data;
					}

				}

				OnoPluginDetails pi_details = new OnoPluginDetails(plugin_id,
						version, cvs_version, name, category);
				pi_details.setDetails(download_url, author, "", desc, comment);

				// loadPluginDetails(pi_details);

				is.close();

				return pi_details;

			} catch (Throwable e) {
				Debug.printStackTrace(e);

				throw (new SFPluginDetailsException("Plugin list load failed",
						e));
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (dl != null) {
					dl.cancel();
					dl = null;
				}
			}
		} else {
			return null;
		}
	}

	protected void loadPluginDetails(OnoPluginDetails details)
			throws SFPluginDetailsException {
		ResourceDownloader p_dl;
		InputStream is = null;

		try {

			p_dl = rd_factory.create(new URL(site_prefix + "pluginPage2.html"));

			p_dl = rd_factory.getTimeoutDownloader(p_dl, 30 * 1000);

			p_dl = rd_factory.getRetryDownloader(p_dl, 3);

			p_dl.addListener(this);

			is = p_dl.download();

			HTMLPage plugin_page = HTMLPageFactory.loadPage(is);

			if (!processPluginPage(details, plugin_page)) {
				is.close();
				throw (new SFPluginDetailsException(
						"Plugin details load fails for '" + details.getId()
								+ "': data not found"));
			}
		} catch (Throwable e) {
			Debug.printStackTrace(e);

			throw (new SFPluginDetailsException("Plugin details load fails", e));
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		if (p_dl != null) {
			p_dl.cancel();
			p_dl = null;
		}

	}

	protected boolean processPluginPage(OnoPluginDetails details, HTMLPage page)
			throws SFPluginDetailsException {
		HTMLTable[] tables = page.getTables();

		// dumpTables("", tables );
		return (processPluginPage(details, tables));
	}

	protected boolean processPluginPage(OnoPluginDetails details,
			HTMLTable[] tables) throws SFPluginDetailsException {
		for (int i = 0; i < tables.length; i++) {
			HTMLTable table = tables[i];

			HTMLTableRow[] rows = table.getRows();

			if (rows.length == 10) {
				HTMLTableCell[] cells = rows[0].getCells();

				if ((cells.length == 6)
						&& cells[0].getContent().trim().equals("Name")
						&& cells[5].getContent().trim().equals("Contact")) {
					// got the plugin details table
					HTMLTableCell[] detail_cells = rows[2].getCells();

					// String plugin_name = detail_cells[0].getContent();
					// String plugin_version = detail_cells[1].getContent();
					String plugin_auth = detail_cells[4].getContent();

					String[] dl_links = detail_cells[2].getLinks();

					String plugin_download;

					if (dl_links.length == 0) {
						plugin_download = "<unknown>";
					} else {
						plugin_download = site_prefix + dl_links[0];
					}

					HTMLTableCell[] cvs_detail_cells = rows[3].getCells();

					// String plugin_cvs_version =
					// cvs_detail_cells[1].getContent();
					String[] cvs_dl_links = cvs_detail_cells[2].getLinks();

					String plugin_cvs_download;

					if (cvs_dl_links.length == 0) {
						plugin_cvs_download = "<unknown>";
					} else {
						plugin_cvs_download = site_prefix + cvs_dl_links[0];
					}

					// System.out.println( "got plugin:" + plugin_name + "/" +
					// plugin_version + "/" + plugin_download + "/" +
					// plugin_auth );
					details.setDetails(plugin_download, plugin_auth,
							plugin_cvs_download, rows[6].getCells()[0]
									.getContent(), rows[9].getCells()[0]
									.getContent());

					return (true);
				}
			}

			HTMLTable[] sub_tables = table.getTables();

			boolean res = processPluginPage(details, sub_tables);

			if (res) {
				return (res);
			}
		}

		return (false);
	}

	protected void dumpTables(String indent, HTMLTable[] tables) {
		for (int i = 0; i < tables.length; i++) {
			HTMLTable tab = tables[i];

			System.out.println(indent + "tab:" + tab.getContent());

			HTMLTableRow[] rows = tab.getRows();

			for (int j = 0; j < rows.length; j++) {
				HTMLTableRow row = rows[j];

				System.out.println(indent + "  row[" + j + "]: "
						+ rows[j].getContent());

				HTMLTableCell[] cells = row.getCells();

				for (int k = 0; k < cells.length; k++) {
					System.out.println(indent + "    cell[" + k + "]: "
							+ cells[k].getContent());
				}
			}

			dumpTables(indent + "  ", tab.getTables());
		}
	}

	public void reportPercentComplete(ResourceDownloader downloader,
			int percentage) {
		log.log(percentage + "% complete");
	}

	public void reportActivity(ResourceDownloader downloader, String activity) {
		log.log("Activity: " + activity);
		informListeners(activity);
	}

	public boolean completed(ResourceDownloader downloader, InputStream data) {
		try {
			data.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// downloader.cancel();
		return (true);
	}

	protected void informListeners(String logMessage) {
		log.log("Inform listeners: " + logMessage); 
		for (int i = 0; i < listeners.size(); i++) {
			((SFPluginDetailsLoaderListener) listeners.get(i)).log(logMessage);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener#failed(org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader,
	 *      org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException)
	 */
	public void failed(ResourceDownloader downloader,
			ResourceDownloaderException e) {
		e.printStackTrace();
	}

	public void reportAmountComplete(ResourceDownloader downloader, long amount) {
		System.out.println(amount + " complete");

	}
}
