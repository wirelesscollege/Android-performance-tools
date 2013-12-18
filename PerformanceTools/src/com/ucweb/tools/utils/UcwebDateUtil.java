package com.ucweb.tools.utils;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class UcwebDateUtil {
	
	//yyyy-MM-dd date format
	public static class YMDDateFormat {
		
		private YMDDateFormat(){}
		
		private static ThreadLocal<SimpleDateFormat> ymdThreadLocal = new ThreadLocal<SimpleDateFormat>() {
			protected SimpleDateFormat initialValue(){
				//fuck, yyyy-MM-dd MM must be upper case, mm means minutes
				return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
			}
		};
		
		public static SimpleDateFormat getYMDFormat(){
			return ymdThreadLocal.get();
		}		
	}
	
	//yyyy-MM-dd hh:mm:ss date format
	public static class YMDHMSDateFormat {
		
		private YMDHMSDateFormat(){}
		
		private static ThreadLocal<SimpleDateFormat> ymdhmsThreadLocal = new ThreadLocal<SimpleDateFormat>() {
			protected SimpleDateFormat initialValue(){
				return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.CHINA);
			}
		};
		
		public static SimpleDateFormat getYMDHMSFormat(){
			return ymdhmsThreadLocal.get();
		}	
	}	
}
