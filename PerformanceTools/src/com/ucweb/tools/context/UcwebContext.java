package com.ucweb.tools.context;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.ucweb.tools.utils.UcwebFileUtils;

public class UcwebContext {
	private final Context mContext;
	
	private UcwebContext(Context context) {
		mContext = context.getApplicationContext();
		fileUtils = new UcwebFileUtils(mContext);
	}
	
	private static UcwebContext instance;
	
	private String fileSavePath;
	
	UcwebFileUtils fileUtils;
	
	public static UcwebContext getContext(Context context) {
		if(instance == null) {
			instance = new UcwebContext(context);
		}
		return instance;
	}
	
	public void init() {
		initFileSavePath();
	}
	
	public String getFileSavePath() {
		synchronized (fileSavePath) {
			if(fileSavePath == null) {
				initFileSavePath();
				writeGlobalConfig("file path", fileSavePath);
			}
		}

		return fileSavePath;
	}
	
	private final void initFileSavePath() {
		if(UcwebFileUtils.isSdcardAvailable()) {
			fileSavePath = UcwebFileUtils.generateSdcardFilePath();
		}
		else {
			fileSavePath = fileUtils.generateLocalFilePath();
		}
	}
	
	private final void onSdcardEject() {
		synchronized (fileSavePath) {
			fileSavePath = fileUtils.generateLocalFilePath();
		}
	}
	
	private final void onSdcardMount() {
		synchronized (fileSavePath) {
			fileSavePath = UcwebFileUtils.generateSdcardFilePath();
		}
	}
	
	public int[] getScreenResolution() {
		//已经获取过一次，则从cache中读取
		if (isAlreadyGetPhoneResolution()) {
			return readCache();
		} 
		else {
			//还未获取，则获取一次并写入cache
			int[] screenInfo = getPhoneResolution();
			writeCache(screenInfo);
			
			return screenInfo;
		}
	}
	
	public <T> void writeGlobalConfig(String key, T values) {
		final SharedPreferences config = PreferenceManager.getDefaultSharedPreferences(mContext);
		final Editor editor = config.edit();
		
		@SuppressWarnings("unchecked")
		Class<T> klass = (Class<T>) values.getClass();
		
		if(klass == boolean.class || klass == Boolean.class) {
			editor.putBoolean(key, (Boolean) values);
		}
		else if (klass == float.class || klass == Float.class) {
			editor.putFloat(key, (Float) values);
		}
		else if(klass == int.class || klass == Integer.class) {
			editor.putInt(key, (Integer) values);
		}
		else if(klass == long.class || klass == Long.class) {
			editor.putLong(key, (Long)values);
		}
		else if(klass == String.class) {
			editor.putString(key, (String) values);
		}
		else {
			throw new IllegalArgumentException();
		}
		
		editor.commit();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getGlobalConfig(String key, Class<T> klass) {
		final SharedPreferences config = PreferenceManager.getDefaultSharedPreferences(mContext);

		if(klass == boolean.class || klass == Boolean.class) {
			return (T) Boolean.valueOf(config.getBoolean(key, false));
		}
		else if (klass == float.class || klass == Float.class) {
			return (T) Float.valueOf(config.getFloat(key, 0.0f));
		}
		else if(klass == int.class || klass == Integer.class) {
			return (T) Integer.valueOf(config.getInt(key, 0));
		}
		else if(klass == long.class || klass == Long.class) {
			return (T) Long.valueOf(config.getLong(key, 0L));
		}
		else if(klass == String.class) {
			return (T) config.getString(key, "");
		}
		else {
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * 检测是否已获取屏幕分辨率
	 * */
	private boolean isAlreadyGetPhoneResolution() {
		
		return getGlobalConfig("AlreadyGetPhoneResolution", boolean.class);
	}
	
	/**
	 * 获取手机分辨率
	 * */
	private int[] getPhoneResolution() {
		final WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

		DisplayMetrics dm = new DisplayMetrics();
		
		wm.getDefaultDisplay().getMetrics(dm);
		
		return new int[] {(int) (dm.heightPixels /* * dm.density */), (int) (dm.widthPixels /* * dm.density */)};
	}
	
	/**
	 * 从缓存中读取
	 * */
	private int[] readCache() {
		return new int[] {
				getGlobalConfig("height", int.class),
				getGlobalConfig("width", int.class)
		};
	}
	
	/***
	 * 写入本地缓存
	 * @param screen
	 */
	private void writeCache(int[] screen) {

		writeGlobalConfig("AlreadyGetPhoneResolution", true);
		writeGlobalConfig("height", screen[0]);
		writeGlobalConfig("width", screen[1]);
	}

	
	class SdcardStatusMonitorBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action  = intent.getAction();
			
			if(action.equals(Intent.ACTION_MEDIA_EJECT)) {
				onSdcardEject();
			}
			else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
				onSdcardMount();
			}
			else {
				//do nothing
			}
			
		}
		
	}
	
}
