package com.ucweb.tools.monitorTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.ucweb.tools.utils.UcwebAppUtil;
import com.ucweb.tools.utils.UcwebDateUtil;
import com.ucweb.tools.utils.UcwebFileUtils.FileType;

import android.content.Context;
import android.util.Log;

public class NetMonitor extends AbstractMonitor{
	
	private static final int INDEX_TOTAL_BYTES = 0;
	private static final int INDEX_RECEIVE_BYTES = 1;
	private static final int INDEX_SEND_BYTES = 2;
	
	private final Context mContext;
	
	private boolean mStopMonitor;
	
	private String mMonitorPkgName;
	
	private final UcwebAppUtil mAppUtils;
	
	private String mFileSavePath;
	
	private final SimpleDateFormat sdf;
	
	public NetMonitor(Context context, String fileSavePath, String pkgName){
		super(context);
		this.mContext = context;
		mStopMonitor = false;
		mMonitorPkgName = pkgName;
		
		mFileSavePath = fileSavePath;
		
		mAppUtils = new UcwebAppUtil(mContext);
		sdf = UcwebDateUtil.YMDHMSDateFormat.getYMDHMSFormat();
	}
	
	public void stopNetMonitor(){
		mStopMonitor = true;
	}

	@Override
	public void startMonitor() {
		final int uid = mAppUtils.getUidByPackagename(mMonitorPkgName);
		if (uid < 0) return;
		
		final String fileName = createFileName(FileType.NetInfoFileType, mMonitorPkgName);
		
		doMonitorLoop(createFileFullPath(mFileSavePath, fileName), uid);
	}
	

	@Override
	public void stopMonitor() {
		stopNetMonitor();		
	}
	
	private final void doMonitorLoop(String fileFullPath, int uid) {
		
		while (!mStopMonitor) {
			long[] bytesChange = getTrafficChangeBytes(uid);
			
			if (bytesChange != null && bytesChange[INDEX_TOTAL_BYTES] > 0L) {
				
				String date = sdf.format(new Date());

				addInBuffer(makeOutputStyle(date, bytesChange[INDEX_TOTAL_BYTES], bytesChange[INDEX_RECEIVE_BYTES], 
						bytesChange[INDEX_SEND_BYTES]));
			}

			writeFileWhenBufferReachMaxCount(fileFullPath, 10);
		}
		
		/**结束测试后，刷新buffer，把buffer剩余数据写到文件*/
		flushBufferAndWriteFile(fileFullPath);
	}
	
	private final String makeOutputStyle(String date, long totalBytes, long receiveBytes, long sendBytes) {
		return date + "|" + totalBytes  + "|" + receiveBytes + "|" + sendBytes + "\n";
	}
	
	private long[] getTrafficChangeBytes(int uid){
		long[] bytesArray1 = mAppUtils.getAppTraffic(uid);
		
		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (Exception e) {
			Log.d(getLogTag(), e.toString());
		}
		long[] bytesArray2 = mAppUtils.getAppTraffic(uid);
		
		if (bytesArray1 != null && bytesArray2 != null) {
			
			return new long[]{
					bytesArray2[INDEX_TOTAL_BYTES] - bytesArray1[INDEX_TOTAL_BYTES], 
					bytesArray2[INDEX_RECEIVE_BYTES] - bytesArray1[INDEX_RECEIVE_BYTES], 
					bytesArray2[INDEX_SEND_BYTES] - bytesArray1[INDEX_SEND_BYTES]
				};
		}
		
		return null;
	}
	
}
