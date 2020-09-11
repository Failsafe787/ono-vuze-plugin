package edu.northwestern.ono.brp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class BRPTrie {
	private static final int MAX_TREE_SIZE = 700;
	private BRPNode root;
	private int traceRoutesProcessed = 0;
	private boolean maxSizeReached = false;

	public BRPTrie()
	{
		root = new BRPNode("self");
	}


	public void addRoute(ArrayList<String> TraceRoute) {
		if (!maxSizeReached) {
			synchronized (this.root) {
				root.addTRoute(TraceRoute);	
			}
			
			traceRoutesProcessed++;
			if (traceRoutesProcessed % 20 == 0) {
				if (this.getTrieSize() > MAX_TREE_SIZE) {
					maxSizeReached = true;
				}
			}
		}
	}


	public ArrayList<String> getRoutes(ArrayList<String> ipList) {
		synchronized (this.root) {
			BRPNode lastNode = root;
			for (String i: ipList) {
				lastNode = lastNode.getNode(i);
				if (lastNode == null)
					return new ArrayList<String>();
			}
			return lastNode.getRoutes();
		}
	}


	public void printRoutes() {
		synchronized (this.root) {
			root.printAllRoutes(0);
		}
	}

	// B: weight threshold
	public HashMap<String, Double> getBranchPoints(float B, byte version) {
		synchronized (this.root) {
			return root.getBranchPoints(B, version);
		}
	}
	
	
	public String getBranchTree() {
		synchronized (this.root) {
			return root.getBranchTree(0);
		}
	}
	
	public int getTrieSize() {
		synchronized (this.root) {
			return root.getSubtrieSize();
		}
	}
	
	public int getTrieDepth() {
		synchronized (this.root){ 
			return root.getSubtrieDepth();
		}
	}
	
	public void serialize(ByteArrayOutputStream baos) throws IOException {
		root.serialize(baos);
	}
	
	public void deserialize(ByteBuffer bBuffer) throws IOException {
		root.deserialize(bBuffer);
	}
}
