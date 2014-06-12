package com.ucweb.tools.monitorTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.ucweb.tools.utils.UcwebAppUtil;
import com.ucweb.tools.utils.UcwebCommonTools;
import com.ucweb.tools.utils.UcwebDateUtil;
import com.ucweb.tools.utils.UcwebFileUtils.FileType;

import android.content.Context;
import android.util.Log;

public class CpuMemMonitor extends AbstractMonitor{

	private static final String[] cmds = {"top", "-m", "1"};
	
	private Context mContext;
	
	/**监控间隔*/
	private int monitorInterval;
		
	private final UcwebAppUtil appUtil;
	
	/**日期格式*/
	private final DecimalFormat format;
	
	/**包名*/
	private final String mPkgName;
	
	/**监控文件保存路径*/
	private final String mFileSavePath;
	
	private static String LOG_TAG;
	
	private final SimpleDateFormat sdf;
	
	public static class Builder {
		private Context mContext;
		
		private String mPkgName;
		
		private static final int DEFAULT_MONITOR_INTERVAL = 10;
		
		private int monitorInterval;
		
		private String filePath;
		
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
		
		public Builder setFileSavePath(String filePath){
			this.filePath = filePath;
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
		mFileSavePath = builder.filePath;
		
		LOG_TAG = getLogTag();
		appUtil = new UcwebAppUtil(mContext);
		sdf = UcwebDateUtil.YMDHMSDateFormat.getYMDHMSFormat();
		
		format = new DecimalFormat();
		format.setMaximumFractionDigits(2);
		format.setMinimumFractionDigits(2);
	}
	
	/**是否暂停监控线程的标志*/
	private boolean mStopMonitor;
	
	public void stopCpuMonitor(){
		mStopMonitor = true;
	}
	
	@Override
	public void startMonitor() {
		String fileName = createFileName(FileType.CpuMemInfoFileType, mPkgName);
		Log.d(getLogTag(), "写的文件全路径:" + mFileSavePath + fileName);
		doMonitorLoop(createFileFullPath(mFileSavePath, fileName));
	
	}
	
	private final void doMonitorLoop(String fileFullPath) {
		final String pid = String.valueOf(appUtil.getRunningAppPid(mPkgName));
		
		InputStream is = null;
		BufferedReader  br = null;
		
		final Runtime runTime = Runtime.getRuntime();
		Process process = null;
		
		try {
			process = runTime.exec(cmds);
		}catch (IOException e) {
			Log.e(LOG_TAG, e.getMessage());
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

				try {
					TimeUnit.MILLISECONDS.sleep(10);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				continue;
			}
			
			/**如果为空，重新读一条*/
			if(temp == null) {
				try {
					TimeUnit.MILLISECONDS.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				continue;
			}

			if (temp.contains(pid) && temp.contains(mPkgName)) {
				/**获取的字符串必须先去掉首尾空格才split*/
				String[] values = temp.trim().split("\\s+");
				
				/**cpu信息字段*/
				String cpuInfo = values[2].split("%")[0];
				if(!isCpuInfoValid(cpuInfo)) {
					/**针对狗日的小米手机，取出的CPU信息偶尔会不对，那么直接抛掉这次取的数据*/
					continue;
				}
				
				/**RSS字段*/
				String RSS = values[6].split("K")[0];
				
				/***当前日期*/
				String now = sdf.format(new Date());
				
				addInBuffer(makeOutputStyle(now, UcwebCommonTools.convertKB2MB(RSS), cpuInfo));
				
			} else {
				/**读出的东西不是想要的，重新读一条*/
				continue;			
			}
			
			try {
				TimeUnit.SECONDS.sleep(monitorInterval);
				} catch (Exception e) {
					e.printStackTrace();
			}
			
			writeFileWhenBufferReachMaxCount(fileFullPath, 10);
		}
		
		/**结束测试后，刷新buffer，把buffer剩余数据写到文件*/
		flushBufferAndWriteFile(fileFullPath);
		
		/**释放资源*/
		closeInputStream(is);
		closeBufferReader(br);
		destroyProcess(process);
	}
	
	private final boolean isCpuInfoValid(String cpuInfo) {
		Integer c = Integer.valueOf(cpuInfo);
		return c >= 0 && c <= 100;
	}
	
	private final String makeOutputStyle(String date, String memUseage, String cpuUseage) {
		return date + "|" + memUseage + "|" + cpuUseage + "\n";
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

	@Override
	public void stopMonitor() {
		stopCpuMonitor();
	}
	
}
