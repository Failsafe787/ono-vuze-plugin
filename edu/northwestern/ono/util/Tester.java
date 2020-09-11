package edu.northwestern.ono.util;


public class Tester {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		long ip = 0;
		long ip2 =  0xCCCCCCCC;
		long mask = 0xFFFFFFFFL;		
		ip = mask & ip2;
		
		System.out.println(ip2+"\n"+ip);
		
//		
//		InetAddress ip1, ip2;
//		try {
//			ip1 = InetAddress.getByName("165.186.124.59");		
//			ip2 = InetAddress.getByName("165.186.124.60");
//
//		
//			byte ip1b[] = ip1.getAddress();
//			byte ip2b[] = ip2.getAddress();
//			boolean ret = ip1b[3]==ip2b[3] && ip1b[1]==ip2b[1] && ip1b[2]==ip2b[2];
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

}
