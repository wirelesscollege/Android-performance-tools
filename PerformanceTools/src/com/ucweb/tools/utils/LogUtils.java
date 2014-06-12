package com.ucweb.tools.utils;

import android.util.Log;

public class LogUtils {
	private boolean isShutDownLog = false;
	
	public void setWriteLogOff(boolean flag) {
		isShutDownLog = flag;
	}
	
	public void printDebugLog(String tag, String msg) {
		if (!isShutDownLog) {
			Log.d(tag, msg);
		}
	}
	
	public void printVerboseLog(String tag, String msg) {
		if (isShutDownLog) {
			Log.v(tag, msg);
		}
	}
	
	public void printErrorLog(String tag, String msg) {
		if (isShutDownLog) {
			Log.e(tag, msg);
		}
	}
}
