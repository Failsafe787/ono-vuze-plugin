/**
 * Ono Project
 *
 * File:         PingExperimentMessage.java
 * RCS:          $Id: PingExperimentMessage.java,v 1.4 2010/03/29 16:48:04 drc915 Exp $
 * Description:  PingExperimentMessage class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 15, 2006 at 9:27:53 AM
 * Language:     Java
 * Package:      edu.northwestern.ono.experiment
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
package edu.northwestern.ono.experiment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.messaging.MessageException;


public class PingExperimentMessage implements Message {
    public static final String ID = "PING_EXPERIMENT_MESSAGE";
    ArrayList<String> edgeServerIps;
    int type = 1;
    String description = "PING_EXPERIMENT_MESSAGE";

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.messaging.Message#getID()
     */
    public String getID() {
        return ID;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.messaging.Message#getType()
     */
    public int getType() {
        return Message.TYPE_PROTOCOL_PAYLOAD;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.messaging.Message#getDescription()
     */
    public String getDescription() {
        return description;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.messaging.Message#getPayload()
     */
    public ByteBuffer[] getPayload() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;

        try {
            oos = new ObjectOutputStream(baos);

            oos.writeObject(edgeServerIps.toArray(new String[1]));
            oos.flush();

            ByteBuffer bb = ByteBuffer.wrap(baos.toByteArray());

            return new ByteBuffer[] { bb };
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.messaging.Message#create(java.nio.ByteBuffer)
     */
    public Message create(ByteBuffer data) throws MessageException {
        ByteArrayInputStream bais;

        if (data.hasArray()) {
            bais = new ByteArrayInputStream(data.array());
        } else {
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            bais = new ByteArrayInputStream(bytes);
        }

        ObjectInputStream ois;

        try {
            PingExperimentMessage pem = new PingExperimentMessage();
            ois = new ObjectInputStream(bais);

            pem.edgeServerIps = new ArrayList<String>();

            for (String s : (String[]) ois.readObject())
                pem.edgeServerIps.add(s);

            return pem;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.messaging.Message#destroy()
     */
    public void destroy() {
        // TODO do i have any work to do here?
    }

    public void addEdgeIp(String s) {
        if (edgeServerIps == null) {
            edgeServerIps = new ArrayList<String>();
        }

        edgeServerIps.add(s);
    }
}
