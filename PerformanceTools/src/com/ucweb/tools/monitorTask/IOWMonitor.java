package com.ucweb.tools.monitorTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ucweb.tools.infobean.RecodeInfo;
import com.ucweb.tools.utils.UcwebDateUtil;
import com.ucweb.tools.utils.UcwebFileUtils;

import android.content.Context;
import android.util.Log;

public class IOWMonitor extends AbstractMonitor{
	
	private final String LOG_TAG;
	
	private boolean mStopMonitor;
	private Context mContext;
	
	private String[] mCmd;
			
	private UcwebFileUtils fileWriter;
	
	private String mPkgName;
	
	private final List<String> infoBuffer;
	
	private final SimpleDateFormat sdf;
	
	public IOWMonitor(Context context, String pkgName, String[] cmd){
		super(context);
		
		LOG_TAG = getLogTag();
		
		this.mContext = context;
		mStopMonitor = false;
		mPkgName = pkgName;
		mCmd = cmd;
		
		sdf = UcwebDateUtil.YMDHMSDateFormat.getYMDHMSFormat();

		infoBuffer = new ArrayList<String>(20);
	}
	
	public void stopIOWMonitor(){
		mStopMonitor = true;
	}
	
	private String getIOWInfo() throws IOException{
		/**get CPU and Memory size*/
		InputStream is = null;
		BufferedReader  br = null;
		
		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (Exception e) {
			Log.e("Johnny", e.toString());
		}
		try {
			Runtime runTime = Runtime.getRuntime();
			Process process = runTime.exec(mCmd);
			is = process.getInputStream();
			br = new BufferedReader(new InputStreamReader(is));
			
			StringBuilder result = new StringBuilder(60);
			
			String temp = null;
			while ((temp = br.readLine()) != null) {
				if (temp.contains("System") && temp.contains("IOW")) {
					String iow = temp.split(", ")[2].trim();
					result.append(sdf.format(new Date()) + "|" + iow + "\n");
				}
			}
			return result.toString();
		} catch (Exception e) {
			throw new IOException("A erroc Accour while execute command: " + e.getMessage());
		} finally {
			try {
				is.close();
			} catch (Exception ignore) {}
			
			try {
				br.close();
			} catch (Exception ignore) {}
		}	
	}
	
	@Override
	public void startMonitor() {
		
		fileWriter = new UcwebFileUtils(mContext);
		String fileName = fileWriter.generateFileName(UcwebFileUtils.FileType.IOWInfoFileType, 
				mPkgName == null? "Unknown" : mPkgName);
		RecodeInfo info = createRecode(fileName);
		
		while (!mStopMonitor) {
			try {
				String iowInfo = getIOWInfo();
//				Log.d(LOG_TAG, iowInfo);
				
				infoBuffer.add(iowInfo);
				if (infoBuffer.size() == 20) {
					fileWriter.writeDatas(fileName, infoBuffer, UcwebFileUtils.FileStorageLocation.LOCATION_SDCARD);
					infoBuffer.clear();
				}
			} catch (IOException e) {
				Log.d(LOG_TAG, "write CpuMemMonitor file, below is exception message:\n" + e.getMessage());				
			}
		}
		addInQueue(info);
	}	

	@Override
	public void stopMonitor() {
		stopIOWMonitor();
	}
}
