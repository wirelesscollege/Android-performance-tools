package com.ucweb.tools.monitorTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.ucweb.tools.utils.UcwebDateUtil;
import com.ucweb.tools.utils.UcwebFileUtils.FileType;

import android.content.Context;
import android.util.Log;

public class IOWMonitor extends AbstractMonitor{
	
	private boolean mStopMonitor;
	
	private String[] mCmd;
	
	private final String mFileSavePath;
	
	private String mPkgName;
	
	private final SimpleDateFormat sdf;
	
	public IOWMonitor(Context context, String fileSavePath, String pkgName, String[] cmd){
		super(context);
		
		mFileSavePath = fileSavePath;
		mStopMonitor = false;
		mPkgName = pkgName;
		mCmd = cmd;
		
		sdf = UcwebDateUtil.YMDHMSDateFormat.getYMDHMSFormat();
	}
	
	public void stopIOWMonitor(){
		mStopMonitor = true;
	}
	
	@Override
	public void startMonitor() {
		
		String fileName = createFileName(FileType.IOWInfoFileType, mPkgName);
		
		doMonitorLoop(createFileFullPath(mFileSavePath, fileName));
	}	
	
	private final void doMonitorLoop(String fileFullPath) {
		
		InputStream is = null;
		BufferedReader  br = null;
		
		final Runtime runTime = Runtime.getRuntime();
		Process process = null;
		
		try {
			process = runTime.exec(mCmd);
		}catch (IOException e) {
			Log.e(getLogTag(), e.getMessage());
			return;
		}
			
		is = process.getInputStream();
			
		String temp = null;
			
		br = new BufferedReader(new InputStreamReader(is));
		
		
		while (!mStopMonitor) {
			try {
				temp = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
				try {
					TimeUnit.MILLISECONDS.sleep(10);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				continue;
			}
		
			/**如果为空，重新读一条*/
			if(temp == null) {
				try {
					TimeUnit.MILLISECONDS.sleep(10);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				continue;
			}

			if (temp.contains("System") && temp.contains("IOW")) {
				String iow = temp.split(", ")[2].trim();
				addInBuffer(makeOutputStyle(sdf.format(new Date()), iow));
			}else {
				/**读出的东西不是想要的，重新读一条*/
				continue;			
			}
			
			try {
				TimeUnit.SECONDS.sleep(5);
				} catch (Exception e) {
					e.printStackTrace();
			}
			
			writeFileWhenBufferReachMaxCount(fileFullPath, 10);
		}
		
		/**结束测试后，刷新buffer，把buffer剩余数据写到文件*/
		flushBufferAndWriteFile(fileFullPath);
		
		/**释放资源*/
		closeInputStream(is);
		closeBufferReader(br);
		destroyProcess(process);
	}
	
	private final String makeOutputStyle(String date, String iow) {
		return date + "|" + iow  + "\n";
	}
	
	
	
	@Override
	public void stopMonitor() {
		stopIOWMonitor();
	}
	
	private void closeInputStream(InputStream in) {
		if(in != null) {
			try {
				in.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void closeBufferReader(BufferedReader br) {
		if(br != null) {
			try {
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void destroyProcess(Process process) {
		try {
			process.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
}
