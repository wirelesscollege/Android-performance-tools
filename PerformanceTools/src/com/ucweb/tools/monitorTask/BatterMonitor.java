package com.ucweb.tools.monitorTask;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import com.ucweb.tools.infobean.RecodeInfo;
import com.ucweb.tools.utils.UcwebDateUtil;
import com.ucweb.tools.utils.UcwebFileUtils;
import com.ucweb.tools.utils.UcwebThreadPoolsManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

public class BatterMonitor extends AbstractMonitor{
	
	private final ExecutorService executor;
	
	private static String LOG_TAG;
	//is add info into queue
	private final Context mContext;
	
	final String mPackageName;
	
	final String mFileName;
	
	final UcwebFileUtils fileWriter;
	
	private BatterMonitorBroadcastReceiver mBroadcast;
	
	private final SimpleDateFormat sdf;
	
	public BatterMonitor(Context context, String pkgName){
		super(context);
		mPackageName = pkgName;
		mContext = context;
	
		LOG_TAG = getLogTag();
		
		fileWriter = new UcwebFileUtils(mContext);
		
		mFileName = fileWriter.generateFileName(UcwebFileUtils.FileType.BatterInfoFileType, 
				pkgName == null? "Unknown" : pkgName);
		
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
		RecodeInfo info = createRecode(mFileName);
		addInQueue(info);
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
						float batter = current*100 / total;
						
						//get batter temperature
						int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);				

						String date = sdf.format(new Date());
						
						final StringBuilder sb = new StringBuilder(32);
						sb.append(date + "|" + String.format(Locale.CHINA, "%.1f", batter) + "|" + String.valueOf(temperature) + "\n");
						
						try {
							fileWriter.writeFile(mFileName, sb.toString(), UcwebFileUtils.FileLocation.SDCARD);
							
						} catch (IOException e) {
							e.printStackTrace();
							Log.d(LOG_TAG, "Hi....Guys, Write the fucking file failed");
						}					
					}
				});
				
			}
		}
		
	}

}

