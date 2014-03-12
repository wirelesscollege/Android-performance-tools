package com.ucweb.tools.monitorTask;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;

import com.ucweb.tools.infobean.RecodeInfo;
import com.ucweb.tools.utils.UcwebDateUtil;
import com.ucweb.tools.utils.UcwebFileUtils;
import com.ucweb.tools.utils.UcwebInfoQueue;

/***
 * 
 * Monitorable接口的直接实现骨架类，具体监控类需要从该类扩展
 *
 */

abstract class AbstractMonitor implements Monitorable{
	private Context mContext;
	private final UcwebInfoQueue recodeInfoQueue;
	
	private final SimpleDateFormat sdf;
	
	public AbstractMonitor(Context context){
		mContext = context;
		recodeInfoQueue = UcwebInfoQueue.getInstance();
		sdf = UcwebDateUtil.YMDDateFormat.getYMDFormat();
	}
	
	/***
	 * 创建入库记录
	 * @param fileName
	 * @return
	 */
	protected RecodeInfo createRecode(String fileName){
		RecodeInfo recodeInfo = new RecodeInfo();
		
		recodeInfo.path = new UcwebFileUtils(mContext).autoGenerateFilePath() + fileName;
		recodeInfo.date = sdf.format(new Date());
		recodeInfo.uploadFlag = RecodeInfo.UploadFlag.NOT_UPLOAD; 
		
		return recodeInfo;
	}
	
	/***
	 * 添加记录到队列
	 * @param info
	 * @return
	 */
	protected boolean addInQueue(RecodeInfo info){
		return recodeInfoQueue.addInfo(info);
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
