package edu.ncsu.asbransc.mouflon.recorder;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class WeeklyChecker extends Service {

	private IBinder mBinder = new WeeklyBinder(); 
	
	public class WeeklyBinder extends Binder {
		WeeklyChecker getService() {
			return WeeklyChecker.this;
		}
	}
	@Override
	public IBinder onBind(Intent intent) {
		
		return mBinder;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		//check if it's been a week
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int day = prefs.getInt(MainScreen.DAY_KEY, 0);
		//Log.i("Mouflon", "day " + day);
		if (day == 6) {
			day = -1;
			if (prefs.getBoolean(MainScreen.UPLOAD_KEY, true)) { //Call UploadFile
				Intent i = new Intent(this, UploadFile.class);
				i.putExtra("edu.ncsu.asbransc.mouflon.recorder.ManualUpload", false);
				startService(i);
			}
			else if (prefs.getBoolean(MainScreen.NOTIFY_KEY, true)) { //Call NotificationService
				startService(new Intent(this, NotificationService.class));
			}
		}
		Editor e = prefs.edit();
		day++;
		e.putInt(MainScreen.DAY_KEY, day);
		e.commit();
		stopSelf();
		
	}

}
