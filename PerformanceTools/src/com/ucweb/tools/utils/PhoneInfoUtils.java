package com.ucweb.tools.utils;

public class PhoneInfoUtils {
	public static String getPhoneModel(){
		String model = android.os.Build.MODEL;
		return model == null ? "Unknown" : model;
	}
}
