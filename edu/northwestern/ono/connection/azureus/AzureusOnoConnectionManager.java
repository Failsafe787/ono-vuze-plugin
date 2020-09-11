/**
 * Ono Project
 *
 * File:         AzureusOnoConnection.java
 * RCS:          $Id: AzureusOnoConnectionManager.java,v 1.11 2010/03/29 16:48:04 drc915 Exp $
 * Description:  AzureusOnoConnection class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 26, 2007 at 9:51:23 AM
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
package edu.northwestern.ono.connection.azureus;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.MessageManager;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnection;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageEndpoint;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageRegistration;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;

import edu.northwestern.ono.connection.IConnectionHandler;
import edu.northwestern.ono.connection.IConnectionListener;
import edu.northwestern.ono.connection.IOnoConnection;
import edu.northwestern.ono.connection.IOnoConnectionManager;
import edu.northwestern.ono.util.NewTagByteBuffer;
import edu.northwestern.ono.util.Pair;
import edu.northwestern.ono.util.PluginInterface;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The AzureusOnoConnection class ...
 */
public class AzureusOnoConnectionManager implements IOnoConnectionManager {
    PluginInterface pi;
    private GenericMessageRegistration gmr;
    private IConnectionListener connectionListener;
    private IConnectionHandler connectionHandler;

    /** Map of ip address -> connections ... */
    private HashMap<InetSocketAddress, GenericMessageConnection> connections;
    private HashMap<InetSocketAddress, AzureusConnection> wrappedConnectionMap;
    private LinkedList<Pair<ByteBuffer, PooledByteBuffer>> cachedByteBuffers;

    /** set of pending connections */
    private HashSet<InetSocketAddress> connecting;

    public AzureusOnoConnectionManager(PluginInterface pi_az) {
        pi = pi_az;
        connections = new HashMap<InetSocketAddress, GenericMessageConnection>();
        connecting = new HashSet<InetSocketAddress>();
        wrappedConnectionMap = new HashMap<InetSocketAddress, AzureusConnection>();
        cachedByteBuffers = new LinkedList<Pair<ByteBuffer, PooledByteBuffer>>();
    }

    /* (non-Javadoc)
     * @see edu.northwestern.ono.connection.IOnoConnection#send(byte[], java.lang.String)
     */
    public void send(byte[] bytes, String ipAddress) {
        PooledByteBuffer pbb = pi.getUtilities().allocatePooledByteBuffer(1);
        ByteBuffer bb = pbb.toByteBuffer();
        bb.put(bytes);
        bb.position(0);

        GenericMessageConnection con = getConnection(ipAddress);

        if (con == null) {
            return;
        }

        try {
            con.send(pbb);

            System.out.println("Sending peer request to " +
                con.getEndpoint().getNotionalAddress() + "...");
        } catch (MessageException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return;
    }

    private GenericMessageConnection getConnection(String dest) {
        for (GenericMessageConnection c : connections.values()) {
            if (getEndpointIpAddress(c).equals(dest)) {
                return c;
            }
        }

        return null;
    }

    /**
     * @param connection
     * @return
     */
    private InetAddress getEndpointInetAddress(
        GenericMessageConnection connection) {
        return connection.getEndpoint().getNotionalAddress().getAddress();
    }

    /**
     * @param connection
     * @return
     */
    private String getEndpointIpAddress(GenericMessageConnection connection) {
        String endpoint = getEndpointInetAddress(connection).toString();

        return endpoint.substring(endpoint.indexOf('/') + 1);
    }

    public void registerManagerListener(String name, String description,
        IConnectionHandler manager) {
        try {
            gmr = pi.getMessageManager()
                    .registerGenericMessageType(name, description,
                    MessageManager.STREAM_ENCRYPTION_NONE,
                    new AzureusConnectionHandler(manager, this));
        } catch (MessageException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Adds the direct connection at the destination (conn => to source)
     * @param sock
     * @param connection
     * @param ac
     */
    public void addDirectConnection(InetSocketAddress sock,
        GenericMessageConnection connection, AzureusConnection ac) {
        connections.put(sock, connection);
        wrappedConnectionMap.put(sock, ac);
    }

    public void connect(InetSocketAddress endPoint, IConnectionListener runner,
        boolean useTCP) {
    	
    	// connection is already established and valid
    	if (connections.containsKey(endPoint)) return;
    	// connection is being established
    	if (connecting.contains(endPoint)) return;
    	
        GenericMessageEndpoint gme = gmr.createEndpoint(endPoint);

        if (useTCP) {
            gme.addTCP(endPoint);
        } else {
            gme.addUDP(endPoint);
        }

        // wait before making connection
        //Thread.sleep(60*1000);
        GenericMessageConnection con;

        try {
            con = gmr.createConnection(gme);
            
            // register w/ framework
            AzureusConnection ac = new AzureusConnection(con, this, true);
    		addDirectConnection(endPoint,
    	            con, ac);    		

            con.addListener(new AzureusConnectionListener(runner, this));
            connecting.add(endPoint);
            con.connect();
        } catch (MessageException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public AzureusConnection cacheConnection(
        GenericMessageConnection connection) {
        InetSocketAddress sock = connection.getEndpoint().getNotionalAddress();
        AzureusConnection wrapped;

        if (connecting.contains(sock)) {
            connections.put(sock, connection); // store the connection
            connecting.remove(sock);
            wrapped = new AzureusConnection(connection, this, true);
        } else {
            wrapped = new AzureusConnection(connection, this, false);
        }

        wrappedConnectionMap.put(sock, wrapped);

        return wrapped;
    }

    public PooledByteBuffer getPooledByteBuffer(ByteBuffer message) {
    	PooledByteBuffer pbb = removePooledByteBuffer(message);
    	if (pbb!=null) return pbb;

//    	if (cachedByteBuffers.containsKey(message)) return cachedByteBuffers.get(message);
//    	else{
    	
    		throw new RuntimeException("Could not find mapped byte buffer");
//	    	PooledByteBuffer pbb = 
//	    		pi.getUtilities().allocatePooledByteBuffer(message.array());   	
//	        message.position(0);
//	        
//	        addPooledByteBuffer(message, pbb);
//	
//	        return pbb;
//    	}
    }

    public IOnoConnection getOnoConnection(GenericMessageConnection connection) {
        // TODO Auto-generated method stub
    	IOnoConnection con = null;
    	con = wrappedConnectionMap.get(connection.getEndpoint()
                .getNotionalAddress());
    	if (con==null){
    		System.err.println("Null connection!");
    	}
    	
        return con;
    }

    public void addPooledByteBuffer(ByteBuffer bb, PooledByteBuffer message) {
    	synchronized(cachedByteBuffers){
    		cachedByteBuffers.add(new Pair<ByteBuffer, PooledByteBuffer>(bb, message));
    	}
    }

    public void returnToPool(NewTagByteBuffer bb) {
        throw new RuntimeException("Not implemented!");
    	//removePooledByteBuffer(bb).returnToPool();
        
    }

    private PooledByteBuffer removePooledByteBuffer(ByteBuffer bb) {
    	synchronized(cachedByteBuffers){
    		Iterator<Pair<ByteBuffer, PooledByteBuffer>> it = cachedByteBuffers.iterator();
    		Pair<ByteBuffer, PooledByteBuffer> key = null;
    		PooledByteBuffer pbb = null;
    		while (it.hasNext()){
    			key = it.next();
    			if (key.getKey() == bb) {
    				it.remove();
    				pbb = key.getValue();
    				break;
    			}    			
    		}
    		return pbb;
    	}
    	
    }

	/**
     * Called when a connection fails.
     * @param connection
     * @param error
     */
    public void failed(GenericMessageConnection connection, Throwable error) {
        System.err.println("Generic message connection error from " +
            connection.getEndpoint().getNotionalAddress() + ":");

        InetSocketAddress sock = connection.getEndpoint().getNotionalAddress();
        error.printStackTrace();

        connections.remove(sock);
        wrappedConnectionMap.remove(sock);

        try {
            connection.close();
        } catch (MessageException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (connecting != null) {
            connecting.remove(sock.getAddress());
        }
    }

	public NewTagByteBuffer getByteBuffer(int size) {
		throw new RuntimeException("Not implemented!");
//		if (size <=0){
//			System.err.println("Wtf!");
//		}
//		PooledByteBuffer pbb =pi.getUtilities()
//        .allocatePooledByteBuffer(size);
//		ByteBuffer bb = pbb.toByteBuffer();
//		synchronized(cachedByteBuffers){
//			cachedByteBuffers.add(new Pair<ByteBuffer, PooledByteBuffer>(bb, pbb));
//		}
//		return bb;
		
	}

	public boolean hasConnection(InetSocketAddress sock) {
		return connections.containsKey(sock);
	}

	public Map<InetSocketAddress, IOnoConnection> getActiveConnections() {
		// TODO Auto-generated method stub
		HashMap<InetSocketAddress, IOnoConnection> toReturn = new HashMap<InetSocketAddress, IOnoConnection>();
		for (Map.Entry<InetSocketAddress, AzureusConnection> ac : wrappedConnectionMap.entrySet()){
			toReturn.put(ac.getKey(), ac.getValue());
		}
		return toReturn;
	}

	public IOnoConnection getConnection(InetSocketAddress dest) {
		return wrappedConnectionMap.get(dest);
	}

	public void removeConnection(AzureusConnection connection) {
		InetSocketAddress sock = connection.getEndpoint().getNotionalAddress();
		connections.remove(sock);
		wrappedConnectionMap.remove(sock);
		
	}

    /* (non-Javadoc)
     * @see edu.northwestern.ono.connection.IOnoConnectionManager#getConnection(java.net.InetAddress)
     */
    public IOnoConnection getConnection(InetAddress dest) {
        for (InetSocketAddress sock : wrappedConnectionMap.keySet()){
            if (sock.getAddress().equals(dest)){
                return wrappedConnectionMap.get(sock);
            }
        }
        return null;
    }
}
