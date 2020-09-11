/**
 * Ono Project
 *
 * File:         TaggedByteBuffer.java
 * RCS:          $Id: TaggedByteBuffer.java,v 1.2 2010/03/29 16:48:04 drc915 Exp $
 * Description:  TaggedByteBuffer class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Oct 22, 2007 at 10:19:11 AM
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
 * The TaggedByteBuffer class provides a field that allows hashing to work 
 * with all byte buffers.
 */
public class TaggedByteBuffer {
	
	ByteBuffer bb;
	
	public TaggedByteBuffer(ByteBuffer bb){
		this.bb = bb;		
	}
	
	public ByteBuffer getRawBuffer(){
		return bb;
	}

}
