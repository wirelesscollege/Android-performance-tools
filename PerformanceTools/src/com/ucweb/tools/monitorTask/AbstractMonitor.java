package com.ucweb.tools.monitorTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.os.Environment;

import com.ucweb.tools.infobean.RecodeInfo;
import com.ucweb.tools.utils.UcwebDateUtil;
import com.ucweb.tools.utils.UcwebFileUtils;
import com.ucweb.tools.utils.UcwebInfoQueue;

/***
 * 
 * 所有详细监控类父类，提供了创建日志记录、获取Log tag、添加日志记录到队列模板方法
 *
 */

abstract class AbstractMonitor {
	private Context mContext;
	private final UcwebInfoQueue recodeInfoQueue;
	
	private final SimpleDateFormat sdf;
	
	public AbstractMonitor(Context context){
		mContext = context;
		recodeInfoQueue = UcwebInfoQueue.getInstance();
		sdf = UcwebDateUtil.YMDDateFormat.getYMDFormat();
	}
	
	/***
	 * 创建入库记录，以便添加进记录队列
	 * @param fileName
	 * @return
	 */
	protected RecodeInfo createRecode(String fileName){
		RecodeInfo recodeInfo = new RecodeInfo();
		
		if (UcwebFileUtils.isSdcardAvailable()) {
			recodeInfo.path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + fileName;
		} else {
			recodeInfo.path = mContext.getFilesDir().getPath() + File.separator + fileName;
		}
		
		recodeInfo.date = sdf.format(new Date());
		recodeInfo.uploadFlag = RecodeInfo.UploadFlag.NOT_UPLOAD; 
		
		return recodeInfo;
	}
	
	/***
	 * 添加入库记录到队列
	 * @param info
	 * @return
	 */
	protected boolean addInQueue(RecodeInfo info){
		return recodeInfoQueue.addInfo(info);
	}
	
	/***
	 * 获取日志Tag
	 * @return
	 */
	final protected String getLogTag(){
		return this.getClass().getSimpleName();
	}
	
	/**
	 * 开始监控抽象方法，需要子类去实现
	 */
	public abstract void startMonitor();
	
	/**
	 * 停止监控抽象方法
	 */
	public abstract void stopMonitor();
	
}
