package com.ucweb.tools.utils;

import android.util.Log;

public class LogUtils {
	private boolean isShutDownLogCat;
	
	public LogUtils(boolean printLog) {
		isShutDownLogCat = printLog;
	}
	
	public void printDebugLog(String tag, String msg) {
		if (isShutDownLogCat) {
			Log.d(tag, msg);
		}
	}
	
	public void printVerboseLog(String tag, String msg) {
		if (isShutDownLogCat) {
			Log.v(tag, msg);
		}
	}
	
	public void printErrorLog(String tag, String msg) {
		if (isShutDownLogCat) {
			Log.e(tag, msg);
		}
	}
}
