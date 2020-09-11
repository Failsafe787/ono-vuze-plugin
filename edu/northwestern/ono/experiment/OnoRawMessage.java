/**
 * Ono Project
 *
 * File:         OnoRawMessage.java
 * RCS:          $Id: OnoRawMessage.java,v 1.3 2010/03/29 16:48:04 drc915 Exp $
 * Description:  OnoRawMessage class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 15, 2006 at 11:46:49 AM
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

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.network.RawMessage;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The OnoRawMessage class ...
 */
public class OnoRawMessage implements RawMessage {
    Message m;
    DirectByteBuffer[] raw;

    /**
     *
     */
    public OnoRawMessage(Message m, DirectByteBuffer[] raw) {
        super();
        this.m = m;
        this.raw = raw;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.network.RawMessage#getRawPayload()
     */
    public ByteBuffer[] getRawPayload() {
        ByteBuffer[] out = new ByteBuffer[raw.length];

        for (int i = 0; i < out.length; i++)
            out[i] = raw[i].getBuffer(DirectByteBuffer.SS_MSG);

        return out;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.network.RawMessage#getOriginalMessage()
     */
    public Message getOriginalMessage() {
        // TODO Auto-generated method stub
        return m;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.messaging.Message#getID()
     */
    public String getID() {
        // TODO Auto-generated method stub
        return m.getID();
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.messaging.Message#getType()
     */
    public int getType() {
        // TODO Auto-generated method stub
        return m.getType();
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.messaging.Message#getDescription()
     */
    public String getDescription() {
        // TODO Auto-generated method stub
        return m.getDescription();
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.messaging.Message#getPayload()
     */
    public ByteBuffer[] getPayload() {
        // TODO Auto-generated method stub
        return m.getPayload();
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.messaging.Message#create(java.nio.ByteBuffer)
     */
    public Message create(ByteBuffer data) throws MessageException {
        // TODO Auto-generated method stub
        return m.create(data);
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.plugins.messaging.Message#destroy()
     */
    public void destroy() {
        m.destroy();

        for (DirectByteBuffer dbb : raw)
            dbb.returnToPool();
    }
}
