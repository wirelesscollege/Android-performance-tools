package com.ucweb.tools.utils;

public class UcwebCommonTools {
	public static String convertKB2MB(String byteInKb) {
		int kb = Integer.parseInt(byteInKb);
		return String.valueOf(kb >>> 10);
	}
}
