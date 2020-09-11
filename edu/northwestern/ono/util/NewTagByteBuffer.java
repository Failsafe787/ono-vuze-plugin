/**
 * Ono Project
 *
 * File:         NewTagByteBuffer.java
 * RCS:          $Id: NewTagByteBuffer.java,v 1.9 2010/03/29 16:48:04 drc915 Exp $
 * Description:  NewTagByteBuffer class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Dec 25, 2007 at 5:23:27 PM
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

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The NewTagByteBuffer class ...
 */
public class NewTagByteBuffer {

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NewTagByteBuffer){
			return ((NewTagByteBuffer)obj).id == id;
		}
		return false;
	}


	private static final boolean DEBUG = true;
	public long id;
	private ByteBuffer buffer;
	boolean isDirect;
	boolean inUse = false;
	private String stackTrace;
	private StackTraceElement lastAccessStackTrace[];
	private String description; // supposed to be used for describing last access
	
	public NewTagByteBuffer(long l){
		id = l;
	}
	
	public void setBuffer(ByteBuffer bb, boolean direct){
		if (inUse) throw new RuntimeException("Buffer in use!");
		buffer = bb;
		this.isDirect = direct;
	}
	
	public ByteBuffer getBuffer(){
		if (!inUse){
			throw new RuntimeException("Buffer not in use!" + "\nStack trace: \n" + stackTrace);
		}
		if (DEBUG) lastAccessStackTrace = Thread.currentThread().getStackTrace();
		return buffer;
	}
	
	public void setUse(boolean use){
		if (inUse == use){
			System.err.println("Use already set to "+use + "... is this " +
					"what you really want?");
		}
		inUse = use;
		if (DEBUG && use == false){
			stackTrace = getStackTrace();
		}
	}

	private String getStackTrace(){
		String st = "";
		for (StackTraceElement e : Thread.currentThread().getStackTrace()){
			st += e.toString() + "\n";
		}
		return st;
	}
	
	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		super.finalize();
	}

	public Long getId() {
		return id;
	}

	public static NewTagByteBuffer wrap(byte[] bytesToSend) {
		NewTagByteBuffer ntbb = NewByteBufferPool.take();
		if (bytesToSend.length > ntbb.getBuffer().capacity()){
			throw new RuntimeException("Too much data! Buffer limit: " + ntbb.getBuffer().capacity() 
					+ ", size to send: " + bytesToSend);
			
		}
		ntbb.getBuffer().limit(bytesToSend.length);
		ntbb.getBuffer().put(bytesToSend);
		return ntbb;
	}

	public void print() {
		int position = buffer.position();
		System.out.print("[");
		while (buffer.remaining()>1) System.out.print(buffer.get()+", ");
		System.out.println(buffer.get() +"]");
		buffer.position(position);
	}


	public String lastUseElement() {
		if (DEBUG && lastAccessStackTrace!=null) return description + "\n" +(lastAccessStackTrace[2].toString()+"\n"+lastAccessStackTrace[3].toString()+"\n");
		return "";
	}

	public void setDescription(String desc){
		description = desc;
	}
}
