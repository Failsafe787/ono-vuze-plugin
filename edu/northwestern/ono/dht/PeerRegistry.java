/**
 * Ono Project
 *
 * File:         PeerRegistry.java
 * RCS:          $Id: PeerRegistry.java,v 1.7 2010/03/29 16:48:03 drc915 Exp $
 * Description:  PeerRegistry class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 7, 2006 at 1:37:47 PM
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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import org.gudy.azureus2.plugins.PluginInterface;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.dht.azureus.DHTManager;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The PeerRegistry class manages the list of peers using the plugin.
 */
public class PeerRegistry extends Thread implements IDDBWriteAction,
    IDDBReadAction {
    private static final String registryKey = "OnoPeerPluginRegistry";

    /** time between checking when ip address is present */
    private static long longSleepTime = 60 * 60 * 1000;
    private DHTManager manager;
    private LinkedList<DHTInode> inodeStack;
    private LinkedList<Integer> inodeIndexes;

    /** data pending to be written to DDB -- maps edge server IPs to list of node IPs */
    private LinkedHashMap<String, DHTInode> pendingWrites;
    LinkedHashSet<String> newList;
    DHTInode output;
    ByteArrayOutputStream baos;
    ByteArrayInputStream bais;
    ObjectOutputStream oos;
    ObjectInputStream ois;
    private PluginInterface pi;

    /** my ip address */
    private String myIp;
    private boolean isActive = true;

    /** sleep time between checks for ip address (default 30 secs) */
    private long sleepTime = 30 * 1000;

    /**
     *
     */
    public PeerRegistry(DHTManager manager, PluginInterface pi) {
        super("PeerRegistry");
        this.manager = manager;
        this.pi = pi;
        inodeStack = new LinkedList<DHTInode>();
        inodeIndexes = new LinkedList<Integer>();
        pendingWrites = new LinkedHashMap<String, DHTInode>();
        this.myIp = (MainGeneric.getPublicIpAddress() != null)
            ? MainGeneric.getPublicIpAddress() : null;
    }

    /* (non-Javadoc)
     * @see edu.northwestern.ono.dht.IDHTReadAction#handleRead(java.lang.String)
     */
    public void handleRead(byte[] b, IDistributedDatabaseEvent event) {
        newList = new LinkedHashSet<String>();

        System.out.println("Peer Registry::Value read: " + b);

        byte[] bytes = b;
        bais = new ByteArrayInputStream(bytes);

//        try {
            DHTInode output = decodeInode(bais);

            //Object o = ois.readObject();
            //if ((o instanceof DHTInode)){
            //     output = (DHTInode) o;
            System.out.println("Peer Registry::Value read from DB: " + "(" +
                event.getKey().getDescription() + ", " + output + ")");

            // quit if using old version
            if (output.getVersionID() < DHTInode.currentVersion) {
                return;
            }

            // look for my ip
            if (output.getType() == DHTInode.TYPE_DATA) {
                for (String ip : output.getValues()) {
                    if (ip.equals(myIp)) {
                        sleepTime = longSleepTime;

                        return;
                    }
                }
            }

            // TODO deal with nested lookups over directory inodes (use stack)

            // my ip isn't there, so add it               
            output.addValue(myIp);

            //}
            // else throw new RuntimeException("Peer Registry::Invalid data item!");

            // if a write is in progress, leave here
//            if (manager.isWriteInProgress(event)) {
//                return;
//            }

            baos = encodeInode();

            System.out.println("Peer Registry::Writing (" +
                baos.toString().length() + "): " + baos.toString());
            ArrayList<ByteArrayOutputStream> values = new ArrayList<ByteArrayOutputStream>();
            values.add(baos);
            MainGeneric.getDistributedDatabase().write((String)event.getKey().getKey(), values, 30, this);
//        }
        //        catch (IOException e) {
        //            // TODO Auto-generated catch block
        //            e.printStackTrace();
        //        } catch (ClassNotFoundException e) {
        // TODO Auto-generated catch block
        //  e.printStackTrace();
        //} 
//        catch (DistributedDatabaseException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }

    /* (non-Javadoc)
     * @see edu.northwestern.ono.dht.IDHTWriteAction#handleWrite(org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent)
     */
    public void handleWrite(IDistributedDatabaseEvent event) {
        pendingWrites.put(event.getKey().getDescription(), null);
    }

    /* (non-Javadoc)
     * @see edu.northwestern.ono.dht.IDHTReadAction#handleTimeout(org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent)
     */
    public void handleTimeout(IDistributedDatabaseEvent event) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        while (isActive) {
            if (myIp != null) {
                DHTInode currentPendingWrites = pendingWrites.get(registryKey);

                if (currentPendingWrites == null) {
                    currentPendingWrites = new DHTInode(DHTInode.TYPE_DATA);
                    pendingWrites.put(registryKey, currentPendingWrites);
                }

                currentPendingWrites.addValue(myIp);

                MainGeneric.getDistributedDatabase()
                .read(registryKey, registryKey, 30, this,
              		  MainGeneric.getDistributedDatabase().getOptions().getExhaustiveRead());

            } else if (MainGeneric.getPublicIpAddress() != null) {
                myIp = MainGeneric.getPublicIpAddress();
            }

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /* (non-Javadoc)
     * @see edu.northwestern.ono.dht.IDHTReadAction#handleComplete(org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent)
     */
    public void handleComplete(IDistributedDatabaseEvent event) {
        if (event.getValue() == null) {
            output = new DHTInode(DHTInode.TYPE_DATA);

//            try {
                // add new entries
                output.addValue(myIp);

                // create output and write
                //        output = new String[newList.size()];
                //        output = newList.toArray(output);
                //        System.out.println("Trying to write "+Arrays.deepToString(output));
                baos = encodeInode();

                System.out.println("Peer Registry::Writing (" +
                    baos.toString().length() + "): " + baos.toString());

                ArrayList<ByteArrayOutputStream> values = new ArrayList<ByteArrayOutputStream>();
                values.add(baos);
                MainGeneric.getDistributedDatabase().write(
                		(String)event.getKey().getKey(), values, 30, this);
//            } catch (DistributedDatabaseException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
        }
    }

    private ByteArrayOutputStream encodeInode() {
        ByteArrayOutputStream baos = null;
        byte[] bytes;

        try {
            while (baos == null /*|| baos.toString().length()>250*/) { // TODO may have to be half this
                                                                       //                  
                                                                       //                  if (baos!=null){
                                                                       //                  // length too long
                                                                       //                  newList.remove(output[1]);
                                                                       //                  }
                                                                       //                  output = new String[newList.size()];
                                                                       //                  output = newList.toArray(output);
                baos = new ByteArrayOutputStream();

                BufferedOutputStream bos = new BufferedOutputStream(baos);

                oos = new ObjectOutputStream(baos);

                output.writeToStream(oos);
                oos.flush();

                /*bytes = baos.toByteArray();
                baos = new ByteArrayOutputStream();
                bos = new BufferedOutputStream(baos);
                ZipOutputStream zos = new ZipOutputStream(bos);
                zos.setLevel(Deflater.BEST_COMPRESSION);
                zos.putNextEntry(new ZipEntry("inode"));
                zos.write(bytes);
                zos.closeEntry();
                zos.flush();*/

                //oos.writeObject(output);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return baos;
    }

    private DHTInode decodeInode(ByteArrayInputStream in) {
        //ZipInputStream zis = new ZipInputStream(in);
        try {
            //zis.getNextEntry();

            // byte bytes[] = new byte[1000];
            // int length = zis.read(bytes, 0, 1000);

            // bais = new ByteArrayInputStream(bytes, 0, length);
            ois = new ObjectInputStream(in);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

            return null;
        }

        return DHTInode.readInode(ois);
    }


}
