package edu.ncsu.asbransc.mouflon.recorder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public class RegisterAlarm extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (prefs.getBoolean(MainScreen.AUTH_KEY, false)) {
			Intent i = new Intent(context, RecorderService.class);
			PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() - (3600*1000), AlarmManager.INTERVAL_HALF_HOUR, pi);
			//Log.i("Mouflon", "Set alarm to launch service");
			
			PendingIntent dailyPendingIntent = PendingIntent.getService(context, 0, new Intent(context, WeeklyChecker.class), PendingIntent.FLAG_UPDATE_CURRENT);
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), AlarmManager.INTERVAL_DAY, dailyPendingIntent);
			//Log.i("Mouflon", "Set alarm to launch reminder");
			//set an alarm to trigger every hourish starting an hour ago.
			
		}
	}

}
