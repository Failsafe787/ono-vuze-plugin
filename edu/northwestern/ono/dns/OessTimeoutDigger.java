/**
 * Ono Project
 *
 * File:         Digger.java
 * RCS:          $Id: OessTimeoutDigger.java,v 1.2 2010/03/29 16:48:04 drc915 Exp $
 * Description:  Digger class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jun 13, 2006 at 8:55:41 AM
 * Language:     Java
 * Package:      edu.northwestern.ono.dns
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
package edu.northwestern.ono.dns;

import java.io.IOException;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;

import edu.northwestern.ono.dht.IDistributedDatabase;
import edu.northwestern.ono.stats.Statistics;
import edu.northwestern.ono.util.ILoggerChannel;
import edu.northwestern.ono.util.PluginInterface;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 * 
 *         The Digger class hunts for Akamai or other CDN entries and reports
 *         them appropriately.
 * 
 *         Note: If DIG_FOR_OTHERS is set to true, you will have to make sure
 *         that you reduce the TCP timeout and increase the number of user ports
 *         available on your OS. For help, see
 *         http://www.minq.se/products/pureload/tuning/connections.html.
 */
public class OessTimeoutDigger extends Thread {
	
	private int sleepInterval = 600;
	private ILoggerChannel log;
	private boolean isActive;
	private Statistics stats;
	private PluginInterface pi;

	private static final boolean DEBUG = false;
	
	public OessTimeoutDigger() {
		super("OessTimeoutDigger");
		
	}
	
	/**
	 * Initialization code.
	 * 
	 * @param sleep
	 * @param l
	 * @param dd
	 * @param pi
	 */
	public void initialize(int sleep, ILoggerChannel l,
			IDistributedDatabase dd, PluginInterface pi) {
		sleepInterval = sleep;
		log = l;
		stats = Statistics.getInstance();
		this.pi = pi;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {

		org.xbill.DNS.Name name = null;
		int type = org.xbill.DNS.Type.A;
		int dclass = DClass.IN;
		boolean again = true;

		Message query;
		Message response = null;
		Record rec;
		SimpleResolver res = null;

		long b4 = System.currentTimeMillis();
		while (again) {
		  try {
			name = Name.fromString("test.ana-aqualab.cs.northwestern.edu", Name.root);
			res = new SimpleResolver("192.168.1.254");
			rec = Record.newRecord(name, type, dclass);
			query = Message.newQuery(rec);
			response = res.send(query);
			again = false;
		  } catch (IOException e1) {
			// TODO Auto-generated catch block
			System.out.println("TIMEOUT: do it again");
			again = true;
			
		  }
		}
		
		long after = System.currentTimeMillis() - b4;
		System.out.println("MARIO: " + Long.toString(after));
		if (true)
			return;
		
	}

	
}
