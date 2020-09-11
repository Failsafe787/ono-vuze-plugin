/**
 * Ono Project
 *
 * File:         PingExperimentMessage.java
 * RCS:          $Id: OnoRatioMessage.java,v 1.9 2010/03/29 16:48:04 drc915 Exp $
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
package edu.northwestern.ono.messaging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.messaging.MessageException;

import edu.northwestern.ono.MainGeneric;
import edu.northwestern.ono.OnoConfiguration;
import edu.northwestern.ono.brp.BRPPeerManager;
import edu.northwestern.ono.position.CDNClusterFinder;
import edu.northwestern.ono.position.OnoPeerManager;
import edu.northwestern.ono.stats.EdgeServerRatio;


public class OnoRatioMessage implements Message {
    public static final String ID = "ONO_RATIO_MESSAGE";
    Map<String, EdgeServerRatio> edgeServerIps;
    int type = 1;
    String description = "ONO_RATIO_MESSAGE";
    HashMap<Byte, HashMap<String, Double>> branchPoints;

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
            oos.writeObject(MainGeneric.getPublicIpAddress());
            Map<String, EdgeServerRatio> ratios = OnoPeerManager.getInstance().getMyRatios();
            oos.writeObject(ratios);
            if (OnoConfiguration.getInstance().enableBRP == true) {
            	for (byte b : BRPPeerManager.getInstance().supportedVersions){
            		oos.writeByte(b);
            		HashMap<String, Double> branch_points =
            		BRPPeerManager.getInstance().getMyBranchPoints(
            				BRPPeerManager.BP_WEIGHT_THRESHOLD, b);

            		oos.writeObject(branch_points);
            	}
            }
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
	@SuppressWarnings("unchecked")
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
            OnoRatioMessage pem = new OnoRatioMessage();
            ois = new ObjectInputStream(bais);
            String peerIp = (String)ois.readObject();
            pem.edgeServerIps = (Map<String, EdgeServerRatio>) ois.readObject();
            if (!MainGeneric.isShuttingDown()) {
            	CDNClusterFinder.getInstance().processMessage(pem, peerIp);
            	if (OnoConfiguration.getInstance().enableBRP == true) {
            		pem.branchPoints = new HashMap<Byte, HashMap<String, Double>>();
            		try {
            			while (true){
            			byte version = ois.readByte();
            			pem.branchPoints.put(version, (HashMap<String, Double>)ois.readObject());
            			}
            			            		}
            		catch (java.io.EOFException e) {
            			// TODO(Ted): Check instead of just catching the exception.
            		}
            		BRPPeerManager.getInstance().processMessage(pem, peerIp);

            	}
            }

            return pem;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e){
        	// don't do anything
        }

        return new OnoRatioMessage();
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.messaging.Message#destroy()
     */
    public void destroy() {
        // TODO do i have any work to do here?
    }

	public Map<String, EdgeServerRatio> getRatios() {
		return edgeServerIps;
	}

	public HashMap<String, Double> getBranchPoints(byte version) {
		return this.branchPoints.get(version);
	}

}
