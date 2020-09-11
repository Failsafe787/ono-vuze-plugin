/**
 * Ono Project
 *
 * File:         StandardClientTest.java
 * RCS:          $Id: StandardClientTest.java,v 1.7 2010/03/29 16:48:04 drc915 Exp $
 * Description:  StandardClientTest class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Feb 1, 2007 at 11:00:41 AM
 * Language:     Java
 * Package:      edu.northwestern.ono.connection.standard
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

import java.net.InetSocketAddress;

import edu.northwestern.ono.MainPlanetLab;
import edu.northwestern.ono.connection.IConnectionListener;
import edu.northwestern.ono.connection.IMessageException;
import edu.northwestern.ono.connection.IOnoConnection;
import edu.northwestern.ono.connection.IOnoConnectionManager;
import edu.northwestern.ono.util.NewTagByteBuffer;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The StandardClientTest class ...
 */
public class StandardClientTest {
	private static IOnoConnectionManager manager;
	static String ipAddress = "165.124.182.135";
	static int port = StandardConnectionManager.STANDARD_PORT;
	static boolean done = false;
	private static IConnectionListener listener = new IConnectionListener(){

		public void connected(IOnoConnection connection) {
			System.out.println("Connected!");
			NewTagByteBuffer bb = manager.getByteBuffer(5);
			bb.getBuffer().put(Util.convertStringToBytes("Pong!"));
			bb.getBuffer().position(0);
			connection.send(bb);
			
		}

		public void failed(IOnoConnection connection, Throwable error) throws IMessageException {
			System.out.println("Connection failed!");
			done = true;
			
		}

		public void receive(IOnoConnection connection, NewTagByteBuffer message) throws IMessageException {
			System.out.println("Got message!");
            byte temp[] = new byte[message.getBuffer().limit()];
            message.getBuffer().get(temp);
			System.out.println("Other side says: " + Util.convertByteToString(temp));
			manager.returnToPool(message);
            
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			NewTagByteBuffer bb = manager.getByteBuffer(5);
			bb.getBuffer().put(Util.convertStringToBytes("Pong!"));
			bb.getBuffer().position(0);
			connection.send(bb);
           
            //connection.close();
            //done = true;
			
		}};
	
	public static void main (String args[]){
		 manager = MainPlanetLab.getConnectionManager();
		
         System.out.println("Trying to connect...");
		manager.connect(new InetSocketAddress(ipAddress, port), listener , true);
		
		while (!done){
			try {
				Thread.sleep(10*1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
}
