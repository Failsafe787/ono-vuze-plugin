package edu.northwestern.ono.dht.azureus;

import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;

import edu.northwestern.ono.dht.IDDBReadAction;

public class AzureusReadAction implements IDHTReadAction {
	
	IDDBReadAction action;
	
	public AzureusReadAction(IDDBReadAction action){
		this.action = action;
	}

	public void handleComplete(DistributedDatabaseEvent event) {
		action.handleComplete(new AzureusDDBEvent(event));

	}

	public void handleRead(byte[] b, DistributedDatabaseEvent event) {
		action.handleRead(b, new AzureusDDBEvent(event));

	}

	public void handleTimeout(DistributedDatabaseEvent event) {
		action.handleTimeout(new AzureusDDBEvent(event));

	}

}
