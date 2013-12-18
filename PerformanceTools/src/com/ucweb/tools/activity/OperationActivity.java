package com.ucweb.tools.activity;

import java.util.concurrent.ExecutorService;

import com.ucweb.tools.R;
import com.ucweb.tools.service.MonitorService;
import com.ucweb.tools.utils.UcwebAppUtil;
import com.ucweb.tools.utils.UcwebThreadPoolsManager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class OperationActivity extends Activity{
	
	private final ExecutorService executor = UcwebThreadPoolsManager.getInstance().getExecutorService();
	
	private String pkgName;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.operation_activity);
		
		pkgName = getIntent().getStringExtra("pkgName");
		
		final Button btnCpuMemMonitor = (Button) findViewById(R.id.btnCpuMemMonitor);
		final Button btnBatterMonitor = (Button) findViewById(R.id.btnBatterMonitor);
		final Button btnIOWMonitor = (Button) findViewById(R.id.btnIOMonitor);
		final Button btnNetMonitor = (Button) findViewById(R.id.btnNetMonitor);
		
		btnCpuMemMonitor.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startMonitorService(pkgName, MonitorType.MONITOR_TYPE_CPUMEM);
				
			}
		});
		
		btnBatterMonitor.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startMonitorService(pkgName, MonitorType.MONITOR_TYPE_BATTER);
				
			}
		});
		
		btnIOWMonitor.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startMonitorService(pkgName, MonitorType.MONITOR_TYPE_IOW);
				
			}
		});
		
		btnNetMonitor.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startMonitorService(pkgName, MonitorType.MONITOR_TYPE_NET);
				
			}
		});
		
	}	
	
	public static class MonitorType{
		// monitor cpu and memory
		public final static int MONITOR_TYPE_CPUMEM = 1;
		//monitor batter
		public final static int MONITOR_TYPE_BATTER = 2;
		//monitor io
		public final static int MONITOR_TYPE_IOW = 3;
		//monitor net
		public final static int MONITOR_TYPE_NET = 4;
	}
	
	private void startMonitorService(final String pkgName, final int typeFlag){
		executor.execute(new Runnable() {
			
			@Override
			public void run() {					
				UcwebAppUtil apputil = new UcwebAppUtil(OperationActivity.this);
				//blocking thread, until test app is running
				apputil.startAppAndGetPid(pkgName);				
				
				Intent intent = new Intent();
				intent.setClass(OperationActivity.this, MonitorService.class);
				intent.putExtra("pkgName", pkgName);
				intent.putExtra("flag", typeFlag);
				
				startService(intent);
				OperationActivity.this.finish();
			}
		});
	}
	
}
