package com.ucweb.tools.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.widget.Toast;

public class UcwebCommonTools {
	public static String convertKB2MB(String byteInKb) {
		int kb = Integer.parseInt(byteInKb);
		return String.valueOf(kb >>> 10);
	}
	
    public static List<NameValuePair> convertMap2List(Map<String, String> params){
    	if (params.isEmpty())
    		return null;
    	List<NameValuePair> paramList = new ArrayList<NameValuePair>();
    	for (Map.Entry<String, String> entry : params.entrySet()) {
			paramList.add(new BasicNameValuePair(entry.getKey().trim(), entry.getValue().trim()));
		}
    	return paramList;
    }
    
    public static void installAPK(Context context, String apkFile) {
    	Intent intent = new Intent();
    	intent.setAction(Intent.ACTION_VIEW);
    	
    	intent.setDataAndType(Uri.fromFile(new File(apkFile)), "application/vnd.android.package-archive");
    	context.startActivity(intent);
    }
    
	public static final boolean isUiThread() {
		final long currentThreadId = Thread.currentThread().getId();
		final long mainThreadId = Looper.getMainLooper().getThread().getId();
		
		return currentThreadId == mainThreadId;
	}
	
	public static class ToastTool{
		private final Context mContext;
		
		public ToastTool(Context context) {
			mContext = context.getApplicationContext();
		}
		
		public void showLongTips(CharSequence tipMsg) {
			if(!isUiThread())
				throw new IllegalStateException("只能在主线程显示Toast");
				
			Toast.makeText(mContext, tipMsg, Toast.LENGTH_LONG).show();
		}
		
		public void showShortTips(CharSequence tipMsg) {
			if(!isUiThread())
				throw new IllegalStateException("只能在主线程显示Toast");
				
			Toast.makeText(mContext, tipMsg, Toast.LENGTH_SHORT).show();
		}
		
	}
}
