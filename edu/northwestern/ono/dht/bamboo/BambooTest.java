/**
 * Ono Project
 *
 * File:         BambooTest.java
 * RCS:          $Id: BambooTest.java,v 1.3 2010/03/29 16:48:04 drc915 Exp $
 * Description:  BambooTest class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 31, 2007 at 5:43:17 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.dht.bamboo
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
package edu.northwestern.ono.dht.bamboo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import edu.northwestern.ono.MainPlanetLab;
import edu.northwestern.ono.dht.IDDBDeleteAction;
import edu.northwestern.ono.dht.IDDBReadAction;
import edu.northwestern.ono.dht.IDDBWriteAction;
import edu.northwestern.ono.dht.IDistributedDatabase;
import edu.northwestern.ono.dht.IDistributedDatabaseEvent;
import edu.northwestern.ono.util.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The BambooTest class tests the functionality of the Bamboo DHT wrapper.
 * 
 */
public class BambooTest {
	
	public static void main(String args[]){
		
		IDistributedDatabase ddb = MainPlanetLab.getDistributedDatabase();
		
		
		
		ArrayList<ByteArrayOutputStream> values = new ArrayList<ByteArrayOutputStream>();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
			baos.write(Util.convertStringToBytes("Prime time?"));
		
        values.add(baos);
		
		ddb.write("somekey", values, 24*60*60, new IDDBWriteAction(){

			public void handleComplete(IDistributedDatabaseEvent event) {
				System.out.println("Write complete!");
				
			}

			public void handleTimeout(IDistributedDatabaseEvent event) {
				System.out.println("Write timed out!");
				
			}

			public void handleWrite(IDistributedDatabaseEvent event) {
				System.out.println("Write registered!");
				
			}

			});
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        try {
			Thread.sleep(10*1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		ddb.read("somekey", "somekey", 30, new IDDBReadAction(){

			public void handleComplete(IDistributedDatabaseEvent event) {
				System.out.println("Read value: complete!");
				
			}

			public void handleRead(byte[] b, IDistributedDatabaseEvent event) {
				System.out.println("Read value:" + Util.convertByteToString(b));
				
			}

			public void handleTimeout(IDistributedDatabaseEvent event) {
				System.out.println("Read value: timeout!");
				
			}}, 0);

		
		// test remove
		ddb.delete("somekey", 30, new IDDBDeleteAction(){

			public void handleComplete(IDistributedDatabaseEvent event) {
				System.out.println("Value deleted!");
				
			}

			public void handleTimeout(IDistributedDatabaseEvent event) {
				// TODO Auto-generated method stub
				
			}}, null);
		
        try {
			Thread.sleep(30*1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ddb.read("somekey", "somekey", 30, new IDDBReadAction(){

			public void handleComplete(IDistributedDatabaseEvent event) {
				System.out.println("Read value: complete!");
				
			}

			public void handleRead(byte[] b, IDistributedDatabaseEvent event) {
				System.out.println("Read value:" + Util.convertByteToString(b));
				
			}

			public void handleTimeout(IDistributedDatabaseEvent event) {
				System.out.println("Read value: timeout!");
				
			}}, 0);
		
        try {
			Thread.sleep(10*1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
