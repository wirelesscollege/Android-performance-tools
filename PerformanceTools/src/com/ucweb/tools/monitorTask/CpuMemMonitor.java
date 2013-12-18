package com.ucweb.tools.monitorTask;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


import java.util.concurrent.TimeUnit;

import com.ucweb.tools.infobean.RecodeInfo;
import com.ucweb.tools.utils.UcwebAppUtil;
import com.ucweb.tools.utils.UcwebDateUtil;
import com.ucweb.tools.utils.UcwebFileUtils;

import android.content.Context;
import android.util.Log;

public class CpuMemMonitor extends AbstractMonitor{
	
	private static final int INDEX_DATE = 0;
	private static final int INDEX_MEM_USE = 1;
	private static final int INDEX_CPU_USE = 2;
	
	private boolean mStopMonitor;
	
	private Context mContext;
	
	private int monitorInterval;
		
	private final UcwebAppUtil appUtil;
	
	private UcwebFileUtils fileWriter;
	//格式化CPU使用率
	private final DecimalFormat format;
	
	private final String mPkgName;
	
	private final ArrayList<String[]> infoBuffer = new ArrayList<String[]>();
	
	private static String LOG_TAG;
	
	private final SimpleDateFormat sdf;
	
	public static class Builder {
		private Context mContext;
		
		private String mPkgName;
		
		private static final int DEFAULT_MONITOR_INTERVAL = 10;
		
		private int monitorInterval;
		
		public Builder(Context context){
			mContext = context;
			monitorInterval = DEFAULT_MONITOR_INTERVAL;
		}
		
		public Builder setMonitorPkg(String pkgName){
			mPkgName = pkgName;
			return this;
		}
		
		public Builder setMonitorIntervalSeconds(int msInterval){
			monitorInterval = msInterval;
			return this;
		}
		
		public CpuMemMonitor build() {
			return new CpuMemMonitor(this);
		}
	}
	
	private CpuMemMonitor(Builder builder) {
		super(builder.mContext);
		mContext = builder.mContext;
		mPkgName = builder.mPkgName;
		mStopMonitor = false;
		monitorInterval = builder.monitorInterval;
		
		LOG_TAG = getLogTag();
		appUtil = new UcwebAppUtil(mContext);
		sdf = UcwebDateUtil.YMDHMSDateFormat.getYMDHMSFormat();
		
		format = new DecimalFormat();
		format.setMaximumFractionDigits(2);
		format.setMinimumFractionDigits(2);
	}

	public void stopCpuMonitor(){
		mStopMonitor = true;
	}
	
	private String[] getCpuMemUsedInfo(int pid){
		/**get CPU and Memory size*/
		
		Log.d(LOG_TAG, "Are u ready? Go.........!");
		
		long totalCpuTime1 = appUtil.getTotalCpuTime();
		long processCpuTime1 = appUtil.getProcessCpuUseByPid(pid);
		
		//processCpuTime is 0, may be means the fucking test app is shutdown
		if (processCpuTime1 == 0) {
			pid = appUtil.getRunningAppPid(mPkgName);
			if (pid == 0) {
				//app is not start
				return null;
			} else {
				processCpuTime1 = appUtil.getProcessCpuUseByPid(pid);
			}			
		}
				
		try {
			TimeUnit.SECONDS.sleep(monitorInterval);
		} catch (Exception e) {
			Log.e("Johnny", e.toString());
		}
		
		long totalCpuTime2 = appUtil.getTotalCpuTime();
		long processCpuTime2 = appUtil.getProcessCpuUseByPid(pid);
		if (processCpuTime2 == 0L) {
			return null;
		}
				
		String now = sdf.format(new Date());
		//get memory user in MB
		int memUse = (appUtil.getAppMemInfoByPid(pid)) >>> 10;
		
		long processTime = processCpuTime2 - processCpuTime1;
		
		//进程时间有变化
		if (processTime != 0L) {
			//当前进程时间比10秒前进程时间少
			if (processTime < 0L) {
				processTime = Math.abs(processTime);
			}
			
			String cpuUsePercent = format.format(100 * ((double) processTime 
					/ ((double) (totalCpuTime2 - totalCpuTime1))));
			
			String[] data = new String[] {now, String.valueOf(memUse), cpuUsePercent};
			
			return data;
			
		} else {
			//进程时间未变化，取buffer中最后一条的cpu使用率
			if (!infoBuffer.isEmpty()) {
				
				int lastElementIndex = infoBuffer.size() - 1;				
				String cpuUsePercent = infoBuffer.get(lastElementIndex)[INDEX_CPU_USE];
				
				String[] data = new String[] {now, String.valueOf(memUse), cpuUsePercent};
				
				return data;
			}
		}
		return null;
	}
	
	@Override
	public void startMonitor() {
		//Generator file name
		fileWriter = new UcwebFileUtils(mContext);
		String fileName = fileWriter.generateFileName(UcwebFileUtils.FileType.CpuMemInfoFileType, 
				mPkgName == null? "Unknown" : mPkgName);
		//生成数据库存储记录bean
		RecodeInfo recodeInfo = this.createRecode(fileName);
		
		//get pid
		int pid = appUtil.getRunningAppPid(mPkgName);
		
		while (!mStopMonitor) {	
			String[] info = getCpuMemUsedInfo(pid);
			if (info == null) continue;
			
			infoBuffer.add(info);
			
			//cache 10 datas
			if (infoBuffer.size() == 10) {				
				StringBuilder sb = new StringBuilder(320);
				
				for (String[] temp : infoBuffer) {
					sb.append(temp[INDEX_DATE] + "|" + temp[INDEX_MEM_USE] + "|" + temp[INDEX_CPU_USE] + "\n");
				}
					
				try {
					fileWriter.writeSingleData(fileName, sb.toString(), UcwebFileUtils.FileStorageLocation.LOCATION_SDCARD);
				} catch (IOException e) {
					Log.d(LOG_TAG, "write CpuMemMonitor file, below is exception message:\n" + e.getMessage());
				}
				
				sb = null;
				infoBuffer.clear();
			}
		}
		//stop monitor
		//refresh buffer
		flushBuffer(fileName, infoBuffer);
		//add info to queue
		this.addInQueue(recodeInfo);
	}

	@Override
	public void stopMonitor() {
		stopCpuMonitor();
	}
	
	private void flushBuffer(String fileName, ArrayList<String[]> buffer){
		if (!buffer.isEmpty()) {
			StringBuilder sb = new StringBuilder(320);
			
			for (String[] temp : buffer) {
				sb.append(temp[INDEX_DATE] + "|" + temp[INDEX_MEM_USE] + "|" + temp[INDEX_CPU_USE] + "\n");
			}
							
			try {
				fileWriter.writeSingleData(fileName, sb.toString(), UcwebFileUtils.FileStorageLocation.LOCATION_SDCARD);
			} catch (IOException e) {
				Log.d(LOG_TAG, "write CpuMemMonitor file, below is exception message:\n" + e.getMessage());
			}
		}
	}
	
}
