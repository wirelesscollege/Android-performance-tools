package com.ucweb.tools.utils;

/***
 * @author yangruyao
 */

public class UcwebPhoneInfoUtils {
	public static String getPhoneModel(){
		return android.os.Build.MODEL == null ? "Unknown" : android.os.Build.MODEL;
	}
}
