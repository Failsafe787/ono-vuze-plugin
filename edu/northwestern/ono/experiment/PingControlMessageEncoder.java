/**
 * Ono Project
 *
 * File:         PingControlMessageEncoder.java
 * RCS:          $Id: PingControlMessageEncoder.java,v 1.3 2010/03/29 16:48:04 drc915 Exp $
 * Description:  PingControlMessageEncoder class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 15, 2006 at 10:55:01 AM
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

import java.nio.ByteBuffer;
import java.util.HashMap;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.MessageStreamEncoder;
import org.gudy.azureus2.plugins.network.RawMessage;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The PingControlMessageEncoder class ...
 */
public class PingControlMessageEncoder implements MessageStreamEncoder {
    static HashMap<String, MessageTypes> types = new HashMap<String, MessageTypes>();
    static HashMap<Byte, MessageTypes> ids = new HashMap<Byte, MessageTypes>();

    static {
        MessageTypes mt = new MessageTypes("PingExperiment", (byte) 1);
        types.put("PingExperiment", mt);
        ids.put((byte) 1, mt);
    }

    PluginInterface pi;

    /**
     *
     */
    public PingControlMessageEncoder(PluginInterface pi) {
        super();
        this.pi = pi;

        //        try{
        //            
        //        pi.getMessageManager().registerMessageType(new PingExperimentMessage());
        //        } catch(MessageException e){
        //           e.printStackTrace();
        //        }
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.messaging.MessageStreamEncoder#encodeMessage(org.gudy.azureus2.plugins.messaging.Message)
     */
    public RawMessage encodeMessage(Message message) {
        if (message instanceof RawMessage) { //used for handshake and keep-alive messages

            return (RawMessage) message;
        }

        MessageTypes myType = types.get(message.getID());

        if (myType == null) {
            return null;
        }

        ByteBuffer[] bbs = message.getPayload();
        DirectByteBuffer[] payload = new DirectByteBuffer[bbs.length];

        for (int i = 0; i < bbs.length; i++)
            payload[i] = new DirectByteBuffer(bbs[i]);

        int payload_size = 0;

        for (int i = 0; i < payload.length; i++) {
            payload_size += payload[i].remaining(DirectByteBuffer.SS_MSG);
        }

        DirectByteBuffer header = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_EXTERNAL,
                5);
        header.putInt(DirectByteBuffer.SS_MSG, 1 + payload_size);
        header.put(DirectByteBuffer.SS_MSG, myType.byte_id);
        header.flip(DirectByteBuffer.SS_MSG);

        DirectByteBuffer[] raw_buffs = new DirectByteBuffer[payload.length + 1];
        raw_buffs[0] = header;

        for (int i = 0; i < payload.length; i++) {
            raw_buffs[i + 1] = payload[i];
        }

        return new OnoRawMessage(message, raw_buffs);
    }

    public static int getMessageType(DirectByteBuffer stream_payload) {
        byte id = stream_payload.get(DirectByteBuffer.SS_MSG, 0);

        if (id == 84) {
            return Message.TYPE_PROTOCOL_PAYLOAD; //handshake message byte in position 4
        }

        return ids.get(id).getType();
    }

    /**
     * @param ref_buff
     * @return
     */
    public static Message createPCMessage(DirectByteBuffer stream_payload)
        throws MessageException {
        byte id = stream_payload.get(DirectByteBuffer.SS_MSG);

        switch (id) {
        case 1:

            ByteBuffer bb = stream_payload.getBuffer(DirectByteBuffer.SS_MSG);

            return (new PingExperimentMessage()).create(bb);

        default: {
            System.out.println("Unknown PC message id [" + id + "]");
            throw new MessageException("Unknown PC message id [" + id + "]");
        }
        }
    }

    public static class MessageTypes {
        public String id;
        public byte byte_id;

        /**
         * @param string
         * @param b
         */
        public MessageTypes(String string, byte b) {
            this.id = string;
            this.byte_id = b;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (obj instanceof MessageTypes) {
                return ((MessageTypes) obj).id.equals(id);
            } else {
                return false;
            }
        }

        public int getType() {
            return Message.TYPE_PROTOCOL_PAYLOAD;
        }
    }
}
