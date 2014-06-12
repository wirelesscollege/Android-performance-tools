package com.ucweb.tools.activity;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.ucweb.tools.R;
import com.ucweb.tools.config.Config;
import com.ucweb.tools.context.UcwebContext;
import com.ucweb.tools.db.UcwebDBManager;
import com.ucweb.tools.infobean.RecodeInfo;
import com.ucweb.tools.infobean.RecodeInfo.UploadFlag;
import com.ucweb.tools.utils.UcwebCommonTools.ToastTool;
import com.ucweb.tools.utils.UcwebDateUtil.YMDDateFormat;
import com.ucweb.tools.utils.UcwebFileUtils;
import com.ucweb.tools.utils.UcwebNetUtils;
import com.ucweb.tools.utils.UcwebThreadPoolsManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class UploadActivity extends Activity {
	
	/**上传完成的消息*/
	static final int MSG_UPLOAD_FINISHED = 1;
	
	/**删除完成的消息*/
	static final int MSG_DELETE_FINISHED = 2;
	
	MyHandler handler;
	
	final static String LOG_TAG = UploadActivity.class.getSimpleName();
	
	UcwebDBManager dbManager;
	
	String fileSavePath;
	
	SimpleDateFormat dateFormater;
	
	private MyAdapter adapter;
	
	ExecutorService es;
	
	ToastTool tt;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upload);
		
		handler = new MyHandler(this);
		
		dbManager = UcwebDBManager.getInstance();
		dbManager.init(getApplicationContext());
		
		es = UcwebThreadPoolsManager.getThreadPoolManager().getExecutorService();
		
		fileSavePath = getFileSavePath();
		
		dateFormater = YMDDateFormat.getYMDFormat();

		ListView lv = (ListView) findViewById(R.id.upload_activity_listview);
		lv.setOnItemClickListener(new MyOnItemClickListener());
		
		adapter = new MyAdapter(this,filterFiles(fileSavePath));
		lv.setAdapter(adapter);
		
		tt = new ToastTool(UploadActivity.this);
		
		Button btnUpload = (Button) findViewById(R.id.btnUploadFile);
		btnUpload.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				List<String> files = getSelectedFile();
				uploadFile(files);
			}
		});
		
		Button btnDeleteFile = (Button) findViewById(R.id.btnDeleteFile);
		btnDeleteFile.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				asyncDeleteFiles();
			}
		});
	}
	
	private final void asyncDeleteFiles() {
		final List<String> selectedFiles = getSelectedFile();
		if(selectedFiles.isEmpty()) {
			tt.showShortTips("请选择要删除的文件");
		}
		else{
			es.execute(new Runnable() {
				
				@Override
				public void run() {
					for (String file : selectedFiles) {
						UcwebFileUtils.deleteFile(fileSavePath + file);
					}
					
					Message msg = Message.obtain();
					msg.what = MSG_DELETE_FINISHED;
					handler.sendMessage(msg);
				}
			});

		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.upload, menu);
		return false;
	}
	
	class MyOnItemClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
			if(isChecked(position)) {
				uncheckBox(position);
			}
			else {
				checkBox(position);
			}
			
			adapter.notifyDataSetChanged();
		}
		
	}
	
	
	static class ViewHolder{
		CheckBox checkBox;
		TextView filePath;
	}
	
	/**复选框的状态*/
	private static final SparseBooleanArray checkBoxStatus = new SparseBooleanArray();
	
	class MyAdapter extends BaseAdapter{
		
		private LayoutInflater mInflater = null;
		private List<String> mDataList;
		
		
		public MyAdapter(Context context, List<String> dataList){
			mInflater = LayoutInflater.from(context);
			mDataList = dataList;
			
			initCheckBox();
		}
		
		private void initCheckBox() {
			for (int i = 0; i < mDataList.size(); i++) {
				checkBoxStatus.put(i, false);
			}
		}
		
		public void setList(List<String> newDatas) {
			mDataList.clear();
			mDataList.addAll(newDatas);
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
				convertView = mInflater.inflate(R.layout.upload_listview_item, null);
				
				holder.checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
				holder.filePath = (TextView) convertView.findViewById(R.id.file_path);

				convertView.setTag(holder);
				
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			holder.checkBox.setChecked(getCheckBoxStatus(position));
			holder.filePath.setText(mDataList.get(position));
			
			return convertView;
		}
		
	}
	
	private final String getFileSavePath(){
	
		return UcwebContext.getContext(this).getFileSavePath();
	}
	
	private void notifyDBChange(List<RecodeInfo> list){
		//insert recode info into DB
		for (RecodeInfo recodeInfo : list) {
			dbManager.insertData(recodeInfo);
		}
	}
	
	/**上传文件*/
	private final void uploadFile(List<String> fileFullPathList) {
		if (fileFullPathList.isEmpty()) {
			tt.showShortTips("请选择勾选需要上传的问题");
		}
		else {
			asyncUploadFileAndUpdateDB(fileFullPathList);
		}
	}
	
	private final void asyncUploadFileAndUpdateDB(final List<String> fileList) {
		es.execute(new Runnable() {
			
			@Override
			public void run() {
				List<RecodeInfo> infoList = doUpload(fileList);
				notifyDBChange(infoList);
				
				Message msg = Message.obtain();
				msg.what = MSG_UPLOAD_FINISHED;
				handler.sendMessage(msg);
			}
		});
		
		
		
	}
	
	private final List<RecodeInfo> doUpload(List<String> fileList) {
		List<RecodeInfo> infoList = new ArrayList<RecodeInfo>();
		
		for(String file : fileList) {
			String fullPath = fileSavePath + file;
			RecodeInfo info = new RecodeInfo();
			try {
				UcwebNetUtils.uploadFile(Config.UPLOAD_URL, "file", fullPath);
				UcwebFileUtils.deleteFile(fullPath);
				
				info.date = dateFormater.format(new Date());
				info.path = fullPath;
				info.uploadFlag = UploadFlag.UPLOAD_SUCCESS;
			} catch (IOException e) {
				info.uploadFlag = UploadFlag.UPLOAD_FAILED;
				
				Log.e(LOG_TAG, e.getMessage());
			}
			
			infoList.add(info);
		}
		
		return infoList;
	}
	
	private boolean isChecked(int position) {
		return checkBoxStatus.get(position, false);
	}
	
	private boolean getCheckBoxStatus(int position) {
		return checkBoxStatus.get(position, false);
	}
	
	private void checkBox(int position) {
		checkBoxStatus.put(position, true);
	}
	
	private void uncheckBox(int position) {
		checkBoxStatus.put(position, false);
	}

	private final List<String> getSelectedFile() {	
		List<String> checkedFileList = new ArrayList<String>();
		
		final int itemCount = adapter.getCount();
		for (int i = 0; i < itemCount; i++) {
			if(isChecked(i)) {
				checkedFileList.add((String) adapter.getItem(i));
			}
		}
		
		return checkedFileList;
	}
	
	private final List<String> filterFiles(String dir) {
		Log.d(LOG_TAG, "开始扫描文件.....");
		File f = new File(dir);
		
		if(!f.exists())
			return null;
		
		List<String> fileList = new ArrayList<String>();
		
		final FilenameFilter filter = new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String filename) {
				if(!filename.contains("_")) {
					return false;
				}
				else {
					String[] str = filename.split("_");
					String fileNamePrefix = str[0];
					
					return "MonitorInfo".endsWith(fileNamePrefix) || "BatterInfo".endsWith(fileNamePrefix) ||
							"NetInfo".endsWith(fileNamePrefix) || "IOWInfo".endsWith(fileNamePrefix);
				}
			}
		};
		
		if(f.isFile()) {
			fileList.add(dir);
		}
		else {

			String[] files = f.list(filter);

			if(files.length > 0) {
				
				fileList.addAll(Arrays.asList(files));
			}
		}
		
		return fileList;
	}
	
	private static class MyHandler extends Handler {
		WeakReference<UploadActivity> mActivity;
		
		MyHandler(UploadActivity activity){
			mActivity = new WeakReference<UploadActivity>(activity);
		}
		
		@Override
		public void handleMessage(Message msg){
			UploadActivity activity = mActivity.get();
			if(activity == null) return;
			
			switch (msg.what) {		
			case MSG_UPLOAD_FINISHED:

				activity.adapter.setList(activity.filterFiles(activity.fileSavePath));
				activity.adapter.notifyDataSetChanged();
				activity.tt.showShortTips("上传成功");

				break;
				
			case MSG_DELETE_FINISHED:
				activity.adapter.setList(activity.filterFiles(activity.fileSavePath));
				activity.adapter.notifyDataSetChanged();
				activity.tt.showShortTips("删除文件成功");
				break;
				
			default:
				break;
			}
			super.handleMessage(msg);
		}
	}

}
