package com.ucweb.tools.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.TrafficStats;
import android.util.Log;

import com.ucweb.tools.infobean.AppInfo;

public class UcwebAppUtil {
	
	private Context mContext;
	
	/**filter self*/
	private final String self;
	
	private static final String LOG_TAG = UcwebAppUtil.class.getSimpleName();
	
	public UcwebAppUtil(Context context){
		mContext = context;
		/**get self package name*/
		self = mContext.getPackageName();
	}
	
	/**
	 * get running app (not include system app) info
	 * */
	public List<AppInfo> getRunningAppInfo(){
		
		ArrayList<AppInfo> appInfoList = new ArrayList<AppInfo>();
		
		ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		PackageManager pm = mContext.getPackageManager();
		
		List<RunningAppProcessInfo> runningAppList = am.getRunningAppProcesses();
		for (RunningAppProcessInfo info : runningAppList) {						
			//获取包名，包名即app在进程中的名字
			String packgeName = info.processName;	
			
			try {
				ApplicationInfo applicationInfo = pm.getApplicationInfo(packgeName, 0);
				//过滤系统应用,过滤APP本身
				if ( ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) && 
						!packgeName.equalsIgnoreCase(self)) {
									
					AppInfo appInfo = new AppInfo();
//					Log.d(LOG_TAG, packgeName);
					//获取PID
					int pId = info.pid;
					//获取内存使用大小									
					//获取应用程序图标
					Drawable appIcon = applicationInfo.loadIcon(pm);
//					Log.d(LOG_TAG, getProcessCpuUseByPid(pId));
					//获取应用程序名称，这个名称即用户能看见的app名字
					String appName = applicationInfo.loadLabel(pm).toString();
						
					appInfo.setpId(pId);
					appInfo.setPackgeName(packgeName);
					appInfo.setAppIcon(appIcon);
					appInfo.setAppName(appName);
						
					appInfoList.add(appInfo);

				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(LOG_TAG, "getAppInfo: " + e.toString());
			}
		}
		return appInfoList;
	}
	
	/**
	 * get installed app (not include system app) info
	 * */
	public List<AppInfo> getInstalledAppInfo(){
		ArrayList<AppInfo> installedAppList = new ArrayList<AppInfo>();
		
		PackageManager pm = mContext.getPackageManager();
		List<PackageInfo> packges = pm.getInstalledPackages(0);
		
		for (PackageInfo packageInfo : packges) {
			if ( ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) && 
					!packageInfo.packageName.equalsIgnoreCase(self)) {

				AppInfo appInfo = new AppInfo();
				/**
				 	*packageName 	是指程序在包名，类似com.xxx
					*AppName		是程序显示给用户的那个名字
					* */
				appInfo.setPackgeName(packageInfo.packageName);
				appInfo.setAppIcon(packageInfo.applicationInfo.loadIcon(pm));
				appInfo.setAppName(packageInfo.applicationInfo.loadLabel(pm).toString());
					
				installedAppList.add(appInfo);
			}
		}
		return installedAppList;
	}
	
	public String[] getCpuMemUsage(final String[] cmds, final String pid, final String pkgName) 
			throws IOException{
	
		InputStream is = null;
		BufferedReader  br = null;
		
		try {
			final Runtime runTime = Runtime.getRuntime();
			
			Process process = runTime.exec(cmds);
			is = process.getInputStream();
			br = new BufferedReader(new InputStreamReader(is));
			
			String temp = null;
			while ((temp = br.readLine()) != null) {
				if (temp.contains(pid) && temp.contains(pkgName)) {
					String[] values = temp.split("\\s+");
					String cpuInfo = values[2].split("%")[0];
					String RSS = values[6].split("K")[0];
					
					return new String[]{cpuInfo, RSS};
				}
			}
			
			return null;
		} catch (Exception e) {
			throw new IOException("A erroc Accour while execute command: " + e.getMessage());
		} finally {
			try {
				is.close();
			} catch (Exception ignore) {}
			
			try {
				br.close();
			} catch (Exception ignore) {}
		}	
	}
	
	/**
	 * get app memory usage by pid
	 * */
	public int getAppMemInfoByPid(int pid){
		ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		android.os.Debug.MemoryInfo[] info = am.getProcessMemoryInfo(new int[] {pid});
		 
		return info[0].getTotalPss();
	}
	
	/***
	 * get system Available memory
	 * @return
	 */
	public long getSytemAvailableMem(){
		ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		MemoryInfo mi = new MemoryInfo();
		am.getMemoryInfo(mi);
		return mi.availMem;
	} 
	
	/**
	 * get app cpu usage by pid
	 * */
	public long getProcessCpuUseByPid(int pid){
		String processCpuInfoFileName = "/proc/" + Integer.toString(pid) + "/stat";
		long processTotalCpuUse = 0;
		RandomAccessFile reader = null;
		
		try {
			/**只读取第一条*/
			reader = new RandomAccessFile(processCpuInfoFileName, "r");
			String temp;
			StringBuilder sb = new StringBuilder(512);
			while ( (temp = reader.readLine()) != null) {
				sb.append(temp + "\n");
			}			
						
			String[] tok = sb.toString().split(" ");
			processTotalCpuUse = Long.parseLong(tok[13]) + Long.parseLong(tok[14]);
//			Log.d("Johnny", "进程cpu占用: " + String.valueOf(processTotalCpuUse));
			
		} catch (FileNotFoundException e) {
			Log.d("AppUtil.getProcessCpuUseByPid", "Can not found file" + processCpuInfoFileName + "\n" + e.toString());
		} catch (Exception e) {
				Log.d("AppUtil.getProcessCpuUseByPid", e.toString());
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return processTotalCpuUse;
	}
	
	/**
	 * total cpu usage
	 * */
	public long getTotalCpuTime(){		
		final String totalCpuUseageFile = "/proc/stat";
		long totalCpuUse = 0;
		RandomAccessFile reader = null;
		
		try {
			reader = new RandomAccessFile(totalCpuUseageFile, "r");
			String info = reader.readLine();
			String[] toks = info.split("\\s+");
			//toks[0]为cpu toks[1]为" ",所以从第3个开始读取
			totalCpuUse = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]) 
					+ Long.parseLong(toks[4]) + Long.parseLong(toks[6]) + Long.parseLong(toks[5])
					+ Long.parseLong(toks[7]);
		} catch (Exception e) {
			Log.d("AppUti.getTotalCpuTime", e.toString());
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return totalCpuUse;
	}	
	
	/***
	 * start app and wait app started, get running app pid
	 * @param packgeName
	 * @return pid
	 */
	public int startAppAndGetPid(String packgeName){
		Intent startMonitorProgram = mContext.getPackageManager().getLaunchIntentForPackage(packgeName);
		mContext.startActivity(startMonitorProgram);
		
		ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		
		final long timeoutExpiredMs = System.currentTimeMillis() + 5000;
		
		while (System.currentTimeMillis() < timeoutExpiredMs) {
			List<RunningAppProcessInfo> runningAppList = am.getRunningAppProcesses();
			for (RunningAppProcessInfo info : runningAppList) {
				if (info.processName.equalsIgnoreCase(packgeName)) {
					return info.pid;
				}
			}				
		}
		return 0;
	}
	
	/***
	 * Judge app is running
	 * @param packgeName
	 * @return
	 */
	public boolean isAppRunning(String packgeName){
		boolean bIsAppRunning = false;
		
		ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
		
		for (RunningAppProcessInfo info : runningApps) {
			if (info.processName.equalsIgnoreCase(packgeName)) {
				bIsAppRunning = true;
				break;
			}
		}
		
		return bIsAppRunning;
	}
	
	/***
	 * get running app pid
	 * @param packgeName
	 * @return
	 */
	public int getRunningAppPid(String packgeName){
		int pid = 0;
		ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> runningAppList = am.getRunningAppProcesses();
		for (RunningAppProcessInfo info : runningAppList) {
			if (info.processName.equalsIgnoreCase(packgeName)) {
				pid = info.pid;
				return pid;
			}
		}
		return pid;
	}
	
	/***
	 * get uid
	 * @param pkgName
	 * @return
	 */
	public int getUidByPackagename(String pkgName){
		
		int uid = -1;
		
		final PackageManager pm  = mContext.getPackageManager();
		
		List<PackageInfo> pkgInfoList = pm.getInstalledPackages(
				PackageManager.GET_UNINSTALLED_PACKAGES | PackageManager.GET_PERMISSIONS);
		
		for (PackageInfo packageInfo : pkgInfoList) {
			//get package name
			String proc = packageInfo.packageName;
			
			if (pkgName.equalsIgnoreCase(proc)) {
				
				String[] permissions = packageInfo.requestedPermissions;
				if (permissions != null && permissions.length > 0) {
					for (String permission : permissions) {
						if ("android.permission.INTERNET".equals(permission)) {
							uid = packageInfo.applicationInfo.uid;
							return uid;
						}
					}
				}
			}
		}
		return uid;
	}
	
	/**
	 * @param uid			package name
	 * @return				success, return int array, index 0 is total bytes in kb, index 1 is receive bytes in kb, 
	 * 						index 2 is send bytes in kb; else return null
	 * 
	 * */
	public int[] getAppTraffic(int uid){
		long receiveBytes = TrafficStats.getUidRxBytes(uid);
		//bytes send
		long sendBytes = TrafficStats.getUidTxBytes(uid);
		
		if (receiveBytes < 0 || sendBytes < 0) {
			//android version below 2.2, can not support TrafficStats API
			return null;
		}
		
		long totalBytes = receiveBytes + sendBytes;
		int[] trafficArray = new int[]{(int) (totalBytes >>> 10), (int) (receiveBytes >>> 10), (int) (sendBytes >>> 10)};
		
		return trafficArray;
	}
	
}
