/**
 * Ono Project
 *
 * File:         StandardConnectionManager.java
 * RCS:          $Id: StandardConnectionManager.java,v 1.82 2010/03/29 16:48:04 drc915 Exp $
 * Description:  StandardConnectionManager class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 29, 2007 at 1:41:59 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.connection
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
package edu.northwestern.ono.connection.standard;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.OnoConfiguration;
import edu.northwestern.ono.connection.IConnectionHandler;
import edu.northwestern.ono.connection.IConnectionListener;
import edu.northwestern.ono.connection.IOnoConnection;
import edu.northwestern.ono.connection.IOnoConnectionManager;
import edu.northwestern.ono.stats.Statistics;
import edu.northwestern.ono.ui.SideStep;
import edu.northwestern.ono.util.NewByteBufferPool;
import edu.northwestern.ono.util.NewTagByteBuffer;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The StandardConnectionManager class ...
 */
public class StandardConnectionManager extends Thread implements IOnoConnectionManager {
	
	
	private class IncomingMessageHandler{

		private IConnectionHandler manager;
		private String description;
		private String name;

		public IncomingMessageHandler(String name, String description, IConnectionHandler manager) {
			this.name = name;
			this.description = description;
			this.manager = manager;
			
		}
		
		public boolean accept(IOnoConnection con){
			return manager.accept(con);
		}
		
	}

	public static final int STANDARD_PORT=34336;
	
	private static final boolean DEBUG = false;
	
	private static StandardConnectionManager self;
	
    /** Map of ip address -> connections ... */
    private HashMap<InetSocketAddress, StandardMessageConnection> connections;
    private HashMap<InetSocketAddress, IConnectionListener> listeners;
    private HashMap<SocketChannel, InetSocketAddress> channelMap;
   // private static ByteBufferPool bufferPool;
    static Selector selector = null;
    //static Selector serverSelector = null;
    static IncomingMessageHandler handler = null;

    /** set of pending connections */
    private HashSet<InetSocketAddress> connecting;

	private boolean isRegistering;
	
	/** map of socket to pending message (one that is not yet finished) */
	private HashMap<InetSocketAddress, NewTagByteBuffer> pendingMessages;
	/** map of socket to pending message-size data */
	private HashMap<InetSocketAddress, NewTagByteBuffer> pendingSizes;

	
	public final static int MAX_MESSAGE_SIZE = 32768;

	private static final boolean VERBOSE = false;

    public StandardConnectionManager(){
    	connecting = new HashSet<InetSocketAddress>();
    	//if (bufferPool == null ) bufferPool = new ByteBufferPool(2000000, MAX_MESSAGE_SIZE, true, ByteOrder.LITTLE_ENDIAN);
    	listeners = new HashMap<InetSocketAddress, IConnectionListener>();
    	channelMap = new HashMap<SocketChannel, InetSocketAddress>();
        connections = new HashMap<InetSocketAddress, StandardMessageConnection>();
        pendingMessages = new HashMap<InetSocketAddress, NewTagByteBuffer>();
        pendingSizes = new HashMap<InetSocketAddress, NewTagByteBuffer>();
    }
    
	public static StandardConnectionManager getInstance(){
		if( self == null){
			self = new StandardConnectionManager();
			self.setDaemon(true);
			NewByteBufferPool.init();
			try {
				selector = Selector.open();
                //serverSelector = Selector.open();
			
				ServerSocketChannel ssChannel1 = ServerSocketChannel.open();
		        ssChannel1.configureBlocking(false);
		        ssChannel1.socket().bind(new InetSocketAddress(STANDARD_PORT));
		        ssChannel1.register(selector, SelectionKey.OP_ACCEPT);
		        
				MainGeneric.createThread("StandardConnectionManager", self);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			}
		}
		return self;
	}

	
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		 // Wait for events
	    while (true) {
	    	try{
	    	int i = -1;
	        try {
	        	if (!isRegistering){
//	        		printSelectedKeys();
		            // Wait for an event
		            i = selector.select();
		           
	        	}
	        	else {
	        		try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	        	}
	        	
	        } catch (IOException e) {
	            // Handle error with selector
	            break;
	        }
	        
	        synchronized(channelMap){
		        for (SocketChannel chan : channelMap.keySet()){
		        	SelectionKey temp = chan.keyFor(selector);
		        	if (temp!=null && chan.isConnected()){
		        		if (temp.isValid()){
		        			if ((temp.interestOps()&SelectionKey.OP_WRITE)!=0)
		        				temp.interestOps(SelectionKey.OP_READ|SelectionKey.OP_WRITE);
		        			else temp.interestOps(SelectionKey.OP_READ);
		        		}
		        	}
		        }
	        }
	        
	        /*if (i==0){
	        	LinkedList<SelectionKey> toRemove = new LinkedList<SelectionKey>();
	        	for (Entry<InetSocketAddress, StandardMessageConnection> ent : connections.entrySet()){
	        		try {
						if (!ent.getValue().channel.finishConnect()){
							toRemove.add(ent.getValue().channel.keyFor(selector));		
							
						}
						else {
							SelectionKey temp = ent.getValue().channel.keyFor(selector);
							temp.interestOps(temp.interestOps()&(~SelectionKey.OP_CONNECT));
						}
					} catch (IOException e) {
						toRemove.add(ent.getValue().channel.keyFor(selector));			
					}
	        	}
	        	for (SelectionKey key: toRemove){
	        		closeConnection(key);
	        		
	        	}
	        }*/
	    
	        // Get list of selection keys with pending events
	        Iterator<SelectionKey> it = selector.selectedKeys().iterator();
	    
	        // Process each key at a time
	        while (it.hasNext()) {
	        	 if (DEBUG) {
		            	System.err.println("Processing selected keys!");
		            }
	            // Get the selection key
	            SelectionKey selKey = it.next();
	    
	            // Remove it from the list to indicate that it is being processed
	            it.remove();	           
	            synchronized (selKey) {
//	          Check if it's a connection request
                if (selKey.isValid() && selKey.isAcceptable()) {
                    // Get channel with connection request
                    ServerSocketChannel ssChannel = (ServerSocketChannel)selKey.channel();
    
                    handleIncomingConnection(ssChannel);
                }
                else {
                	try {

                		processSelectionKey(selKey);
                	} catch (IOException e) {
                		// Handle error with channel and unregister
                		selKey.cancel();
                		closeConnection(selKey, e.getMessage());
                	} catch (CancelledKeyException e2){
                	    closeConnection(selKey, e2.getMessage());
                    }
                }
	            }
	        }
	    	} // end try 
	    	catch (Exception e){
	    		e.printStackTrace();
	    		Statistics.getInstance().reportError(e);
	    	}
	    } // end while
	}

	private void printSelectedKeys() {
		for (SelectionKey key : selector.keys()){
			if (key.isValid()) System.out.println("Key: " + key.channel() + "\t"+key.interestOps());
		}
		
	}

	// TODO finish
	private void handleIncomingConnection(ServerSocketChannel ssChannel) {
	    // Get port that received the connection request; this information
	    // might be useful in determining how to handle the connection
	    int localPort = ssChannel.socket().getLocalPort();
	    
	    try {
	        // Accept the connection request.
	        // If serverSocketChannel is blocking, this method blocks.
	        // The returned channel is in blocking mode.
	    	
	    	SocketChannel sChannel = ssChannel.accept(); 
	    	InetSocketAddress sock = new InetSocketAddress(sChannel.socket().getInetAddress(), sChannel.socket().getPort());
		    StandardMessageConnection con = new StandardMessageConnection(null, null, sock, false, null);
	    	con.setChannel(sChannel);
		        // If serverSocketChannel is non-blocking, sChannel may be null
		        if (sChannel == null) {
		            
		            
		        } else {
		        	if (handler.accept(con)){

			            // Use the socket channel to communicate with the client
                        synchronized(connections){connections.put(sock, con);}
			        	 synchronized(channelMap){channelMap.put(sChannel, sock);}
			        	sChannel.configureBlocking(false);
			        	SelectionKey key = sChannel.register(selector, SelectionKey.OP_CONNECT|SelectionKey.OP_READ);
                        con.setSelectionKey(key);
//			        	con.registerConnected();
			        	
		        	}
		        	else{
		        		sChannel.close();
		        	}
		        }
	    	
	    } catch (IOException e) {
	    }
		
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnectionManager#connect(java.net.InetSocketAddress, edu.northwestern.ono.connection.IConnectionListener, boolean)
	 */
	public void connect(InetSocketAddress endPoint, IConnectionListener runner,
			boolean useTCP) {
		if (!useTCP){
			throw new RuntimeException("UDP not supported!");			
		}

		if (connecting.contains(endPoint)) return;
		else if (connections.containsKey(endPoint)) {
			runner.connected(connections.get(endPoint));
			return;
		}
		else {

	        SocketChannel sChannel;
			try {
				sChannel = createSocketChannel(endPoint);
	    
				// Register the channel with selector, listening for all events
				
				
				connecting.add(endPoint);
				listeners.put(endPoint, runner);
				 synchronized(channelMap){channelMap.put(sChannel, endPoint);}
				
				
				isRegistering = true;
				selector.wakeup();
                SelectionKey sk = sChannel.register(selector, sChannel.validOps());
                isRegistering = false;
                StandardMessageConnection con = new StandardMessageConnection(sChannel, runner, endPoint, true, sk);
                synchronized(connections){connections.put(endPoint, con);}
                
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnectionManager#getActiveConnections()
	 */
	public Map<InetSocketAddress, IOnoConnection> getActiveConnections() {
		// TODO Auto-generated method stub
		HashMap<InetSocketAddress, IOnoConnection> toReturn = new HashMap<InetSocketAddress, IOnoConnection>();
        synchronized(connections){
            for (Map.Entry<InetSocketAddress, StandardMessageConnection> ac : connections.entrySet()){
        
                toReturn.put(ac.getKey(), ac.getValue());
            }
        }
		return toReturn;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnectionManager#getByteBuffer(int)
	 */
	public NewTagByteBuffer getByteBuffer(int size) {
//		if (size==-1) size = 65536;
//		// TODO find better buffer pool impl
//		NewTagByteBuffer buffer;
//		boolean direct = true;
//		if (direct) {
//			try {
//				buffer = NewTagByteBuffer.allocateDirect(size);
//			} catch (OutOfMemoryError e) {
//				//log.warn("OutOfMemoryError: No more direct buffers available; trying heap buffer instead");
//			} 
//		}
//		buffer = NewTagByteBuffer.allocate(size);
//		
//		buffer.clear();
//		if (buffer.order() != ByteOrder.LITTLE_ENDIAN) {
//			buffer.order(ByteOrder.LITTLE_ENDIAN);
//		}
		if (size > NewByteBufferPool.MAX_SIZE) throw new RuntimeException("Too big!");
		NewTagByteBuffer ntbb = NewByteBufferPool.take();
		ntbb.getBuffer().limit(size>0?size:ntbb.getBuffer().capacity());
		return ntbb;
		/*NewTagByteBuffer toReturn;
		toReturn = bufferPool.take();
		toReturn.position(0);		
		try{
			
			if (size!=-1) toReturn.limit(size);
		} catch (RuntimeException e){
			Statistics.getInstance().reportError(new Exception("Bad size: "+size));
			throw e;
		}
		if (VERBOSE){
			try{
			System.err.println("Taking "+toReturn.array().hashCode());
			} catch (Exception e){
				try {System.err.println("Taking "+toReturn.arrayOffset());}
				catch(Exception e2){System.err.println("Taking "+toReturn);}
			}
			Thread.dumpStack();
		}
		return toReturn; */
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnectionManager#getConnection(java.net.InetSocketAddress)
	 */
	public IOnoConnection getConnection(InetSocketAddress dest) {
		if (!connecting.contains(dest))
			return connections.get(dest);
		else 
			return null;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnectionManager#hasConnection(java.net.InetSocketAddress)
	 */
	public boolean hasConnection(InetSocketAddress sock) {
		// TODO Auto-generated method stub
		return connections.containsKey(sock);
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnectionManager#registerManagerListener(java.lang.String, java.lang.String, edu.northwestern.ono.connection.IConnectionHandler)
	 */
	public void registerManagerListener(String name, String description,
			IConnectionHandler manager) {
		handler = new IncomingMessageHandler(name, description, manager);

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnectionManager#returnToPool(java.nio.NewTagByteBuffer)
	 */
	public void returnToPool(NewTagByteBuffer bb) {
		if (bb!=null)
			NewByteBufferPool.put(bb);
		/*if (VERBOSE){
			try{
			System.err.println("Returning "+bb.array().hashCode());
			} catch (Exception e){try {System.err.println("Returning "+bb.arrayOffset());}
			catch(Exception e2){System.err.println("Returning "+bb);}
			}
			Thread.dumpStack();
		}
		bufferPool.put(bb);
		*/
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnectionManager#send(byte[], java.lang.String)
	 */
	public void send(byte[] bytes, String ipAddress) {
        synchronized(connections){
    		for (InetSocketAddress sock : connections.keySet()){
    			if (sock.getAddress().toString().substring(1).equals(ipAddress)){
    				NewTagByteBuffer bb = getByteBuffer(bytes.length);
    				bb.getBuffer().put(bytes);
    				connections.get(sock).send(bb);
    				return;
    			}
    		}
        }
		throw new RuntimeException("Tried to send w/o connection!");

	}

    // Creates a non-blocking socket channel for the specified host name and port.
    // connect() is called on the new channel before it is returned.
    public static SocketChannel createSocketChannel(InetSocketAddress sock) throws IOException {
        // Create a non-blocking socket channel
        SocketChannel sChannel = SocketChannel.open();
        sChannel.configureBlocking(false);      
        
    
        // Send a connection request to the server; this method is non-blocking
        sChannel.connect(sock);
        return sChannel;
    }
    
    public void processSelectionKey(SelectionKey selKey) throws IOException {
    	if (DEBUG){
    		System.err.println("processSelectionKey: Processing...");
    	}
        // Since the ready operations are cumulative,
        // need to check readiness for each operation
    	if (!selKey.isValid()){
    		// connection closed
    		closeConnection(selKey, "Selection key is not valid!");
    	}
        if (selKey.isValid() && selKey.isConnectable()) {        	
            // Get channel with connection request
            SocketChannel sChannel = (SocketChannel)selKey.channel();
            InetSocketAddress sock = channelMap.get(sChannel);
            
            
            
            // create connection object
            StandardMessageConnection con = 
        		new StandardMessageConnection(sChannel, 
        				listeners.get(sock), sock, true, selKey);
            
            boolean success = sChannel.finishConnect();
            if (!success) {
            	 synchronized(channelMap){channelMap.remove(sock);}
            	
                 synchronized(connections){connections.remove(sock);}
            	connecting.remove(sock);
                // An error occurred; handle it
            	listeners.get(sock).failed(con, new Error("Failed during " +
            			"finish connect!"));
    
                // Unregister the channel with this selector
                selKey.cancel();
            }
            else {
            	
            	if (sChannel.keyFor(selector)==null)
            		sChannel.register(selector, sChannel.validOps());
                synchronized(connections){connections.put(sock, con);}
            	connecting.remove(sock);
            	if (listeners.get(sock)!=null) listeners.get(sock).connected(con);
            	
            }
        }
        if (selKey.isValid() && selKey.isReadable()) {
        	if (DEBUG){
        		System.err.println("processSelectionKey: Reading...");
        	}
            // Get channel with bytes to read
            SocketChannel sChannel = (SocketChannel)selKey.channel();
            InetSocketAddress sock = channelMap.get(sChannel);
            NewTagByteBuffer bb=null;
            try {
            	bb = getByteBuffer(-1);
            	int numBytesRead = sChannel.read(bb.getBuffer());

            	if (numBytesRead > 0) {


            		// To read the bytes, flip the buffer
            		bb.getBuffer().flip();

            		// if new message, read the length 
            		// and start filling
            		handleIncomingData(sock, bb);

            	}
            	else if (numBytesRead == -1){
            		closeConnection(selKey, "Number of bytes read is -1");
            	}
            } finally{
            	returnToPool(bb);
            }
        }
        if (selKey.isValid() && selKey.isWritable()) {
        	if (DEBUG){
        		System.err.println("processSelectionKey: Writing...");
        	}
            // Get channel that's ready for more bytes
            SocketChannel sChannel = (SocketChannel)selKey.channel();
            InetSocketAddress sock = channelMap.get(sChannel);
            
            if (connections.get(sock) == null){
            	closeConnection(selKey, "No connection for socket "+sock);
            } 
            int numBytesWritten = connections.get(sock).sendData(sChannel);
            if (numBytesWritten==-1){
            	closeConnection(selKey, "Can't write to socket "+sock);
            }
            if (OnoConfiguration.getInstance().isVisualize()){
            	SideStep.getInstance().addUlDataPoint(sock.getAddress().getHostAddress(), 
            			System.currentTimeMillis(), numBytesWritten);
            }
        }
        
    }

	private void handleIncomingData(InetSocketAddress sock, NewTagByteBuffer bb) {
		
		int currentSize = -1;
		NewTagByteBuffer message;

		if (OnoConfiguration.getInstance().isVisualize())
			SideStep.getInstance().addDlDataPoint(sock.getAddress().getHostAddress(), 
    			System.currentTimeMillis(), bb.getBuffer().remaining());
		
		while (bb.getBuffer().remaining()>0){
			synchronized (pendingSizes){
				// if completely new, allocate an integer buffer to read message 
				// size
				if (pendingSizes.get(sock) == null && pendingMessages.get(sock)==null){
					NewTagByteBuffer lengthBuffer = getByteBuffer(4);
					pendingSizes.put(sock, lengthBuffer);
				}

				// at this point, either we just processed a brand new message 
				// or we are continuing to process the size from a previous read
				if (pendingSizes.get(sock)!=null){

					if (protectedBufferRead(bb, pendingSizes.get(sock))>0){
						// the whole int still isn't here
						return;
					}
					else{
						NewTagByteBuffer temp = pendingSizes.remove(sock);
						temp.getBuffer().flip();
						currentSize = temp.getBuffer().getInt();
						returnToPool(temp); // return buffer
//						System.out.println("Message size: "+currentSize);
						if (currentSize <= 0 || currentSize > 2*MAX_MESSAGE_SIZE ){
							System.err.println("Invalid message size("+currentSize+") from "+sock+"! Probably " +
							"messed up packet processing!");
							// close the connection to reset this garbage
							if (connections.get(sock)!=null){
								connections.get(sock).close();
							}
							temp = pendingMessages.get(sock);
							if (temp!=null) returnToPool(temp);
							return;
						}

						// use standard allocation so client code can return the buffer
						message = getByteBuffer(currentSize);
						message.getBuffer().limit(currentSize);
						pendingMessages.put(sock, message);
					}
				}
			}

			synchronized (pendingMessages){
				// now we try to read message data			
				if (pendingMessages.get(sock)!=null && bb.getBuffer().remaining()>0){
					message = pendingMessages.remove(sock);
					boolean returnToPool = false;
					try{
						protectedBufferRead(bb, message);
						if (message.getBuffer().remaining()==0){
							message.getBuffer().flip();

							
							if (connections.get(sock)!=null) {	
								returnToPool = true;
								connections.get(sock).receive(message);
//								if (pendingMessages.remove(sock)!=null) {

//								}
							}
							returnToPool(message);
							returnToPool = false;

						} else {
							pendingMessages.put(sock, message);
						}
					} catch (Exception e){
						Statistics.getInstance().reportError(e);
						if (returnToPool) returnToPool(message);

					} finally {
//						
					}

				}		
			}
		}
	}

	private int protectedBufferRead(NewTagByteBuffer source, NewTagByteBuffer dest) {
		int oldLimit = source.getBuffer().limit();
		
		// TODO remove when fixed
//		source.print();
		
		if (source.getBuffer().remaining()>dest.getBuffer().remaining()){
			// prevent overflow
			source.getBuffer().limit(dest.getBuffer().remaining()+source.getBuffer().position());                			
		}
		dest.getBuffer().put(source.getBuffer());
		source.getBuffer().limit(oldLimit);
		return dest.getBuffer().remaining();
	}

	private void cleanupConnection(SocketChannel channel){
		InetSocketAddress sock = channelMap.get(channel);
		connecting.remove(sock);
		IConnectionListener list = listeners.remove(sock);
		NewTagByteBuffer bb;
		synchronized(pendingMessages){
	        bb = pendingMessages.remove(sock);
	        if (bb!=null) 
	        	returnToPool(bb);
		}
		synchronized(pendingSizes){
	        bb = pendingSizes.remove(sock);
	        if (bb!=null) 
	        	returnToPool(bb);
		}
        StandardMessageConnection con = null;
        synchronized(connections){con = connections.remove(sock);}
		 synchronized(channelMap){channelMap.remove(channel);}

		try {
			channel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void closeConnection(SelectionKey selKey, String message)  {
		SocketChannel sChannel = (SocketChannel)selKey.channel();
		InetSocketAddress sock = channelMap.get(sChannel);
		StandardMessageConnection con = connections.get(sock);
        if (con==null) return; // TODO why null?        
		IConnectionListener list = listeners.remove(sock);
		if (list!=null){
			list.failed(con, new Error("Other end terminated connection!"));            	
		}
		else if (con!=null) con.notifyFailed(new Error(message)); // TODO more meaningful
		synchronized (selKey) {selKey.cancel();}
		
		cleanupConnection(sChannel);

	}

	public void closeChannel(SocketChannel channel) throws IOException {
		
		cleanupConnection(channel);

		
	}

    /* (non-Javadoc)
     * @see edu.northwestern.ono.connection.IOnoConnectionManager#getConnection(java.net.InetAddress)
     */
    public IOnoConnection getConnection(InetAddress dest) {
        synchronized (connections){
            for (InetSocketAddress sock : connections.keySet()){
                if (sock.getAddress().equals(dest)){
                    if (!connecting.contains(sock)){
                        return connections.get(sock);
                    }
                }
            }
        }
        return null;
    }

//	public String getReturnTrace(NewTagByteBuffer bb) {
//		return bufferPool.getReturnTrace(bb);
//		
//	}

	public static int getDefaultMessageSize() {
		return MAX_MESSAGE_SIZE;
	}

}
