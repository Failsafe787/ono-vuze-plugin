/**
 * Ono Project
 *
 * File:         OnoPluginDetails.java
 * RCS:          $Id: OnoPluginDetails.java,v 1.5 2007/09/15 13:40:04 drc915 Exp $
 * Description:  OnoPluginDetails class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jun 27, 2006 at 4:31:33 PM
 * Language:     Java
 * Package:      edu.northwestern.ono.update
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
package edu.northwestern.ono.update;

import org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetails;
import org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetailsException;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The OnoPluginDetails class ...
 */
public class OnoPluginDetails implements SFPluginDetails {
    String id;
    String name;
    String category;
    String comment;
    String cvsVersion;
    String downloadURL;
    String author;
    String description;
    String version;
    String cvsDownloadURL;
    private final String baseUrl = "http://aqualab.cs.northwestern.edu/azplugin/plugins/";
    private boolean fully_loaded;

    /**
     * @param string5
     * @param string4
     * @param string3
     * @param string2
     * @param string
     *
     */
    public OnoPluginDetails(String _id, String _version, String _cvs_version,
        String _name, String _category) {
        id = _id;
        version = _version;
        cvsVersion = _cvs_version;
        name = _name;
        category = _category;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetails#getId()
     */
    public String getId() {
        // TODO Auto-generated method stub
        return id;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetails#getName()
     */
    public String getName() {
        // TODO Auto-generated method stub
        return name;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetails#getCategory()
     */
    public String getCategory() {
        // TODO Auto-generated method stub
        return category;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetails#getVersion()
     */
    public String getVersion() {
        // TODO Auto-generated method stub
        return version;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetails#getDownloadURL()
     */
    public String getDownloadURL() throws SFPluginDetailsException {
        // TODO Auto-generated method stub
        return downloadURL;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetails#getAuthor()
     */
    public String getAuthor() throws SFPluginDetailsException {
        // TODO Auto-generated method stub
        return author;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetails#getCVSVersion()
     */
    public String getCVSVersion() throws SFPluginDetailsException {
        // TODO Auto-generated method stub
        return cvsVersion;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetails#getCVSDownloadURL()
     */
    public String getCVSDownloadURL() throws SFPluginDetailsException {
        // TODO Auto-generated method stub
        return cvsDownloadURL;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetails#getDescription()
     */
    public String getDescription() throws SFPluginDetailsException {
        // TODO Auto-generated method stub
        return description;
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetails#getComment()
     */
    public String getComment() throws SFPluginDetailsException {
        // TODO Auto-generated method stub
        return comment;
    }

    public void setDetails(String _download_url, String _author,
        String _cvs_download_url, String _desc, String _comment) {
        fully_loaded = true;

        downloadURL = _download_url;
        author = _author;
        cvsDownloadURL = _cvs_download_url;
        description = _desc;
        comment = _comment;
    }

	public String getInfoURL() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getRelativeURLBase() {
		// TODO Auto-generated method stub
		return null;
	}

}
