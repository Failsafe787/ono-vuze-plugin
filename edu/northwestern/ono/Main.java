/**
 * Ono Project
 *
 * File:         Main.java
 * RCS:          $Id: Main.java,v 1.76 2011/07/06 15:45:52 mas939 Exp $
 * Description:  Main class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      May 30, 2006 at 7:31:14 AM
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
package edu.northwestern.ono;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.Map.Entry;

import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.clientid.ClientIDManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageRegistration;
import org.gudy.azureus2.plugins.network.ConnectionManager;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.update.UpdateCheckInstance;
import org.gudy.azureus2.plugins.update.UpdateManager;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.xbill.DNS.Address;
import org.xbill.DNS.Record;

import com.aelitis.azureus.core.dht.control.DHTControlContact;
import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;
import com.aelitis.azureus.core.dht.netcoords.vivaldi.ver1.VivaldiPosition;
import com.aelitis.azureus.core.dht.netcoords.vivaldi.ver1.impl.HeightCoordinatesImpl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportFullStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandler;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.azureus.plugins.aznetmon.main.RSTPacketCountPerformer;
import com.azureus.plugins.aznetmon.main.RSTPacketStats;

import edu.northwestern.ono.util.PersistentData;
import edu.northwestern.ono.asn.NetworkAdminASNLookupImpl;
import edu.northwestern.ono.brp.BRPPeerManager;
import edu.northwestern.ono.connection.IOnoConnectionManager;
import edu.northwestern.ono.connection.azureus.AzureusOnoConnectionManager;
import edu.northwestern.ono.dht.IDistributedDatabase;
import edu.northwestern.ono.dht.azureus.AzureusDistributedDatabase;
import edu.northwestern.ono.dns.Digger;
import edu.northwestern.ono.experiment.OessExperimentManager;
import edu.northwestern.ono.experiment.OnoExperiment;
import edu.northwestern.ono.experiment.OnoExperimentManager;
import edu.northwestern.ono.messaging.OnoRatioMessage;
import edu.northwestern.ono.position.CIDRMatcher;
import edu.northwestern.ono.position.OnoPeerManager;
import edu.northwestern.ono.position.PeerFinderGlobal;
//import edu.northwestern.ono.position.VivaldiScanner;
import edu.northwestern.ono.stats.DownloadStats;
import edu.northwestern.ono.stats.StatManager;
import edu.northwestern.ono.stats.Statistics;
import edu.northwestern.ono.stats.VivaldiResult;
import edu.northwestern.ono.test.EdgeServerRatioTester;
import edu.northwestern.ono.timer.AzureusTimer;
import edu.northwestern.ono.timer.ITimer;
import edu.northwestern.ono.ui.OnoSimpleView;
//import edu.northwestern.ono.update.OnoUpdater;
import edu.northwestern.ono.util.AzPluginInterface;
import edu.northwestern.ono.util.AzureusLoggerChannel;
import edu.northwestern.ono.util.HTTPPoller;
import edu.northwestern.ono.util.ILoggerChannel;
import edu.northwestern.ono.util.PluginInterface;
import edu.northwestern.ono.util.PopupManager;
import edu.northwestern.ono.util.Randomness;
import edu.northwestern.ono.util.Util;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The Main class launches this plugin.
 */
public class Main extends MainGeneric implements UnloadablePlugin, DownloadListener {

	/** whether UI is created yet */
	boolean isCreated = false; 
	UISWTInstance swtInstance = null;
	  UISWTViewEventListener myView = null;
	  
    static PluginInterface pi_az = null;

    private static GenericMessageRegistration gmr;
    
    private static final String GENERIC_CLUSTER_OBJECT = "Unknown Download (Found through DHT?)";

    DownloadManager dm;
    ConnectionManager cm;
    static IDistributedDatabase dd;
    ClientIDManager cidm;
    Utilities ut;
    BasicPluginViewModel model;
    UpdateCheckInstance uci;
    private static LoggerChannel log;
    private BooleanParameter enableStatsParam;
    private ArrayList<URL> urls = new ArrayList<URL>();
	private UTTimer torrentCheckTimer;
	private boolean USE_ONO_UPDATE = false;
	private UIManagerListener uiListener;
//	private VivaldiScanner vivaldiScanner;
	BasicPluginConfigModel config_model;
	private LoggerChannelListener logListener;
	private IntParameter reportingTimeoutParam;
	private IntParameter maxPingsParam;
	private UTTimer utTimer;
	private RSTPacketCountPerformer perf;
	private UTTimerEvent rstTimerEvent;
	protected static boolean canUsePluginManager = false; // if false, plugin initialization hasn't finished
	

	private static final boolean DO_EXPERIMENTS = false;
	private static final boolean DO_OESS_EXPERIMENT = true;
    /** minimum delay to check for a new torrent, in milliseconds */
    private static final long TORRENT_CHECK_DELAY = 15 * 60 * 1000;
    /** whether to use plugin-specific download-only torrent */
    public static final boolean USE_DO_TORRENT = false;
    
    /** the time (in System.getMillis()) at which the plugin should check for a new torrent to add */
    private static long nextTorrentCheckTime = -1L;

    /** downloader factory */
    protected static ResourceDownloaderFactory rd_factory = ResourceDownloaderFactoryImpl.getSingleton();

	private static IOnoConnectionManager connectionManager;

	private static ArrayList<String> propertiesLocations = new ArrayList<String>();
	
	OessExperimentManager oessem;
	PopupManager ppm;
	private static String publicIp;
	private static String localIp;
	private static String onoDomain;
	public static PersistentData persistentDataManager = new PersistentData();
	private static String persistentDataFileName;  //= Main.getDir() + File.separatorChar + "ono.data." + getDomainName();
	private static String interfaceName = null;
    
    public Main(){
    	super();
    	
    }

    @Override
	public void loadProperties() {
    	props = new Properties();

    	//try {

    	// here's the name of the file

    	boolean found = false;
    	try {
    		for (String location : propertiesLocations){
    			File properties_file = new File(location);
    			// if properties file exists on its own then override any properties file
    			// potentially held within a jar
    			if (properties_file.exists()) {
    				FileInputStream fis = null;

    				try {
    					fis = new FileInputStream(properties_file);

    					props.load(fis);
    					found = true;
    					break;
    				} finally {
    					if (fis != null) {
    						fis.close();
    					}
    				}
    			}
    		}
    		if (!found) {
    			// properties file wasn't there, so use the jar
    			ClassLoader classLoader = pi_az.getPluginClassLoader();

    			if (classLoader instanceof URLClassLoader) {
    				URLClassLoader current = (URLClassLoader) classLoader;

    				URL url = current.findResource("ono.properties");

    				if (url != null) {
    					props.load(url.openStream());
    				} else {
    					throw (new Exception(
    							"failed to load ono.properties from jars"));
    				}
    			}
    		}
    	} catch (Throwable e) {
    		Debug.printStackTrace(e);

    		String msg = "Can't read 'ono.properties' for plugin '" +
    		pi_az.getPluginName() + "': file may be missing";

    		Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_ERROR,
    				msg));

    		System.out.println(msg);

    		try {
    			throw (new PluginException(msg, e));
    		} catch (PluginException e1) {
    			// TODO Auto-generated catch block
    			e1.printStackTrace();
    		}
    	}

	}

	/* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.Plugin#initialize(org.gudy.azureus2.plugins.PluginInterface)
     */
    public void initialize(org.gudy.azureus2.plugins.PluginInterface arg0) throws PluginException {
    	MainGeneric.startUp();
    	MainGeneric.setType("Azureus");
        pi_az = new AzPluginInterface();
        ((AzPluginInterface)pi_az).setPluginInterface(arg0);

        pi_az.addListener(new PluginListener(){
	
	     	public void closedownComplete() {
	     		// TODO Auto-generated method stub
	     		
	     	}
	
	     	public void closedownInitiated() {
	     		// TODO Auto-generated method stub
	     		
	     	}
	
	     	public void initializationComplete() {
	     		canUsePluginManager  = true;
	     		
     	}});
        
        propertiesLocations.add(pi_az.getPluginDirectoryName() +
        		java.io.File.separator + "ono.properties");
        if (System.getProperties().getProperty("user.home")!=null){
	        propertiesLocations.add(System.getProperties().getProperty("user.home") +
	        		java.io.File.separator + "ono.properties");
        }
        
        // get cdn names
        pi_az.getUtilities().createThread("CDN Name Fetcher", new Runnable(){

			public void run() {
				ResourceDownloaderFactory rd_factory = ResourceDownloaderFactoryImpl
				.getSingleton();
				
				ResourceDownloader rd = null;
				try {
					rd = rd_factory.create(new URL(
							"http://www.aqua-lab.org/ono/cdn_names.properties"));
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (rd == null) return;
				Properties cdnNames = new Properties();
				try {
					cdnNames.load(rd.download());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ResourceDownloaderException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				for (Entry<Object, Object> ent: cdnNames.entrySet())
					Digger.getInstance().addCDN((String)ent.getValue(), 
							Integer.parseInt((String)ent.getKey()));
				
			}});
        
        globalPeerFinder = new PeerFinderGlobal(pi);
        globalPeerFinder.setDaemon(true);
        globalPeerFinder.start();
        
//        dm = pi_az.getDownloadManager();
//        if (dm != null){
//        	for (Download download : dm.getDownloads()){
//        		if (download.getState()==Download.ST_DOWNLOADING || 
//        				download.getState() == Download.ST_SEEDING)
//        		{
//        		if (!peerFinders.containsKey(download)) {
//                    PeerFinder pf = new PeerFinder(dig, download,
//                            "PF-" + download.getName());
//                    peerFinders.put(download, pf);
//                    pf.start();
//                }
//        		}
//        	}
//        }
        //        dm.addListener(null); // TODO implement
        cm = pi_az.getConnectionManager();



   		if (canUsePluginManager){
	        for (org.gudy.azureus2.plugins.PluginInterface pi : pi_az.getPluginManager().getPlugins()) {
	            if (pi.getPluginName().contains("DistributedDatabase")) {
	                dd = new AzureusDistributedDatabase(pi.getDistributedDatabase());
	
	                break;
	            }
	        }
   		}
        
        // while we're here, register our custom message types
        //        try {
        //            pi.getMessageManager().registerMessageType(new PingExperimentMessage());
        //            pi.getMessageManager().registerMessageType(new OnoDataMessage());
        //            
        //        } catch (MessageException e1) {
        //            // TODO Auto-generated catch block
        //            e1.printStackTrace();
        //        }

        // here we use a properties file to get some global configuration options
        
        //            
        //        } catch (FileNotFoundException e) {
        //            // TODO Auto-generated catch block
        //            e.printStackTrace();
        //        } catch (IOException e) {
        //            // TODO Auto-generated catch block
        //            e.printStackTrace();
        //        }        

        loadProperties();
        // load properties 
        initializeValues();
        
        // grab a bunch of component references
        cidm = pi_az.getClientIDManager();
        ut = pi_az.getUtilities();
        
        
        //For now we are getting data for only Windows //ToDo: make it run on multiple OSs
        if( ut.isWindows() ){

            utTimer = ut.createTimer("RST Packet Monitor",false);
            perf = new RSTPacketCountPerformer(pi_az);

            //start on event within one second
            utTimer.addEvent(1000,perf);
            //follow-up every 1 minute
            rstTimerEvent = utTimer.addPeriodicEvent(
            		OnoConfiguration.getInstance().getPeerUpdateIntervalSec()*1000,
            		perf);
           

        }else{
            //Currently only works with windows
            RSTPacketStats stats = RSTPacketStats.getInstance();
            stats.setStatus("Plug-in currently only works with Windows machines.");
        }
        
        // this does nothing for now
        pi_az.getUtilities().getLocaleUtilities()
          .integrateLocalisedMessageBundle("edu.northwestern.ono.internat.Messages");

        UIManager ui_manager = pi_az.getUIManager();
        

        // TODO add more GUI options
    	config_model =
            ui_manager.createBasicPluginConfigModel(
                    "plugins", "ono.name");
        model = pi_az.getUIManager()
        .createBasicPluginViewModel("Ono Logs");

        model.getActivity().setVisible(false);
        model.getProgress().setVisible(false);

        uiListener = new UIManagerListener() {
        	public void UIAttached(UIInstance instance) {
        		String parts[] = pi_az.getAzureusVersion().split("\\.");
        		if (Integer.parseInt(parts[0])>=4 && Integer.parseInt(parts[1])>=3){
        			if (instance instanceof UISWTInstance) {
	        			swtInstance = (UISWTInstance)instance;
	        			myView = new OnoSimpleView(pi_az);
	        			swtInstance.addView(UISWTInstance.VIEW_MAIN, OnoSimpleView.VIEWID, myView);
	        		}
        		} else {
	        		if (instance instanceof UISWTInstance) {
	        			swtInstance = (UISWTInstance)instance;
	        			myView = new OnoSimpleView(pi_az);
	        			swtInstance.addView(UISWTInstance.VIEW_MAIN, OnoSimpleView.VIEWID, myView);
	        		}
        		}
        	}

        	public void UIDetached(UIInstance instance) {
        		if (instance instanceof UISWTInstance) {
        			swtInstance = null;

        		}
        	}
        };

        pi_az.getUIManager().addUIListener(uiListener);  
        
        
        
        
        
        
        log = pi_az.getLogger().getChannel("Ono");
        
        log.log("Initializing Ono version " + pi_az.getPluginVersion());

        //        fixJars(pi);
        UpdateManager um = pi_az.getUpdateManager();

        //        Download[] dls = dm.getDownloads();
        //        if (dls.length>0)dls[0].getPeerManager();

        // handle Ono updates
        if (USE_ONO_UPDATE ) checkForUpdate(um);

      
   
        
//        enableStatsParam.addListener(
//                new ParameterListener()
//                {
//                    public void
//                    parameterChanged(
//                        Parameter   param )
//                    {
//                    	
//                    }
//                });
            

        
        // print output to log window
        logListener = new LoggerChannelListener() {
            public void messageLogged(int type, String message) {
                model.getLogArea().appendText(message + "\n");
            }

            public void messageLogged(String str, Throwable error) {
                model.getLogArea().appendText(error.toString() + "\n");
            }
        };
        
        log.addListener(logListener);
        


        // create and initialize digger
        startDigger();
        
        //create and initialize OessExperimentManager
        if (DO_OESS_EXPERIMENT) {
        	startOessExperimentManager();
        }
        
        OnoOptionsView oov = OnoOptionsView.getInstance();
        oov.setConfigModel(config_model);
        oov.setProperties(props);
        oov.setPropsLocations(propertiesLocations);
        oov.setPluginInterface(pi_az);
        oov.initialize();

        // create and start experiment manager
        if (DO_EXPERIMENTS ) startExperimentManager();
        
        if (USE_DO_TORRENT){
        	UTTimerEventPerformer performer = new UTTimerEventPerformer(){

				public void perform(UTTimerEvent event) {
					MainGeneric.checkForTorrentToDownload();
					
				}};
        	torrentCheckTimer = pi_az.getUtilities().createTimer("Torrent check");
        	torrentCheckTimer.addEvent(System.currentTimeMillis()+1000, performer);
        	torrentCheckTimer.addPeriodicEvent(60*60*1000, performer);
        	
        }

        // register listeners for downloads               
        pi_az.getDownloadManager().addListener(new DownloadManagerListener() {
                public void downloadAdded(Download download) {
                    download.addListener(Main.this);
                    download.addTrackerListener(DownloadStats.getInstance());
                }

                public void downloadRemoved(Download download) {
                    download.removeListener(Main.this);
                    download.removeTrackerListener(DownloadStats.getInstance());
                }
            });
        
//        vivaldiScanner  = new VivaldiScanner();
//        pi_az.getUtilities().createThread("VivaldiScanner", vivaldiScanner);
        
        if (OnoConfiguration.getInstance().getCidrFile()!=null){
        	CIDRMatcher.getInstance().loadFromFile(OnoConfiguration.getInstance().getCidrFile());
        }
        
        pi_az.addListener(new PluginListener(){

			public void closedownComplete() {
				// TODO Auto-generated method stub
				
			}

			public void closedownInitiated() {
				try {
					unload();
				} catch (PluginException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}

			public void initializationComplete() {
				// TODO Auto-generated method stub
				
			}});
        
        if (TEST_EDGE_RATIO){
        	pi_az.getUtilities().createThread("EdgeTester", new EdgeServerRatioTester());
        }
        
        pi.getUtilities().createThread("Register Usage", new Runnable(){

			public void run() {
				StatManager.start();
				
			}});

        
    	persistentDataFileName = Main.getDir() + File.separatorChar + "ono.data." + getDomainName();
        loadPersistentData();
        startPopupManager();
    }
    
    public void updateRSTTimer(){
    	if (rstTimerEvent!=null){
    		rstTimerEvent.cancel();
    		rstTimerEvent = utTimer.addPeriodicEvent(RSTPacketStats.INTERVAL,perf);
    	}
    }

    /**
     * @param downloader
     * @param data
     */
    protected void writeAndLoadFile(ResourceDownloader downloader,
        InputStream data) {
        File outputFile = new File(pi_az.getPluginDirectoryName() +
                File.separatorChar +
                downloader.getName()
                          .substring(downloader.getName().lastIndexOf('/')));

        if (outputFile.exists()) {
            outputFile.delete();
        }

        FileWriter out;

        try {
            out = new FileWriter(outputFile);

            int c;

            while ((c = data.read()) != -1)
                out.write(c);

            out.flush();
            out.close();

            urls.add(outputFile.toURL());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Starts the update checker.
     * @param um The update manager.
     */
    private void checkForUpdate(UpdateManager um) {
//        uci = um.createEmptyUpdateCheckInstance(UpdateCheckInstance.UCI_UPDATE,
//                "Ono Updater");
//        uci.addUpdatableComponent(new OnoUpdater(this, RD_SIZE_TIMEOUT,
//                RD_SIZE_RETRIES, log, pi_az, uci), true);
//
//        uci.start();
    }

    // from here on down, these are download listener actions.
    // can be used for net positioning control by adding/removing "nearby" peers
    public void stateChanged(Download download, int old_state, int new_state) {
        if (!DO_NET_POS) {
            return;
        }

//        if ((new_state == Download.ST_DOWNLOADING) ||
//                (new_state == Download.ST_SEEDING)) {
//            if (!peerFinders.containsKey(download)) {
//                PeerFinder pf = new PeerFinder(dig, download,
//                        "PF-" + download.getName());
//                peerFinders.put(download, pf);
//                pf.start();
//            }
//        } else if ((new_state == Download.ST_QUEUED) ||
//                (new_state == Download.ST_STOPPED)) {
//            PeerFinder pf = peerFinders.remove(download);
//
//            if (pf != null) {
//                pf.setActive(false);
//            }
//           
//        }
    }

    public void positionChanged(Download download, int oldPosition,
        int newPosition) {
    }

    protected void inject(Download[] downloads, String cause) {
        /*String  x = peers.getValue().trim();
        
        StringTokenizer tok = new StringTokenizer(x, "," );
        
        List    ips     = new ArrayList();
        List    ports   = new ArrayList();
        
        while( tok.hasMoreTokens()){
        
            String  s = tok.nextToken();
        
            int p = s.indexOf(":");
        
            if ( p == -1 ){
        
                log.log( "Invalid peer spec '" + s + "', must be 'IP:port'" );
        
                continue;
            }
        
            ips.add( s.substring( 0, p ).trim());
        
            ports.add( s.substring( p+1 ).trim());
        
        }
        
        try{
        
            for (int i=0;i<downloads.length;i++){
        
                Download    download = downloads[i];
        
                PeerManager pm = download.getPeerManager();
        
                if ( pm != null ){
        
                    log.log( "Injecting " + ips.size() +
                            " peers into '" + download.getName() + "': " + cause );
        
                    for (int j=0;j<ips.size();j++){
        
                        try{
        
                            String  ip      = (String)ips.get(j);
                            int     port    = Integer.parseInt((String)ports.get(j));
        
                            pm.addPeer( ip, port, false );
        
                        }catch( Throwable e ){
        
                            log.log(e);
                        }
                    }
                }
            }
        }catch( Throwable e ){
        
            log.log(e);
        }*/
    	
    }

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        unload();

        super.finalize();
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.UnloadablePlugin#unload()
     */
    public void unload() throws PluginException {
    	
    	try {
    		
    		MainGeneric.startShutdown();
        	if (swtInstance!=null) swtInstance.removeViews(UISWTInstance.VIEW_MAIN, OnoSimpleView.VIEWID);    	
        	pi_az.getUIManager().removeUIListener(uiListener);
        	model.destroy();
        	config_model.destroy();
        	log.removeListener(logListener);
    		if (pi!=null && pi.getMessageManager()!=null) {
        		pi.getMessageManager().deregisterMessageType(new OnoRatioMessage());
        	}
        // catch all exceptions so it is sure to unload
    		MainGeneric.getReporter().setExtendedConnection(true);

    		
        	try {onoUnload();} catch (Exception e){
        		e.printStackTrace();
        	}
    		
    	
    	
//    	vivaldiScanner.setActive(false);

    	if (DEBUG) System.out.println("Stopping Ono peer manager...");
    	OnoPeerManager.getInstance().stop();
    	
    	if (OnoConfiguration.getInstance().enableBRP) {
    		if (DEBUG) System.out.println("Stopping BRP peer manager...");
    		BRPPeerManager.getInstance().stop();
    	}
    	
    	if (DEBUG) System.out.println("Saving persistent data...");
		Main.saveObject(persistentDataManager, persistentDataFileName);
        
		if ( DEBUG ) System.out.println("Stopping poup manager...");
		stopPopupManager();
		
		if ( DEBUG ) System.out.println("Stopping oess experiment manager...");
		stopOessExperimentManager();
		
		
        MainGeneric.getReporter().setExtendedConnection(false);
        MainGeneric.getReporter().disconnect();
        
        if (torrentCheckTimer!=null) torrentCheckTimer.destroy();
        
        peerFinders.clear();
        
        
    	} catch (Exception e){
    		e.printStackTrace();
    		MainGeneric.getReporter().disconnect();
    	}
    	
    }    


    public static GenericMessageRegistration getGMR() {
        return gmr;
    }

    public static PluginInterface getPluginInterface() {
        // TODO Auto-generated method stub
        return pi_az;
    }

    protected void startExperimentManager() {
		OnoExperimentManager oem = new OnoExperimentManager();
		oem.setConnectionManager(getConnectionManager());
        pi_az.getUtilities().createThread("OnoExperimentManager", oem);
	}       

    protected void startOessExperimentManager() {
		oessem = new OessExperimentManager();
        pi_az.getUtilities().createThread("OessExperimentManager", oessem);
	}
    
    protected void startPopupManager() {
    	ppm = PopupManager.getInstance();
		pi_az.getUtilities().createThread("PopupManager", ppm);		
    }
    
    protected void stopOessExperimentManager() {
    	
    	if ( oessem != null )
    		OessExperimentManager.stop();
    }
    
    protected void stopPopupManager() {
    	if ( ppm != null )
    		ppm.stop();
    }
    
    protected void loadPersistentData() {
		Object pd = Main.loadObject(persistentDataFileName);
		if (pd != null) persistentDataManager = (PersistentData) pd;
    }
    
    public static void doAppPing(DHTControlContact contact, final VivaldiPosition position, 
    		final DHTNetworkPosition position2,
			final HeightCoordinatesImpl coord, final String peerIp) {
		contact.getTransportContact().sendImmediatePing(new DHTTransportReplyHandler(){
	
			public void failed(DHTTransportContact contact, Throwable error) {
				// TODO Auto-generated method stub
				
			}
	
			public void findNodeReply(DHTTransportContact contact, DHTTransportContact[] contacts) {
				// TODO Auto-generated method stub
				
			}
	
			public void findValueReply(DHTTransportContact contact, DHTTransportValue[] values, byte diversification_type, boolean more_to_come) {
				// TODO Auto-generated method stub
				
			}
	
			public void findValueReply(DHTTransportContact contact, DHTTransportContact[] contacts) {
				// TODO Auto-generated method stub
				
			}
	
			public void keyBlockReply(DHTTransportContact contact) {
				// TODO Auto-generated method stub
				
			}
	
			public void keyBlockRequest(DHTTransportContact contact, byte[] key, byte[] key_signature) {
				// TODO Auto-generated method stub
				
			}
	
			public void pingReply(DHTTransportContact contact, int elapsed_time) {
				VivaldiResult v = new VivaldiResult(position, position2, coord, elapsed_time, peerIp);
	    		 Statistics.getInstance().addVivaldiResult(v);
				
			}
	
			public void statsReply(DHTTransportContact contact, DHTTransportFullStats stats) {
				// TODO Auto-generated method stub
				
			}
	
			public void storeReply(DHTTransportContact contact, byte[] diversifications) {
				// TODO Auto-generated method stub
				
			}

			public void queryStoreReply(DHTTransportContact contact,
					List<byte[]> response) {
				// TODO Auto-generated method stub
				
			}
			
		}, 1000);
		
	}

	/**
	 *
	 */
	public static void getHardCodedMappings(OnoExperiment onoExp, 
			HashMap<String, HashSet<String>> hardCodedMappings, 
			Vector<InetAddress> otherPeers) {
	    String hostName;
	    InputStream is = null;
	
	    try {
	        hostName = java.net.InetAddress.getLocalHost().getHostName();
	
	        ResourceDownloaderFactory rd_factory = ResourceDownloaderFactoryImpl.getSingleton();
	
	        ResourceDownloader dl = rd_factory.create(new URL(
	                    "http://165.124.180.20/~aqualab/azplugin/hardcode/" +
	                    "hardCodedMappings-" + hostName + ".txt"));
	        dl = rd_factory.getTimeoutDownloader(dl, 60 * 1000);
	
	        // download the experiment list 
	        Properties details = new Properties();
	        is = dl.download();
	        details.load(is);
	        dl.cancel();
	
	        Iterator it = details.entrySet().iterator();
	
	        while (it.hasNext()) {
	            Entry ent = ((Entry) it.next());
	            String peerIp = (String) ent.getKey();
	            String edges = (String) ent.getValue();
	            String[] edgeArray = edges.split(";");
	
	            for (String edge : edgeArray) {
	                if (hardCodedMappings.get(Util.getClassCSubnet(edge)) == null) {
	                    hardCodedMappings.put(Util.getClassCSubnet(edge),
	                        new HashSet<String>());
	                }
	
	                hardCodedMappings.get(Util.getClassCSubnet(edge)).add(peerIp);
	            }
	        }
	
	        // now get active nodes 
	        dl = rd_factory.create(new URL(
	                    "http://165.124.180.20/~aqualab/azplugin/hardcode/allNodesIps.txt"));
	        dl = rd_factory.getTimeoutDownloader(dl, 60 * 1000);
	
	        // download the experiment list 
	        if (is != null) {
	            is.close();
	        }
	
	        is = dl.download();
	
	        StringBuffer sb = new StringBuffer();
	
	        while (is.available() > 0) {
	            sb.append((char) is.read());
	        }
	
	        String[] lines = sb.toString().split("\n");
	
	        for (String line : lines) {
	            otherPeers.add(Address.getByAddress(line));
	        }
	        dl.cancel();
	    } catch (UnknownHostException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    } catch (MalformedURLException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    } catch (ResourceDownloaderException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
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
	}

	public static IDistributedDatabase getDistributedDatabase() {
		
		if (!canUsePluginManager) return null;

		org.gudy.azureus2.plugins.PluginInterface[] pis = pi_az.getPluginManager().getPlugins();
        String s;

        try {
        for (org.gudy.azureus2.plugins.PluginInterface pi : pis) {
            s = pi.getPluginName();

            if (s.contains("Distributed")) {
                dd = new AzureusDistributedDatabase(pi.getDistributedDatabase());
                return dd;

            }
        }
        } catch (Exception e){
        	// failed
        }
		return null;
	}
    
    /**
     * Checks for torrents that this node should connect to for the purpose of
     * getting peers for experiments.
     */
    public static void checkForTorrentToDownload() {
    	if (!USE_DO_TORRENT) return;
        if (nextTorrentCheckTime < System.currentTimeMillis()) {
            nextTorrentCheckTime = (long) (Randomness.getRandom().nextDouble()) * TORRENT_CHECK_DELAY;            
            String url = "http://aqualab.cs.northwestern.edu/azplugin/torrents.dat";
            // don't do this to me...
           // if (true || !getIp().equals("165.124.182.135")) {
           if (pi_az.getDownloadManager().getDownloads().length==0 || 
        		   HTTPPoller.hasBeenModified(url)){
            InputStream is = null;

                try {
                    ResourceDownloader dl = rd_factory.create(new URL(
                                url));
                    // TODO put file there in format: ono.torrent.1={URL to torrent}
                    dl = rd_factory.getTimeoutDownloader(dl, 60 * 1000);

                    // download the torrent location 
                    final Properties details = new Properties();

                    dl.addListener(new ResourceDownloaderListener(){

						public boolean completed(ResourceDownloader downloader, InputStream data) {
							
							try {
								details.load(data);
							
								data.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							return true;
							
						}

						public void failed(ResourceDownloader downloader, ResourceDownloaderException e) {
							// TODO Auto-generated method stub
							
						}

						public void reportActivity(ResourceDownloader downloader, String activity) {
							// TODO Auto-generated method stub
							
						}

						public void reportPercentComplete(ResourceDownloader downloader, int percentage) {
							// TODO Auto-generated method stub
							
						}

					
						public void reportAmountComplete(
								ResourceDownloader downloader, long amount) {
							// TODO Auto-generated method stub
							
						}});
                    is = dl.download();
                    

                    Iterator it = details.keySet().iterator();

                    //					String fileName = "temp.torrent";
                    while (it.hasNext()) {
                        String torrentName = (String) it.next();
                        String torrentName2 = (String) details.get(torrentName);

                        /*boolean hasTorrent = false;
                        if (torrentName2.contains("OnoExp")) {
                        	if (pi_az.getDownloadManager()!=null){
                            for (Download d : pi_az.getDownloadManager()
                                                .getDownloads()) {
                                if (d.getName().equals(torrentName2)) {
                                    if (d.getState() == Download.ST_SEEDING) {
                                        try {
                                            d.initialize();
                                        } catch (DownloadException e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                        }
                                    }

                                    hasTorrent = true;
                                }
                            }
                        	}
                        }*/
                        
                        

                        //dl.cancel();
                        is.close();

                       //if (hasTorrent) continue;
                        
                        String torrentURL = (String) details.get(torrentName);

                        if (torrentURL.equals("")) {
                            continue;
                        }

                        // download the torrent itself
                        dl = rd_factory.create(new URL(torrentURL));

                        System.out.println("Getting torrent "+torrentURL);
                        dl = rd_factory.getTimeoutDownloader(rd_factory.getTorrentDownloader(
                                    dl,
                                    false /*, new File(pi.getPluginDirectoryName())*/),
                                    3*60 * 1000);

                        is = dl.download();
                        is.close();
                    } // end while for properties

                    //dl.cancel();
                    dl = null;
                    
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ResourceDownloaderException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
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
            } // end if not me
        } // end if right time
    }
    
    public static HashMap<String, Integer> getOnoPeers(int limit){
    	HashMap<String, Integer> peerIps = new HashMap<String, Integer>();
    	for (Download d : pi.getDownloadManager().getDownloads()) {
            if (d.getName().contains("OnoExp")) {
                if (d.getPeerManager() == null) {
                    return peerIps;
                }

                Peer[] ps = d.getPeerManager().getPeers();

                for (Peer p : ps) {
                    peerIps.put(p.getIp(), p.getTCPListenPort());
                }
            }
        }
    	return peerIps;
    }
    
    public static ILoggerChannel getLoggerChannel(String name){
    	return new AzureusLoggerChannel(pi_az.getLogger().getChannel(name));
    }
    
    public static IOnoConnectionManager getConnectionManager(){
    	if (connectionManager==null){
    		connectionManager = new AzureusOnoConnectionManager(pi_az);
    	}
    	return connectionManager;
    }
    
    public static int getPeerPort(String dest){
        // find the other guy's port via the custom torrent
        for (Download d : pi_az.getDownloadManager().getDownloads()) {
                if (d.getPeerManager() != null) {
                    for (Peer p : d.getPeerManager().getPeers()) {
                        if (p.getIp().contains(dest)) {
                            return p.getTCPListenPort();                            
                        } // end if
                    } // end for peers
                }

                return -1;
        } // end for downloads
        return -1;
    }
    
    public static ITimer createTimer(String name) {
		return new AzureusTimer(pi_az.getUtilities()
                          .createTimer(name));
	}
    
    public static Properties downloadFromURL(String url, int timeout) {
        InputStream is = null;
        ResourceDownloader dl = null;
        
		Properties details = new Properties();
        try {
			dl = rd_factory.create(new URL(url));
		

			dl = rd_factory.getTimeoutDownloader(dl, timeout);

	        // download the experiment list 
	        is = dl.download();
	        details.load(is);
	        dl.cancel();
        } catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ResourceDownloaderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return details;
    }

	public static boolean isRunning() {
		return pi_az!=null;
	}
	
	public static Object getClusterFinderObject(String peerIp){
		if (peerIp == null ) return GENERIC_CLUSTER_OBJECT;
		if (pi_az.getDownloadManager()!=null){
            for (Download d : pi_az.getDownloadManager()
                                .getDownloads()) {
                PeerManager pm = d.getPeerManager();
                if (pm!=null && pm.getPeers() !=null){
                	for (Peer p : pm.getPeers()){
                		if (p.getIp().equals(peerIp)){
                			return d;
                		}
                	}
                }
            }
		}
		
		return GENERIC_CLUSTER_OBJECT;
	}

	public static void doAppPing(DHTControlContact contact, final VivaldiPosition _position, 
			final DHTNetworkPosition _position2, final String peerIp) {
		contact.getTransportContact().sendImmediatePing(new DHTTransportReplyHandler(){
			
			public void failed(DHTTransportContact contact, Throwable error) {
				// TODO Auto-generated method stub
				
			}
	
			public void findNodeReply(DHTTransportContact contact, DHTTransportContact[] contacts) {
				// TODO Auto-generated method stub
				
			}
	
			public void findValueReply(DHTTransportContact contact, DHTTransportValue[] values, byte diversification_type, boolean more_to_come) {
				// TODO Auto-generated method stub
				
			}
	
			public void findValueReply(DHTTransportContact contact, DHTTransportContact[] contacts) {
				// TODO Auto-generated method stub
				
			}
	
			public void keyBlockReply(DHTTransportContact contact) {
				// TODO Auto-generated method stub
				
			}
	
			public void keyBlockRequest(DHTTransportContact contact, byte[] key, byte[] key_signature) {
				// TODO Auto-generated method stub
				
			}
	
			public void pingReply(DHTTransportContact contact, int elapsed_time) {
				VivaldiResult v = new VivaldiResult(_position, _position2, (HeightCoordinatesImpl)_position.getCoordinates(), elapsed_time, peerIp);
	    		 Statistics.getInstance().addVivaldiResult(v);
				
			}
	
			public void statsReply(DHTTransportContact contact, DHTTransportFullStats stats) {
				// TODO Auto-generated method stub
				
			}
	
			public void storeReply(DHTTransportContact contact, byte[] diversifications) {
				// TODO Auto-generated method stub
				
			}

			public void queryStoreReply(DHTTransportContact contact,
					List<byte[]> response) {
				// TODO Auto-generated method stub
				
			}
			
		}, 1000);
		
	}


	public static ArrayList<String> getPropertiesLocations(){
		return propertiesLocations;
	}
	  /*protected void showStats() {
		    if (stats_tab == null) {
		      stats_tab = new Tab(new StatsView(globalManager,azureus_core));
		      stats_tab.getView().getComposite().addDisposeListener(new DisposeListener() {
		      	public void widgetDisposed(DisposeEvent e) {
							stats_tab = null;
						}
					});
		    } else {
		      stats_tab.setFocus();
		    }
		  }*/

	public static boolean isDoneInitializing() {		
		return canUsePluginManager;
	}

	public static void log(String string) {
		log.log(string);
		
	}
	
	/*SERIALIZABLE OPERATIONS API*/

	public static void saveObject(Serializable object, String filename) {
		ObjectOutputStream objstream;
		try {
			objstream = new ObjectOutputStream(new FileOutputStream(filename));
			objstream.writeObject(object);
			objstream.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Loads an object.
	 */
	public static Object loadObject(String filename) {
		ObjectInputStream objstream;
		Object object = null; 

		try {
			File f = new File(filename);
			if (f.isFile()) {
				objstream = new ObjectInputStream(new FileInputStream(f));
				object = objstream.readObject();
				objstream.close();
			}
		} catch (FileNotFoundException e) {
			//e.printStackTrace();
		} catch (IOException e) {
			//e.printStackTrace();
			//when versionID for persistenData() class changed, then don't load,
			//ignore error and simply override file
		} catch (ClassNotFoundException e) {
			//e.printStackTrace();
		}

		return object;
	}
	
	public static String getDomainName() {

		if ((onoDomain != null) && (!onoDomain.equals("default")))
			return onoDomain;

		String myDomain = "default";
		String myIp = null;

		try {

			myIp = Main.getPublicIpAddressFirstTime();

			if (myIp != null) {
				Record[] records = Util.doLookup(new String[]{"-x", myIp});
				if (records == null)
					return myDomain;

				String domain = records[0].toString();

				if (domain != null) {

					String[] parts = domain.split("\\.");
					int length = parts.length;

					/* 
					 * If last part of domain name is 2 character's long, 
					 * then it contains CC top level domain
					 */
					if ((length > 3) && ( parts[length-1].length() < 3 ))
						myDomain = parts[length-3] + "." +
						parts[length-2] + "." + parts[length-1];

					else 
						myDomain = parts[length-2] + "." + parts[length-1];

				}
			}


		} catch (IOException e) {
			e.printStackTrace();
		}

		onoDomain = myDomain;
		
		return myDomain;

	}
	
	public static String getDir() {
		return pi_az.getPluginconfig().getPluginUserFile("").getAbsolutePath();
	}
	

	public static String getPublicIpAddressFirstTime() {
		checkForLocalIpAddress();
		try {
			if (Main.getPluginInterface().getUtilities().getPublicAddress() != null) {
				NetworkAdminASNLookupImpl.getInstance().setAddress(
						Main.getPluginInterface().getUtilities().getPublicAddress());
				String ip = Main.getPluginInterface().getUtilities().getPublicAddress().toString().substring(1);

				publicIp = ip;
				return ip;
			} else {
				String s = getLocalPublicAddress();
				if (s.startsWith("192.168")) return null;
				publicIp = s;
				return s;
			}
		} catch (Exception e){
			//LOGGER.warning(e.getMessage());
			return null;
		}
	}
	
	private static void checkForLocalIpAddress(){
		InetAddress addresses[];
		try {
			addresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
			for (InetAddress addr : addresses){
				if (addr.isSiteLocalAddress()) {
					NetworkInterface eth = NetworkInterface.getByInetAddress(addr);
					String interfaceName = getInterfaceName();
					if(eth != null && eth.getDisplayName().equals(interfaceName)){
						localIp = addr.getHostAddress();
					}
				}
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	
	/* return eth0 by default if the interface cant' be found */
	public static String getInterfaceName() {

		if(interfaceName != null) return interfaceName;
		
		String ret = "eth0";
		try {
			NetworkInterface eth = NetworkInterface.getByInetAddress(getLocalIPAddress());
			if (eth != null)
				ret = eth.getDisplayName();

		} catch (Exception e) {
			interfaceName = "eth0";
			return "eth0";
		}

		interfaceName = ret;
		return ret;

	}

	/* return eth0 by default if the interface cant' be found */
	public static String getInterfaceMAC() {

		String ret = "eth0";
		try {
			NetworkInterface eth = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
			if (eth != null) {
				byte[] mac =eth.getHardwareAddress();  
				StringBuilder macaddress= new StringBuilder();  
				for (int i = 0; i < mac.length; i++) {  
					macaddress.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : "")) ;  
				}  

				ret = macaddress.toString();  

			}

		} catch (UnknownHostException e) {
			return "unk";
		} catch (SocketException e) {
			return "unk";
		}

		return ret;

	}
	
	public static InetAddress getLocalIPAddress() {
		
		URL origin = null;
		String hostName= null;
		String strProtocol = null;
		Socket local = null;
		InetAddress objLocalHost = null;
		try {
		  origin = new URL("http://www.google.com"); //this is the Applet-class
		  hostName = origin.getHost();
		  strProtocol = origin.getProtocol();

		  if (origin.getPort() != -1)
		     local = new Socket(hostName, origin.getPort());
		  else
		  {
		     if (strProtocol.equalsIgnoreCase("http"))
		        local = new Socket(hostName, 80);

		     else if (strProtocol.equalsIgnoreCase("https"))
		        local = new Socket(hostName, 443);
		  }

		  objLocalHost = local.getLocalAddress();
		  return objLocalHost;
		  
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
		  try {
			  if (local != null)
				  local.close();
		  } catch (IOException ioe) {
		     ioe.printStackTrace();
		  }
		}

		return objLocalHost;
	}
	
	public static PersistentData getPersistentDataManager() {
		return persistentDataManager;
	}

}
