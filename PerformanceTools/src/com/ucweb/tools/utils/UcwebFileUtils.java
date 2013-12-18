package com.ucweb.tools.utils;

/***
 * @author yangruyao
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
//import android.widget.Toast;

public class UcwebFileUtils {	
	
	private Context mContext;
	
	//date format
	private final SimpleDateFormat sDateFormat;
	
	public UcwebFileUtils(Context context){
		mContext = context;
		sDateFormat = UcwebDateUtil.YMDDateFormat.getYMDFormat();
	}
	
	/**
	 * if sdcard available return true ,else return false
	 * */
	public static boolean isSdcardAvailable (){
		String sdcardStatus = Environment.getExternalStorageState();
		return sdcardStatus.equals(Environment.MEDIA_MOUNTED)? true : false;
	}
	
	public static class FileStorageLocation{
		/**path flag, use this flag, file will be save in sdcard*/
		public static final int LOCATION_SDCARD = 1;
		/**path flag, use this flag, file will be save in internal storage*/
		public static final int LOCATION_LOCAL = 2;
	}
	
	public static class FileType{
		/**cpu memory monitor info*/
		public static final int CpuMemInfoFileType = 1;
		/**batter monitor info*/
		public static final int BatterInfoFileType = 2;
		/**net monitor info*/
		public static final int NetInfoFileType = 3;
		/**iowait monitor info*/
		public static final int IOWInfoFileType = 4;
	}
	
	public String generateFileName(int fileFlag, String pkgName){
		String fileName = "";
		switch (fileFlag) {
		case FileType.CpuMemInfoFileType:
			fileName = "MonitorInfo";
			break;
			
		case FileType.BatterInfoFileType:
			fileName = "BatterInfo";
			break;
			
		case FileType.NetInfoFileType:
			fileName = "NetInfo";
			break;
			
		case FileType.IOWInfoFileType:
			fileName = "IOWInfo";
			break;

		default:
			return null;
		}
		//get phone model
		final String PHONE_MODEL = UcwebPhoneInfoUtils.getPhoneModel();
		//get date
		final String date = sDateFormat.format(new Date());

		return fileName + "_" + pkgName + "_" + PHONE_MODEL + "_" + date + ".txt";
	}
				
	public <T> void writeDatas(String fileName, List<T> dataList, int flag) throws IOException{
		
		StringBuilder sb = new StringBuilder(dataList.size());
		for (T t : dataList) {
			sb.append(t.toString());
		}
		
		try {
			writeSingleData(fileName, sb.toString(), flag);
		} catch (IOException e) {
			throw new IOException(e);
		}		
	}
	
	public <T> void writeSingleData(String fileName, T data, int flag) throws IOException{		
		//is sdcard available?
		if (isSdcardAvailable()) {
			//sdcard available,judge save path, sdcard or internal storage?
			switch (flag) {
			case FileStorageLocation.LOCATION_SDCARD:				
				try {
					writeToSdcard(fileName, data);
				} catch (IOException e) {
					throw new IOException(e);
				}
				break;
			case FileStorageLocation.LOCATION_LOCAL:
				try {
					writeToInternalStorage(fileName, data);
				} catch (Exception e) {
					throw new IOException(e);
				}
				break;
			default:
				break;
			}
		} 
		else {
			//write to internal storage
			try {
				writeToInternalStorage(fileName, data);
			} catch (Exception e) {
				throw new IOException(e);
			}
		}
	}
	
	private <T> void writeToSdcard(String fileName, T data) throws IOException{
		FileOutputStream fos = null;
		Log.d("UcwebFileUtils", "start write file to sdcard.......");
		try {
			String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + fileName;
			fos = new FileOutputStream(path, true);
			fos.write(data.toString().getBytes());
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			try {
				fos.close();
			} catch (Exception ignore) {
				
			}			
		}
	}
	
	private <T> void writeToInternalStorage(String fileName, T data) throws IOException{
		FileOutputStream fos = null;		
		
		try {
			Log.d("UcwebFileUtils", "start write file to internal storage.......");
			fos = mContext.openFileOutput(fileName, Context.MODE_APPEND);
			fos.write(data.toString().getBytes());
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			try {
				fos.close();
			} catch (Exception ignore) {
				
			}			
		}
	}
	
}
