package com.ucweb.tools.utils;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

public class UcwebJsonUtil {
	
	/***
	 * 获取指定tag的值
	 * @param json 需要解析的json
	 * @param tag 需要获取的tag
	 * @return 如tag存在，那么返回对应的值，若tag不存在，则返回空字符串
	 * @throws JSONException
	 */
	public static String getTagText(String json, String tag) throws JSONException {
		JSONObject jsonObj = new JSONObject(json);
		
		Iterator<?> keys = jsonObj.keys();
		
		while (keys.hasNext()) {
			
			String key = (String) keys.next();
			
			if(tag.equals(key)) {
				return jsonObj.getString(tag);
			}
			else {
				//如果值是json对象，那么递归解析
				Object value = jsonObj.get(key);
				
				if(value instanceof JSONObject) {
					return getTagText(value.toString(), tag);
				}
			}
		
		}
		
		return "";
	}
	
}
