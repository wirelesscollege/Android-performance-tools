package com.ucweb.tools.utils;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.ucweb.tools.infobean.RecodeInfo;

public class UcwebInfoQueue {
	//recode info queue
	
	private final ConcurrentLinkedQueue<RecodeInfo> recodeInfoQueue;
	
	private static UcwebInfoQueue infoQueue = null;
	private UcwebInfoQueue(){
		recodeInfoQueue = new ConcurrentLinkedQueue<RecodeInfo>();
	}
	public static UcwebInfoQueue getInstance(){
		if (infoQueue == null) {
			synchronized (UcwebInfoQueue.class) {
				if (infoQueue == null) {
					infoQueue = new UcwebInfoQueue();
				}
			}
		}
		return infoQueue;
	}
	
	public boolean addInfo(RecodeInfo info){
		
		return info != null? this.recodeInfoQueue.offer(info) : false;
	}
	
	public RecodeInfo getInfo(){
		return this.recodeInfoQueue.poll();
	}
	
	public boolean isQueueEmpty(){
		return this.recodeInfoQueue.isEmpty();
	}
}
