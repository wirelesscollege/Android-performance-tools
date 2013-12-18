package com.ucweb.tools.monitorTask;

public class MonitorProxy{
	private final AbstractMonitor mMonitor;
	
	public MonitorProxy(AbstractMonitor monitor){
		mMonitor = monitor;
	}

	public void start(){
		mMonitor.startMonitor();
	}
	
	public void stop(){
		mMonitor.stopMonitor();
	}
}
