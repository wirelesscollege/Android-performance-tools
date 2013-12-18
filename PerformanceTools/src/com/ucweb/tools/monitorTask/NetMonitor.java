package com.ucweb.tools.monitorTask;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.ucweb.tools.infobean.RecodeInfo;
import com.ucweb.tools.utils.UcwebAppUtil;
import com.ucweb.tools.utils.UcwebDateUtil;
import com.ucweb.tools.utils.UcwebFileUtils;

import android.content.Context;
import android.util.Log;

public class NetMonitor extends AbstractMonitor{
	
	private static String LOG_TAG;
	
	private static final int INDEX_TOTAL_BYTES_IN_KB = 0;
	private static final int INDEX_RECEIVE_BYTES_IN_KB = 1;
	private static final int INDEX_SEND_BYTES_IN_KB = 2;
	
	private final Context mContext;
	
	private boolean mStopMonitor;
	
	private String mMonitorPkgName;
	
	private UcwebFileUtils mFileWriter;
	
	private final UcwebAppUtil mAppUtils;
	
	
	private final SimpleDateFormat sdf;
	
	private final ArrayList<String> buffer;
	
	public NetMonitor(Context context, String pkgName){
		super(context);
		LOG_TAG = getLogTag();
		this.mContext = context;
		mStopMonitor = false;
		mMonitorPkgName = pkgName;
		
		mAppUtils = new UcwebAppUtil(mContext);
		sdf = UcwebDateUtil.YMDHMSDateFormat.getYMDHMSFormat();
		buffer = new ArrayList<String>(6);
	}
	
	public void stopNetMonitor(){
		mStopMonitor = true;
	}

	@Override
	public void startMonitor() {
		final int uid = mAppUtils.getUidByPackagename(mMonitorPkgName);
		if (uid < 0) return;
		
		mFileWriter = new UcwebFileUtils(mContext);
		final String fileName = mFileWriter.generateFileName(UcwebFileUtils.FileType.NetInfoFileType, 
				mMonitorPkgName == null? "Unknown" : mMonitorPkgName);
		
		RecodeInfo info = createRecode(fileName);
		
		while (!mStopMonitor) {
			int[] bytesChange = getTrafficChangeBytes(uid);

			if (bytesChange != null && bytesChange[INDEX_TOTAL_BYTES_IN_KB] > 0) {
				
				String date = sdf.format(new Date());
				StringBuffer sb = new StringBuffer();
				
				sb.append(date + "|" + bytesChange[INDEX_TOTAL_BYTES_IN_KB] + "|" + bytesChange[INDEX_RECEIVE_BYTES_IN_KB] 
						+ "|" + bytesChange[INDEX_SEND_BYTES_IN_KB] + "\n");
				
				buffer.add(sb.toString());
			}
			
			if (buffer.size() == 6) {
				try {
					mFileWriter.writeDatas(fileName, buffer, UcwebFileUtils.FileStorageLocation.LOCATION_SDCARD);
				} catch (IOException e) {
					Log.d(LOG_TAG, e.toString());
				}
				buffer.clear();
			}			
		}
		
		flushBuffer(buffer, fileName);
		addInQueue(info);
	}
	
	private void flushBuffer(ArrayList<String> buffer, String fileName){
		if (!buffer.isEmpty()) {
			try {
				Log.d(LOG_TAG, "write buffer");
				mFileWriter.writeDatas(fileName, buffer, UcwebFileUtils.FileStorageLocation.LOCATION_SDCARD);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stopMonitor() {
		stopNetMonitor();		
	}
	
	private int[] getTrafficChangeBytes(int uid){
		int[] bytesArray1 = mAppUtils.getAppTrafficByUid(uid);
		
		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (Exception e) {
			Log.d(LOG_TAG, e.toString());
		}
		int[] bytesArray2 = mAppUtils.getAppTrafficByUid(uid);
		
		if (bytesArray1 != null && bytesArray2 != null) {
			return new int[]{bytesArray2[INDEX_TOTAL_BYTES_IN_KB] - bytesArray1[INDEX_TOTAL_BYTES_IN_KB], 
					bytesArray2[INDEX_RECEIVE_BYTES_IN_KB] - bytesArray1[INDEX_RECEIVE_BYTES_IN_KB], 
					bytesArray2[INDEX_SEND_BYTES_IN_KB] - bytesArray1[INDEX_SEND_BYTES_IN_KB]};
		}
		return null;
	}
	
}
