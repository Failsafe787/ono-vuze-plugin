/**
 * Ono Project
 *
 * File:         DHTInodeTest.java
 * RCS:          $Id: DHTInodeTest.java,v 1.4 2010/03/29 16:48:04 drc915 Exp $
 * Description:  DHTInodeTest class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 7, 2006 at 1:51:05 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.test
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
package edu.northwestern.ono.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashSet;

import edu.northwestern.ono.dht.IDistributedDatabase;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The DHTInodeTest class tests the DHTInode class.
 */
public class DHTInodeTest {
    ByteArrayOutputStream baos;
    ByteArrayInputStream bais;
    ObjectOutputStream oos;
    ObjectInputStream ois;
    LinkedHashSet<String> newList;
    String[] output;
    IDistributedDatabase ddb;
}
