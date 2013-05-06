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
