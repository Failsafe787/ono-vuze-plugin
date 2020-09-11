/**
 * Ono Project
 *
 * File:         Util.java
 * RCS:          $Id: Util.java,v 1.18 2011/07/06 15:45:53 mas939 Exp $
 * Description:  Util class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 28, 2006 at 11:21:35 AM
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



import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import org.xbill.DNS.DClass;
import org.xbill.DNS.ExtendedFlags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.ReverseMap;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The Util class provides utility functions for the package.
 */
public class Util {
    
	static BufferedReader in;

	private static Util self;
    
    
    public static interface PingResponse {
    	public void response(double rtt);
    }

    
    public static long currentGMTTime(){
    	return System.currentTimeMillis()-TimeZone.getDefault().getRawOffset();
    } 
    
    public synchronized static Util getInstance(){

	    	if (self == null){
	    		self = new Util();
	    	}
    	
    	return self;
    }

   

    /**
     * @param newEdge
     * @return
     */
    public static String getClassCSubnet(String ipaddress) {
        // TODO Auto-generated method stub
        return ipaddress.substring(0, ipaddress.lastIndexOf("."));
    }

	public static byte[] convertLong(long l) {
		byte[] bArray = new byte[8];
        ByteBuffer bBuffer = ByteBuffer.wrap(bArray);
        bBuffer.order(ByteOrder.LITTLE_ENDIAN);
        LongBuffer lBuffer = bBuffer.asLongBuffer();
        lBuffer.put(0, l);
        return bArray;
	}
	
	public static  byte[] convertShort(short s) {
		byte[] bArray = new byte[2];
        ByteBuffer bBuffer = ByteBuffer.wrap(bArray);
        bBuffer.order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer sBuffer = bBuffer.asShortBuffer();
        sBuffer.put(0, s);
        return bArray;
	}
	
	public static  byte[] convertInt(int s) {
		byte[] bArray = new byte[4];
        ByteBuffer bBuffer = ByteBuffer.wrap(bArray);
        bBuffer.order(ByteOrder.LITTLE_ENDIAN);
        IntBuffer sBuffer = bBuffer.asIntBuffer();
        sBuffer.put(0, s);
        return bArray;
	}
	
	public static  byte[] convertFloat(float s) {
		byte[] bArray = new byte[4];
        ByteBuffer bBuffer = ByteBuffer.wrap(bArray);
        bBuffer.order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer sBuffer = bBuffer.asFloatBuffer();
        sBuffer.put(0, s);
        return bArray;
	}
	
	public static long byteToLong(byte data[]){
		ByteBuffer bBuffer = ByteBuffer.wrap(data);
		bBuffer.order(ByteOrder.LITTLE_ENDIAN);
		LongBuffer  lBuffer = bBuffer.asLongBuffer();
		return lBuffer.get();
	}

	public static byte[] convertStringToBytes(String key) {
		try {
			return key.getBytes("ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static String convertByteToString(byte[] value){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write(value);
		
			return baos.toString("ISO-8859-1");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	public static long convertIpToLong(String ipv4) {
		String parts[] = ipv4.split("\\.");
		if (parts.length!=4) return -1;
		long ip = 0;
		long temp;
		for (int i = 0; i < parts.length; i++){
			temp = Integer.parseInt(parts[i]);
			temp <<= (8*(3-i));
			ip+=temp;
		}
		return ip;
	}
	
	public static boolean isV6Address(String dest) {
		try {
			InetAddress addr = InetAddress.getByName(dest);
			if (addr instanceof Inet6Address) return true;
		} catch (UnknownHostException e) {
			// do nothing
		}		
		return false;
	}
	
	public static long getUptime(PluginInterface pi){
		long uptime = -1;
		
		try {
            if (pi==null || pi.getUtilities().isWindows()) {
                Process p;

                p = Runtime.getRuntime().exec("systeminfo");
                
                BufferedReader in = new BufferedReader(new InputStreamReader(
                            p.getInputStream()));

                String line;
                
                int notReadyCount = 0;
                boolean notDone = true;
                int lineCount =0;
                boolean isVistaOrLater = false;
                while (notDone){
                	
                	if ((line = in.readLine()) == null){
                		notDone = false;
                		break;
                	}
                	if (line.trim().length()==0) continue;
                	
                	lineCount++;
                	
                	if (lineCount==3){
                		String parts[] = line.split("\\:");
                		if (parts.length==2 && parts[1].trim().startsWith("6"))
                			isVistaOrLater = true;
                	}
                	
                	if (lineCount==11 && isVistaOrLater ){
                		// this is windows 7
                		String parts[] = line.split("   ");
                		long seconds = 0;
                		if (parts.length>=2){
                			String dateString = parts[parts.length-1].trim();
                			dateString = dateString.replace(",", "");
                			
                			 DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

                			Date d;
							try {
								d = df.parse(dateString);
								
	                			long time = d.getTime();
	                			seconds = (System.currentTimeMillis()-time)/1000;
	                			
	                			if (seconds>0) uptime = seconds;
							} catch (ParseException e) {
 								// TODO Auto-generated catch block
 								e.printStackTrace();
 							}
                			break;
                		}
                	}
                	
                	// get uptime
                	if (lineCount==11) {
                		String parts[] = line.split("\\:");
                		long seconds = 0;
                		if (parts.length==2){
                			parts = parts[1].split(",");
                			int j = 0;
                			for (int i = parts.length-1; i >=0; i--){
                				int temp=0;
                				try {
                					temp = Integer.parseInt(parts[i].trim().split(" ")[0]);
                				} catch (NumberFormatException e){
                					
                				}
                				if (j==0) seconds+=temp;
                				else if (j==1) seconds+=temp*60;
                				else if (j==2) seconds+=temp*60*60;
                				else if (j==3) seconds+=temp*24*60*60;
                				j++;
                			}
                			if (seconds>0) uptime = seconds;
                		}
                		break;
                	}
                	

                }
                // then destroy
                p.destroy();
            } else if (pi.getUtilities().isLinux() || pi.getUtilities().isOSX()) {
                Process p;
                p = Runtime.getRuntime().exec("uptime");

                BufferedReader in = new BufferedReader(new InputStreamReader(
                            p.getInputStream()));
                String line;

                while ((line = in.readLine()) != null) {
                    // find RTT
                    if (line.contains("up")) {
                        try {
                    	String[] vals = line.trim().split("  ");
                        vals = vals[0].split("up");
                        vals = vals[1].split(",");
                        
                        String hourMinute = vals[vals.length-1];
                        String parts[] = hourMinute.trim().split(":");
                        int hour = Integer.parseInt(parts[0]);
                        int minute = Integer.parseInt(parts[1]);
                        int day = 0;
                        if (vals.length>1){
                        	String d = vals[vals.length-2].trim().split(" ")[0];
                        	day = Integer.parseInt(d);
                        } 
                        long seconds = minute*60+hour*3600+day*24*3600;
                        if (seconds>0) uptime = seconds;
                        } catch (NumberFormatException e){
                        	
                        } catch (ArrayIndexOutOfBoundsException e){
                        	
                        }
                        break;
                    }
                }

                // then destroy
                p.destroy();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		
		return uptime;
	}
	
	/**
	 * Does a remote lookup. If the -x option is specified, only authoritative
	 * name servers are returned. Otherwise, an array of resolved IP addresses
	 * is returned
	 * 
	 * @param argv
	 *            the dig command parameterse
	 * @return
	 * @throws IOException
	 */
	public static Record[] doLookup(String[] argv) throws IOException {

		org.xbill.DNS.Name name = null;
		int type = org.xbill.DNS.Type.A;
		int dclass = DClass.IN;
		String server = null;
		int arg;
		Message query;
		Message response = null;
		Record rec;
		SimpleResolver res = null;
		boolean printQuery = false;

		if (argv.length < 1) {
			throw new RuntimeException("Invalid dig format!");
		}

		try {
			arg = 0;

			if (argv[arg].startsWith("@")) {
				server = argv[arg++].substring(1);
				res = new SimpleResolver(server);
			}

			String nameString = argv[arg++];

			if (nameString.equals("-x")) {
				name = ReverseMap.fromAddress(argv[arg++]);
				type = Type.PTR;
				dclass = DClass.IN;
			} else {
				name = Name.fromString(nameString, Name.root);
				type = Type.value(argv[arg]);

				if (type < 0) {
					type = Type.A;
				} else {
					arg++;
				}

				dclass = DClass.value(argv[arg]);

				if (dclass < 0) {
					dclass = DClass.IN;
				} else {
					arg++;
				}
			}

			while (argv[arg].startsWith("-") && (argv[arg].length() > 1)) {
				switch (argv[arg].charAt(1)) {
				case 'p':

					String portStr;
					int port;

					if (argv[arg].length() > 2) {
						portStr = argv[arg].substring(2);
					} else {
						portStr = argv[++arg];
					}

					port = Integer.parseInt(portStr);

					if ((port < 0) || (port > 65536)) {
						//LOGGER.warning("Invalid port");

						return null;
					}

					res.setPort(port);

					break;

				case 'b':

					String addrStr;

					if (argv[arg].length() > 2) {
						addrStr = argv[arg].substring(2);
					} else {
						addrStr = argv[++arg];
					}

					InetAddress addr;

					try {
						addr = InetAddress.getByName(addrStr);
					} catch (Exception e) {
						//LOGGER.warning("Invalid address");

						return null;
					}

					res.setLocalAddress(addr);

					break;

				case 'k':

					String key;

					if (argv[arg].length() > 2) {
						key = argv[arg].substring(2);
					} else {
						key = argv[++arg];
					}

					res.setTSIGKey(TSIG.fromString(key));

					break;

				case 't':
					res.setTCP(true);

					break;

				case 'i':
					res.setIgnoreTruncation(true);

					break;

				case 'e':

					String ednsStr;
					int edns;

					if (argv[arg].length() > 2) {
						ednsStr = argv[arg].substring(2);
					} else {
						ednsStr = argv[++arg];
					}

					edns = Integer.parseInt(ednsStr);

					if ((edns < 0) || (edns > 1)) {
						//LOGGER.info("Unsupported " + "EDNS level: " + edns);

						return null;
					}

					res.setEDNS(edns);

					break;

				case 'd':
					res.setEDNS(0, 0, ExtendedFlags.DO, null);

					break;

				case 'q':
					printQuery = true;

					break;

				default:
					//LOGGER.info("Invalid option: " + argv[arg]);
					System.err.println("Invalid option: " + argv[arg]);
				}

				arg++;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			if (name == null) {
				// log.log("Name is null!");
				throw new RuntimeException("Name is null!");
			}
		} catch (UnknownHostException e) {
			//LOGGER.warning(e.toString());
			System.err.println(e.toString());
		} catch (TextParseException e) {
			//LOGGER.warning(e.toString());
			System.err.println(e.toString());
		}

		if (res == null) {
			try {
				res = new SimpleResolver();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				//LOGGER.warning(e.toString());
				System.err.println(e.toString());
			}
		}

		if (name == null) {
			return null;
		}

		rec = Record.newRecord(name, type, dclass);
		query = Message.newQuery(rec);

		if (printQuery) {
			System.out.println(query);
		}

		response = res.send(query);

		// return only the authority section
		if ((response != null) && argv[0].equals("-x")) {
			if (argv.length == 2)
				return response.getSectionArray(Section.ANSWER);
		}

		// return the actual ip addresses
		if (response != null) {
			return response.getSectionArray(Section.ANSWER);
		}

		return null;
	}
}
