package com.ucweb.tools.monitorTask;

public class MonitorProxy{
	private final Monitorable mMonitor;
	
	public MonitorProxy(Monitorable monitor){
		mMonitor = monitor;
	}

	public void start(){
		mMonitor.startMonitor();
	}
	
	public void stop(){
		mMonitor.stopMonitor();
	}
}
