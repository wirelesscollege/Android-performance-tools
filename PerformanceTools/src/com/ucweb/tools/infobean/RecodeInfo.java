package com.ucweb.tools.infobean;

public class RecodeInfo {
	public String path;
	public String date;
	public int uploadFlag;
	
	//upload status
	public static class UploadFlag{
		/**
		 * have upload file*/
		public static final int UPLOAD_SUCCESS = 1;		
		/**
		 * not upload file*/
		public static final int NOT_UPLOAD = 2;
		/**
		 * upload file failed*/
		public static final int UPLOAD_FAILED = 3;
	}
}
