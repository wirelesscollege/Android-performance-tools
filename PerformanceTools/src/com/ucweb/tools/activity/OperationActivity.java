package com.ucweb.tools.activity;

import java.util.concurrent.ExecutorService;

import com.ucweb.tools.R;
import com.ucweb.tools.config.Config;
import com.ucweb.tools.context.UcwebContext;
import com.ucweb.tools.monitorTask.MonkeyTest;
import com.ucweb.tools.service.MonitorService;
import com.ucweb.tools.utils.UcwebAppUtil;
import com.ucweb.tools.utils.UcwebThreadPoolsManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class OperationActivity extends Activity implements View.OnClickListener{
	
	private final ExecutorService executor = UcwebThreadPoolsManager.getThreadPoolManager().getExecutorService();
	
	private String pkgName;

	private UcwebContext env;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.operation_activity);
		
		pkgName = getIntent().getStringExtra("pkgName");
		
		env = UcwebContext.getContext(this);
		
		final Button btnCpuMemMonitor = (Button) findViewById(R.id.btnCpuMemMonitor);
		btnCpuMemMonitor.setOnClickListener(this);
		
		final Button btnBatterMonitor = (Button) findViewById(R.id.btnBatterMonitor);
		btnBatterMonitor.setOnClickListener(this);
		
		final Button btnIOWMonitor = (Button) findViewById(R.id.btnIOMonitor);
		btnIOWMonitor.setOnClickListener(this);
		
		final Button btnNetMonitor = (Button) findViewById(R.id.btnNetMonitor);
		btnNetMonitor.setOnClickListener(this);
		
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
		final Context applicationContext = this.getApplicationContext();
		
		executor.execute(new Runnable() {
			
			@Override
			public void run() {			
				UcwebAppUtil apputil = new UcwebAppUtil(applicationContext);

				//blocking thread, until test app is running
				apputil.startAppAndGetPid(pkgName);				
				
				Intent intent = new Intent();
				intent.setClass(OperationActivity.this, MonitorService.class);
				intent.putExtra("pkgName", pkgName);
				intent.putExtra("flag", typeFlag);
				
				final String fileWritePath = env.getFileSavePath();
				
				intent.putExtra("file path", fileWritePath);
				
				startService(intent);
				OperationActivity.this.finish();
			}
		});
	}
	
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		case R.id.btnCpuMemMonitor:
			startMonitorService(pkgName, MonitorType.MONITOR_TYPE_CPUMEM);
			break;
			
		case R.id.btnBatterMonitor:
			startMonitorService(pkgName, MonitorType.MONITOR_TYPE_BATTER);
			break;
			
		case R.id.btnIOMonitor:
			startMonitorService(pkgName, MonitorType.MONITOR_TYPE_IOW);
			break;
			
		case R.id.btnNetMonitor:
			startMonitorService(pkgName, MonitorType.MONITOR_TYPE_NET);
			break;
			
		default:
			break;
		}
				 
		boolean isDoMonkeyTest = env.getGlobalConfig("monkeySetting", boolean.class);

		if (isDoMonkeyTest) {
		
			String configWaitTime = env.getGlobalConfig("monkeySleepTime", String.class);
			
			int waitTime;
			try {
				waitTime = Integer.parseInt(configWaitTime.equals("")? "30" : configWaitTime);
			} catch (NumberFormatException e) {
				waitTime = 30;
			}
//			Log.d("configWaitTime", String.valueOf(waitTime));
			int[] screenXY = UcwebContext.getContext(this).getScreenResolution();
			Toast.makeText(getApplicationContext(), "屏幕分辨率: " +screenXY[0] + " X " + screenXY[1], Toast.LENGTH_LONG).show();
			MonkeyTest.Builder builder = new MonkeyTest.Builder(getApplicationContext());
			builder.setPackage(pkgName);
			builder.setUrl(Config.GET_MONKEY_SCRIPT_URL);
			builder.setScreenXY(screenXY);
			builder.setWaitTime(waitTime);
			builder.setMonkeyScriptFileName(Config.MONKEY_SCRIPT_FILE_NAME);
			
			executor.execute(builder.build());
		}
	}
	
}
