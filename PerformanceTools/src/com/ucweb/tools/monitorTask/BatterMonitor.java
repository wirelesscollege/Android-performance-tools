package com.ucweb.tools.monitorTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import com.ucweb.tools.utils.UcwebDateUtil;
import com.ucweb.tools.utils.UcwebFileUtils.FileType;
import com.ucweb.tools.utils.UcwebThreadPoolsManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

public class BatterMonitor extends AbstractMonitor{
	
	private final ExecutorService executor;
	
	//is add info into queue
	private final Context mContext;
	
	final String mPackageName;
	
	final String mFilefullPath;

	private BatterMonitorBroadcastReceiver mBroadcast;
	
	private final SimpleDateFormat sdf;
	
	public BatterMonitor(Context context, String fileSavePath, String pkgName){
		super(context);
		mPackageName = pkgName;
		mContext = context;

		mFilefullPath = createFileFullPath(fileSavePath, createFileName(FileType.BatterInfoFileType, pkgName));
		
		executor = UcwebThreadPoolsManager.getThreadPoolManager().getExecutorService();
		
		sdf = UcwebDateUtil.YMDHMSDateFormat.getYMDHMSFormat();
		
		mBroadcast = new BatterMonitorBroadcastReceiver();
	}

	@Override
	public void startMonitor() {
		Log.d("BatterMonitor", "start broadcast");
		IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		mContext.registerReceiver(mBroadcast, filter);
		
	}

	@Override
	public void stopMonitor() {
		Log.d("BatterMonitor", "stop broadcast");
		mContext.unregisterReceiver(mBroadcast);
	}
	
	private class BatterMonitorBroadcastReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, final Intent intent) {
			if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
				
				executor.execute(new Runnable() {
					
					@Override
					public void run() {
						//get current power
						int current = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
						// get total power
						int total = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
						
						String batter = String.format(Locale.CHINA, "%.1f", current*100 / total);
						//get batter temperature
						String temperature = String.valueOf(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0));				

						String date = sdf.format(new Date());
						
						writeFile(mFilefullPath, makeOutputStyle(date, batter,temperature));
					}
				});
				
			}
		}
		
	}
	
	private final String makeOutputStyle(String date, String batter, String temperature) {
		return date + "|" + batter + "|" + temperature + "\n";
	}

}

