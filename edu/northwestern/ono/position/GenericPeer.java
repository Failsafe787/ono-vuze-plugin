package edu.northwestern.ono.position;


/* 
 * Peer is a generic class allowing edu.northwestern.ono.position.PeerFinderGlobal to easily manage
 * suggestions from different blocks of code that internally represent peers with their own classes.
 */

public abstract class GenericPeer {
	public static final byte GENERIC_PEER = 0;
	public static final byte ONO_PEER = 1;
	public static final byte BRP_PEER = 2;
	public byte peer_type = GENERIC_PEER;
	public String ip;
	public int port;
	public short protocol = 1;
	public byte source;
	
	public GenericPeer(String ip, int port) {
		this.peer_type = GENERIC_PEER;
		this.ip = ip;
		this.port = port;
		this.protocol = 1;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		
		if (obj instanceof GenericPeer) {
			return ((GenericPeer)obj).ip.equals(this.ip);
		} else {
			return false;
		}
	}
	
	public void copy(GenericPeer peer) {
		this.ip = peer.getIp();
		this.port = peer.getPort();
		this.protocol = peer.getProtocol();
		this.source = peer.source;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public String getIp() {
		return this.ip;
	}
	
	public short getProtocol() {
		return this.protocol;
	}
	
	public short getSource() {
		return this.source;
	}

	public boolean isOnoPeer() {
		return (this.peer_type == ONO_PEER ? true : false);
	}
	
	public boolean isBRPPeer() {
		return (this.peer_type == BRP_PEER ? true : false);
	}

	public abstract String printCosSim(boolean strongOnly);
	
	public abstract String getRelativeDownloadPerformance();
	
	public abstract String getRelativeUploadPerformance();
	
	public abstract String getToolTipText(String text);
}
