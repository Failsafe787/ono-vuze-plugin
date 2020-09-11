/**
 * Ono Project
 *
 * File:         IMessageException.java
 * RCS:          $Id: IMessageException.java,v 1.3 2007/02/24 05:19:43 drc915 Exp $
 * Description:  IMessageException class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jan 26, 2007 at 10:15:27 AM
 * Language:     Java
 * Package:      edu.northwestern.ono.connection
 * Status:       Experimental (Do Not Distribute)
 *
 * (C) Copyright 2007, Northwestern University, all rights reserved.
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
package edu.northwestern.ono.connection;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The IMessageException class wraps a generic exception that occurred during message handling.
 */
public class IMessageException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4521961228167156563L;
	Exception e;
	
	public IMessageException(Exception e) {
		this.e = e;
	}


	/* (non-Javadoc)
	 * @see java.lang.Throwable#getCause()
	 */
	@Override
	public Throwable getCause() {
		// TODO Auto-generated method stub
		return e.getCause();
	}

	/* (non-Javadoc)
	 * @see java.lang.Throwable#getLocalizedMessage()
	 */
	@Override
	public String getLocalizedMessage() {
		// TODO Auto-generated method stub
		return e.getLocalizedMessage();
	}

	/* (non-Javadoc)
	 * @see java.lang.Throwable#getMessage()
	 */
	@Override
	public String getMessage() {
		// TODO Auto-generated method stub
		return e.getMessage();
	}

	/* (non-Javadoc)
	 * @see java.lang.Throwable#getStackTrace()
	 */
	@Override
	public StackTraceElement[] getStackTrace() {
		// TODO Auto-generated method stub
		return e.getStackTrace();
	}

	/* (non-Javadoc)
	 * @see java.lang.Throwable#initCause(java.lang.Throwable)
	 */
	@Override
	public synchronized Throwable initCause(Throwable cause) {
		// TODO Auto-generated method stub
		return e.initCause(cause);
	}

	/* (non-Javadoc)
	 * @see java.lang.Throwable#printStackTrace()
	 */
	@Override
	public void printStackTrace() {
		// TODO Auto-generated method stub
		e.printStackTrace();
	}

	/* (non-Javadoc)
	 * @see java.lang.Throwable#printStackTrace(java.io.PrintStream)
	 */
	@Override
	public void printStackTrace(PrintStream s) {
		// TODO Auto-generated method stub
		e.printStackTrace(s);
	}

	/* (non-Javadoc)
	 * @see java.lang.Throwable#printStackTrace(java.io.PrintWriter)
	 */
	@Override
	public void printStackTrace(PrintWriter s) {
		// TODO Auto-generated method stub
		e.printStackTrace(s);
	}

	/* (non-Javadoc)
	 * @see java.lang.Throwable#setStackTrace(java.lang.StackTraceElement[])
	 */
	@Override
	public void setStackTrace(StackTraceElement[] stackTrace) {
		// TODO Auto-generated method stub
		e.setStackTrace(stackTrace);
	}

	/* (non-Javadoc)
	 * @see java.lang.Throwable#toString()
	 */
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "IMessageException: " + e.toString();
	}

	
	

}
