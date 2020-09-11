package edu.northwestern.ono.util;

import java.util.LinkedList;

public class ASNTester {

	public static void main (String[] args){
		String toTest = "begin\nverbose\n165.124.182.135 2005-12-25 13:23:01 GMT\n68.22.187.5\n207.229.165.18\n122.169.3.119\nend\n";
		LinkedList<Integer> ids = new LinkedList<Integer>();
		ids.add(1);
		ids.add(2);
		ids.add(3);
		ids.add(4);
		//LookupService.fetchAndStoreASNs(toTest, ids, "peerASNs");
	}
}
