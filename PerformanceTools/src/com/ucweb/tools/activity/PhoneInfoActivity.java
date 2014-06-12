package com.ucweb.tools.activity;

import java.util.ArrayList;
import com.ucweb.tools.R;
import com.ucweb.tools.utils.UcwebPhoneInfoUtils;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class PhoneInfoActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_phone_info);
		
		final ListView listView = (ListView) findViewById(R.id.phone_activity_listView);
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, 
				getPhoneInfo());
		
		listView.setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.phone_info, menu);
		return false;
	}
	
	
	private final ArrayList<String> getPhoneInfo() {
		ArrayList<String> infoList = new ArrayList<String>();
		final UcwebPhoneInfoUtils phoneInfoUtil = new UcwebPhoneInfoUtils(this);
		
		infoList.add("手机型号: " + UcwebPhoneInfoUtils.getPhoneModel());
		infoList.add("手机IMEI: " + phoneInfoUtil.getPhoneIMEI());
		infoList.add("Android版本: " + UcwebPhoneInfoUtils.getOsVersion());
		
		return infoList;
	}
	
}
