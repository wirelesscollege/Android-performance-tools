package com.ucweb.tools.monitorTask;

public interface Monitorable {
	/**
	 * 开始监控
	 */
	public void startMonitor();
	
	/**
	 * 停止监控
	 */
	public void stopMonitor();
}
