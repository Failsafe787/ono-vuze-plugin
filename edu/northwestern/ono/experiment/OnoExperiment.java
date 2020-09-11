/**
 * Ono Project
 *
 * File:         OnoExperiment.java
 * RCS:          $Id: OnoExperiment.java,v 1.22 2010/03/29 16:48:04 drc915 Exp $
 * Description:  OnoExperiment class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 7, 2006 at 1:23:35 PM
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

import java.util.Iterator;
import java.util.Properties;
import java.util.Map.Entry;

import edu.northwestern.ono.connection.IOnoConnection;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The OnoExperiment class defines an Ono experiment.
 */
public class OnoExperiment {
    public static final int CATEGORY_WILD = 1;
    public static final int CATEGORY_PL = 2;
    public static final int CATEGORY_BOTH = 3;
    public static final int EXP_PING = 1;
    public static final int EXP_DL = 2;
    public static final int CURRENT_VERSION = 1;
    private static final boolean DEBUG = true;

    /** experiment file version */
    public int version = CURRENT_VERSION;

    /** do experiment in PL, wild, or both*/
    public int targetCategory = CATEGORY_PL;

    /** time at which experiment begins */
    public long beginTime;

    /** time at which experiment expires */
    public long endTime;

    /** the unique id for this experiment (for DB tracking) */
    public int experimentId = -1;

    /** experiment type */
    public int experimentType = EXP_PING;

    /** the other plugin peer in this experiment */
    public String otherEndIp;

    /** (NOT USED) the port on which to connect (this is random) */
    public int port = 24356;

    /** the size of the file to transfer, in bytes */
    public int size = 1;

    /** the number of times to transfer the file */
    public int numFileTransfers = 1;

    /** the IP address of the source of traffic */
    public String source;

    /** the IP address of the destination of traffic */
    public String dest;

    /** forces delay on a path, generating intervals */
    public boolean forceDelay = false;

    /** determines whether detouring is even attempted */
    public boolean useDetouring = true;

    /** if ture, will caues Ono to pick random peers for detouring */
    public boolean randomPeers = false;
    
    /** the preferred name for clustering nodes */
    public String preferredName = null;
    
    /** time this exp was found */
    public long foundTime = 0;
    
	public Properties props;
	public IOnoConnection sourceCon;

    /**
     * @param version
     * @param targetCategory
     * @param beginTime
     * @param endTime
     * @param experimentId
     * @param experimentType
     */
    public OnoExperiment(int version, int targetCategory, long beginTime,
        long endTime, int experimentId, int experimentType) {
        this.version = version;
        this.targetCategory = targetCategory;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.experimentId = experimentId;
        this.experimentType = experimentType;
    }

    public OnoExperiment() {
    }

    /**
     * @return Returns the beginTime.
     */
    public long getBeginTime() {
        return beginTime;
    }

    /**
     * @param beginTime The beginTime to set.
     */
    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    /**
     * @return Returns the endTime.
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * @param endTime The endTime to set.
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * @return Returns the experimentId.
     */
    public int getExperimentId() {
        return experimentId;
    }

    /**
     * @param experimentId The experimentId to set.
     */
    public void setExperimentId(int experimentId) {
        this.experimentId = experimentId;
    }

    /**
     * @return Returns the experimentType.
     */
    public int getExperimentType() {
        return experimentType;
    }

    /**
     * @param experimentType The experimentType to set.
     */
    public void setExperimentType(int experimentType) {
        this.experimentType = experimentType;
    }

    /**
     * @return Returns the targetCategory.
     */
    public int getTargetCategory() {
        return targetCategory;
    }

    /**
     * @param targetCategory The targetCategory to set.
     */
    public void setTargetCategory(int targetCategory) {
        this.targetCategory = targetCategory;
    }

    /**
     * @return Returns the version.
     */
    public int getVersion() {
        return version;
    }

    /**
     * @param otherEndIp
     */
    public void setOtherEndIp(String otherEndIp) {
        this.otherEndIp = otherEndIp;
    }

    /**
     * @return Returns the port.
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port The port to set.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return Returns the otherEndIp.
     */
    public String getOtherEndIp() {
        return otherEndIp;
    }

    /**
     * @param version The version to set.
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OnoExperiment && (obj != null)) {
            OnoExperiment o = (OnoExperiment) obj;

            if (o.experimentId == experimentId) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static OnoExperiment fromProperties(Properties p, String otherEndIp) {
    	
       
        OnoExperiment result = new OnoExperiment();
        result.foundTime = System.currentTimeMillis();
        result.props = p;
        result.otherEndIp = otherEndIp;

        Iterator it2 = p.entrySet().iterator();

        while (it2.hasNext()) {
            Entry e = (Entry) it2.next();
            String val = (String) e.getValue();
            String k = (String) e.getKey();
            int i = -1;
            if (k.equals("version")) {
                result.version = Integer.parseInt(val);
            } else if (k.equals("targetCategory")) {
                result.targetCategory = Integer.parseInt(val);
            } else if (k.equals("beginTime")) {
                result.beginTime = Long.parseLong(val);
            } else if (k.equals("endTime")) {
                result.endTime = Long.parseLong(val);
            } else if (k.equals("experimentId")) {
                result.experimentId = Integer.parseInt(val);
                if (result.experimentId == -1){
                	result.experimentType = -1;
                }
            } else if (k.equals("experimentType")) {
                result.experimentType = Integer.parseInt(val);
            } else if (k.equals("port")) {
                result.port = Integer.parseInt(val);
            } else if (k.equals("size")) {
                result.size = Integer.parseInt(val);
            } else if (k.equals("numFileTransfers")) {
                result.numFileTransfers = Integer.parseInt(val);
            } else if (k.equals("source")) {
                result.source = val;
            } else if (k.equals("dest")) {
                result.dest = val;
            } else if (k.equals("forceDelay")) {
            	try {
            		i = Integer.parseInt(val);
            		if (i==1) result.forceDelay = true;
            		else result.forceDelay = false;
            	} catch (NumberFormatException e3){
            		result.forceDelay = Boolean.parseBoolean(val);
            	}
            } else if (k.equals("useDetouring")) {
            	try {
            		i = Integer.parseInt(val);
            		if (i==1) result.useDetouring = true;
            		else result.useDetouring = false;
            	} catch (NumberFormatException e3){
            		result.useDetouring = Boolean.parseBoolean(val);
            	}                
            } else if (k.equals("randomPeers")) {
            	try {
            		i = Integer.parseInt(val);
            		if (i==1) result.randomPeers = true;
            		else result.randomPeers = false;
            	} catch (NumberFormatException e3){
            		result.randomPeers = Boolean.parseBoolean(val);
            	}     
            } else if (k.equals("preferredName")){
            	result.preferredName = val;
            }else if (k.equals("otherEndIp")){
            	result.otherEndIp = val;
            } else {
                System.err.println("Property \"" + k + "\" not supported!");
            }
        }

        if (DEBUG) {
            result.printConfiguration();
        }

        return result;
    }

    /**
    *
    */
    private void printConfiguration() {
        // TODO Auto-generated method stub
        System.out.println("Begin time:\t" + beginTime);
        System.out.println("Dest:\t" + dest);
        System.out.println("end time:\t" + endTime);
        System.out.println("Size:\t" + size);
        System.out.println("source:\t" + source);

        //       System.out.println("Begin time:\t"+beginTime);
        //       System.out.println("Begin time:\t"+beginTime);
        //       System.out.println("Begin time:\t"+beginTime);
        //       System.out.println("Begin time:\t"+beginTime);
        //       System.out.println("Begin time:\t"+beginTime);
    }

    /**
         * @return Returns the forceDelay.
         */
    public boolean isForceDelay() {
        return forceDelay;
    }

    /**
     * @param forceDelay The forceDelay to set.
     */
    public void setForceDelay(boolean forceDelay) {
        this.forceDelay = forceDelay;
    }

	@Override
	public String toString() {
		if (props!=null)
			return props.toString();
		else return "";
	}

    /**
     * @return
     */
    public boolean hasExpired() {        
        return System.currentTimeMillis() - foundTime > 4.5*60*1000;
    }
}
