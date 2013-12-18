package com.ucweb.tools.activity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.ucweb.tools.db.UcwebDBManager;
import com.ucweb.tools.infobean.AppInfo;
import com.ucweb.tools.infobean.RecodeInfo;
import com.ucweb.tools.service.MonitorService;
import com.ucweb.tools.utils.UcwebAppUtil;
import com.ucweb.tools.utils.UcwebInfoQueue;
import com.ucweb.tools.utils.UcwebNetUtils;
import com.ucweb.tools.utils.UcwebThreadPoolsManager;
import com.ucweb.tools.R;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
	private static final int MSG_TAG = 1;
	
	private final String LOG_TAG = MainActivity.class.getSimpleName();
	
	//thread pool
	private final UcwebThreadPoolsManager threadPoolManager = UcwebThreadPoolsManager.getInstance();
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
	
	private static final String UPLOAD_URL = "http://115.238.230.18:28043/Analysis/UploadServlet";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		//show loading dialog
		mDialog = new ProgressDialog(this);
		mDialog.setMessage("正在加载本机进程信息，请稍后.....");
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
				msg.what = MSG_TAG;
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
						notifyDBChange(uploadList);
						
						Looper.prepare();
						Toast.makeText(getApplicationContext(), "上传文件完毕.....!", Toast.LENGTH_LONG).show();
						Looper.loop();
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
				UcwebNetUtils.uploadFile(UPLOAD_URL, "file", recodeInfo.path);
				//upload success, update uploadFlag to UPLOADED
				recodeInfo.uploadFlag = RecodeInfo.UploadFlag.UPLOADED;
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
	
	private void notifyDBChange(List<RecodeInfo> list){
		//insert recode info into DB
		if (list != null) {
			for (RecodeInfo recodeInfo : list) {
				dbManager.insertData(recodeInfo);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
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
			
			switch (msg.what) {
			
			case MSG_TAG:
				MainActivity activity = mActivity.get();
				if (activity != null) {					
					activity.mDialog.dismiss();	
					
					@SuppressWarnings("unchecked")
					ArrayList<HashMap<String, Object>> infoList = (ArrayList<HashMap<String, Object>>) msg.obj;
					activity.adapter = activity.new MyAdapter(activity, infoList);
					activity.setListAdapter(activity.adapter);
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
