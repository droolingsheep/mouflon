package edu.ncsu.asbransc.mouflon.recorder;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbAdapter {

	public static final String KEY_ROWID = "_id";
	public static final String KEY_TIME = "time";
	public static final String KEY_PROCESSTAG = "processtag";
	public static final String KEY_EXTRA_1 = "extra1";
	public static final String KEY_EXTRA_2 = "extra2";
	public static final String KEY_EXTRA_3 = "extra3";
	public static final String KEY_EXTRA_4 = "extra4";
	
	private static final String DB_NAME = "data";
	private static final String TABLE_NAME = "logs";
	private static final int DB_VERSION = 2;
	private static final String DB_CREATE = "create table logs (_id integer primary key autoincrement, time text not null, processtag text not null, extra1 text not null, extra2 text not null, extra3 text not null, extra4 text not null);"; 
	//private static final String TAG = "Mouflon";
	
	private final Context mCtx;
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb; 
	
	public DbAdapter(Context mCtx) {
		this.mCtx = mCtx;
	}

	private class DatabaseHelper extends SQLiteOpenHelper {

		

		public DatabaseHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
			
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			
			db.execSQL(DB_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			
			//Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
		}
		
	}
	
	public DbAdapter open() {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}
	
	public void close() {
		mDbHelper.close();
	}
	
	public void clearDB() {
		//Log.i("Mouflon", "Clearing logs database");
		mDbHelper.onUpgrade(mDb, 2, 2);
	}
	
	public Cursor fetchAll() {
		return mDb.query(TABLE_NAME, new String[] {KEY_TIME, KEY_PROCESSTAG, KEY_EXTRA_1, KEY_EXTRA_2, KEY_EXTRA_3, KEY_EXTRA_4}, null, null, null, null, null);
	}
	
	public Cursor fetchPowerOn() {
		return mDb.query(TABLE_NAME, new String[] {KEY_TIME}, KEY_EXTRA_1 + " LIKE '1'", null, null, null, null);
	}
	
	public Cursor fetchPowerOff() {
		return mDb.query(TABLE_NAME, new String[] {KEY_TIME}, KEY_EXTRA_1 + " LIKE '0'", null, null, null, null);
	}
	
	public Cursor fetchStart() {
		return mDb.query(TABLE_NAME, new String[] {KEY_TIME}, KEY_EXTRA_1 + " LIKE 'START'", null, null, null, null);
	}
	
	public long addEntry(String time, String processtag, String extra1, String extra2, String extra3, String extra4) {
		ContentValues initialVals = new ContentValues();
		initialVals.put(KEY_TIME, time);
		initialVals.put(KEY_PROCESSTAG, processtag);
		initialVals.put(KEY_EXTRA_1, extra1);
		initialVals.put(KEY_EXTRA_2, extra2);
		initialVals.put(KEY_EXTRA_3, extra3);
		initialVals.put(KEY_EXTRA_4, extra4);
		
		return mDb.insert(TABLE_NAME, null, initialVals);
	}

	public Cursor fetchSelected(CharSequence search) {
		if (search.length() == 0)
			search = new String("%");
		else
			search = new String("%" + search + "%");
		
		return mDb.query(TABLE_NAME, new String[] {KEY_TIME, KEY_PROCESSTAG, KEY_EXTRA_1, KEY_EXTRA_2, KEY_EXTRA_3, KEY_EXTRA_4}, KEY_EXTRA_2 + " LIKE '" + search + "' OR " + KEY_EXTRA_3 + " LIKE '" + search + "' OR " + KEY_EXTRA_4 + " LIKE '" + search + "'", null, null, null, null);
	}

	public Cursor fetchApps() {
		return mDb.query(true, TABLE_NAME, new String[] {KEY_EXTRA_2}, null, null, null, null, null, null);
	}
}
