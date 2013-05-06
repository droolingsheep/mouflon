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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class NotificationService extends Service {
	
	public class NotificationBinder extends Binder {
		NotificationService getService() {
			return NotificationService.this;
		}
	}

	private final IBinder mBinder = new NotificationBinder();
	private Runnable mNotifyTask = new Runnable() {

		@Override
		public void run() {
			NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(NotificationService.this)
												.setSmallIcon(R.drawable.ic_launcher_bw)
												.setContentTitle("Mouflon Recorder")
												.setContentText("Please upload your files")
												.setAutoCancel(true)
												.setOngoing(false); 
			Intent toLaunch = new Intent(NotificationService.this, MainScreen.class);
			PendingIntent pi = PendingIntent.getActivity(NotificationService.this, 0, toLaunch, PendingIntent.FLAG_CANCEL_CURRENT);
			mBuilder.setContentIntent(pi);
			NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			manager.notify(0, mBuilder.build());
			stopSelf();
		}
		
	}; 
	
	@Override
	public IBinder onBind(Intent intent) {
		
		return mBinder;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Thread thr = new Thread(null, mNotifyTask, "Mouflon Notification");
		thr.start();
	}

}
