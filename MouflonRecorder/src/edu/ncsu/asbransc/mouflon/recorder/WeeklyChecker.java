/*Mouflon: an Android app for collecting and reporting application usage.
    Copyright (C) 2013 Andrew Branscomb

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/
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
