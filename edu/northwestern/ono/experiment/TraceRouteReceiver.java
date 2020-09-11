package edu.northwestern.ono.experiment;

import edu.northwestern.ono.experiment.TraceRouteRunner.TraceResult;


public interface TraceRouteReceiver {
	public void receiveTraceResult(String ip, TraceResult tr);
}
