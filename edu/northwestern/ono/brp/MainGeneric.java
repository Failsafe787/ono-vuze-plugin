package edu.northwestern.ono.brp;
import java.util.ArrayList;
import java.util.Arrays;


public class MainGeneric {

	public static final void main(String[] args) {
		
		BRPTrie brpTrie = new BRPTrie();
		
		ArrayList<String> tr = new ArrayList<String>(
				Arrays.asList(new String[]{"192.168.0.1",
					"200.10.6.8", "214.11.3.6", "22.3.4.3"}));		
		brpTrie.addRoute(tr);
		
		tr = new ArrayList<String>(
				Arrays.asList(new String[]{"192.168.0.1",
					"1.1.1.1", "2.1.3.6", "3.3.3.3"}));
		brpTrie.addRoute(tr);
		
		tr = new ArrayList<String>(
				Arrays.asList(new String[]{"192.168.0.1",
					"1.1.1.1", "2.1.4.4", "8.8.8.8"}));
		brpTrie.addRoute(tr);


		tr = new ArrayList<String>(
				Arrays.asList(new String[]{"192.168.0.1",
					"200.10.6.8", "2.1.4.4", "8.8.8.8"}));
		brpTrie.addRoute(tr);


		tr = new ArrayList<String>(
				Arrays.asList(new String[]{"192.168.0.1",
					"20.10.6.8", "21.1.4.4", "18.8.8.8"}));
		brpTrie.addRoute(tr);

		tr = new ArrayList<String>(
				Arrays.asList(new String[]{"192.168.0.1",
					"20.14.6.8", "21.1.14.4", "18.81.8.8", "22.22.22.22"}));
		brpTrie.addRoute(tr);
		
		tr = new ArrayList<String>(
				Arrays.asList(new String[]{"192.168.0.1",
					"20.14.6.8", "21.1.14.4", "18.81.8.8", "33.33.33.33"}));
		brpTrie.addRoute(tr);

		
		for ( Object l : brpTrie.getRoutes( new ArrayList<String>(Arrays.asList(new String[]{"192.168.0.1"}) )) ) {
			System.out.println(l);
		}

		System.out.println("\n");
		brpTrie.printRoutes();
		
//		System.out.println(brpTrie.getBranchPoints(0.25F));
		
		
	}
}
