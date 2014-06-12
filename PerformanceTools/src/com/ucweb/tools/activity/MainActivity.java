package com.ucweb.tools.activity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.json.JSONException;

import com.ucweb.tools.config.Config;
import com.ucweb.tools.infobean.AppInfo;
import com.ucweb.tools.service.MonitorService;
import com.ucweb.tools.utils.UcwebAppUtil;
import com.ucweb.tools.utils.UcwebCommonTools.ToastTool;
import com.ucweb.tools.utils.UcwebJsonUtil;
import com.ucweb.tools.utils.UcwebNetUtils;
import com.ucweb.tools.utils.UcwebThreadPoolsManager;
import com.ucweb.tools.R;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
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

public class MainActivity extends ListActivity {
	//msg tag
	private static final int MSG_UPDATE_UI = 1;
	
	private static final int MSG_NO_NEW_VERSION = 99;
	
	private static final int MSG_NEW_VERSION_FOUND = 999;
	
	//thread pool
	private final UcwebThreadPoolsManager threadPoolManager = UcwebThreadPoolsManager.getThreadPoolManager();
	private ExecutorService mExecutor;
	
	//list view item tag
	private final String TAG_APP_ICON = "AppIcon";	//icon
	private final String TAG_APP_NAME = "AppName";	//app name
	private final String TAG_APP_PKGNAME = "AppPackgeName";	//package name
			
	private MyAdapter adapter;
	private ToastTool tt;
	private final MyHandler mHandler = new MyHandler(this);
	
	private Button btnStartTest;
	private Button btnStopTest;
	private Button btnUpload;
	private Button btnViewUploadRecode;
		
	private ProgressDialog mDialog;
	
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
		
		tt = new ToastTool(MainActivity.this);
		//init threadpool, init db
		threadPoolManager.init();
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
		final ToastTool toast = new ToastTool(getApplicationContext());
		btnStopTest.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				btnUpload.setEnabled(true);
				stopService(new Intent(MainActivity.this, MonitorService.class));
				toast.showLongTips("停止服务成功, 请到上传页面上传测试数据");
			}
		});
		
		//upload file button
		btnUpload = (Button) findViewById(R.id.btnUpload);
		btnUpload.setEnabled(false);
		btnUpload.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {				
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, UploadActivity.class);
				
				startActivity(intent);
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		menu.add(0, 1, 0, "设置");
		menu.add(0, 2, 0, "手机信息");
		menu.add(0, 3, 0, "更新");
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		
		switch (item.getItemId()) {
		case 1:
			startActivity(new Intent(this, SettingActivity.class));
			break;
			
		case 2:
			startActivity(new Intent(this, PhoneInfoActivity.class));
			break;
		
		case 3:
			onUpdateSelected();
			break;
			
		default:
			break;
		}
		
		return super.onMenuItemSelected(featureId, item);
	}
	
	private final void onUpdateSelected() {
		mExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				Message msg = Message.obtain();

				try {
					String retJson = UcwebNetUtils.doGet(Config.UPDATE_URL, null);
					String serverVersion = UcwebJsonUtil.getTagText(UcwebJsonUtil.getTagText(retJson, "version"), "version");
					Log.d("asd", retJson);
					if(isNewVersionRelease(serverVersion)) {
						msg.what = MSG_NEW_VERSION_FOUND;
						
//						String downloadUrl = UcwebJsonUtil.getTagText(retJson, "fileUrl");
//						
//						Uri uri = Uri.parse(downloadUrl);
//						Intent intent = new Intent(Intent.ACTION_VIEW, uri); 
//						startActivity(intent);		
					}
					else {
						msg.what = MSG_NO_NEW_VERSION;
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				mHandler.sendMessage(msg);
			}
		});

	}
	
	private boolean isNewVersionRelease(String serverVersion){
		boolean hasNewVersion = false;
		
		try {
			PackageInfo packInfo = getPackageManager().getPackageInfo(getPackageName(),0);
			String localVersion = packInfo.versionName;

			hasNewVersion = !(localVersion.equals(serverVersion));
			
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return hasNewVersion;
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
	}
	
	private static class MyHandler extends Handler{
		WeakReference<MainActivity> mActivity;
		
		public MyHandler(MainActivity activity){
			mActivity = new WeakReference<MainActivity>(activity);
		}
		
		@Override
		public void handleMessage(Message msg){
			MainActivity activity = mActivity.get();
			
			if(activity == null) return;
			
			switch (msg.what) {		
			case MSG_UPDATE_UI:
				
				activity.mDialog.dismiss();	
					
				@SuppressWarnings("unchecked")
				ArrayList<HashMap<String, Object>> infoList = (ArrayList<HashMap<String, Object>>) msg.obj;
				activity.adapter = activity.new MyAdapter(activity, infoList);
				activity.setListAdapter(activity.adapter);

				break;
				
			case MSG_NO_NEW_VERSION:
				activity.tt.showLongTips("当前版本就是最新的");
				break;

			case MSG_NEW_VERSION_FOUND:
				activity.tt.showShortTips("检测到新版本");
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
	
	private final HashMap<Integer, Integer> stats = new HashMap<Integer, Integer>(1);
	
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
			
			ViewHolder holder = null;
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
		/**
		 * 对于Listview来说，Activity OnCreate方法会调用Adapter的getView方法，当前屏幕显示几个Item就调用多少次
		 * 如果点击了Listview的item，那么系统会对当前屏幕可见的每一个item调用一次getView
		 * */

		@Override
		public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
			/**必须要先clear，这样保证只有1个RadioButton被选中*/
			stats.clear();
			stats.put(position, 100);
			adapter.notifyDataSetChanged();
			
			btnStartTest.setEnabled(true);
			
			@SuppressWarnings("unchecked")
			final HashMap<String, Object> item = (HashMap<String, Object>) parent.getAdapter().getItem(position);
			pkgName = (String) item.get(TAG_APP_PKGNAME);
			
		}
		
	}			
}
