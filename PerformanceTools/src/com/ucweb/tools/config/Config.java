package com.ucweb.tools.config;

public class Config {
	/**上传URL*/
	public static final String UPLOAD_URL = "http://115.238.230.18:28043/Analysis/UploadServlet";
	
	public static final String UPDATE_URL = "http://an.test2.game.uc.cn:28043/Analysis/uploadsApp/CheckVersion.jsp";
	
	/**线程池大小*/
	public static final int MAX_THREAD_POOL_SIZE = 3;
	
	/**获取Monkey脚本URL*/
	public static final String GET_MONKEY_SCRIPT_URL = "http://an.test2.game.uc.cn:28043/Analysis/MonkeyServlet";
	
	/**Monkey脚本名字*/
	public static final String MONKEY_SCRIPT_FILE_NAME = "monkey_script.txt";
	
}
