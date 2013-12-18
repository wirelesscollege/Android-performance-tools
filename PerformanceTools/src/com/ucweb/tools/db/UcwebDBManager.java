package com.ucweb.tools.db;

import com.ucweb.tools.infobean.RecodeInfo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class UcwebDBManager {
	private static UcwebDBManager instance;
	
	private UcwebDBHelper helper;
	private SQLiteDatabase db;
	
	//retrieve the records in recent week.
	private final String getLatestWeekUploadRecode = "SELECT * FROM T_UPLOAD_RECODE T WHERE "
			+ "(julianday(date('now', 'localtime')) - julianday(T.date)) < 8";
	
	//delete the records while saved over 30days.
	private final String deleteOver30DaysUploadRecode = "DELETE FROM T_UPLOAD_RECODE T WHERE "
			+ "(julianday(date('now', 'localtime')) - julianday(T.date)) > 30";
	
	private UcwebDBManager(){}
	
	public static UcwebDBManager getInstance(){
		if (instance == null) {
			synchronized (UcwebDBManager.class) {
				if (instance == null) {
					instance = new UcwebDBManager();
				}
			}
		}
		return instance;
	}
	
	public void init(Context context){
		Log.d("UcwebDBManager", "init database....");
		helper = new UcwebDBHelper(context);
		db = helper.getWritableDatabase();
	}
	
	public void insertData(RecodeInfo info){
		db.beginTransaction();
		try {
			db.execSQL("INSERT INTO T_UPLOAD_RECODE VALUES(null, ?, ?, ?)", new Object[]{info.path, info.date, info.uploadFlag});
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
	
	public void updateUploadStatus(RecodeInfo info){
		ContentValues cv = new ContentValues();
		cv.put("isUploaded", info.uploadFlag);
		db.update(helper.TABLE_NAME	, cv, "path = ? and date = ?", new String[]{info.path, info.date});
	}
	
	public void deleteOldDatas(){
		db.execSQL(deleteOver30DaysUploadRecode);
	}
	
	public int getAllDataCount(){
		Cursor c = db.rawQuery("SELECT * FROM T_UPLOAD_RECODE", null);
		return c.getCount();
	}
	
	
	public Cursor queryData(){
		return db.rawQuery(getLatestWeekUploadRecode, null);
	}
	
	public void closeDB(){
		if (db.isOpen()) {
			Log.d("UcwebDBManager", "shutdown database....");
			db.close();
		}
	}
}
