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
	
	/**file type*/
	public static enum FileType {
		CpuMemInfoFileType,
		BatterInfoFileType,
		NetInfoFileType,
		IOWInfoFileType
	}
	
	/**根据文件类型，生成前缀名*/
	public static String generateFileNamePrefix(FileType fileFlag) {
		String fileTypeName;
		switch (fileFlag) {
		case CpuMemInfoFileType:
			fileTypeName = "MonitorInfo";
			break;
			
		case BatterInfoFileType:
			fileTypeName = "BatterInfo";
			break;
			
		case NetInfoFileType:
			fileTypeName = "NetInfo";
			break;
			
		case IOWInfoFileType:
			fileTypeName = "IOWInfo";
			break;

		default:
			fileTypeName = "Unknown";
			break;
		}
		
		return fileTypeName;
	}
	
	/**文件扩展名*/
	private static final String FILE_EXTENSION = ".txt";
	
	/**获取文件扩展名*/
	public static String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	/**文件名分隔符*/
	private static final String FILE_NAME_SEPARATION = "_";
	
	/**获取文件名分隔符*/
	public static String getFileNameSeparation() {
		return FILE_NAME_SEPARATION;
	}
	
	/**生成文件名*/
	public String generateFileName(FileType fileFlag, String pkgName){
		//get file name prefix
		final String fileNamePrefix = generateFileNamePrefix(fileFlag);		
		//get phone model
		final String PHONE_MODEL = UcwebPhoneInfoUtils.getPhoneModel();
		//get current date
		final String date = sDateFormat.format(new Date());

		return fileNamePrefix + FILE_NAME_SEPARATION + pkgName + FILE_NAME_SEPARATION 
				+ PHONE_MODEL + FILE_NAME_SEPARATION + date + getFileExtension();
	}
	
	/**File save path*/
	public static enum FileLocation {
		/**save file in sdcard*/
		SDCARD,
		/**save file in internal storage*/
		LOCAL
	}
	
	/**写数据*/
	public <T> void writeFile(String fileName, List<T> dataList, FileLocation position) throws IOException{
		
		StringBuilder sb = new StringBuilder(dataList.size());
		for (T t : dataList) {
			sb.append(t.toString());
		}
		
		try {
			writeFile(fileName, sb.toString(), position);
		} catch (IOException e) {
			throw new IOException(e);
		}		
	}
	
	/**写数据*/
	public <T> void writeFile(String fileName, T data, FileLocation position) throws IOException{		
		//is sdcard available?
		if (isSdcardAvailable()) {
			//sdcard available,judge save path, sdcard or internal storage?
			switch (position) {
			case SDCARD:				
				try {
					//write to sdcard
					writeToSdcard(fileName, data);
				} catch (IOException e) {
					throw new IOException(e);
				}
				break;
			case LOCAL:
				try {
					//write to local
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
			//No sdcard available, ignore position, write to internal storage
			try {
				writeToInternalStorage(fileName, data);
			} catch (Exception e) {
				throw new IOException(e);
			}
		}
	}
	
	/**生成Sd卡文件路径*/
	public static String generateSdcardFilePath() {
		return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
	}
	
	/**生成本地文件路径*/
	public String generateLocalFilePath() {
		return mContext.getFilesDir().getPath() + File.separator;
	}
	
	/**自动生成文件路径*/
	public String autoGenerateFilePath() {
		String path;
		
		if (isSdcardAvailable()) {
			path = generateSdcardFilePath();
		} else {
			path = generateLocalFilePath();
		}
		
		return path;
	}
	
	public String generateFilePath(FileLocation location) {
		String path;
		
		if (isSdcardAvailable()) {
			//judge flag
			switch (location) {
			case SDCARD:
				path = generateSdcardFilePath();
				break;
				
			case LOCAL:
				path = generateLocalFilePath();
				break;
				
			default:
				throw new IllegalArgumentException();
			}
		} else {
			//ignore location flag, generate local file path
			path = generateLocalFilePath();
		}
		return path;
	}
	
	public static void deleteFile(String fileFullPath) {
		File file = new File(fileFullPath);
		if (file.exists() && file.isFile()) {
			file.delete();
		}
	}
	
	private <T> void writeToSdcard(String fileName, T data) throws IOException{
		FileOutputStream fos = null;
		Log.d("UcwebFileUtils", "start write file to sdcard.......");
		
		try {
			String path = generateSdcardFilePath() + fileName;
			
			fos = new FileOutputStream(path, true);
			fos.write(data.toString().getBytes());
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			try {
				fos.close();
			} catch (Exception ignore) {}			
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
			} catch (Exception ignore) {}			
		}
	}
	
}
