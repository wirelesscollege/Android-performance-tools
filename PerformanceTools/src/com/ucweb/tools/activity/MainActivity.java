package com.ucweb.tools.activity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.ucweb.tools.config.Config;
import com.ucweb.tools.db.UcwebDBManager;
import com.ucweb.tools.infobean.AppInfo;
import com.ucweb.tools.infobean.RecodeInfo;
import com.ucweb.tools.service.MonitorService;
import com.ucweb.tools.utils.UcwebAppUtil;
import com.ucweb.tools.utils.UcwebFileUtils;
import com.ucweb.tools.utils.UcwebInfoQueue;
import com.ucweb.tools.utils.UcwebNetUtils;
import com.ucweb.tools.utils.UcwebThreadPoolsManager;
import com.ucweb.tools.R;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ListActivity {
	//msg tag
	private static final int MSG_UPDATE_UI = 1;
	//show tips msg tag
	private static final int MSG_TAG_SHOW_TIPS = 2;
	//tips flag, 1 means upload success
	private static final int UPLOAD_SUCCESS = 1;
	//tips flag, 2 means no file need to upload
	private static final int NO_FILE_NEED_TO_UPLOAD = 2;
	
	//Log tag
	private final String LOG_TAG = MainActivity.class.getSimpleName();
	
	//thread pool
	private final UcwebThreadPoolsManager threadPoolManager = UcwebThreadPoolsManager.getThreadPoolManager();
	private ExecutorService mExecutor;
	
	//db manager
	final UcwebDBManager dbManager = UcwebDBManager.getInstance();
	
	//list view item tag
	private final String TAG_APP_ICON = "AppIcon";	//icon
	private final String TAG_APP_NAME = "AppName";	//app name
	private final String TAG_APP_PKGNAME = "AppPackgeName";	//package name
			
	private MyAdapter adapter;
	
	private final MyHandler mHandler = new MyHandler(this);
	
	private Button btnStartTest;
	private Button btnStopTest;
	private Button btnUpload;
	private Button btnViewUploadRecode;
		
	private ProgressDialog mDialog;
	
	@SuppressLint("UseSparseArrays")
	private HashMap<Integer, Integer> stats = new HashMap<Integer, Integer>(1);
	
	private String pkgName;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		//show loading dialog
		mDialog = new ProgressDialog(this);
		mDialog.setMessage("正在加载手机应用信息，请稍后.....");
		mDialog.setIndeterminate(false);
		mDialog.setCancelable(false);
		mDialog.show();		
		
		//init threadpool, init db
		threadPoolManager.init();
		dbManager.init(getApplicationContext());
		mExecutor = threadPoolManager.getExecutorService();	
		
		//loading process info and update in UI
		mExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				ArrayList<HashMap<String, Object>> mLoadDataList = getData();
				
				Message msg = Message.obtain();
				msg.what = MSG_UPDATE_UI;
				msg.obj = mLoadDataList;
				MainActivity.this.mHandler.sendMessage(msg);
			}
		});
		
		final ListView listView = (ListView) findViewById(android.R.id.list);
		listView.setOnItemClickListener(new UcwebOnItemClickListener());
		
		//start test button
		btnStartTest = (Button) findViewById(R.id.btnStartTest);
		btnStartTest.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, OperationActivity.class);
				intent.putExtra("pkgName", pkgName);
				
				startActivity(intent);
			}
		});
		
		//stop test button
		btnStopTest = (Button) findViewById(R.id.btnStopTest);
		btnStopTest.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				btnUpload.setEnabled(true);
				stopService(new Intent(MainActivity.this, MonitorService.class));
				
			}
		});
		
		//upload file button
		btnUpload = (Button) findViewById(R.id.btnUpload);
		btnUpload.setEnabled(false);
		btnUpload.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {				
				mExecutor.execute(new Runnable() {
					
					@Override
					public void run() {
						//upload file
						List<RecodeInfo> uploadList = upload();
						//insert data into db
						Message msg = Message.obtain();
						msg.what = MSG_TAG_SHOW_TIPS;
						
						if (uploadList != null) {
							notifyDBChange(uploadList);
							msg.arg1 = UPLOAD_SUCCESS;
						} else {
							msg.arg1 = NO_FILE_NEED_TO_UPLOAD;
						}			
						MainActivity.this.mHandler.sendMessage(msg);
					}
				});
			}
		});
		
		btnViewUploadRecode = (Button) findViewById(R.id.btnViewUploadRecode);
		btnViewUploadRecode.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, UploadRecodeActivity.class);				
				startActivity(intent);				
			}
		});
	}
	
	//loading app info, and show on main activity
	private ArrayList<HashMap<String, Object>> getData(){
		ArrayList<HashMap<String, Object>> dataList = new ArrayList<HashMap<String,Object>>(32);
		
		UcwebAppUtil apputil = new UcwebAppUtil(getApplicationContext());	
		
		List<AppInfo> processInfoList = apputil.getInstalledAppInfo();;
		
		for (AppInfo appInfo : processInfoList) {
			HashMap<String, Object> app = new HashMap<String, Object>(4);
				
			app.put(TAG_APP_ICON, appInfo.getAppIcon());
			app.put(TAG_APP_NAME, appInfo.getAppName());
			app.put(TAG_APP_PKGNAME, appInfo.getPackgeName());
			
			dataList.add(app);				
		}
		return dataList;
	}
	
	//upload file
	private List<RecodeInfo> upload(){
		//get info queue instance
		final UcwebInfoQueue infoQueue = UcwebInfoQueue.getInstance();
		if (infoQueue.isQueueEmpty()) {
			Log.d(LOG_TAG, "no file need to upload");
			return null;
		}
		
		List<RecodeInfo> infoList = new ArrayList<RecodeInfo>();			
		RecodeInfo recodeInfo = infoQueue.getInfo();
		
		while (recodeInfo != null) {
			try {
				//upload file
				UcwebNetUtils.uploadFile(Config.UPLOAD_URL, "file", recodeInfo.path);
				//upload success, update uploadFlag to UPLOADED
				recodeInfo.uploadFlag = RecodeInfo.UploadFlag.UPLOADED;
				//delete file
				UcwebFileUtils.deleteFile(recodeInfo.path);
				Log.d(LOG_TAG, "upload success");
			} catch (IOException e) {
				//occur exception ,upload file failed, and update uploadFlag to UPLOAD_FAILED
				recodeInfo.uploadFlag = RecodeInfo.UploadFlag.UPLOAD_FAILED;
			}
			
			infoList.add(recodeInfo);
			
			recodeInfo = infoQueue.getInfo();
		}
		return infoList;
	}
	
	//update DB
	private void notifyDBChange(List<RecodeInfo> list){
		//insert recode info into DB
		for (RecodeInfo recodeInfo : list) {
			dbManager.insertData(recodeInfo);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		menu.add(0, 1, 0, "设置");
		menu.add(0, 2, 0, "关于");
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		
		switch (item.getItemId()) {
		case 1:
			startActivity(new Intent(this, SettingActivity.class));
			break;
			
		case 2:
			Toast.makeText(getApplicationContext(), "还没有写内容，呵呵！", Toast.LENGTH_LONG).show();
			break;

		default:
			break;
		}
		
		return super.onMenuItemSelected(featureId, item);
	}
	
	@Override
	public void onBackPressed(){
		super.onBackPressed();
		releaseResource();
		MainActivity.this.finish();
	}	
	
	@Override
	public void onDestroy(){
		releaseResource();
		super.onDestroy();
	}
	
	private void releaseResource(){
		if (!mExecutor.isShutdown()) {
			threadPoolManager.shutdownThreadPool();
		}
		dbManager.closeDB();
	}
	
	private static class MyHandler extends Handler{
		WeakReference<MainActivity> mActivity;
		
		public MyHandler(MainActivity activity){
			mActivity = new WeakReference<MainActivity>(activity);
		}
		
		@Override
		public void handleMessage(Message msg){
			MainActivity activity = mActivity.get();
			
			switch (msg.what) {		
			case MSG_UPDATE_UI:
				
				if (activity != null) {					
					activity.mDialog.dismiss();	
					
					@SuppressWarnings("unchecked")
					ArrayList<HashMap<String, Object>> infoList = (ArrayList<HashMap<String, Object>>) msg.obj;
					activity.adapter = activity.new MyAdapter(activity, infoList);
					activity.setListAdapter(activity.adapter);
					}
				break;
			
			case MSG_TAG_SHOW_TIPS:
				if (msg.arg1 == UPLOAD_SUCCESS) {
					Toast.makeText(activity.getApplicationContext(), "上传记录成功", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(activity.getApplicationContext(), "没有记录需要上传或已经上传", Toast.LENGTH_SHORT).show();
				}
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
		
	}
	
	static class ViewHolder{
		RadioButton radioBtn;
		ImageView appIcon;
		TextView appName;
		TextView pkgName;
	}
	
	class MyAdapter extends BaseAdapter{
		
		private LayoutInflater mInflater = null;
		private ArrayList<HashMap<String, Object>> mDataList;
		
		public MyAdapter(Context context, ArrayList<HashMap<String, Object>> dataList){
			mInflater = LayoutInflater.from(context);
			mDataList = dataList;
		}
		
		@Override
		public int getCount() {			
			return mDataList.size();
		}

		@Override
		public Object getItem(int position) {
			return mDataList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder =null;
			if (convertView == null) {
				
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.main_activity_listview_item, null);
				
				holder.radioBtn = (RadioButton) convertView.findViewById(R.id.radiobutton);
				holder.appIcon = (ImageView) convertView.findViewById(R.id.appicon);
				holder.appName = (TextView) convertView.findViewById(R.id.app_name);
				holder.pkgName = (TextView) convertView.findViewById(R.id.packge_name);
				
				convertView.setTag(holder);
				
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			holder.radioBtn.setChecked(stats.get(position) == null ? false : true);
			holder.appIcon.setBackgroundDrawable((Drawable)mDataList.get(position).get(TAG_APP_ICON));
			holder.appName.setText((String)mDataList.get(position).get(TAG_APP_NAME));
			holder.pkgName.setText((String)mDataList.get(position).get(TAG_APP_PKGNAME));
			
			return convertView;
		}
		
	}
	
	class UcwebOnItemClickListener implements OnItemClickListener{

		@Override
		public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
			stats.clear();
			stats.put(position, 100);
			adapter.notifyDataSetChanged();
			
			btnStartTest.setEnabled(true);
			
			@SuppressWarnings("unchecked")
			final HashMap<String, Object> item = (HashMap<String, Object>) parent.getAdapter().getItem(position);
			pkgName = (String) item.get(TAG_APP_PKGNAME);
			
			Log.d(LOG_TAG, "get package name: " + pkgName);
		}
		
	}			
}
