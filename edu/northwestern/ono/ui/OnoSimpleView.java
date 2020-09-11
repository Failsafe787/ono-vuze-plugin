/**
 * Ono Project
 *
 * File:         OnoSimpleView
 * RCS:          $Id: OnoSimpleView.java,v 1.3 2011/07/06 15:45:53 mas939 Exp $
 * Description:  OnoSimpleView class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Apr 16, 2007 at 3:33:47 PM
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedTruncatedLabel;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.brp.BRPPeerManager;
import edu.northwestern.ono.brp.BRPPeerManager.BRPPeer;
import edu.northwestern.ono.dns.Digger;
import edu.northwestern.ono.position.CDNClusterFinder;
import edu.northwestern.ono.position.GenericPeer;
import edu.northwestern.ono.position.OnoPeerManager;
import edu.northwestern.ono.position.OnoPeerManager.OnoPeer;
import edu.northwestern.ono.stats.EdgeServerRatio;
import edu.northwestern.ono.ui.items.CDNItem;
import edu.northwestern.ono.ui.items.PeerRow;
import edu.northwestern.ono.util.ILoggerChannel;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The OnoSimpleView class ...
 */
public class OnoSimpleView  implements UISWTViewEventListener {
  


public static class EdgeData{
	public EdgeData(String custName, String ip, Double rtt, Double ratio) {
		this.custName = custName;
		this.ip = ip;
		this.rtt = rtt;
		this.ratio = ratio;
	}
	public String custName;
	public String ip;
	public double rtt;
	public double ratio;
	@Override
	public boolean equals(Object obj) {
		if (obj==null) return false;
		if (obj instanceof EdgeData){
			EdgeData ed = (EdgeData) obj;
			if (ed.ip.equals(ip) && ed.custName.equals(custName)){
				return true;
			}
		}
		return false;
	}
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return ip.hashCode()+custName.hashCode();
	}
	public void copy(EdgeData data) {
		this.rtt = data.rtt;
		this.ratio = data.ratio;
		
	}

}

public static class IpData{
	public String ip;
	public String dl;
	public IpData(String ip, String dl) {
		super();
		this.ip = ip;
		this.dl = dl;
	}
	
	
}

public static class IpStatData {
	public String ip;
	boolean isDHT;
	boolean isDig;
	boolean hasRatio;
	public IpStatData(String ip, boolean isDHT, boolean isDig, boolean hasRatio) {
		super();
		this.ip = ip;
		this.isDHT = isDHT;
		this.isDig = isDig;
		this.hasRatio = hasRatio;
	}
	
	public String getStatus(){
		StringBuffer s = new StringBuffer();
		if (isDHT) s.append("Looked up in DHT");
		if (isDig){
			if (s.length()>0) s.append(" | ");
			s.append("Digging remotely");
		}
		if (hasRatio){
			if (s.length()>0) s.append(" | ");
			s.append("Tracking ratio");
		}
		
		return s.toString();
		
	}
}

public static final String VIEWID = "Ono";


public static final String EDGE_MAPPINGS = "EdgeMappings";
public static final String NEARBY_PEERS = "NearbyPeers";

/** in seconds */
private static final int PERIODIC_UPDATE_INTERVAL = 5;


//private static final String CFG_PREFIX = "news.anomaly.";


private static final int MAX_ALARMS = 50;


private static final int MAX_ALERTS = 50;
  
  Composite panel;
  
  
  boolean activityChanged;
  Table activityTable;
  Table edgeTable;
 

  private boolean isCreated = false;




	private Map<String, String> allIps;




protected boolean ipsChanged;



private PluginInterface pi;


private UTTimer timer;


boolean listenersAdded = false;


private boolean showEdge = true;


private Table alertTable;


private boolean noChange;

private ArrayList<PeerRow> onoPeers;

private ArrayList<CDNItem> cndItems;


private Table edgeMapTable;


private Table nearbyPeersTable;


private ILoggerChannel log;

private boolean printLogs = false;

  public OnoSimpleView( PluginInterface pi ) {
   this.pi = pi;
   log = MainGeneric.getLoggerChannel("OnoUI");
	if (printLogs) log.setDiagnostic();
    init();
  }
  
  private void init() {
	  allIps = new HashMap<String, String>();

	    activityChanged = true;
  }
  
  public void initialize(Composite composite) {
	  panel = new Composite(composite,SWT.NULL);
	    GridLayout layout = new GridLayout();
	    layout.numColumns = 2;
	    
	    panel.setLayout(layout);
	    
	    timer = pi.getUtilities().createTimer("OnoViewUpdateTimer");
	    timer.addPeriodicEvent(PERIODIC_UPDATE_INTERVAL*1000, new UTTimerEventPerformer(){

			public void perform(UTTimerEvent event) {
				if (printLogs) log.log("@"+System.currentTimeMillis()+"Updating Ono UI tables...");
				periodicUpdate();
				
			}});    
	    
	    Label label = new Label(panel, SWT.WRAP);
	    Messages.setLanguageText(label,"OnoView.intro");
	    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
	    gridData.horizontalSpan = 2;
	    label.setLayoutData(gridData);
	    
	    final BufferedTruncatedLabel trackerUrlValue = new BufferedTruncatedLabel(panel, SWT.LEFT,70);        
		
		  trackerUrlValue.setForeground(Colors.blue);
//		  trackerUrlValue.setCursor(Cursors.handCursor);
		  Messages.setLanguageText(trackerUrlValue.getWidget(), "OnoView.label.aqualab.tooltip", true);
	    Messages.setLanguageText(trackerUrlValue.getWidget(), "OnoView.label.aqualab");

		  
	    trackerUrlValue.addMouseListener(new MouseAdapter() {
				public void mouseDown(MouseEvent event) {
					if (event.button == 1) {
						String url = "http://aqualab.cs.northwestern.edu";
						if (url.startsWith("http://") || url.startsWith("https://")) {
							int pos = -1;
							if ((pos = url.indexOf("/announce")) != -1) {
								url = url.substring(0, pos + 1);
							}
							Utils.launch(url);
						}
					}
				}
			});
	    
	    final BufferedTruncatedLabel newsLabel = new BufferedTruncatedLabel(panel, SWT.LEFT,70);        
		
	    newsLabel.setForeground(Colors.blue);
//	    newsLabel.setCursor(Cursors.handCursor);
		  Messages.setLanguageText(newsLabel.getWidget(), "OnoView.label.news.tooltip", true);
	  Messages.setLanguageText(newsLabel.getWidget(), "OnoView.label.news");

		  
	  newsLabel.addMouseListener(new MouseAdapter() {
				public void mouseDown(MouseEvent event) {
					if (event.button == 1) {
						String url = "http://aqualab.cs.northwestern.edu/projects/NEWS.html";
						if (url.startsWith("http://") || url.startsWith("https://")) {
							int pos = -1;
							if ((pos = url.indexOf("/announce")) != -1) {
								url = url.substring(0, pos + 1);
							}
							Utils.launch(url);
						}
					}
				}
			});
	    
	    gridData = new GridData(GridData.FILL_HORIZONTAL);
	    gridData.horizontalSpan = 2;
	    trackerUrlValue.setLayoutData(gridData);
	    newsLabel.setLayoutData(gridData);
	    
	    Composite c = new Composite(panel, SWT.NULL);
	    c.setLayout(new GridLayout());
	    c.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
	    SashForm sf = new SashForm(c, SWT.BORDER |SWT.HORIZONTAL);
	    sf.setLayout(new GridLayout());
	    sf.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
	    
		  Composite temp = new Composite(sf, SWT.NULL);
		  temp.setLayout(new GridLayout());
		  temp.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
//			Messages.setLanguageText(temp,"OnoView.edge.title");


		    
		    final Label edgemaplabel = new Label(temp, SWT.WRAP);
		    Messages.setLanguageText(edgemaplabel,"EdgeMappings.intro");

		    
		  edgeMapTable = new Table(temp, SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
		  String[] headers = { 
				  "OnoView.edge.custName",
				  "OnoView.edge.ip",
				  "OnoView.edge.rtt",
				  "OnoView.edge.ratio",
		    		};
		    int[] sizes = { 100, 100, 100, 100};
		    int[] aligns = { SWT.LEFT, SWT.LEFT, SWT.LEFT, SWT.LEFT};
		    for (int i = 0; i < headers.length; i++) {
		      TableColumn tc = new TableColumn(edgeMapTable, aligns[i]);
		      tc.setText(headers[i]);
		      tc.setWidth(sizes[i]);
		      Messages.setLanguageText(tc, headers[i]); //$NON-NLS-1$
		    }
		    edgeMapTable.setHeaderVisible(true);
		    gridData = new GridData(GridData.FILL_BOTH);
		    gridData.heightHint = edgeMapTable.getHeaderHeight() * 6;
//				gridData.widthHint = 200;
		    edgeMapTable.setLayoutData(gridData);

		    temp = new Composite(sf, SWT.NULL);
			  temp.setLayout(new GridLayout());
			  temp.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
		    Label label2 = new Label(temp, SWT.WRAP);
		    Messages.setLanguageText(label2,"NearbyPeers.intro");
		     nearbyPeersTable = new Table(temp, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
		    String[] headers2 = { 
//		    	    new NPIpItem(),
//		    	    new PortItem(),
//		    	    new CosineSimilarityItem(),
//		    	    new CDNNameItem(),
//		    	    new DownloadSeenFromItem(), 
//		    	    new PctDownloadImprovementItem(),
//		    	    new PctUploadImprovementItem()
		    	    "NearbyPeers.column.ip",
		    	    "NearbyPeers.column.port",
		    	    "NearbyPeers.column.cossim",
		    	    "NearbyPeers.column.cdnname",
		    	    "NearbyPeers.column.dl",
		    	    "NearbyPeers.column.downloadImprovement",
		    	    "NearbyPeers.column.uploadImprovement",

		    		};
		    int[] sizes2 = { 100, 100, 100, 100, 100, 100, 100, 100};
		    int[] aligns2 = { SWT.LEFT, SWT.CENTER, SWT.CENTER, SWT.LEFT, 
		    		SWT.LEFT, SWT.LEFT, SWT.LEFT, SWT.LEFT, SWT.LEFT};
		    for (int i = 0; i < headers2.length; i++) {
		      TableColumn tc = new TableColumn(nearbyPeersTable, aligns2[i]);
		      tc.setText(headers2[i]);
		      tc.setWidth(sizes2[i]);
		      Messages.setLanguageText(tc, headers2[i]); //$NON-NLS-1$
		    }
    
    
		    nearbyPeersTable.setHeaderVisible(true);
		    gridData = new GridData(GridData.FILL_BOTH);
		    gridData.heightHint = nearbyPeersTable.getHeaderHeight() * 6;
//				gridData.widthHint = 200;
		    nearbyPeersTable.setLayoutData(gridData);

 



	
	

  }
  

  public String getFullTitle() {
    return MessageText.getString("OnoView.title.full");
  }
  
  public Composite getComposite() {
    return panel;
  }

  
  private void refreshActivity() {

  }
  
  public EdgeData[] getEdgeDataInArray() {
	  int i = 0;
	  ArrayList<EdgeData> tempEdgeData = new ArrayList<EdgeData>();
	  Map<String, EdgeServerRatio> edgeRatios = OnoPeerManager.getInstance().getMyRatios();
	  if (edgeRatios==null || edgeRatios.size()==0) return null;
	  Map<String, Double> edgePings = Digger.getInstance().getEdgePings();
	  for (Entry<String, EdgeServerRatio> ent : edgeRatios.entrySet()){
		  for ( Entry<String, Double> ratios : ent.getValue().getEntries()){
			  if (ratios.getValue()<0.001) continue;
			  double rtt = getMinPing(ratios.getKey(), edgePings);			  
			  tempEdgeData.add(new EdgeData(ent.getKey(), ratios.getKey(), rtt, ratios.getValue()*100));
			  
		  }
	  }

		return tempEdgeData.toArray(new EdgeData[]{});
	
}

  private double getMinPing(String key, Map<String, Double> edgePings) {
		double rtt = Double.MAX_VALUE;
		for (Entry<String, Double> ent : edgePings.entrySet()){
			if (key.equals(ent.getKey()) || key.equals(Util.getClassCSubnet(ent.getKey()))){
				if (rtt > ent.getValue()) rtt = ent.getValue();
			}
		}
		if (rtt == Double.MAX_VALUE) return -1;
		else return rtt;
	}
  
  GenericPeer[] getAllDataInArray(){
		ArrayList<GenericPeer> activities = new ArrayList<GenericPeer>();
		activities.addAll(CDNClusterFinder.getInstance().getAllPreferredPeers());
		activities.addAll(BRPPeerManager.getInstance().getAllPreferredPeers());
		  if (activities == null || activities.size()==0) return activities.toArray(new GenericPeer[]{});
		  synchronized (activities){
			  HashSet<GenericPeer> toRemove = new HashSet<GenericPeer>();
			  for (GenericPeer p : activities){
				  if (p.isOnoPeer()) {
					  if (((OnoPeer)p).getMaxCosSim()==null && 
							  !((OnoPeer)p).isCIDRMatch()) 
						  toRemove.add(p);
				  } else {
					  assert(p.isBRPPeer()); //remove this if a third peer type is added
					  if (BRPPeerManager.getInstance().getMaxCosineSimilarity(((BRPPeer) p)) >
					  		BRPPeerManager.COS_SIM_THRESHOLD) {
						  toRemove.add(p);
					  }
				  }
			  }
			  activities.removeAll(toRemove);
			  return activities.toArray(new GenericPeer[]{});
		  }
		  
		  
	  }
  
  public void periodicUpdate() {


	  refresh();
  }
  
  

public String getData() {
    return "DHTView.title.full";
  }

	public boolean eventOccurred(UISWTViewEvent event) {
		switch (event.getType()) {
		case UISWTViewEvent.TYPE_CREATE:
	        if (isCreated){
	        	noChange = false;
	        	refresh();
	          return false;
	        }
	        noChange = false;
	        isCreated  = true;
	        break;
	        
		 case UISWTViewEvent.TYPE_DESTROY:
		        delete();
		        isCreated = false;
		        break;
		        
		        
		 case UISWTViewEvent.TYPE_INITIALIZE:
		        initialize((Composite)event.getData());
		        break;

	      case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
	        //updateLanguage();
	        break;

	      case UISWTViewEvent.TYPE_REFRESH:
	        refresh();
	        break;
	    }

	    return true;
	}


	private void initializeUI(Composite composite) {
		System.out.println("Initializing UI!");
		
	}

	public void delete() {
		if ( timer != null)
			timer.destroy();
		
	    Utils.disposeComposite(panel);
	    
	}
    
	
	public void refresh() {
		if (printLogs) log.log("In refresh()...");
		
		if (nearbyPeersTable != null && !nearbyPeersTable.isDisposed() )
		{
	  
	    
			if (printLogs) log.log("Getting nearby peers...");
	    try {
	    	GenericPeer myPeers[] = getAllDataInArray();
	    	if (printLogs) log.log("Found "+myPeers.length+" peers");
	    	nearbyPeersTable.removeAll();
	    
	    	nearbyPeersTable.setItemCount(myPeers.length);
	   
	    
	    noChange = true;
	      
	    int count = 0;
	    TableItem[] items = nearbyPeersTable.getItems();
	    for (int i = 0; i < items.length; i++) {
	    	if (items[i]==null) items[i] = new TableItem(nearbyPeersTable, 7);
	    	items[i].setText(0, myPeers[count].ip);
    		items[i].setText(1,myPeers[count].port+"");
    		items[i].setText(2,myPeers[count].isOnoPeer()?(((OnoPeer)myPeers[count]).isCIDRMatch()?"IP Match":((OnoPeer)myPeers[count]).printCosSim(true)+""):"BRP");
    		items[i].setText(3,myPeers[count].isOnoPeer()?((OnoPeer)myPeers[count]).isCIDRMatch()?"Manual Selection":((OnoPeer)myPeers[count]).printCustomers(true):"BRP"+"");
    		items[i].setText(4,myPeers[count].isOnoPeer()?((OnoPeer)myPeers[count]).getKeyNames():"BRP");
    		items[i].setText(5,myPeers[count].getRelativeDownloadPerformance());
    		items[i].setText(6,myPeers[count].getRelativeUploadPerformance());

	    	
	    	count++;


	    }
	    } catch (Exception e) {
	    	if (printLogs) e.printStackTrace();   
	    	//return; // access violation, try again later
		    }
		}
		
		if (edgeMapTable != null && !edgeMapTable.isDisposed() )
		{
	  
			if (printLogs)  log.log("Getting edges...");
	    
	    try {
	    	EdgeData myData[] = getEdgeDataInArray();
	    	if (myData!=null){
	    	if (printLogs) log.log("Found "+myData.length+" edges");
	    	edgeMapTable.removeAll();
	    
	    	edgeMapTable.setItemCount(myData.length);
	   
	    
	    noChange = true;
	      
	    int count = 0;
	    TableItem[] items = edgeMapTable.getItems();
	    for (int i = 0; i < items.length; i++) {
	    	if (items[i]==null) items[i] = new TableItem(edgeMapTable, 4);
	    	items[i].setText(0, myData[count].custName);
    		items[i].setText(1,myData[count].ip);
    		items[i].setText(2,myData[count].rtt+"");
    		items[i].setText(3,myData[count].ratio+"");
    		
	    	
	    	count++;


	    }
	    	}
	    } catch (Exception e) {
	    	if (printLogs) e.printStackTrace();      
	    	return; // access violation, try again later
		    }
		}
	
	  
	
	  }
 
	  private void resizeTable() {
		  int iNewWidth = alertTable.getClientArea().width - 
	                    alertTable.getColumn(1).getWidth() - 
	                    alertTable.getColumn(2).getWidth() -
	                    alertTable.getColumn(3).getWidth() -
	                    alertTable.getColumn(4).getWidth();
	    if (iNewWidth > 100)
	      alertTable.getColumn(0).setWidth(iNewWidth);    
	    

	  }

	
	
}


