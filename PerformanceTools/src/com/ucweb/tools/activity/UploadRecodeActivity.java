package com.ucweb.tools.activity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.ucweb.tools.R;
import com.ucweb.tools.db.UcwebDBManager;
import com.ucweb.tools.infobean.RecodeInfo.UploadFlag;
import com.ucweb.tools.utils.UcwebThreadPoolsManager;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class UploadRecodeActivity extends Activity{
	
	private final static int MSG_TAG = 1;
	
	private final ExecutorService es = UcwebThreadPoolsManager.getInstance().getExecutorService();
	
	private ListView lv;
	
	private UcwebDBManager dbManager;
	
	private ProgressDialog mDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.upload_recode_activity);
		
		mDialog = new ProgressDialog(this);
		mDialog.setMessage("正在加载上传信息，请稍后.....");
		mDialog.setIndeterminate(false);
		mDialog.setCancelable(false);
		mDialog.show();
		
		dbManager = UcwebDBManager.getInstance();		
		
		lv = (ListView) findViewById(R.id.upload_listview);
			
		final MyHandler handler = new MyHandler(this);
		
		es.execute(new Runnable() {
			
			@Override
			public void run() {
				List<Map<String, Object>> dataList = getData();
				
				Message msg = Message.obtain();
				msg.what = MSG_TAG;
				msg.obj = dataList;
				handler.sendMessage(msg);
			}
		});
		
	}
	
	@Override
	public void onBackPressed(){
		super.onBackPressed();
		this.finish();
	}
	
	private static class MyHandler extends Handler{
		WeakReference<UploadRecodeActivity> mActivity;
		
		public MyHandler(UploadRecodeActivity activity){
			mActivity = new WeakReference<UploadRecodeActivity>(activity);
		}
		
		@Override
		public void handleMessage(Message msg){
			
			switch (msg.what) {
			
			case MSG_TAG:
				UploadRecodeActivity activity = mActivity.get();
				if (activity != null) {					
					activity.mDialog.dismiss();	
					
					@SuppressWarnings("unchecked")
					List<HashMap<String, Object>> infoList = (List<HashMap<String, Object>>) msg.obj;
					SimpleAdapter adapter = new SimpleAdapter(activity, infoList, 
							R.layout.upload_recode_activity_item, 
							new String[] {"TAG_PATH", "TAG_DATE", "TAG_IS_UPLOADED"}, 
							new int[] {R.id.file_path, R.id.date, R.id.upload_flag});
					
					activity.lv.setAdapter(adapter);
					}
				else {
					Log.d("UploadRecodeActivity", "activity is null");
				}
				break;

			default:
				break;
			}
			super.handleMessage(msg);
		}
		
	}
	
	private List<Map<String, Object>> getData(){
		List<Map<String, Object>> dataList = new ArrayList<Map<String,Object>>();
		
		//get recently week upload info
		Cursor c = dbManager.queryData();;
		try {								
			while(c.moveToNext()) {
				
				Map<String, Object> data = new HashMap<String, Object>(4);
				
				String path = c.getString(c.getColumnIndex("path"));
				String date = c.getString(c.getColumnIndex("date"));
				int uploadFlag = c.getInt(c.getColumnIndex("isUploaded"));
				
				switch (uploadFlag) {
				
					case UploadFlag.NOT_UPLOAD:
						data.put("TAG_PATH", "文件路径: " + path);
						data.put("TAG_DATE", "日期: " + date);
						data.put("TAG_IS_UPLOADED", "上传状态：未上传");
						break;
						
					case UploadFlag.UPLOAD_FAILED:
						data.put("TAG_PATH", "文件路径: " + path);
						data.put("TAG_DATE", "上传日期: " + date);
						data.put("TAG_IS_UPLOADED", "上传状态：上传失败");
						break;
						
					case UploadFlag.UPLOADED:
						data.put("TAG_PATH", "文件路径: " + path);
						data.put("TAG_DATE", "上传日期: " + date);
						data.put("TAG_IS_UPLOADED", "上传状态：已上传");
						break;
					default:
						break;
				}
				dataList.add(data);
			}
			
			return dataList;
		} finally {
			c.close();
		}				
	}
	
}
