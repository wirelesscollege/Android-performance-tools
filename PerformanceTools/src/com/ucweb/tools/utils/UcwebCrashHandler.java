package com.ucweb.tools.utils;

/***
 * @author yangruyao
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ucweb.tools.service.MonitorService;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class UcwebCrashHandler implements UncaughtExceptionHandler{
	
	public final String LOG_TAG = UcwebCrashHandler.class.getSimpleName();
	
	private Thread.UncaughtExceptionHandler mDefaultHanlder;
	
	private Context mContext;	
	
	private SimpleDateFormat format = UcwebDateUtil.YMDDateFormat.getYMDFormat();
	
	private UcwebCrashHandler(){}
	private static class UcwebCrashHandlerHolder{
		private static UcwebCrashHandler instance = new UcwebCrashHandler();
	}
	public static UcwebCrashHandler getInstance(){
		return UcwebCrashHandlerHolder.instance;
	}
	
	public void init(Context context){
		mContext = context;
		//get system default Uncaught Exception Handler
		mDefaultHanlder = Thread.getDefaultUncaughtExceptionHandler();
		//use customized handler instead of Uncaught Exception Handler
		Thread.setDefaultUncaughtExceptionHandler(this);
	}

	private boolean isExceptionHandled(Throwable ex){
		if (ex == null) {
			return false;
		}
		
		new Thread() {
			@Override
			public void run(){
				//如果要在线程里面显示Toast，必须要用Looper
				Looper.prepare();
				Toast.makeText(mContext, "抱歉，程序遇到异常，即将退出" + "\n" + "请联系作者：15-C2-3 杨如耀", Toast.LENGTH_LONG).show();
				Looper.loop();
			}
		}.start();

		saveCrashInfo2File(ex);
		
		return true;
	}
	
	private void saveCrashInfo2File(Throwable ex){
		//save crash info
		StringBuilder sb = new StringBuilder();
		
		Writer writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		ex.printStackTrace(printWriter);
		
		Throwable cause = ex.getCause();
		while(cause != null){
			cause.printStackTrace(printWriter);
			cause = cause.getCause();
		}
		
		printWriter.close();
		sb.append(writer.toString());
		
		String time = format.format(new Date());
		String fileName = "crashLog-" + time + "-" + UcwebPhoneInfoUtils.getPhoneModel() + ".log";
		
		UcwebFileUtils fileWriter = new UcwebFileUtils(mContext);
		
		try {			
			fileWriter.writeFile(fileName, sb, UcwebFileUtils.FileLocation.SDCARD);
			
		} catch (Exception e) {
			Log.e(LOG_TAG, "an error occured while writing file...", e);  
		}
	}
	
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		if (!isExceptionHandled(ex) && mDefaultHanlder != null) {
			//如果用户没有处理则让系统默认的异常处理器来处理
			mDefaultHanlder.uncaughtException(thread, ex);
		} else {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				 Log.e(LOG_TAG, "error : ", e);
			}
		}
		Intent intent = new Intent(mContext, MonitorService.class);
		mContext.stopService(intent);
		
		android.os.Process.killProcess(android.os.Process.myPid());
		System.exit(0);
	}	
}
