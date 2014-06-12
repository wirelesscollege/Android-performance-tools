package com.ucweb.tools.service;

import java.util.concurrent.ExecutorService;

import com.ucweb.tools.activity.OperationActivity.MonitorType;
import com.ucweb.tools.monitorTask.BatterMonitor;
import com.ucweb.tools.monitorTask.CpuMemMonitor;
import com.ucweb.tools.monitorTask.IOWMonitor;
import com.ucweb.tools.monitorTask.MonitorProxy;
import com.ucweb.tools.monitorTask.NetMonitor;
import com.ucweb.tools.utils.UcwebThreadPoolsManager;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


public class MonitorService extends Service{
	
	private final UcwebThreadPoolsManager manager = UcwebThreadPoolsManager.getThreadPoolManager();
	private final ExecutorService executor = manager.getExecutorService();
	
	private final String[] cmds = {"top", "-m", "5", "-n", "1"};
	
	private MonitorProxy mProxy;
			
	@Override
	public void onCreate(){
		super.onCreate();
		
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		super.onStartCommand(intent, flags, startId);
		return START_NOT_STICKY;
	}
	
	@Override
	public void onStart(Intent intent, int startId) {	
		final int flag = intent.getIntExtra("flag", -1);
		final String pkgName = intent.getStringExtra("pkgName");
		final String fileSavePath = intent.getStringExtra("file path");
		
		switch (flag) {	
		
		case MonitorType.MONITOR_TYPE_CPUMEM:
			CpuMemMonitor cpuMemMonitor = new CpuMemMonitor.Builder(getApplicationContext()).
														setMonitorIntervalSeconds(5).
														setMonitorPkg(pkgName).
														setFileSavePath(fileSavePath).
														build();
			mProxy = new MonitorProxy(cpuMemMonitor);
			executor.execute(new Runnable() {
				
				@Override
				public void run() {
					mProxy.start();
				}
			});
			break;
		
		case MonitorType.MONITOR_TYPE_BATTER:
			BatterMonitor bm = new BatterMonitor(getApplicationContext(), fileSavePath, pkgName);
			mProxy = new MonitorProxy(bm);
			mProxy.start();
			break;
			
		case MonitorType.MONITOR_TYPE_IOW:
			IOWMonitor iowMonitor = new IOWMonitor(getApplicationContext(), fileSavePath, pkgName, cmds);
			mProxy = new MonitorProxy(iowMonitor);
			executor.execute(new Runnable() {
				
				@Override
				public void run() {
					mProxy.start();
				}
			});
			break;
			
		case MonitorType.MONITOR_TYPE_NET:
			NetMonitor netMonitor = new NetMonitor(getApplicationContext(), fileSavePath, pkgName);
			mProxy = new MonitorProxy(netMonitor);
			executor.execute(new Runnable() {
				
				@Override
				public void run() {
					mProxy.start();					
				}
			});
			break;
			
		default:
			break;
		}

		super.onStart(intent, startId);	
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	 
	@Override
	public void onDestroy(){
		releaseResource();
		super.onDestroy();
	}
	
	private void releaseResource(){
		mProxy.stop();
	}

}
