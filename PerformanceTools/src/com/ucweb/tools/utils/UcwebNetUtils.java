package com.ucweb.tools.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public class UcwebNetUtils {
	
	/**
	 * @param url			upload url
	 * @param params		post params
	 * @param file			absolute file path
	 * @return				if success, return String; else return null
	 * */
	public static String uploadFile(String url, String params, String file) throws IOException{
		Log.d("UcwebNetUtils", "start upload method");
		HttpClient httpClient = new DefaultHttpClient();
		httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		
		HttpPost post = new HttpPost(url);
		File uploadFile = new File(file);
		
		InputStream is = null;
		
		// <input type="file" name="userfile" />
		MultipartEntity entity = new MultipartEntity();
		ContentBody cbFile = new FileBody(uploadFile);
		entity.addPart(params, cbFile);
		
		post.setEntity(entity);
		try {
			HttpResponse response = httpClient.execute(post);
			if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
				
				HttpEntity httpEntity = response.getEntity();
				
				if (httpEntity != null) {
					
					is = httpEntity.getContent();
    				@SuppressWarnings("unused")
					int bytesRead = -1;
    				byte[] buffer = new byte[1024];
    				StringBuffer stringBuffer = new StringBuffer();
    				while ((bytesRead = is.read(buffer)) != -1) {
    					stringBuffer.append(new String(buffer).toCharArray());
    				}
    				
    				buffer = null;				
    				return stringBuffer.toString();
				}
			}
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return null;
	}
	
    public static String doGet(String baseUrl, List<NameValuePair> params) throws IOException{
    	HttpClient httpClient = new DefaultHttpClient();
    	HttpGet httpGet = new HttpGet(getCompletedUrl(baseUrl, params));
    	
    	ResponseHandler<String> handler = new BasicResponseHandler();
    	try {
    		String response = httpClient.execute(httpGet, handler);
    		return response;
    	} finally {
    		httpClient.getConnectionManager().shutdown();
    	} 	
    }
    
    public static String doGet(String baseUrl, Map<String, String> params) throws IOException{
    	List<NameValuePair> paramList = convertMap2List(params);
    	return doGet(baseUrl, paramList);
    }
    
    private static String getCompletedUrl(String baseUrl, List<NameValuePair> params) throws IOException{
    	if (!baseUrl.endsWith("?")) {
			baseUrl = baseUrl + "?";
		}
    	return baseUrl + EntityUtils.toString(new UrlEncodedFormEntity(params));
    }
    
    private static List<NameValuePair> convertMap2List(Map<String, String> params){
    	if (params.isEmpty())
    		return null;
    	List<NameValuePair> paramList = new ArrayList<NameValuePair>();
    	for (Map.Entry<String, String> entry : params.entrySet()) {
			paramList.add(new BasicNameValuePair(entry.getKey().trim(), entry.getValue().trim()));
		}
    	return paramList;
    }
}
