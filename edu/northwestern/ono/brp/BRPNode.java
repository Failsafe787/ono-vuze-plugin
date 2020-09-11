package edu.northwestern.ono.brp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import edu.northwestern.ono.util.Util;

/**
 * The BRPNode class does not contain methods that are explicitly thread-safe. Any accesses to it
 * should be made so at that level (e.g., in BRPTrie).
 */
public class BRPNode {
	private static final int MAX_DEPTH = 15;
	private BRPNode parent;
	private HashMap<String, BRPNode> childrenMap;
	private boolean isLeaf; // Quick way to check if any children exist
	private boolean isRoute; // Does this node represent the last ip of a route?
	private String ip; // The ip this node represents
	private float weight; // Weight
	private static int depth_counter = 0;

	public BRPNode() {
		childrenMap = new HashMap<String, BRPNode>();
		isLeaf = true;
		isRoute = false;
	}

	public BRPNode(String ip) {
		this();
		this.ip = ip;
	}

	protected void addTRoute(ArrayList<String> TraceRoute) {
		if (BRPNode.depth_counter >= BRPNode.MAX_DEPTH) {
			BRPNode.depth_counter = 0;
			return;
		} else {
			BRPNode.depth_counter++;
		}
		
		this.isLeaf = false;
		String ipPos = "";
		
		if (!TraceRoute.get(0).equals("something")) {
			String ipParts[] = TraceRoute.get(0).split("\\.");
			for (int i = 0; i < 3; ++i) {
				ipPos += ipParts[i] + ".";
			}
		} else {
			ipPos = "something";
		}
		
		if (this.ip.equals(ipPos)) {
			// No need for repeated /24s.
			BRPNode.depth_counter--;
			this.addTRoute(new ArrayList<String>(TraceRoute.subList(1, TraceRoute.size())));
		} else {
			if (!childrenMap.containsKey(ipPos)) {
				childrenMap.put(ipPos, new BRPNode(ipPos));
				childrenMap.get(ipPos).parent = this;
				weight = 1;
			}

			if (TraceRoute.size() > 1) {
				childrenMap.get(ipPos).addTRoute(
						new ArrayList<String>(TraceRoute.subList(1, TraceRoute.size())));
			} else {
				childrenMap.get(ipPos).isRoute = true;
			}
		}

		/*
		 * Without this, giving the "self" (root) node a second child will cause
		 * its weight to become < 1, an effect which cascades and repeats until
		 * the tree is all zeros.
		 */
//		if (this.ip.equals("self")) {
//			switch (BRPPeerManager.getInstance().BRPVersion) {
//			case 1:
//				updateAllWeightsV1(1.0F);
//				break;
//			case 2:
//				updateAllWeightsV2(1.0F);
//				break;
//			}
//		} else {
//			if (childrenMap.size() > 0) {
//				switch (BRPPeerManager.getInstance().BRPVersion) {
//				case 1:	
//					updateAllWeightsV1(weight / childrenMap.size());
//					break;
//				case 2:
//					updateAllWeightsV2(weight);
//					break;
//				}
//			}
//		}
	}

	protected BRPNode getNode(String s) {
		if (childrenMap.containsKey(s))
			return childrenMap.get(s);
		return null;
	}

	protected ArrayList<String> getRoutes() {
		ArrayList<String> list = new ArrayList<String>();

		if (isRoute)
			list.add(toRoute());

		if (!isLeaf) {
			for (String i : childrenMap.keySet()) {
				if (childrenMap.containsKey(i)) {
					list.addAll(childrenMap.get(i).getRoutes());
				}
			}

		}
		return list;
	}

	protected HashMap<String, Double> getBranchPoints(float B, byte version) {
		HashMap<String, Double> BPSet = new HashMap<String, Double>();

		if (this.ip.equals("self")) {
			if (version==BRPPeerManager.BRP_DIST_WT)
				updateAllWeightsV1(1.0F);
			else if (version==BRPPeerManager.BRP_HIER_WT)
				updateAllWeightsV2(1.0F);
			
		} else {
			if (childrenMap.size() > 0) {
				if (version==BRPPeerManager.BRP_DIST_WT)
					updateAllWeightsV1(weight / childrenMap.size());
				else if (version==BRPPeerManager.BRP_HIER_WT)
					updateAllWeightsV2(weight);
			}
		}
		
		if (weight >= B) {
			if (!ip.equals("self") && !ip.equals("something")) {
				// When there's a direct chain of hops, this adds only the first.
				if (!this.parent.ip.equals("self") && this.weight == this.parent.weight) {
					assert(this.childrenMap.size() < 2);
				} else {
					BPSet.put(ip, (double)weight);
				}
			}
			for (String i : childrenMap.keySet()) {
				if (childrenMap.containsKey(i)) {
					BPSet.putAll(childrenMap.get(i).getBranchPoints(B, version));
				}
			}
		}
		return BPSet;
	}

	protected String getBranchTree(int depth) {
		String ret = new String();
		if (!this.ip.equals("self")) {
			for (int i = 0; i < depth; ++i) {
				ret += "\t";
			}
			ret += this.ip + ": " + Float.toString(this.weight) + "\n";
		} else {
			depth = -1;
		}

		for (String i : childrenMap.keySet()) {
			ret += childrenMap.get(i).getBranchTree(depth + 1);
		}
		return ret;
	}

	public String toRoute() {
		if (parent == null)
			return "";
		else
			return parent.toRoute() + " - " + ip;
	}

	public void printAllRoutes(int indent) {
		for (int i = 0; i < indent; i++)
			System.out.print("  ");

		System.out.println(ip + ", wt: " + weight + "\n");

		if (!isRoute) {
			for (BRPNode n : childrenMap.values()) {
				n.printAllRoutes(indent + 4);

			}
		}
	}

	void updateAllWeightsV1(float toWeight) {
		weight = toWeight;

		if (!isLeaf) {
			for (BRPNode n : childrenMap.values()) {
				n.updateAllWeightsV1(weight / childrenMap.size());
			}
		}
	}
	
	void updateAllWeightsV2(float toWeight) {
		if (this.ip.equals("self") || 
				this.parent.ip.equals("self") ||
				this.ip.equals("something")) {
			weight = toWeight;
		} else {
			weight = toWeight / 2.0F;
		}

		if (!isLeaf) {
			for (BRPNode n : childrenMap.values()) {
				n.updateAllWeightsV2(weight);
			}
		}
	}

	/**
	 * @return the number of nodes in the subtrie of a node, including itself.
	 */
	public int getSubtrieSize() {
		int retval = 1;
		for (BRPNode n : this.childrenMap.values()) {
			retval += n.getSubtrieSize();
		}
		return retval;
	}

	/**
	 * @return the depth of the deepest descendant node below this one. The depth of a single node
	 * is 1.
	 */
	public int getSubtrieDepth() {
		int retval = 1;
		int candidate;
		for (BRPNode n : this.childrenMap.values()) {
			candidate = n.getSubtrieDepth() + 1;
			if (candidate > retval) {
				retval = candidate;
			}
		}
		return retval;
	}
	
	public void serialize(ByteArrayOutputStream baos) throws IOException {
		baos.write(this.ip.length()); //length of the ip string
		baos.write(Util.convertStringToBytes(this.ip));
		
		if (this.isLeaf) {
			baos.write(1);
		} else {
			baos.write(0);
		}
		
		if (this.isRoute) {
			baos.write(1);
		} else {
			baos.write(0);
		}
		
		baos.write(childrenMap.size());
		
		for (BRPNode n : childrenMap.values()) {
			n.serialize(baos);
		}
	}
	
	public void deserialize(ByteBuffer bBuffer) throws IOException {
		byte nameLength;
		nameLength = bBuffer.get();
		byte[] inputBuffer = new byte[nameLength];
		bBuffer.get(inputBuffer, 0, nameLength);
		this.ip = Util.convertByteToString(inputBuffer);
		
		inputBuffer[0] = bBuffer.get();
		if (inputBuffer[0] == 0) {
			this.isLeaf = false;
		} else {
			this.isLeaf = true;
		}
		
		inputBuffer[0] = bBuffer.get();
		if (inputBuffer[0] == 0) {
			this.isRoute = false;
		} else {
			this.isRoute = true;
		}
		
		byte numChildren = bBuffer.get();
		for (int i = 0; i < numChildren; ++i) {
			BRPNode newKid = new BRPNode();
			newKid.deserialize(bBuffer);
			newKid.parent = this;
			this.childrenMap.put(newKid.ip, newKid);
		}
	}
}
