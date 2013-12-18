package com.ucweb.tools.infobean;

import android.graphics.drawable.Drawable;

public class AppInfo{
	/**bean*/
	private String packgeName;
	private String appName;
	private Drawable appIcon;
	private int pId;
	
	public AppInfo() {
		packgeName = "";
		appName = "";
		appIcon = null;
	}
	
	public int getpId() {
		return pId;
	}
	public void setpId(int pId) {
		this.pId = pId;
	}

	public String getPackgeName() {
		return packgeName;
	}
	public void setPackgeName(String packgeName) {
		this.packgeName = packgeName;
	}
	public String getAppName() {
		return appName;
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}

	public Drawable getAppIcon() {
		return appIcon;
	}
	public void setAppIcon(Drawable appIcon) {
		this.appIcon = appIcon;
	}
}	
