package com.ucweb.tools.monitorTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


import java.util.concurrent.TimeUnit;

import com.ucweb.tools.infobean.RecodeInfo;
import com.ucweb.tools.utils.UcwebAppUtil;
import com.ucweb.tools.utils.UcwebCommonTools;
import com.ucweb.tools.utils.UcwebDateUtil;
import com.ucweb.tools.utils.UcwebFileUtils;
import com.ucweb.tools.utils.UcwebFileUtils.FileLocation;

import android.content.Context;
import android.util.Log;

public class CpuMemMonitor extends AbstractMonitor{
	
	private static final int INDEX_DATE = 0;
	private static final int INDEX_MEM_USE = 1;
	private static final int INDEX_CPU_USE = 2;
	
	private static final String[] cmds = {"top", "-m", "1"};
	
	private boolean mStopMonitor;
	
	private Context mContext;
	
	private int monitorInterval;
		
	private final UcwebAppUtil appUtil;
	
	private UcwebFileUtils fileWriter;
	
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
	
	@Override
	public void startMonitor() {
		
		fileWriter = new UcwebFileUtils(mContext);
		String fileName = fileWriter.generateFileName(UcwebFileUtils.FileType.CpuMemInfoFileType, 
				mPkgName == null? "Unknown" : mPkgName);
		
		RecodeInfo recodeInfo = createRecode(fileName);

		doLoop(fileName);
	
		addInQueue(recodeInfo);
	}
	
	private final void doLoop(String fileName) {
		final String pid = String.valueOf(appUtil.getRunningAppPid(mPkgName));
		
		InputStream is = null;
		BufferedReader  br = null;
		
		final Runtime runTime = Runtime.getRuntime();
		Process process = null;

		try {
			process = runTime.exec(cmds);
		}catch (IOException e) {
			e.printStackTrace();
			return;
		}
			
		is = process.getInputStream();
			
		String temp = null;
			
		br = new BufferedReader(new InputStreamReader(is));
		
		
		while (!mStopMonitor) {
			try {
				temp = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
			
			/**如果为空，重新读一条*/
			if(temp == null) 
				continue;

			if (temp.contains(pid) && temp.contains(mPkgName)) {
				/**获取的字符串必须先去掉首尾空格才split*/
				String[] values = temp.trim().split("\\s+");
				
				/**cpu信息字段*/
				String cpuInfo = values[2].split("%")[0];
				/**RSS字段*/
				String RSS = values[6].split("K")[0];
				
				/***当前日期*/
				String now = sdf.format(new Date());
				
				infoBuffer.add(new String[] {now, UcwebCommonTools.convertKB2MB(RSS), cpuInfo});
				
			} else {
				/**读出的东西不是想要的，重新读一条*/
				continue;			
			}
			
			try {
				TimeUnit.SECONDS.sleep(monitorInterval);
				} catch (Exception e) {
					e.printStackTrace();
			}
			
			writeFileWhenBufferReachMaxCount(fileName, 10);
		}
		
		/**结束测试后，刷新buffer，把buffer剩余数据写到文件*/
		flushBuffer(fileName, infoBuffer);
		
		/**释放资源*/
		closeInputStream(is);
		closeBufferReader(br);
		destroyProcess(process);
	}
	
	private void closeInputStream(InputStream in) {
		if(in != null) {
			try {
				in.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void closeBufferReader(BufferedReader br) {
		if(br != null) {
			try {
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void destroyProcess(Process process) {
		try {
			process.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	private void writeFileWhenBufferReachMaxCount(String fileName, int maxBufferCount) {
		if (infoBuffer.size() >= maxBufferCount) {				
			StringBuilder sb = new StringBuilder(320);
			
			for (String[] temp : infoBuffer) {
				sb.append(temp[INDEX_DATE] + "|" + temp[INDEX_MEM_USE] + "|" + temp[INDEX_CPU_USE] + "\n");
			}
				
			try {
				fileWriter.writeFile(fileName, sb.toString(), FileLocation.SDCARD);
			} catch (IOException e) {
				Log.d(LOG_TAG, "write CpuMemMonitor file, below is exception message:\n" + e.getMessage());
			}
			
			sb = null;
			infoBuffer.clear();
		}
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
				fileWriter.writeFile(fileName, sb.toString(), FileLocation.SDCARD);
			} catch (IOException e) {
				Log.d(LOG_TAG, "write CpuMemMonitor file, below is exception message:\n" + e.getMessage());
			}
		}
	}
	
}
