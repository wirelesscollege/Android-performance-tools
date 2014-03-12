package com.ucweb.tools.monitorTask;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;

import com.ucweb.tools.utils.UcwebFileUtils;
import com.ucweb.tools.utils.UcwebJsonUtil;
import com.ucweb.tools.utils.UcwebNetUtils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class MonkeyTest implements Runnable{
	
	private Context context;
	
	//待测试app包名
	private String pkgName;
	
	//获取Monkey脚本URL
	private String url;
	
	//Monkey脚本名称
	private String monkeyScriptFileName;
	
	//等待时间
	private int mWaitTime;
	
	//屏幕分辨率
	private int[] screenXY;
	
	private static final int DEAULT_WAIT_TIME_IN_SECONDS = 50; 
	
	public static class Builder {
		private Context mContext;
		private String mPkgName;
		private String mUrl;
		
		private String fileName;
		
		private int[] screenXY;
		
		private int waitTime;
		
		public Builder(Context context) {
			mContext = context;
		}
		
		public Builder setPackage(String pkg) {
			mPkgName = pkg;
			return this;
		}
		
		public Builder setUrl(String url) {
			mUrl = url;
			return this;
		}
		
		public Builder setWaitTime(int time) {
			waitTime = time;
			return this;
		}
		
		public Builder setMonkeyScriptFileName(String fileName) {
			this.fileName = fileName;
			return this;
		}
		
		public Builder setScreenXY(int[] xy) {
			this.screenXY = xy;
			return this;
		}
		
		public MonkeyTest build() {
			return new MonkeyTest(this);
		}
	}
	
	public MonkeyTest(Builder builder) {
		context = builder.mContext;
		pkgName = builder.mPkgName;
		url = builder.mUrl;
		mWaitTime = builder.waitTime;
		if (mWaitTime == 0) {
			mWaitTime = DEAULT_WAIT_TIME_IN_SECONDS;
		}
		
		monkeyScriptFileName = builder.fileName;
		screenXY = builder.screenXY;
	}
	
	public void doMonkeyTest() {
//		启动待测试APP
//		startApp(pkgName);
		
		//获取脚本
		String script = getScript();
		Log.d("Johnny", script);
		
		//把获取的脚本写入本地
		writeScriptToFile(monkeyScriptFileName, script);
		
		try {
			TimeUnit.SECONDS.sleep(mWaitTime);
		} catch (InterruptedException ignore) {}
		
		//执行脚本
		executeScript(monkeyScriptFileName);
	}
	
//	private void startApp(String pkgName) {
//		UcwebAppUtil appUtil = new UcwebAppUtil(context);
//		appUtil.startAppAndGetPid(pkgName);
//	}
	
	private String getScript() {
		ArrayList<NameValuePair> param = new ArrayList<NameValuePair>();
		
		param.add(new BasicNameValuePair("type", "0"));
		param.add(new BasicNameValuePair("name", pkgName));
		Log.d("Johnny", "屏幕分辨率: " + screenXY[0] + "*" + screenXY[1]);
		param.add(new BasicNameValuePair("x", String.valueOf(screenXY[0])));
		param.add(new BasicNameValuePair("y", String.valueOf(screenXY[1])));
		
		try {
			String json = UcwebNetUtils.doGet(url, param);
			
			UcwebJsonUtil jsonUtil = new UcwebJsonUtil();
			jsonUtil.init(json);
			int retCode = (Integer) jsonUtil.getTagText("code");
			if (retCode == 0)
				return (String) jsonUtil.getTagText("msg");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private void writeScriptToFile(String fileName, String scriptMsg) {
		UcwebFileUtils fileWriter = new UcwebFileUtils(context);
		try {
			fileWriter.writeFile(fileName, scriptMsg, UcwebFileUtils.FileLocation.SDCARD);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public int executeScript(String fileName) {
		Process mProcess = null;
		DataOutputStream mDataOutputStream = null;
		int FLAG = 0;
		
		Runtime runtime = Runtime.getRuntime();
		Log.d("Tag","获取root权限:");

		try {
			mProcess = runtime.exec("su");
			mDataOutputStream = new DataOutputStream(mProcess.getOutputStream());
			mDataOutputStream.writeBytes("monkey -f "+Environment.getExternalStorageDirectory().getAbsolutePath()+
						File.separator+fileName + " -v 1" + "\n");
			
			mDataOutputStream.writeBytes("exit\n");
			mDataOutputStream.flush();
			mProcess.waitFor();
		} catch (Exception e) {
			Log.e("Johnny", e.getMessage());
		} finally {
			if(mDataOutputStream != null){
				try {
					mDataOutputStream.close();
				} catch (IOException ignore) {}
			}
			mProcess.destroy();
		}
//		Log.d("Tag","monkey -f "+"ִ�е������ǣ�"+ Environment.getExternalStorageDirectory().getAbsolutePath()+
//					File.separator+ fileName + " -v 1");

		FLAG = mProcess.exitValue();

		return FLAG;
	}
	
	private void checkFile(String fileName) {
		File script = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+
				File.separator+ fileName);
		if (script.exists()) {
			boolean bResult = script.delete();
			Log.d("Johnny", bResult ? "删除文件成功" : "删除文件失败");
		}
	}
	
	@Override
	public void run() {
		checkFile(monkeyScriptFileName);
		doMonkeyTest();		
	}

}
