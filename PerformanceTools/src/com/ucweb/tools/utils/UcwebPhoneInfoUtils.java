package com.ucweb.tools.utils;

import android.content.Context;
import android.telephony.TelephonyManager;

/***
 * @author yangruyao
 */

public class UcwebPhoneInfoUtils {
	private final Context mContext;
	
	private static final String UNKNOWN_VALUE = "Unknown";
	
	public UcwebPhoneInfoUtils(Context context) {
		mContext = context;
	}
	
	public static String getPhoneModel(){
		return android.os.Build.MODEL == null ? UNKNOWN_VALUE : android.os.Build.MODEL;
	}
	
	public String getPhoneIMEI() {
		final TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		final String imei = tm.getDeviceId();
		
		return imei != null? imei : UNKNOWN_VALUE;
	}
	
	public static String getOsVersion() {
		final String version = android.os.Build.VERSION.RELEASE;
		
		return version != null? version : UNKNOWN_VALUE;
	}
}
