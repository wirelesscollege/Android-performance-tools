package com.ucweb.tools.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class UcwebDBHelper extends SQLiteOpenHelper{
	
	private final static String DB_NAME = "upload.db";
	private final static int DB_VERSION = 1;
	public final String TABLE_NAME = "T_UPLOAD_RECODE";
	
	public UcwebDBHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
	
	public UcwebDBHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, null, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		initDb(db);
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// do nothing
		
	}
	
	private void initDb(SQLiteDatabase db){
		//create table
		db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + 
				"(_id INTEGER PRIMARY KEY AUTOINCREMENT, path VARCHAR(128), date VARCHAR(12), isUploaded TINYINT)");
	}

}
