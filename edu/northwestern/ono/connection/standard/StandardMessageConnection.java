/**
 * Ono Project
 *
 * File:         StandardMessageConnection.java
 * RCS:          $Id: StandardMessageConnection.java,v 1.48 2010/03/29 16:48:04 drc915 Exp $
 * Description:  StandardMessageConnection class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 30, 2007 at 9:17:09 AM
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
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.LinkedList;

import edu.northwestern.ono.connection.IConnectionListener;
import edu.northwestern.ono.connection.IEndpoint;
import edu.northwestern.ono.connection.IMessageException;
import edu.northwestern.ono.connection.IOnoConnection;
import edu.northwestern.ono.stats.Statistics;
import edu.northwestern.ono.util.NewTagByteBuffer;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The StandardMessageConnection class ...
 */
public class StandardMessageConnection implements IOnoConnection {
	
	SocketChannel channel;
	private HashSet<IConnectionListener> listeners;
	LinkedList<NewTagByteBuffer> pendingToSend;
	private boolean isLocal;
	private InetSocketAddress sock;
    private SelectionKey key;
	private IEndpoint endPoint;    
	private static final boolean DEBUG = false;
	private static final boolean SEND_DEBUG = true;
	private static final boolean METERED_SEND = true;
	private static final boolean DEBUG_BB = false;
	private int numPacketsSent = 0;
	private Boolean closed;
	


	public StandardMessageConnection(SocketChannel channel, IConnectionListener listener, 
            InetSocketAddress sock, boolean isLocal, SelectionKey key) {
		closed = false;
		this.channel = channel;
		listeners = new HashSet<IConnectionListener>();
		if (listener!=null) listeners.add(listener);
		pendingToSend = new LinkedList<NewTagByteBuffer>();
		this.isLocal = isLocal;
		this.sock = sock;
        this.key = key;
        if (channel!=null){
        	endPoint = new StandardEndpoint(channel, sock);
        	setBufferSizes();
        }
	}

	private void setBufferSizes() {
		try {
			channel.socket().setSendBufferSize(StandardConnectionManager.MAX_MESSAGE_SIZE);		
			channel.socket().setReceiveBufferSize(StandardConnectionManager.MAX_MESSAGE_SIZE);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnection#addListener(edu.northwestern.ono.connection.IConnectionListener)
	 */
	public void addListener(IConnectionListener listener) {
		if (closed) return;
		listeners.add(listener);

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnection#close()
	 */
	public void close() throws IMessageException {
		try {
			
			synchronized (pendingToSend){
				closed = true;
				for (NewTagByteBuffer bb : pendingToSend){
					StandardConnectionManager.getInstance().returnToPool(bb);
				}
				pendingToSend.clear();
			}
			if (key!=null) synchronized(key){key.cancel();}
			if (channel!=null) StandardConnectionManager.getInstance().closeChannel(channel);	
			
		} catch (IOException e) {
			throw new IMessageException(e);
		} finally {
			synchronized (pendingToSend){
				for (NewTagByteBuffer bb : pendingToSend){
					StandardConnectionManager.getInstance().returnToPool(bb);
				}
				pendingToSend.clear();
			}
		}

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnection#connect()
	 */
	public void connect() throws IMessageException {
		try {
			channel.connect(sock);
		} catch (IOException e) {
			for (IConnectionListener listener: listeners)
				listener.failed(this, new IMessageException(e));
			
		} 

	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnection#getEndpoint()
	 */
	public IEndpoint getEndpoint() {
		return endPoint;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnection#getMaximumMessageSize()
	 */
	public int getMaximumMessageSize() {
		// TODO Auto-generated method stub
		try {
			int size = channel.socket().getSendBufferSize();
			return StandardConnectionManager.MAX_MESSAGE_SIZE; // TODO fix this hack
		} catch (SocketException e) {
			try {
				synchronized(key){key.cancel();}
				StandardConnectionManager.getInstance().closeChannel(channel);
			} catch (IOException e1) {
				
				e1.printStackTrace();
			}
			notifyFailed(new Error(e));			
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnection#isLocallyIniated()
	 */
	public boolean isLocallyIniated() {
		return isLocal;
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnection#removeListener(edu.northwestern.ono.connection.IConnectionListener)
	 */
	public void removeListener(IConnectionListener listener) {
		listeners.remove(listener);
	}

	/* (non-Javadoc)
	 * @see edu.northwestern.ono.connection.IOnoConnection#send(java.nio.NewTagByteBuffer)
	 */
	public void send(NewTagByteBuffer message) throws IMessageException {
		// TODO  get the map from this NewTagByteBuffer to the pooled one
		
		synchronized(pendingToSend){
			if (closed){
				StandardConnectionManager.getInstance().returnToPool(message);
				return;
			}
			if (pendingToSend.size()>150){
				System.out.println("Too many packets queued!");
			}
			NewTagByteBuffer header = getPacketHeader(message);
			if (DEBUG_BB){
				int i = 0;
				for (NewTagByteBuffer bb : pendingToSend ){
					i++;
					if (bb==header || bb==message){
//						System.out.print(StandardConnectionManager.
//								getInstance().getReturnTrace(bb));
						throw new RuntimeException("Adding same buffer!");
					}
				}
			}
			pendingToSend.add(header);
			pendingToSend.add(message);
		}
		if (DEBUG){
    		System.err.println("send: Setting interest ops, pending size: " + pendingToSend.size() + "!"+this);
    	}
		if (key==null) key = channel.keyFor(StandardConnectionManager.selector);
		if (key==null) return;
		synchronized(key){
			
			key = channel.keyFor(StandardConnectionManager.selector);
	        if (key!=null && key.isValid()){
	            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
	            key.selector().wakeup();
	        }
		}

	}

	public void receive(NewTagByteBuffer bb) {
		if (closed) return;
		for (IConnectionListener listener: listeners)
			listener.receive(this, bb);
		
	}

	public int sendData(SocketChannel localChannel) throws IOException {
		int notWrite = ~SelectionKey.OP_WRITE;
		int totalNumBytesWritten = 0;
		NewTagByteBuffer buf = null;
		boolean returnToPool = true;
		try {
		synchronized(key){
			key = channel.keyFor(StandardConnectionManager.selector);
			if (!key.isValid()) return -1;
			synchronized(pendingToSend){
				if (closed) return -1;
				if (pendingToSend.size()==0){
					if (DEBUG){
						System.err.println("sendData: Nothing to send!"+this);
					}
					key.interestOps((key.interestOps()|SelectionKey.OP_READ)&notWrite);
					return 0;
				}
//				totalNumBytesWritten = 0;

				buf = pendingToSend.removeFirst();
				if (SEND_DEBUG){
					if (buf.getBuffer().limit()==4){
						int oldPos = buf.getBuffer().position();
						buf.getBuffer().position(0);
						int t = buf.getBuffer().getInt();
						if (t<=0 || t>StandardConnectionManager.MAX_MESSAGE_SIZE){
							System.err.println("Wtf!");
							Statistics.getInstance().reportError(new RuntimeException("Bad size!"));
							close();
							return -1;
						}
//						System.out.println("Sending size: "+t+" to "+endPoint.getNotionalAddress());
						buf.getBuffer().position(oldPos);
					}
//					byte temp[] = new byte[12<=buf.getBuffer().limit()?12:buf.getBuffer().limit()];
//					buf.getBuffer().get(temp);
//					buf.getBuffer().position(0);
//					System.out.println(Arrays.toString(temp));
				}
				int numBytesWritten = localChannel.write(buf.getBuffer());
				totalNumBytesWritten+=numBytesWritten;
				while (buf.getBuffer().remaining()==0){
					
					if (DEBUG){
						numPacketsSent++;
						System.err.println("sendData: Packet # " + numPacketsSent + " done!");
					}
					if (pendingToSend.size()>0){
						if (METERED_SEND) break;
						if (buf!=null) {							
							StandardConnectionManager.getInstance().returnToPool(buf);
							returnToPool = false;
							buf = null;
						}
						buf = pendingToSend.removeFirst();
						returnToPool = true;
						numBytesWritten = localChannel.write(buf.getBuffer());
						totalNumBytesWritten+=numBytesWritten;
					}
					else {
						break;
					}
				}
				if (buf!=null){
					if (buf.getBuffer().remaining()>0){
						pendingToSend.addFirst(buf);
						returnToPool = false;
					} else{
						StandardConnectionManager.getInstance().returnToPool(buf);
						returnToPool = false;
					}
				}
				
				synchronized(key){
					if (pendingToSend.size() == 0) {
						if (DEBUG){
							System.err.println("sendData: No more packets to send!");
						}
						key.interestOps((key.interestOps()|SelectionKey.OP_READ)&notWrite);
					}
					else {
						if (DEBUG) System.err.println("sendData: Still more to write...");
						key.interestOps((key.interestOps()|SelectionKey.OP_READ)|SelectionKey.OP_WRITE);
					}
				}
			}
		}
		} finally {
			if (buf!=null && returnToPool) StandardConnectionManager.getInstance().returnToPool(buf);
		}
		if (DEBUG) System.err.println("sendData: Wrote "+ totalNumBytesWritten + " bytes...");
		return totalNumBytesWritten;
		
	}

	private NewTagByteBuffer getPacketHeader(NewTagByteBuffer buf) {
		int length = buf.getBuffer().limit();
		buf.setDescription("Socket " + sock);
		if (length <0 || length > StandardConnectionManager.MAX_MESSAGE_SIZE) {
			StandardConnectionManager.getInstance().returnToPool(buf);
			throw new RuntimeException("Invalid size!");
		}
		NewTagByteBuffer lengthBuffer = StandardConnectionManager.getInstance().getByteBuffer(4);
		lengthBuffer.setDescription("Socket " + sock);
		lengthBuffer.getBuffer().putInt(length);
		lengthBuffer.getBuffer().flip();
		return lengthBuffer;
	}

	public void setChannel(SocketChannel sChannel) {
		channel = sChannel;
		if (channel!=null) endPoint = new StandardEndpoint(channel, (InetSocketAddress)channel.socket().getRemoteSocketAddress());
		
	}
	
	public void acceptFailed(){
		for (IConnectionListener listener : listeners){
			listener.failed(this, new Error("Accept failed!"));
		}
	}

	public void registerConnected() {
		for (IConnectionListener listener : listeners){
			listener.connected(this);
		}
		
	}

    /**
     * @param key2
     */
    public void setSelectionKey(SelectionKey key) {
        this.key = key;
        
    }
    
    public void notifyFailed(Error e){
    	synchronized (pendingToSend){
    		closed = true;
			for (NewTagByteBuffer bb : pendingToSend){
				StandardConnectionManager.getInstance().returnToPool(bb);
			}
			pendingToSend.clear();
		}
    	
    	synchronized(key){key.cancel();}
    	for (IConnectionListener listener : listeners){
			listener.failed(this, e);
		}

    }

	@Override
	protected void finalize() throws Throwable {
		synchronized (pendingToSend){
			for (NewTagByteBuffer bb : pendingToSend){
				StandardConnectionManager.getInstance().returnToPool(bb);
			}
			pendingToSend.clear();
		}
	}

}
