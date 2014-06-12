package com.ucweb.tools.monitorTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.ucweb.tools.utils.UcwebFileUtils;
import com.ucweb.tools.utils.UcwebFileUtils.FileType;

/***
 * 
 * Monitorable接口的直接实现骨架类，具体监控类需要从该类扩展
 *
 */

abstract class AbstractMonitor implements Monitorable{
	
	private List<String> mBuffer;
	private UcwebFileUtils mFileWriter;
	private Context mContext;
	
	AbstractMonitor(Context context) {
		mContext = context.getApplicationContext();
		
		mBuffer = new ArrayList<String>(20);
		mFileWriter = new UcwebFileUtils(mContext);
	}
	
	protected final String createFileName(FileType monitorType, String pkgName) {
		return mFileWriter.generateFileName(monitorType, 
				pkgName == null? "Unknown" : pkgName);
	}
	
	protected final String createFileFullPath(String filePath, String fileName) {
		return filePath + fileName;
	}
	
	protected void addInBuffer(String data) {
		mBuffer.add(data);
	}
	
	protected final void writeFile(String fileFullPath, String data) {
		try {
			mFileWriter.writeFile(fileFullPath, data);
		} catch (IOException e) {
			Log.e(getLogTag(), e.getMessage());
		}
	}

	protected final void writeFileWhenBufferReachMaxCount(String fileFullPath, int maxCount) {
		if(mBuffer.size() >= maxCount) {
			try {
				mFileWriter.writeFile(fileFullPath, mBuffer);
				mBuffer.clear();
			} catch (IOException e) {
				Log.e(getLogTag(), e.getMessage());
			}
		}
	}
	
	protected final void flushBufferAndWriteFile(String fileFullPath){
		if(!mBuffer.isEmpty()) {
			try {
				mFileWriter.writeFile(fileFullPath, mBuffer);
				mBuffer.clear();
			} catch (IOException e) {
				Log.e(getLogTag(), e.getMessage());
			}
		}
	}
	
	/***
	 * 获取Log tag
	 * @return
	 */
	final protected String getLogTag(){
		return getClass().getSimpleName();
	}
	
	/**
	 * 开始监控
	 */
	public abstract void startMonitor();
	
	/**
	 * 停止监控
	 */
	public abstract void stopMonitor();
	
}
