package com.ucweb.tools.utils;

import org.json.JSONException;
import org.json.JSONObject;

public class UcwebJsonUtil {
	
	private JSONObject mObj;
	
	public void init(String json) {
		try {
			mObj = new JSONObject(json);
		} catch (JSONException e) {
			
		}
	}
	
	public Object getTagText(String tag) throws JSONException {
		if (tag == null || "".equals(tag)) 
			throw new IllegalArgumentException("Illegal argument: tag may be null or empty");
		
		if (mObj == null)
			throw new JSONException("you must call init method before getTagText");
		
		return mObj.get(tag);
	}
	
}
