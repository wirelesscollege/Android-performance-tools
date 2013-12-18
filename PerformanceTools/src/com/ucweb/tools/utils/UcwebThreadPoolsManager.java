package com.ucweb.tools.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Log;

public final class UcwebThreadPoolsManager {
	//thread pools
	private static final String LOG_TAG = UcwebThreadPoolsManager.class.getSimpleName();
	
	private static UcwebThreadPoolsManager manager;
	private UcwebThreadPoolsManager(){}
	
	private static final int MAX_POOL_SIZE = 3;
	
	private ExecutorService mService = null;
	
	public static UcwebThreadPoolsManager getInstance(){
		if (manager == null) {
			synchronized (UcwebThreadPoolsManager.class) {
				if (manager == null) {
					manager = new UcwebThreadPoolsManager();
				}
			}
		}
		return manager;
	}
	
	public void init(){
		Log.d(LOG_TAG, "init thread pool........");
		mService = Executors.newFixedThreadPool(MAX_POOL_SIZE);
	}
	
	public boolean isThreadPoolAlive(){
		return mService == null? false : true;
	}
	
	public ExecutorService getExecutorService(){
		return mService;
	}
	
	public void shutdownThreadPool(){
		Log.d(LOG_TAG, "shutdown thread pool........");
		mService.shutdown();
	}
}
