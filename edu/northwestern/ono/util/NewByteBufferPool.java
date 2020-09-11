/**
 * Ono Project
 *
 * File:         NewByteBufferPool.java
 * RCS:          $Id: NewByteBufferPool.java,v 1.9 2008/01/14 13:13:59 drc915 Exp $
 * Description:  NewByteBufferPool class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Dec 25, 2007 at 7:41:44 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.util
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
package edu.northwestern.ono.util;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The NewByteBufferPool class ...
 */
public class NewByteBufferPool {

	private static final int BUFFER_SIZE = 32 * 1024;
	public static final int MAX_SIZE = 8*1024*1024;
	
	private static Map<Long, NewTagByteBuffer> usedBuffers;
	private static Map<Long, NewTagByteBuffer> unusedBuffers;
	
	private static int currentSize = 0;
	private static int totalTaken;
	
	public static void init(){
		usedBuffers = new HashMap<Long, NewTagByteBuffer>();
		unusedBuffers = new HashMap<Long, NewTagByteBuffer>();
		currentSize = 0;
		totalTaken = 0;
	}
	
	public static NewTagByteBuffer take(){
		NewTagByteBuffer bb = null;
		synchronized (unusedBuffers){
			if (unusedBuffers.size()>0){
				bb = unusedBuffers.remove(
						unusedBuffers.keySet().iterator().next());
				
			}
		}
		
		if (bb!=null){
			if (bb.inUse) throw new RuntimeException("Already in use!");
			bb.setUse(true);
			initializeBuffer(bb);
			synchronized (usedBuffers){
				usedBuffers.put(bb.getId(), bb);
			}
			return bb;
		}
		
		synchronized (usedBuffers){
		if (currentSize > MAX_SIZE) {
			
				for (Entry<Long, NewTagByteBuffer> ent : usedBuffers.entrySet()){
					System.out.println(ent.getValue().lastUseElement());
				}
			
			throw new RuntimeException("Out of capacity!");
		}
		}
		
		ByteBuffer temp = null;
		boolean direct = false;
		try {
			temp = ByteBuffer.allocateDirect(BUFFER_SIZE);
			direct = true;
		} catch (Exception e){
			try{
				temp = ByteBuffer.allocate(BUFFER_SIZE);
			} catch (Exception e2){
				return null;
			}
		}
		
		if (temp!=null){
			// update total taken
			
			bb = new NewTagByteBuffer(totalTaken);
			bb.setBuffer(temp, direct);
			synchronized(usedBuffers){
				currentSize+=BUFFER_SIZE;
				totalTaken++;
				usedBuffers.put(bb.getId(), bb);
			}
			bb.setUse(true);
		}
		return bb;
	}
	
	public static NewTagByteBuffer initializeBuffer(NewTagByteBuffer bb){
	
		bb.getBuffer().position(0);
		bb.getBuffer().limit(bb.getBuffer().capacity());
		
		return bb;
	}
	
	public static void put(NewTagByteBuffer bb){
		bb.setUse(false);
		synchronized(usedBuffers){usedBuffers.remove(bb.getId());}
		synchronized (unusedBuffers){
			unusedBuffers.put(bb.getId(), bb);
		}
	}

	public static boolean hasBuffersInUse() {
		return usedBuffers.size()>1;
	}

	public static void printBuffers() {
		for (NewTagByteBuffer bb : usedBuffers.values()) System.out.println(bb.lastUseElement());
		
	}
	
}
