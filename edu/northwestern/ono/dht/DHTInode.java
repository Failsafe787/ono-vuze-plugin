/**
 * Ono Project
 *
 * File:         DHTInode.java
 * RCS:          $Id: DHTInode.java,v 1.3 2010/03/29 16:48:03 drc915 Exp $
 * Description:  DHTInode class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 7, 2006 at 1:38:21 PM
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
package edu.northwestern.ono.dht;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The DHTInode class implements an inode structure over the Azureus DHT.
 */
public class DHTInode implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public static final int currentVersion = 1;
    private static final int MAX_KEYS = 5;
    public static final int TYPE_DIR = 1;
    public static final int TYPE_DATA = 2;
    private static final int MAX_STRINGS = 10;
    public final int versionID = currentVersion;

    /** these values are used to generate a DDB key, but are not actually the hash value */
    public String[] keys;
    public int size = 0;
    public int type;
    public String[] values;
    public int valueSize = 0;

    /**
     *
     */
    public DHTInode() {
        super();

        //        keys = new DistributedDatabaseKey[MAX_KEYS];
        // TODO Auto-generated constructor stub
    }

    /**
     *
     * @param versionID the inode version
     */
    public DHTInode(int type) {
        if (type == TYPE_DIR) {
            keys = new String[MAX_KEYS];
        } else if (type == TYPE_DATA) {
            values = new String[MAX_STRINGS];
        } else {
            throw new RuntimeException("Invalid type!");
        }

        this.type = type;
    }

    public int getSize() {
        return size;
    }

    public boolean addKey(String key) {
        if (size < MAX_KEYS) {
            keys[size++] = key;

            return true;
        } else {
            return false;
        }
    }

    /**
     * @return Returns the versionID.
     */
    public int getVersionID() {
        return versionID;
    }

    public boolean addValue(String s) {
        if (valueSize < values.length) {
            values[valueSize++] = s;

            return true;
        } else {
            return false;
        }
    }

    public int getValueSize() {
        return valueSize;
    }

    public int getType() {
        return type;
    }

    /**
     * @return
     */
    public String[] getValues() {
        return values;
    }

    /**
     * @return Returns the keys.
     */
    public String[] getKeys() {
        return keys;
    }

    /**
     * @param keys The keys to set.
     */
    public void setKeys(String[] keys) {
        this.keys = keys;
    }

    /**
     * @param size The size to set.
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * @param type The type to set.
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * @param values The values to set.
     */
    public void setValues(String[] values) {
        this.values = values;
    }

    /**
     * @param valueSize The valueSize to set.
     */
    public void setValueSize(int valueSize) {
        this.valueSize = valueSize;
    }

    public static DHTInode readInode(ObjectInputStream in) {
        DHTInode inode = null;

        try {
            if (in.readShort() != currentVersion) {
                throw new RuntimeException("Wrong version!");
            }

            inode = new DHTInode(in.readShort()); // sets type

            if (inode.type == TYPE_DATA) {
                inode.valueSize = in.readShort();
                inode.values = (String[]) in.readObject();
            } else if (inode.type == TYPE_DIR) {
                inode.size = in.readShort();
                inode.keys = (String[]) in.readObject();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

            return null;
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return inode;
    }

    public void writeToStream(ObjectOutputStream out) {
        try {
            out.writeShort(versionID);
            out.writeShort(type);

            if (type == TYPE_DATA) {
                out.writeShort(valueSize);
                out.writeObject(values);
            } else if (type == TYPE_DIR) {
                out.writeShort(size);
                out.writeObject(keys);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
