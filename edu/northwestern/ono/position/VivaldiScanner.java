/**
 * Ono Project
 *
 * File:         VivaldiScanner.java
 * RCS:          $Id: VivaldiScanner.java,v 1.21 2011/07/06 15:45:53 mas939 Exp $
 * Description:  VivaldiScanner class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Nov 20, 2006 at 1:03:36 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.position
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
package edu.northwestern.ono.position;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.control.DHTControlContact;
import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;
import com.aelitis.azureus.core.dht.netcoords.vivaldi.ver1.VivaldiPosition;
import com.aelitis.azureus.core.dht.netcoords.vivaldi.ver1.impl.HeightCoordinatesImpl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
//import com.aelitis.azureus.vivaldi.ver2.VivaldiV2PositionProvider;

import edu.northwestern.ono.Main;
import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.OnoConfiguration;
import edu.northwestern.ono.dns.Digger;
import edu.northwestern.ono.stats.Statistics;
import edu.northwestern.ono.stats.VivaldiResult;
import edu.northwestern.ono.util.PingManager;
import edu.northwestern.ono.util.Util.PingResponse;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The VivaldiScanner class ...
 */
public class VivaldiScanner implements Runnable {
	
	/** the dht to watch */
	DHT dht;

	/** runs while true */
	private boolean active = true;
	
	/** the Ono digger */
	private Digger digger = null;
	
	/** set of IPs that don't respond to pings */
	private HashSet<String> badSet = new HashSet<String>();
	
	VivaldiPosition ownPosition;
	 HeightCoordinatesImpl ownCoords;
	 String ownIp;
	 final Object pingSync = new Integer(1);
	 
	private void init(){
		try{
			// try and pull in the v2 class
		
		Class.forName( "com.aelitis.azureus.vivaldi.ver2.VivaldiV2PositionProvider" );
		//VivaldiV2PositionProvider.initialise();
		
	}catch( Throwable e ){
		System.err.println("Vivaldi v2 not found!");
	}

		
		try {
			if (digger == null){
				digger = Digger.getInstance();
			}
			
			while (!Main.isDoneInitializing()){
				Thread.sleep(5*1000);
			}
			
		      PluginInterface dht_pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );
		    
		      if ( dht_pi == null ){
		    	   
		    	return;
		      }
		       
		      DHT[] dhts = ((DHTPlugin)dht_pi.getPlugin()).getDHTs();
		    
		      if (dhts.length == 0){
		    	  return;
		      }
		      
		      dht = dhts[dhts.length-1];
		      
		      DHTTransportContact self = dht.getControl().getTransport().getLocalContact();
		      DHTNetworkPosition _ownPosition = self.getNetworkPosition(DHTNetworkPosition.POSITION_TYPE_VIVALDI_V1);

			    if ( _ownPosition == null ){
			    	return;
			    }
			    
			    ownPosition = (VivaldiPosition)_ownPosition;			   
			    ownCoords =
			    	(HeightCoordinatesImpl) ownPosition.getCoordinates();
		    } catch(Exception e) {
		      Debug.printStackTrace( e );
		    }
	}

	public void run() {
		
		while (!MainGeneric.isShuttingDown()){
			long startTime = System.currentTimeMillis();

			if (dht==null){
				init();
				
			}
			else{
				List contacts = dht.getControl().getContacts();				
				
			    Iterator iter = contacts.iterator();
			    while(iter.hasNext()) {
			      final DHTControlContact contact = (DHTControlContact) iter.next();
			      
			      
//			    	  byte type;
//			    	  if (i==0){
//			    		  type = DHTNetworkPosition.POSITION_TYPE_VIVALDI_V1;
//			    	  } else {
//			    		  type = DHTNetworkPosition.POSITION_TYPE_VIVALDI_V2;
//			    	  }
//			    	  final DHTNetworkPosition _position = contact.getTransportContact().getNetworkPosition(type);
			    	  
			    	  

			    	  
			    		  final DHTNetworkPosition _position = contact.getTransportContact().getNetworkPosition(DHTNetworkPosition.POSITION_TYPE_VIVALDI_V1);
			    		  final DHTNetworkPosition _position2 = contact.getTransportContact().getNetworkPosition(DHTNetworkPosition.POSITION_TYPE_VIVALDI_V2);
			    		  if ( _position == null && _position2==null) continue;
			    		  doVivaldiV1Capture(contact, _position, _position2);
			    	  
			    	  
			      
			    }
			}

			long remainingTime = OnoConfiguration.getInstance().getVivaldiFrequencySec()
				*1000 - (System.currentTimeMillis() - startTime);
			
			if (dht!=null){
				DHTTransportContact self = dht.getControl().getTransport().getLocalContact();
			      DHTNetworkPosition _ownPosition = self.getNetworkPosition(DHTNetworkPosition.POSITION_TYPE_VIVALDI_V1);

				    if ( _ownPosition != null ){
				    
				    ownPosition = (VivaldiPosition)_ownPosition;			   
				    ownCoords =
				    	(HeightCoordinatesImpl) ownPosition.getCoordinates();
				    VivaldiResult.current = ownPosition;
			
			}
			DHTNetworkPosition _ownPosition2 = self.getNetworkPosition(DHTNetworkPosition.POSITION_TYPE_VIVALDI_V2);

			    if ( _ownPosition2 != null ){
			   
			    
			    
			    VivaldiResult.currentV2 = _ownPosition2;
			    }
			    if(ownPosition!=null && Digger.getInstance().getIp()!=null){
			
			    	VivaldiResult v = new VivaldiResult(ownPosition, _ownPosition2, ownCoords, 0f, Digger.getInstance().getIp());
					 Statistics.getInstance().addVivaldiResult(v);
		}
			    
			}

			if (remainingTime > 0){
				try {
					Thread.sleep(remainingTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		//pingSync = null;
		/*digger = null;
		badSet = null;
		dht = null;
		ownCoords = null;
		ownPosition = null;
		ownIp = null;*/
	}

//	private void doVivaldiV2Capture(final DHTControlContact contact, final DHTNetworkPosition _position) {
//		
//		if(_position.isValid()) {
//	  		  // if valid, ping and record
//	  		  final String peerIp = contact.getTransportContact().getAddress().getAddress().toString().substring(1);
//	  		  //if (badSet.contains(peerIp)) continue; // stop if not responding to pings...
//
//	  		  OnoPeerManager.getInstance().addPeer(peerIp, 
//	  				  contact.getTransportContact().getAddress().getPort(), 
//	  				  dht, true);
//	  		  OnoPeerManager.getInstance().registerPeerSource(peerIp, OnoPeerManager.SOURCE_VIVALDI);
////	  		  digger.addPeerIpForDigging(peerIp);
//	  		  if (badSet.contains(peerIp)){
//	  			  Main.doAppPing(contact, _position, peerIp);
//	  		  }
//	  		  else{
//
//	  			  PingManager.getInstance().doPing(peerIp, MainGeneric.getPluginInterface(), new PingResponse(){
//
//	  				  public void response(double rtt) {
////	  					  check on validity of ping
//	  					  if (rtt<0){
//	  						  Main.doAppPing(contact, _position, peerIp);
//	  						  badSet.add(peerIp);
//	  					  }			    	
//	  					  else{
//	  						  VivaldiResult v = new VivaldiResult(_position, rtt, peerIp);
//	  						  Statistics.getInstance().addVivaldiResult(v);
//	  					  }
//	  					  synchronized(pingSync){pingSync.notifyAll();}
//
//	  				  }});
//	  			  synchronized(pingSync){
//	  				  try {
//	  					  pingSync.wait();
//	  				  } catch (InterruptedException e) {
//	  					  // TODO Auto-generated catch block
//	  					  e.printStackTrace();
//	  				  }
//	  			  }
//	  		  }
//	  	  }
//		
//	}

	private void doVivaldiV1Capture(final DHTControlContact contact, DHTNetworkPosition _position, 
			final DHTNetworkPosition _position2) {
		final VivaldiPosition position = (VivaldiPosition)_position;
  	  final HeightCoordinatesImpl coord = (HeightCoordinatesImpl) position.getCoordinates();
  	  if (Main.isShuttingDown()) return;
  	  if(coord.isValid()) {
  		  // if valid, ping and record
  		  final String peerIp = contact.getTransportContact().getAddress().getAddress().toString().substring(1);
  		  //if (badSet.contains(peerIp)) continue; // stop if not responding to pings...
  		  
  		  OnoPeerManager.getInstance().addPeer(peerIp, 
  				  contact.getTransportContact().getAddress().getPort(), 
  				  dht, true);
  		  OnoPeerManager.getInstance().registerPeerSource(peerIp, OnoPeerManager.SOURCE_VIVALDI);
//  		  digger.addPeerIpForDigging(peerIp);
  		  if (badSet.contains(peerIp)){
  			  Main.doAppPing(contact, position, _position2, coord, peerIp);
  		  }
  		  else{

  			  if (PingManager.getInstance()==null) return;
  			  PingManager.getInstance().doPing(peerIp, MainGeneric.getPluginInterface(), new PingResponse(){

  				  public void response(double rtt) {
  					  if (!MainGeneric.isShuttingDown()){
//  					  check on validity of ping
//  					  if (rtt<0){
//  						  Main.doAppPing(contact, position, _position2, coord, peerIp);
//  						  badSet.add(peerIp);
//  					  }			    	
//  					  else{
  						  VivaldiResult v = new VivaldiResult(position, _position2, coord, rtt, peerIp);
  						  Statistics.getInstance().addVivaldiResult(v);
//  					  }
  					  }
  					  synchronized(pingSync){pingSync.notifyAll();}

  				  }});
  			  synchronized(pingSync){
  				  try {
  					  if (!MainGeneric.isShuttingDown())
  						  pingSync.wait(30*1000);
  				  } catch (InterruptedException e) {
  					  // TODO Auto-generated catch block
  					  e.printStackTrace();
  				  }
  			  }
  		  }
  	  }
		
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		if (!active) {
			this.active = active;
			synchronized(pingSync){
				pingSync.notifyAll();
			}
		}
		
		synchronized(this){
			this.notifyAll();
		}
	}

}
