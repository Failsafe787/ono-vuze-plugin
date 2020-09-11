/**
 * Ono Project
 *
 * File:         DHTManager.java
 * RCS:          $Id: DHTManager.java,v 1.19 2010/03/29 16:48:04 drc915 Exp $
 * Description:  DHTManager class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 2, 2006 at 4:59:40 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.dht
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
package edu.northwestern.ono.dht.azureus;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map.Entry;

import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.position.OnoPeerManager;
import edu.northwestern.ono.position.OnoPeerManager.OnoPeer;
import edu.northwestern.ono.util.HashMapLifo;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The DHTManager class manages reads writes and updates to the DHT.
 */
public class DHTManager {
	private static final boolean DEBUG = false;
    private static final long READ_TIMEOUT = 60 * 1000;
    
    private static final long OPERATION_TIMEOUT = 3* 60 * 1000;

	private static final int MAX_CONCURRENT_READS = 10;
	private static final int MAX_CONCURRENT_WRITES = 10;

	public static long WRITE_EXPIRE = 60*60*1000;

	public static final long DELETE_EXPIRE = 1*60*1000;

	public static final long READ_EXPIRE = 3*60*1000;

    /** set of DDB keys for which writes are in progress. used to suppress multiple writes to same key */
    private HashMap<String, PendingWrite> writesInProgress;

    /** set of DDB keys for which reads are in progress. used to suppress multiple reads to same key */
    private HashMap<String, PendingRead> readsInProgress;
    
    /** set of DDB keys for which deletes are in progress. used to suppress multiple deletes to same key */
    private HashMap<String, Long> deletesInProgress = new HashMap<String, Long>();
    
    private DistributedDatabaseListener listener;
    private DistributedDatabase ddb;

    /** the next data to be written to the DDB */
    private HashMapLifo<String, PendingWrite> pws = new HashMapLifo<String, PendingWrite>();

    /** the next key to be read from the DDB */
    private HashMapLifo<String, PendingRead> prs = new HashMapLifo<String, PendingRead>();

    private HashMapLifo<String, PendingDelete> pds = new HashMapLifo<String, PendingDelete>();
    
    /** list of reads that will be completed when the queued ones have finished
     * maps ip to pending read object
     */
    private HashMapLifo<String, PendingRead> cachedReads = new HashMapLifo<String, PendingRead>();
    private HashMapLifo<String, PendingWrite> cachedWrites = new HashMapLifo<String, PendingWrite>();
    
    private static DHTManager self;
    
    public DHTManager(DistributedDatabase ddb) {
        this.ddb = ddb;
        writesInProgress = new HashMap<String, PendingWrite>();
        readsInProgress = new HashMap<String, PendingRead>();
        initListener();
        if (self==null){
        	self = this;
        }
    }

    public void initListener() {
        listener = new DistributedDatabaseListener() {
                    public void event(DistributedDatabaseEvent event) {
                        IDHTReadAction readAction;
                        IDHTWriteAction writeAction;
                        PendingRead pr;
                        PendingWrite pw;
                        boolean removed = false;
                        synchronized (self){
                        try {
                            switch (event.getType()) {
                            case DistributedDatabaseEvent.ET_VALUE_READ:
                            	if (readsInProgress.get(event.getKey()
                                                                      .getKey())==null) break;
                                readAction = readsInProgress.get(event.getKey()
                                                                      .getKey()).readAction;

                                byte[] valueRead = (byte[]) event.getValue()
                                                                 .getValue(byte[].class);

                                //                            for (int i = 0; i < valueRead.length; i++) System.out.print((int)valueRead[i]+ " ");
                                //                            System.out.print("\n");
                                if (readAction != null) {
                                    readAction.handleRead(valueRead, event);
                                }

                                break;

                            case DistributedDatabaseEvent.ET_OPERATION_COMPLETE:
                                //System.out.println("Done with operation");
                            	
                            	
                            	pr = readsInProgress.remove(event.getKey()
                                                                         .getKey());
                                // this was a read
                                if (pr != null) {
                                	if (!tryNextDelete()){
                                		if (!tryNextWrite(null)){
                                			tryNextRead();
                                		}
                                	}
                                	readAction = pr.readAction;
                                    readAction.handleComplete(event);
                                    break;
                                } 
                                
                                // try to see if this is a delete
                                removed = (deletesInProgress.remove(event.getKey()
                                        .getKey())!=null);
                                if (removed){                                	
                                	if (!tryNextWrite((String)event.getKey().getKey())){
                                		if (!tryNextRead()){
                                			tryNextDelete();
                                		}
                                	}                                	
                                    break;
                                }
                                
                                // try to see if this is a write
                                if (DEBUG) System.out.println("Before: "+writesInProgress.size());
                                pw = writesInProgress.remove(event.getKey()                                		
                                        .getKey());
                                if (DEBUG) System.out.println("After: "+writesInProgress.size());
                                if (pw!=null){
                                	try {
	                                	long time = Long.parseLong(event.getKey().getDescription().
	                                			substring(event.getKey().getDescription().indexOf('@')+2));
	                                	
	                                	if (time-pw.expire > 1000){
	                                		if (DEBUG) System.out.println("Multiple complete to same write (" +
	                                				event.getKey().getKey()+")!");
	                                		writesInProgress.put((String)event.getKey().getKey(), pw);
	                                		break;
	                                	}
                                	} catch (NumberFormatException e){
                                		writesInProgress.put((String)event.getKey().getKey(), pw);
                                		break;
                                	}
                                	tryNextDelete();
                                	tryNextWrite(null);
//                                	if (!){
//                                		if (!){
//                                			;
//                                		}
//                                	}
                                	pw.write.handleComplete(event);
                                	if (DEBUG) System.out.println("Complete: Write to "+event.getKey().getKey()+ " took " 
                                    		+ ((System.currentTimeMillis()-pw.expire)/1000) 
                                    		+ " seconds");
                                	//WRITE_EXPIRE = (System.currentTimeMillis()-pw.expire)*2;
                                    break;
                                }
                                
//                              
//                                else {
//                                    synchronized (writesInProgress) {
//                                        synchronized (pws) {
//                                            writeAction = 
//
//                                            if (writeAction != null) {
//                                            	synchronized(writesInProgress){
//                                            		addPendingWrite();
//                                            	}
//                                                writeAction.handleComplete(event);
//                                            
//
//	                                            if (prs.get(event.getKey().getKey()) != null) {
//	                                                PendingRead pr = prs.get(event.getKey()
//	                                                                              .getKey());
//	
//	                                                if (pr.description != null) {
//	                                                    doRead(pr.key,
//	                                                        pr.description,
//	                                                        pr.readAction,
//	                                                        pr.option, pr.timeout);
//	                                                }
//	                                            }
//                                            }
//                                        }
//                                    }
//                                    
//                                }
//                                
//                                readsInProgress.remove(event.getKey().getKey());                           
//                                writesInProgress.remove(event.getKey().getKey()); 
//                                deletesInProgress.remove(event.getKey().getKey()); 
                                if (DEBUG) System.err.println("Got to end of complete without finding operation in progress (" +
                                		event.getKey().getKey()+")!");
                                break;

                            case DistributedDatabaseEvent.ET_OPERATION_TIMEOUT:
                            	pr = readsInProgress.remove(event.getKey()
                                        .getKey());
                            	// this was a read
                            	if (pr != null) {
                            		if (!tryNextDelete()){
                            			if (!tryNextWrite(null)){
                            				tryNextRead();
                            			}
                            		}
                            		readAction = pr.readAction;
                            		readAction.handleComplete(event);
                            		break;
                            	} 

                            	// try to see if this is a write
                            	pw = writesInProgress.remove(event.getKey()
                            			.getKey());
                            	if (pw!=null){
                            		long time = Long.parseLong(event.getKey().getDescription().
                                			substring(event.getKey().getDescription().indexOf('@')+2));
                                	if (time-pw.expire > 1000){
                                		if (DEBUG) System.out.println("Multiple complete to same write (" +
                                				event.getKey().getKey()+")!");
                                		writesInProgress.put((String)event.getKey().getKey(), pw);
                                		break;
                                	}
                                	tryNextDelete();
                                	tryNextWrite(null);
//                            		if (!tryNextRead()){
//                            			if (!tryNextDelete()){
//                            				tryNextWrite(null);
//                            			}
//                            		}
                            		pw.write.handleComplete(event);
                            		if (DEBUG) System.out.println("Timeout: Write to "+event.getKey().getKey()+ " took " 
                                    		+ ((System.currentTimeMillis()-pw.expire)/1000) 
                                    		+ " seconds");
                            		break;
                            	}

                            	//try to see if this is a read
                            	removed = (deletesInProgress.remove(event.getKey()
                            			.getKey())!=null);
                            	if (removed){                                	
                            		if (!tryNextWrite((String)event.getKey().getKey())){
                            			if (!tryNextRead()){
                            				tryNextDelete();
                            			}
                            		}                                	
                            		break;
}
                            	/*synchronized(deletesInProgress){
                            		if (deletesInProgress.remove((String)event.getKey().getKey())){
	                            		if (pws.containsKey((String)event.getKey().getKey())){
	                            			writeCachedValue(event);
	                            			break;
	                            		}
                            		}
                            	}
                                //System.out.println("DHT operation failed due to timeout!");
                                readAction = readsInProgress.remove(event.getKey()
                                                                         .getKey()).readAction;

                                if (readAction != null) {
                                    readAction.handleTimeout(event);
                                }

                                synchronized (writesInProgress) {
                                    synchronized (pws) {
                                        writeAction = writesInProgress.remove(event.getKey()
                                                                                   .getKey()).write;

                                        if (writeAction != null) {
                                            writeAction.handleTimeout(event);
                                        }

                                        if (prs.get(event.getKey().getKey()) != null) {
                                            PendingRead pr = prs.get(event.getKey()
                                                                          .getKey());

                                            if (pr.description != null) {
                                                doRead(pr.key, pr.description,
                                                    pr.readAction, pr.option,
                                                    pr.timeout);
                                            }
                                        } else {
                                            writeCachedValue(event);
                                        }
                                    }
                                }
                                
                                if (deletesInProgress.contains((String)event.getKey().getKey())){
                                	synchronized(deletesInProgress){
                                		deletesInProgress.remove((String)event.getKey().getKey());
                                		doDelete(event.getKey());
                                	}
                                }
                                
                                readsInProgress.remove(event.getKey().getKey());                           
                                writesInProgress.remove(event.getKey().getKey()); 
                                deletesInProgress.remove(event.getKey().getKey()); 
*/
                            	if (DEBUG) System.err.println("Got to end of timeout without finding operation in progress (" +
                            			event.getKey().getKey()+")!");
                                break;

                            case DistributedDatabaseEvent.ET_VALUE_WRITTEN:

                                // mark as complete
                               
                                    	pw = writesInProgress.get(event.getKey()
                                        .getKey());
                                        
                                        if (pw != null) {
                                        	writeAction = pw.write;
                                            writeAction.handleWrite(event);
                                            if (DEBUG) System.out.println("Written: Write to "+event.getKey().getKey()+ " took " 
                                            		+ ((System.currentTimeMillis()-pw.expire)/1000) 
                                            		+ " seconds");
                                        }
                                        
                                        //writeCachedValue(event);


                                break;
                                
                            case DistributedDatabaseEvent.ET_VALUE_DELETED :
                            	if (DEBUG) System.out.println(event.getKey().getKey()+" deleted!");
//                            	synchronized(deletesInProgress){
//                            		if (deletesInProgress.remove((String)event.getKey().getKey())){
//	                            		if (pws.containsKey((String)event.getKey().getKey())){
//	                            			writeCachedValue(event);
//	                            			break;
//	                            		}
//                            		}
//                            	}
                            	break;
                            case 7:
                            	break; // this is a new event i'm not handling yet
                            default:
                                System.err.println("DHT event: " +
                                    event.getType());
                            }
                        } catch (DistributedDatabaseException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    }
                };
    }

    protected boolean tryNextDelete() {
		// TODO Auto-generated method stub
		return false;
	}

	protected boolean tryNextWrite(String key) {
//		System.out.println("tryNextWrite: "+writesInProgress.size());
		if (cachedWrites.size()>0 && 
				writesInProgress.size()<MAX_CONCURRENT_WRITES){
			PendingWrite pw = null;
			
			// first, try to see if we can prioritize dht writes for 
			// the local node
			//if (key==null){
				String myIp = MainGeneric.getPublicIpAddress();
				OnoPeer me = OnoPeerManager.getInstance().getOnoPeer(myIp);
				Set<String> myKeys = new LinkedHashSet<String>();
				myKeys.add(myIp);
				Set<String> serverIps = me.getGoodServerIps();
				if (serverIps!=null) myKeys.addAll(me.getGoodServerIps());
				for (String ip : myKeys){
					// write for local node waiting and not in progress
					if (cachedWrites.containsKey(ip) && !writesInProgress.containsKey(ip)){
						pw = cachedWrites.remove(ip);
						try {
							if (DEBUG) System.out.println("Writing: "+pw.k.getKey());
							pws.put((String) pw.k.getKey(), new PendingWrite());	
							if (pds.containsKey(ip)){
								PendingDelete pd = pds.remove(ip);
								ddb.delete(listener, pd.key);								
							}
							ddb.write(pw.l, pw.k, pw.v);
							pw.expire = System.currentTimeMillis();
							writesInProgress.put((String) pw.k.getKey(), pw);
							return true;
						} catch (DistributedDatabaseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							pw = null;
						}
					}
				}
			//}
			
			
			Iterator<Entry<String, PendingWrite>> it = cachedWrites.entrySet().iterator();
			while (true){
				boolean doNext = true;
				if (!it.hasNext())break;
				if (key!=null){
					if (cachedWrites.containsKey(key)){
						pw = cachedWrites.remove(key);
					}
				}
				if (pw==null || pw.k==null){
					it = cachedWrites.entrySet().iterator();
					pw = it.next().getValue();
					it.remove();
					doNext = false;
				}
				
				if (pw==null || pw.k == null) continue;
				PendingWrite pw2;
				try {
					if (writesInProgress.containsKey((String) pw.k.getKey()) || 
							readsInProgress.containsKey((String) pw.k.getKey())
//							|| deletesInProgress.contains((String) pw.k.getKey())
							){
						if (doNext) it.next();
						continue;
					}

					// clear value in pws if one is there
				
					pw2 = pws.get((String) pw.k.getKey());			
					if ((pw2 != null) && (pw2.l != null)) {		            		            
						pws.put((String) pw.k.getKey(), new PendingWrite());		           
					}
					
					if (DEBUG) System.out.println("Writing: "+pw.k.getKey());
					if (pds.containsKey(pw.k.getKey())){
						PendingDelete pd = pds.remove(pw.k.getKey());
						ddb.delete(listener, pd.key);								
					}
					ddb.write(pw.l, pw.k, pw.v);
					pw.expire = System.currentTimeMillis();
					writesInProgress.put((String) pw.k.getKey(), pw);
					return true;
				} catch (DistributedDatabaseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return false;
	}

	protected boolean tryNextRead() {
		
    	if (cachedReads.size()>0){
			Iterator<Entry<String, PendingRead>> it = cachedReads.entrySet().iterator();
			PendingRead pr = it.next().getValue();
			it.remove();			
			doRead(pr.key, pr.description, pr.readAction, pr.option, pr.timeout);
			return true;
    	}
		
		return false;
	}

	/**
     * @return
     */
    public synchronized boolean isWriteInProgress(
        DistributedDatabaseEvent event) {
        try {
        
            return writesInProgress.containsKey(event.getKey().getKey());
        } catch (DistributedDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

            return false;
        }
    }
    
    public synchronized void doWrite(DistributedDatabaseKey key,
            ArrayList<ByteArrayOutputStream> baoss, IDHTWriteAction write)
            throws DistributedDatabaseException {
    	if (baoss.size()==0){
    		if (DEBUG) System.out.println("What's going on?");
    	}
    	DistributedDatabaseValue ddbValues[] = new DistributedDatabaseValue[baoss.size()];
    	try {
    		
    		
    		
    		for (int i = 0; i < ddbValues.length; i++){
    			byte[] bytes = baoss.get(i).toByteArray();
    			ddbValues[i] = ddb.createValue(bytes);
    		}
    		
    		expireWrites();
    	
    	    	if (writesInProgress.size()>=MAX_CONCURRENT_WRITES){
    	    		PendingWrite pw = pws.get((String) key
                            .getKey());
    	    		 if (pw == null){
    	    			 pw = new PendingWrite();
						pw.k = key;
						pw.l = listener;
						pw.v = ddbValues;
						pw.write = write;
					
    		             cachedWrites.put((String) key.getKey(), pw);
						
    	    		 }
    	    		 else {
    	    			 pw.k = key;
 						pw.l = listener;
 						pw.v = ddbValues;
 						pw.write = write;
 					
 							pws.put((String) key.getKey(), pw);
 						cachedWrites.put((String)key.getKey(), pw);
 						
    	    		 }
    	    		return;
    	    	}
        	

            //          	for (int i = 0; i < bytes.length; i++) System.out.print((int)bytes[i]+ " ");
            //          	System.out.print("\n");
           
                if ((writesInProgress.get((String) key.getKey()) == null) &&
                        (readsInProgress.get(key.getKey()) == null)
//                        && !deletesInProgress.contains(key.getKey())
                        ) {
                   
                		if (DEBUG) System.out.println("Writing to: "+key.getKey());
                		if (pds.containsKey(key.getKey())){
							PendingDelete pd = pds.remove(key.getKey());
							ddb.delete(listener, pd.key);								
						}
                		
                        ddb.write(listener, key, ddbValues);
                        PendingWrite pw = new PendingWrite();
						pw.k = key;
						pw.l = listener;
						pw.v = ddbValues;
						pw.write = write;
                        writesInProgress.put((String) key.getKey(),
                            pw);
                        pws.put((String) key.getKey(),
                            new PendingWrite());
                    
                } else {
                   
                        if (pws.get((String) key.getKey()) == null) {
                            pws.put((String) key.getKey(),
                                new PendingWrite());
                        }

                        PendingWrite pw = pws.get((String) key
                                                                .getKey());
                        
                        // this will happen in LIFO hash map if full
                        if (pw==null) return;
                        
                        // otherwise, fill in data
                        pw.k = key;
                        pw.l = listener;
                        pw.v = ddbValues;
                        pw.write = write;
                        cachedWrites.put((String)key.getKey(), pw);
                        //System.out.println("Caching write!");
                    }
                
            
        } catch (DistributedDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    
    }

    private void expireWrites() {
    	
		synchronized (writesInProgress){
			HashSet<String> keys = new HashSet<String>();
			long time = System.currentTimeMillis();
			for (Entry<String, PendingWrite> ent : writesInProgress.entrySet()){
				if (ent.getValue().expire  < time-WRITE_EXPIRE){
					keys.add(ent.getKey());
				}
			}
			for (String key : keys){
				if (DEBUG) System.err.println("Expired put to: "+key);
				writesInProgress.remove(key);
			}
		}
	}

	/**
     * @param event
     * @param baos
     * @throws DistributedDatabaseException
     */
    public void doWrite(DistributedDatabaseEvent event,
        ByteArrayOutputStream baos, IDHTWriteAction write)
        throws DistributedDatabaseException {
//        
//        DistributedDatabaseKey ddbKey = event.getKey();
//
//        doWrite(ddbKey, baos, write);
    }

    /**
     *
     * @param key
     * @param description
     * @param readAction
     * @return
     */
    public boolean doRead(String key, String description,
        IDHTReadAction readAction, int option) {
        return doRead(key, description, readAction, option, READ_TIMEOUT);
    }

    /**
     *
     * @param key
     * @param description
     * @param readAction
     * @return
     */
    public synchronized boolean doRead(String key, String description,
        IDHTReadAction readAction, int option, long timeout) {
    	
    	expireReads();
    
	    	if (readsInProgress.size()>MAX_CONCURRENT_READS){
	    		synchronized (prs){
		    		 PendingRead pr = prs.get(description);
		    		 if (pr == null){
		    			 pr = new PendingRead();
			             pr.description = description;
			             pr.key = key;
			             pr.readAction = readAction;
			             pr.option = option;
			             pr.timeout = timeout;
			             cachedReads.put(key, pr);
		    		 }
		    		 else {
		    			 pr = new PendingRead();
			             pr.description = description;
			             pr.key = key;
			             pr.readAction = readAction;
			             pr.option = option;
			             pr.timeout = timeout;
			             prs.put(key, pr);
		    		 }
		    		return false;
	    		}
	    	
    	}
    	
	    	PendingRead pr;
            if (readsInProgress.containsKey(key) ||
                    writesInProgress.containsKey(key)) {
            	
            	synchronized (prs){
	                
	                pr = prs.get(description);
	                if (pr== null) pr = new PendingRead();
	                pr.description = description;
	                pr.key = key;
	                pr.readAction = readAction;
	                pr.option = option;
	                pr.timeout = timeout;
	                prs.put(description, pr);
	
	                return false;
            	}
            }

            // read here, write after completing read
            DistributedDatabaseKey ddbKey;

            try {
                ddbKey = ddb.createKey(key, description);

                ddb.read(listener, ddbKey, timeout, option);
                pr = new PendingRead();
                pr.description = description;
                pr.key = key;
                pr.readAction = readAction;
                pr.option = option;
                pr.timeout = timeout;
                readsInProgress.put(key, pr);
            } catch (DistributedDatabaseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();

                return false;
            }

            return true;
        
    }

    private void expireReads() {
    	synchronized (readsInProgress){
			HashSet<String> keys = new HashSet<String>();
			long time = System.currentTimeMillis();
			for (Entry<String, PendingRead> ent : readsInProgress.entrySet()){
				if (ent.getValue().expire  < time){
					keys.add(ent.getKey());
				}
			}
			for (String key : keys) readsInProgress.remove(key);
		}
		
	}

	private void writeCachedValue(DistributedDatabaseEvent event)
        throws DistributedDatabaseException {
    	
    	PendingWrite pw = pws.get((String) event.getKey().getKey());
    	
    	synchronized(writesInProgress){
	    	if (writesInProgress.size()>MAX_CONCURRENT_WRITES){
	    		
	    		synchronized(pws){

		    		pws.put((String) event.getKey().getKey(), new PendingWrite());
			        if (!cachedWrites.containsKey((String)event.getKey().getKey())){
			        	synchronized (cachedWrites){
			        		cachedWrites.put((String) event.getKey().getKey(), pw);
			        	}
			        }
	    		}
	    		 
	    		return;
	    	}
    	}
    	
        

        if ((pw != null) && (pw.l != null)) {
            ddb.write(pw.l, pw.k, pw.v);
            writesInProgress.put((String) pw.k.getKey(), pw);
            pws.put((String) event.getKey().getKey(), new PendingWrite());
        }
    }
    
    /*public static class QueuedOperation implements Comparable {

    	public Long creationTime = System.currentTimeMillis();
    	
		public int compareTo(Object o) {
			if (o instanceof QueuedOperation){
				return ((QueuedOperation)o).creationTime.compareTo(creationTime);
			}
			return 1;
		}
    	
    }*/

    public static class PendingWrite {
        public long expire = System.currentTimeMillis();
		public DistributedDatabaseListener l;
        public DistributedDatabaseKey k;
        public DistributedDatabaseValue[] v;
        public IDHTWriteAction write;
    }

    public static class PendingDelete {
        public DistributedDatabaseListener l;
        public DistributedDatabaseKey key;
        public IDHTWriteAction write;
        public long expire = System.currentTimeMillis()+DELETE_EXPIRE;
    }
    
    public static class PendingRead {
        String key;
        String description;
        IDHTReadAction readAction;
        int option;
        long timeout;
        long expire = System.currentTimeMillis()+READ_EXPIRE;
    }   

	public DistributedDatabaseKey createKey(String key, String description) throws DistributedDatabaseException {
		// TODO Auto-generated method stub
		return ddb.createKey(key, description);
	}

	public synchronized boolean doDelete(DistributedDatabaseKey ddbKey) {
		expireDeletes();
		String description = ddbKey.getDescription();
		String key;
		try {
			key = (String)ddbKey.getKey();
		
			if (pds.get(key)==null){
				PendingDelete pd = new PendingDelete();
            	pd.key = ddbKey;	            	
            	pds.put(key, pd);
			}
			return true;
//	            if (readsInProgress.containsKey(key) ||
//	                    writesInProgress.containsKey(key) 
//	                    || writesInProgress.size() >= MAX_CONCURRENT_WRITES
////	                    || deletesInProgress.contains(key)
//	                    ) {
//	            	PendingDelete pd = new PendingDelete();
//	            	pd.key = ddbKey;	            	
//	            	pds.put(key, pd);
//	                return false;
//	            }
//	            
//	        
//			
//			deletesInProgress.put((String)ddbKey.getKey(), System.currentTimeMillis()+DELETE_EXPIRE);
//			ddb.delete(listener, ddbKey);
//			return true;
			
		} catch (DistributedDatabaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		
		
	}

	private void expireDeletes() {
		synchronized (deletesInProgress){
			HashSet<String> keys = new HashSet<String>();
			long time = System.currentTimeMillis();
			for (Entry<String, Long> ent : deletesInProgress.entrySet()){
				if (ent.getValue()  < time){
					keys.add(ent.getKey());
				}
			}
			for (String key : keys) deletesInProgress.remove(key);
		}
		
	}

}
