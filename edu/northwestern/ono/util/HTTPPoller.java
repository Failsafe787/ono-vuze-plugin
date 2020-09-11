/**
 * Ono Project
 *
 * File:         HTTPPoller.java
 * RCS:          $Id: HTTPPoller.java,v 1.3 2010/03/29 16:48:04 drc915 Exp $
 * Description:  HTTPPoller class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Nov 14, 2006 at 11:43:44 AM
 * Language:     Java
 * Package:      edu.northwestern.ono.util
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
package edu.northwestern.ono.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The HTTPPoller class efficiently polls a URL.
 */
public class HTTPPoller {
	
	private static HashMap<String, Long> modifiedDates = new HashMap<String, Long>();
	private static HashMap<String, String> etags = new HashMap<String, String>();

	/**
	 * Returns true if the url has been modified since this method was last 
	 * called. Returns false otherwise.
	 * @param urlString the url to check
	 * @return
	 */
    public static boolean hasBeenModified(String urlString) {
    	URL url = null;
        HttpURLConnection httpCon = null;
        String etag = etags.get(urlString);
        if (etag == null) etag = " ";
        boolean isNew = false;
		try {
			url = new URL(urlString);
		
			if (!modifiedDates.containsKey(urlString)){
				httpCon = (HttpURLConnection)url.openConnection();
				if(httpCon.getResponseCode() == HttpURLConnection.HTTP_OK) {					 
	        		 modifiedDates.put(urlString, httpCon.getHeaderFieldDate("Last-Modified", 0));
	        		 etags.put(urlString, httpCon.getHeaderField("ETag")); // modifiedDates.get(urlString).toString());
	        		 isNew = true;
	        	}
			}
			else{
        	httpCon = (HttpURLConnection)url.openConnection();
        	//httpCon.setRequestProperty("If-None-Match", etag);
        	httpCon.setIfModifiedSince(modifiedDates.get(urlString));
        	
        	if(httpCon.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
        		isNew = false;
        	}
        	else if(httpCon.getResponseCode() == HttpURLConnection.HTTP_OK) {
        		//etags.put(urlString, httpCon.getHeaderField("ETag"));
        		if (httpCon.getHeaderFieldDate("Last-Modified", 0)==modifiedDates.get(urlString).longValue()){
        			isNew = false;
        		}
        		else{ 
        			modifiedDates.put(urlString,  httpCon.getHeaderFieldDate("Last-Modified", 0));
        			isNew = true;
        		}
        	}
			}
		} catch (MalformedURLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (httpCon !=null){
				httpCon.disconnect();				
			}
		}
		return isNew;
	}
}
