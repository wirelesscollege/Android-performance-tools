package com.ucweb.tools.activity;

import java.util.concurrent.ExecutorService;

import com.ucweb.tools.R;
import com.ucweb.tools.config.Config;
import com.ucweb.tools.monitorTask.MonkeyTest;
import com.ucweb.tools.service.MonitorService;
import com.ucweb.tools.utils.UcwebAppUtil;
import com.ucweb.tools.utils.UcwebThreadPoolsManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class OperationActivity extends Activity implements View.OnClickListener{
	
	private final ExecutorService executor = UcwebThreadPoolsManager.getThreadPoolManager().getExecutorService();
	
	private String pkgName;
	
	private SharedPreferences config;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.operation_activity);
		
		pkgName = getIntent().getStringExtra("pkgName");
		
		config = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
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
	
	private int[] getScreenResolution() {
		//已经获取过一次，则从cache中读取
		if (isAlreadyGetPhoneResolution()) {
			return readCache();
		} 
		//还未获取，则获取一次并写入cache
		else {
			int[] screenInfo = getPhoneResolution();
			writeCache(screenInfo);
			return screenInfo;
		}
	}
	
	/**
	 * 获取手机分辨率
	 * */
	private int[] getPhoneResolution() {
		DisplayMetrics dm = new DisplayMetrics();
		
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		
		return new int[] {(int) (dm.heightPixels /* * dm.density */), (int) (dm.widthPixels /* * dm.density */)};
	}
	
	/**
	 * 检测是否已获取屏幕分辨率
	 * */
	private boolean isAlreadyGetPhoneResolution() {
		SharedPreferences config = getSharedPreferences("config", Context.MODE_PRIVATE);
		return config.getBoolean("isAlreadyGetPhoneResolution", false);
	}
	
	/**
	 * 从缓存中读取
	 * */
	private int[] readCache() {
		SharedPreferences config = getSharedPreferences("config", Context.MODE_PRIVATE);
		return new int[] {
				config.getInt("height", 0),
				config.getInt("width", 0)
		};
	}
	
	/***
	 * 写入本地缓存
	 * @param screen
	 */
	private void writeCache(int[] screen) {
		
		SharedPreferences config = getSharedPreferences("config", Context.MODE_PRIVATE);
		Editor editor = config.edit();
		
		editor.putBoolean("isAlreadyGetPhoneResolution", true);
		editor.putInt("height", screen[0]);
		editor.putInt("width", screen[1]);
		editor.commit();
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
				 
		boolean isDoMonkeyTest = config.getBoolean("monkeySetting", false);
		if (isDoMonkeyTest) {
		
			String configWaitTime = config.getString("monkeySleepTime", "30");
			int waitTime;
			try {
				waitTime = Integer.parseInt(configWaitTime);
			} catch (NumberFormatException e) {
				waitTime = 30;
			}
//			Log.d("configWaitTime", String.valueOf(waitTime));
			int[] screenXY = getScreenResolution();
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
