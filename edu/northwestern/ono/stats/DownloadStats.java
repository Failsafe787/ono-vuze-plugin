/**
 * Ono Project
 *
 * File:         DownloadStats.java
 * RCS:          $Id: DownloadStats.java,v 1.32 2010/03/29 16:48:04 drc915 Exp $
 * Description:  DownloadStats class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Apr 10, 2007 at 1:39:15 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.stats
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
package edu.northwestern.ono.stats;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.download.DownloadTrackerListener;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.OnoConfiguration;
import edu.northwestern.ono.position.OnoPeerManager;
import edu.northwestern.ono.timer.ITimerEvent;
import edu.northwestern.ono.timer.ITimerEventPerformer;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The DownloadStats class manages per-download stats
 */
public class DownloadStats  implements ITimerEventPerformer, DownloadTrackerListener {

	public static final int MAX_DSD_ENTRIES = 100;
	private static final long ONO_PEER_EXPIRE = 1*60*60*1000; // one hour
	public static long UPDATE_INTERVAL = 30 * 1000;
	public static long STATIC_FLUSH_INTERVAL = 30*60*1000;
	long lastStaticFlushTime = -1;
	private static DownloadStats self;
	private int selfId = -1;
	protected HashMap<Download, DownloadScrapeResult> scrapeResults = 
		new HashMap<Download, DownloadScrapeResult>();
	
	private HashMap<String, DownloadStatsSummary> dlStats = 
		new HashMap<String, DownloadStatsSummary>();
	private int originalMaxPings;
	
	
	public synchronized static DownloadStats getInstance(){
		if (self==null){
			self = new DownloadStats();
			self.originalMaxPings = OnoConfiguration.getInstance().getMaxPings();
		}
		return self;
	}

	public static class DownloadStatsSummary{

		private Download download;
		private long downloadingTime;
		private long seedingTime;
		private int maxDownloadRateLimit;
		private int maxUploadLimit;
		private ArrayList<DownloadStatsDynamic> dynamicStats;
		private long totalUploaded;
		private long totalDownloaded;
		private long size=0;
		private int downloadId;
		private int priority;
		private long pieceSize;
		private DownloadListener dlist;
		private HashMap<String, Long> onoCompatiblePeers = new HashMap<String, Long>();

		public DownloadStatsSummary(Download d) {
			this.download = d;
			if (d.getTorrent()!=null)
				this.pieceSize = d.getTorrent().getPieceSize();
			else this.pieceSize = -1;
			downloadId =getId(d);
			dynamicStats = new ArrayList<DownloadStatsDynamic>();
			dlist= new DownloadListener(){

				public void positionChanged(Download download, int oldPosition, int newPosition) {
					
					
				}

				public void stateChanged(Download download, int old_state, int new_state) {
					if ((new_state == Download.ST_DOWNLOADING) ||
			                (new_state == Download.ST_SEEDING)) {
			            // taken care of by periodic handler
			        } else if ((new_state == Download.ST_QUEUED) ||
			                (new_state == Download.ST_STOPPED)) {
			            flush();
			            stop();
			        }
					
				}};
			d.addListener(dlist);
		}

		private int getId(Download d) {
			MessageDigest md;
			try {
				md = MessageDigest.getInstance ("SHA");
			
			
		        md.update(d.getName().getBytes());
		        md.update(Util.convertLong(d.getCreationTime()));
		        byte hash[] = md.digest();
		        String s = new String(hash);
		        return s.hashCode();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
			return (int)(d.getName().hashCode() + d.getCreationTime());
		}
		
		protected void summarize(){
			
			if (MainGeneric.getStatsEnabled()){
				MainGeneric.getReporter().insertDownloadStatSummary(
					DownloadStats.getInstance().getSelfId(), 
					downloadId,
					Util.currentGMTTime(), 
					downloadingTime, seedingTime, maxDownloadRateLimit, maxUploadLimit, 
					totalUploaded, totalDownloaded, size, pieceSize);
			}
		}

		protected void stop() {
			update();
			flush();

			
			DownloadStats.getInstance().removeDownload(download);
			download.removeListener(dlist);
			dynamicStats = null;
			download = null;
			
		}

		protected void flush() {
			synchronized (dynamicStats){
				LinkedList<DownloadStatsDynamic> toRemove = new 
				LinkedList<DownloadStatsDynamic>();
                // TODO synchronize
				for (DownloadStatsDynamic dsd: dynamicStats){
					if (dsd.flush()){
						toRemove.add(dsd);
					}
				}
				// just clearing this now because duplicate data is 
				// not helping
				dynamicStats.clear();
			}
			MainGeneric.getReporter().disconnect();
			
		}

		public void update() {
			if (download.getState()== Download.ST_DOWNLOADING ||
			     download.getState() == Download.ST_SEEDING){
				downloadingTime = download.getStats().getSecondsDownloading();
				seedingTime = download.getStats().getSecondsOnlySeeding();
				maxDownloadRateLimit = download.getMaximumDownloadKBPerSecond();
				maxUploadLimit = download.getUploadRateLimitBytesPerSecond();
				totalUploaded = download.getStats().getUploaded();
				if (download.getStats().getCompleted()>900)
					totalDownloaded = roundSize(download.getStats().getDownloaded());
				else totalDownloaded = download.getStats().getDownloaded();
				if (download.getTorrent()!=null && size==0) 
					size = roundSize(download.getTorrent().getSize());
				priority = download.getIndex();
				
				DownloadStatsDynamic dsd = new DownloadStatsDynamic();
				dsd.update(this);
				addEntry(dsd);
			}			
		}


		/**
		 * Adds an entry to the cache and ensures that the 
		 * cache is not taking up too much memory.
		 * @param dsd the entry to add
		 */
		private void addEntry(DownloadStatsDynamic dsd) {
			if (dynamicStats==null) return;
			synchronized(dynamicStats){
			if (dynamicStats.size()>=MAX_DSD_ENTRIES){
				flush();
			}
			
			if (dynamicStats.size()>=MAX_DSD_ENTRIES)			
				dynamicStats.remove(0);
			} 
			dynamicStats.add(dsd);
			
		}

		
		
	}
	

	public static class DownloadStatsDynamic{

		private DownloadStatsSummary dss;
		private long timeRecorded;
		private int numPeers;
		private float availability;
		private int completed;
		private int discarded;
		private int downloadRate;
		private int shareRatio;
		private int uploadRate;
		private int numKnownNonSeeds;
		private int numKnownSeeds;
		private int totalNumKnownNonSeeds=-1;
		private int totalNumKnownSeeds=-1;
		private int state;
		private int priority;
		private long downloaded;
		private long uploaded;
		private int position;
		private int numOnoPeers=-1;
		private long hashFails = 0;
		
		public void update(DownloadStatsSummary dss) {
			this.dss = dss;
			Download download = dss.download;
			timeRecorded = Util.currentGMTTime();
			availability = download.getStats().getAvailability();
			completed = download.getStats().getCompleted();
			discarded = (int) download.getStats().getDiscarded();
			hashFails = download.getStats().getHashFails();
			downloadRate = (int) download.getStats().getDownloadAverage();
			shareRatio = download.getStats().getShareRatio();
			uploadRate = (int) download.getStats().getUploadAverage();
			numKnownNonSeeds = download.getLastAnnounceResult().getNonSeedCount();
			numKnownSeeds = download.getLastAnnounceResult().getSeedCount();
			totalNumKnownNonSeeds = download.getLastScrapeResult().getNonSeedCount();
			totalNumKnownSeeds = download.getLastScrapeResult().getSeedCount();
			state = download.getState();
			priority = download.getIndex();
			position = download.getPosition();
			if (download.getStats().getCompleted()>900)
				downloaded = roundSize(download.getStats().getDownloaded());
			else downloaded = download.getStats().getDownloaded();
			// only hide precise upload value if it is close to the 
			// size of the file (prevents reverse engineering to find 
			// file size, probably can be even less strict but I'm erring 
			// on the side of privacy for users)
			if (Math.abs(shareRatio-1000) <100)
				uploaded = roundSize(download.getStats().getUploaded());
			else uploaded = download.getStats().getUploaded();
			PeerManager pm = download.getPeerManager();			
			if (pm!=null){
				numPeers = pm.getPeers().length;
				numOnoPeers = 0;
				for (Peer p : pm.getPeers()){
					if (OnoPeerManager.getInstance().getOnoPeer(p.getIp())!=null && 
							OnoPeerManager.getInstance().getOnoPeer(p.getIp()).isSameCluster()){
						numOnoPeers++;
					}
				}
			}
			
			

		}
		
		public boolean flush(){
			if (!MainGeneric.getStatsEnabled()) return true;
			
			return MainGeneric.getReporter().insertDownloadStatDynamic(
					DownloadStats.getInstance().getSelfId(), 
					dss.downloadId, timeRecorded, numPeers, availability,
					completed, discarded, downloadRate, uploadRate, shareRatio, 
					numKnownNonSeeds, numKnownSeeds, state, priority,
					downloaded, uploaded, position, numOnoPeers, totalNumKnownNonSeeds, 
					totalNumKnownSeeds, hashFails);
		}
	}
	
	/**
	 * Perform periodic data-collections
	 */
	public void perform(ITimerEvent event) {

		
		
		DownloadManager dm = MainGeneric.getPluginInterface().getDownloadManager();
		
		Download dls[] = dm.getDownloads();
		int maxOnoCompatiblePeers = 0;
		for (Download d : dls){		
			getDownloadStats(d).update();
			int count = getDownloadStats(d).onoCompatiblePeers.size();
			if (count>maxOnoCompatiblePeers){
				maxOnoCompatiblePeers = count;
			}

		}
		int maxPings = originalMaxPings;
		if (maxPings-maxOnoCompatiblePeers<=1) OnoConfiguration.getInstance().setMaxPings(1);
		else OnoConfiguration.getInstance().setMaxPings(maxPings-maxOnoCompatiblePeers);
		
		if (Util.currentGMTTime()-lastStaticFlushTime >= STATIC_FLUSH_INTERVAL){
			for (Download d : dls){	
				if (d!=null && getDownloadStats(d)!=null){
					getDownloadStats(d).update();
					getDownloadStats(d).summarize();
					// check on expiring Ono peers per download
					HashSet<String> toRemove = new HashSet<String>();
					for (Entry<String, Long> ent: 
						getDownloadStats(d).onoCompatiblePeers.entrySet()){
						if (ent.getValue()+ONO_PEER_EXPIRE>System.currentTimeMillis()){
							toRemove.add(ent.getKey());
						}						
					}
					for (String key : toRemove) getDownloadStats(d).onoCompatiblePeers.remove(key);
				}

			}
			MainGeneric.getReporter().flush();
			lastStaticFlushTime = Util.currentGMTTime();
		}
		
		

	}
	
	/**
	 * Remove the download from the cache of download objects
	 * @param download
	 */
	public void removeDownload(Download download) {
		synchronized (dlStats){
			dlStats.remove(download.getName());
		}
		
	}

	/**
	 * Get the stats object associated with the specified Download object.
	 * @param d the download object
	 * @return
	 */
	private DownloadStatsSummary getDownloadStats(Download d) {
		DownloadStatsSummary stat = null;
		if (d.getName()!=null) stat = dlStats.get(d.getName());		
		if (stat == null){
			stat = new DownloadStatsSummary(d);
			synchronized (dlStats){
				if (d.getName()!=null)
					dlStats.put(d.getName(), stat);
			}
		}
		return stat;
	}
	
	/**
	 * Returns the database identifier for this node.
	 * @return
	 */
	public int getSelfId(){
		if (selfId == -1){
			selfId  = Statistics.getInstance().getSelfId();
		}
		return selfId;
	}

	/**
	 * Returns the periodicity for calling this object's perform method
	 * @return
	 */
	public static long getInterval() {
		return UPDATE_INTERVAL;
	}

	public void stop() {
		if (!MainGeneric.isAzureus()) return;
		DownloadManager dm = MainGeneric.getPluginInterface().getDownloadManager();
		
		Download dls[] = dm.getDownloads();
		
		for (Download d : dls){	
			if (d!=null && getDownloadStats(d)!=null){
				getDownloadStats(d).summarize();
			}

		}
		MainGeneric.getReporter().flush();
		
		int i = 0;
		for (Download d : dls){	
			if (d!=null && getDownloadStats(d)!=null){
				getDownloadStats(d).stop();
			}
//			if (i==0) {
//				try {
//				Thread.sleep(28*1000);
//				} catch (InterruptedException e){
//					e.printStackTrace();
//				}
//			}
//			i++;
		}
		dlStats = null;
		self = null;
		
	}

	public void flush() {
		DownloadManager dm = MainGeneric.getPluginInterface().getDownloadManager();
		if (dm==null) return;
		Download dls[] = dm.getDownloads();
		for (Download d : dls){		
			getDownloadStats(d).flush();
			
		}
		
		
	}

	public static void setRecordingInterval(int interval) {
		UPDATE_INTERVAL = interval;
		
	}
	

	/**
	 * Rounds the size of the download.
	 * 
	 * @param dlSize
	 * @return
	 */
	public static long roundSize(long dlSize) {
		if (dlSize < 10000) return dlSize;
		else if (dlSize < 100000) return 1000* (dlSize/1000);
		else if (dlSize < 1000000) return 10000* (dlSize/10000); 
		else if (dlSize < 10000000) return 100000* (dlSize/100000); 
		else if (dlSize < 100000000) return 1000000* (dlSize/1000000);
		else return 10000000* (dlSize/10000000); 
	}

	public void announceResult(DownloadAnnounceResult result) {
		
	}

	public void scrapeResult(DownloadScrapeResult result) {
		scrapeResults.put(result.getDownload(), result);
		
	}

	public void addOnoCompatiblePeer(Peer peer, Download download) {
		getDownloadStats(download).onoCompatiblePeers.put(peer.getIp(), System.currentTimeMillis());
		
	}

}
