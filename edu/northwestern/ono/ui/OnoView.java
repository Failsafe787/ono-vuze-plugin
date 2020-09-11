/**
 * Ono Project
 *
 * File:         OnoView.java
 * RCS:          $Id: OnoView.java,v 1.39 2011/07/06 15:45:53 mas939 Exp $
 * Description:  OnoView class (see below)
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
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedTruncatedLabel;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
//import org.gudy.azureus2.ui.swt.mainwindow.Cursors;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

import edu.northwestern.ono.brp.BRPPeerManager;
import edu.northwestern.ono.brp.BRPPeerManager.BRPPeer;
import edu.northwestern.ono.dns.Digger;
import edu.northwestern.ono.position.CDNClusterFinder;
import edu.northwestern.ono.position.GenericPeer;
import edu.northwestern.ono.position.NeighborhoodListener;
import edu.northwestern.ono.position.OnoPeerManager;
import edu.northwestern.ono.position.OnoPeerManager.OnoPeer;
import edu.northwestern.ono.stats.EdgeServerRatio;
import edu.northwestern.ono.ui.items.CDNNameItem;
import edu.northwestern.ono.ui.items.CosineSimilarityItem;
import edu.northwestern.ono.ui.items.DownloadSeenFromItem;
import edu.northwestern.ono.ui.items.NPIpItem;
import edu.northwestern.ono.ui.items.PctDownloadImprovementItem;
import edu.northwestern.ono.ui.items.PctUploadImprovementItem;
import edu.northwestern.ono.ui.items.PortItem;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The OnoView class ...
 */
public class OnoView extends AbstractIView implements UISWTViewEventListener {
  

private class UpdateThread extends Thread {
    boolean bContinue;
    OnoView view;
    
		public UpdateThread(OnoView view) {
			super("Ono Update Thread");
			this.view = view;
			
		}
    
    public void run() {
      try {
        bContinue = true;
        while(bContinue) {   
          
        	view.refreshActivity();

          
          Thread.sleep(100);
        }
      } catch(Exception e) {
      	Debug.printStackTrace( e );  
      }
    }
    
    public void stopIt() {
      bContinue = false;
    }
  }

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
  
  Composite panel;
  
  
  boolean activityChanged;
  NeighborhoodListener neighborhoodListener;
  Table activityTable;
  Table edgeTable;
  Set<GenericPeer> activities;
 

  private boolean isCreated = false;

  UpdateThread updateThread;
  private GenericPeer[] activityArray;



	private Map<String, String> allIps;




protected boolean ipsChanged;


private EdgeMapTable edgeMapTable;



private PluginInterface pi;


private UTTimer timer;



private NearbyPeersTable nearbyPeersTable;

private Table nearbyPeersTableSwt;

private Table edgeMapTableSwt;


private boolean showEdge = true;

  public OnoView( PluginInterface pi ) {
   this.pi = pi;
    init();
  }
  
  private void init() {
	  activities = new HashSet<GenericPeer>();
	  activities.addAll(CDNClusterFinder.getInstance().getAllPreferredPeers());
	  activities.addAll(BRPPeerManager.getInstance().getAllPreferredPeers());
	  
	  allIps = new HashMap<String, String>();
	  
	  neighborhoodListener = new NeighborhoodListener(){
		  
		


		public void foundNewNeighborhoodPeer(GenericPeer p) {
			
			synchronized (activities){
				activities.remove(p);
				activities.add(p);
			}
			activityChanged = true;
			//refreshActivity();
			
		}

		public void foundNewIpAddress(Map<String, String> allIpsSeen) {
	
			synchronized(allIps){
				allIps.putAll(allIpsSeen);
			}
			ipsChanged = true;
			
		}


		  
	  };

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
			periodicUpdate();
			
		}});    
    
    Label label = new Label(panel, SWT.WRAP);
    Messages.setLanguageText(label,"OnoView.intro");
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
    
    final BufferedTruncatedLabel trackerUrlValue = new BufferedTruncatedLabel(panel, SWT.LEFT,70);        
	
	  trackerUrlValue.setForeground(Colors.blue);
	  //trackerUrlValue.setCursor(Cursors.handCursor);
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
    //newsLabel.setCursor(Cursors.handCursor);
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
    
    initialiseActivityGroup(sf);    
    if (showEdge ) initializeEdgeGroup(sf);
	    

    CDNClusterFinder.getInstance().addListener(neighborhoodListener);
    BRPPeerManager.getInstance().addListener(neighborhoodListener);

  }
  
  
  
  private void initialiseActivityGroup(SashForm c) {
	  Composite temp = new Composite(c, SWT.NULL);
	  temp.setLayout(new GridLayout());
	  temp.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));

//    Messages.setLanguageText(temp,"OnoView.activity.title");


    Label label = new Label(temp, SWT.WRAP);
    Messages.setLanguageText(label,"NearbyPeers.intro");
    
   /* nearbyPeersTableSwt = new Table(temp, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
    String[] headers = { 
//    	    new NPIpItem(),
//    	    new PortItem(),
//    	    new CosineSimilarityItem(),
//    	    new CDNNameItem(),
//    	    new DownloadSeenFromItem(), 
//    	    new PctDownloadImprovementItem(),
//    	    new PctUploadImprovementItem()
    	    "NearbyPeers.column.ip",
    	    "NearbyPeers.column.port",
    	    "NearbyPeers.column.cossim",
    	    "NearbyPeers.column.cdnname",
    	    "NearbyPeers.column.dl",
    	    "NearbyPeers.column.downloadImprovement",
    	    "NearbyPeers.column.uploadImprovement",

    		};
    int[] sizes = { 100, 100, 100, 100, 100, 100, 100, 100};
    int[] aligns = { SWT.LEFT, SWT.CENTER, SWT.CENTER, SWT.LEFT, 
    		SWT.LEFT, SWT.LEFT, SWT.LEFT, SWT.LEFT, SWT.LEFT};
    for (int i = 0; i < headers.length; i++) {
      TableColumn tc = new TableColumn(nearbyPeersTableSwt, aligns[i]);
      tc.setText(headers[i]);
      tc.setWidth(sizes[i]);
      Messages.setLanguageText(tc, headers[i]); //$NON-NLS-1$
    }
    
    nearbyPeersTableSwt.setHeaderVisible(true);
    GridData gridData = new GridData(GridData.FILL_BOTH);
    gridData.heightHint = nearbyPeersTableSwt.getHeaderHeight() * 6;
//		gridData.widthHint = 200;
    nearbyPeersTableSwt.setLayoutData(gridData);
    
    nearbyPeersTableSwt.setItemCount(alerts.size()+alarms.size());
    nearbyPeersTableSwt.clearAll();
	// bug 69398 on Windows
    nearbyPeersTableSwt.redraw();
    
    nearbyPeersTableSwt.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
        resizeTable();
			}
		});

		temp.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
        resizeTable();
			}});*/
		
    
    nearbyPeersTable = new NearbyPeersTable(this);
    nearbyPeersTable.dataSourceChanged(this);
		
		
    nearbyPeersTable.initialize(temp);
    nearbyPeersTable.getComposite().setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));

  }
  
  private void initializeEdgeGroup(SashForm c) {

	  Composite temp = new Composite(c, SWT.NULL);
	  temp.setLayout(new GridLayout());
	  temp.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
//		Messages.setLanguageText(temp,"OnoView.edge.title");


	    
	    final Label label = new Label(temp, SWT.WRAP);
	    Messages.setLanguageText(label,"EdgeMappings.intro");

	    
	  edgeMapTable = new EdgeMapTable(this);
	  edgeMapTable.dataSourceChanged(this);
		
		
    	edgeMapTable.initialize(temp);
    	edgeMapTable.getComposite().setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
    	

	  }

  
  public String getFullTitle() {
    return MessageText.getString("OnoView.title.full");
  }
  
  public Composite getComposite() {
    return panel;
  }
  
  public void refresh() {    

  }  

  
  private void refreshActivity() {
	  try {
//	      if (activityTable==null) return;
//		  if(true || activities.size()==0) {
//      activityChanged = false;      
//      activityArray = getAllDataInArray();
//      if (activityArray == null) return;
//      activityTable.setItemCount(activityArray.length);
//      activityTable.clearAll();
//      //Dunno if still needed?
//      activityTable.redraw();   
		  if (nearbyPeersTable!=null){
			  nearbyPeersTable.dataSourceChanged(this);
		 if (edgeMapTable !=null){
			 edgeMapTable.dataSourceChanged(null);
		 }
		  
      //panel.pack();
    }    
	  } catch (Exception e){
		  e.printStackTrace();
	  }
  }

private void refreshEdge() {
	  try {
//	      if (edgeTable==null) return;
//		
//      activityChanged = false;   
//      
//      edgeArray = getEdgeDataInArray();
//      if (edgeArray == null) return;
//      edgeTable.setItemCount(edgeArray.length);
//      edgeTable.clearAll();
//      //Dunno if still needed? 
//      //panel.pack();
//      if (gEdge!=null){
//    	 // gEdge.pack();
//    	//  panel.redraw();
//      }
		  if (showEdge ) if (edgeMapTable!=null) edgeMapTable.dataSourceChanged(null);
     
	  } catch (Exception e){
		  e.printStackTrace();
	  }
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
	activities.clear();
	activities.addAll(CDNClusterFinder.getInstance().getAllPreferredPeers());
	activities.addAll(BRPPeerManager.getInstance().getAllPreferredPeers());
	  if (activities == null || activities.size()==0) return activityArray;
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
    //getPeerIpInformation();
	  refreshEdge();
	  refreshActivity();
  }
  
  

public String getData() {
    return "DHTView.title.full";
  }

	public boolean eventOccurred(UISWTViewEvent event) {
		switch (event.getType()) {
		case UISWTViewEvent.TYPE_CREATE:
	        if (isCreated)
	          return false;

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
		// TODO Auto-generated method stub
		
	}

	public void delete() {
		timer.destroy();
	    Utils.disposeComposite(panel);
	    CDNClusterFinder.getInstance().removeListener(neighborhoodListener);
	    if (showEdge ) edgeMapTable.delete();
	    nearbyPeersTable.delete();
	    
//	    updateThread.stopIt();
//	    if (dht != null) {
//	      dht.getControl().removeListener(controlListener);
//	    }
//	    outGraph.dispose();
//	    inGraph.dispose();
		
	}
    
}


