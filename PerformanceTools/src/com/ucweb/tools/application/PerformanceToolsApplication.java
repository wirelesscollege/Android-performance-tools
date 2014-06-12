package com.ucweb.tools.application;

/***
 * @author yangruyao
 */

import com.ucweb.tools.context.UcwebContext;
import com.ucweb.tools.utils.UcwebCrashHandler;

import android.app.Application;

public class PerformanceToolsApplication extends Application{

	
	@Override
	public void onCreate(){
		
		super.onCreate();
		
		UcwebContext.getContext(this).init();
		//use our error handler to handler uncaught exception
		UcwebCrashHandler.getInstance().init(getApplicationContext());
		
	}
	
}
