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

import java.util.ArrayList;
import java.util.UUID;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

public class MainScreen extends Activity {
	//public static final String PREFS_NAME = "MouflonPrefs";
	public static final String AUTH_KEY = "authorized";
	public static final String EMAIL_KEY = "email";
	public static final String NOTIFY_KEY = "notify";
	public static final String UUID_KEY = "uuid";
	public static final String DELETE_KEY = "deleteOnUpload";
	public static final String DAY_KEY = "day";
	public static final String UPLOAD_KEY = "upload";
	private static final int DIALOG_CLEAR_ID = 0;
	
	SharedPreferences mSettings;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		boolean authorized = mSettings.getBoolean(AUTH_KEY, false);
		if (authorized) {
			setupMainList();
		}
		else {
			setupAuth();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(0);
		
	}
	
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch(id) {
		case DIALOG_CLEAR_ID:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Would you like to clear the log database?")
					.setCancelable(false)
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							DbAdapter dba = new DbAdapter(MainScreen.this);
							dba.open();
							dba.clearDB();
							dba.close();
						}
					})
					.setNegativeButton("No", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							
						}
					});
			
			dialog = builder.create();
			break;
		default:
			dialog = null;
		}
		return dialog;
		
	}

	private void setupAuth() {
		setContentView(R.layout.auth_dialog);
		Button authOKButton = (Button) findViewById(R.id.auth_ok_button);
		
		authOKButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				TextView tv = (TextView)findViewById(R.id.auth_field);
				//check if the user inputs "OvalDrive"
				String input = tv.getText().toString();
				RadioButton uploadButton = (RadioButton) findViewById(R.id.radio_upload);
				if ("OvalDrive".equals(input)) {
					//then save authorization as a setting
					InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(tv.getWindowToken(), 0);
					SharedPreferences.Editor editor = mSettings.edit();
					editor.putBoolean(AUTH_KEY, true);
					if (uploadButton.isChecked()) {
						editor.putBoolean(MainScreen.UPLOAD_KEY, true);
						editor.putBoolean(MainScreen.NOTIFY_KEY, false);
					} else {
						editor.putBoolean(MainScreen.UPLOAD_KEY, false);
						editor.putBoolean(MainScreen.NOTIFY_KEY, true);
					}
					editor.commit();
					setupMainList();
				}
				
			}
		});
	}

	private void setupMainList() {
		setContentView(R.layout.main);
		populateList();
		setAlarms();
		TextView tv = (TextView) findViewById(R.id.uuidTextView);
		tv.setText("Your UUID is: \n" + getOrCreateUUID(this).toString());
	}

	private void setAlarms() {
		Intent i = new Intent(this, RecorderService.class);
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		if ( PendingIntent.getService(this, 0, i, PendingIntent.FLAG_NO_CREATE) == null) {
			PendingIntent pi = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
			
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() - (3600*1000), AlarmManager.INTERVAL_HALF_HOUR, pi);
			//Log.i("Mouflon", "Set alarm to launch service");
		}
		else {
			//Log.d("Mouflon", "Alarm already exists");
		}
		Intent j = new Intent(this, WeeklyChecker.class);
		if ( PendingIntent.getService(this, 0, j, PendingIntent.FLAG_NO_CREATE) == null) {
			PendingIntent dailyPendingIntent = PendingIntent.getService(this, 0, j, PendingIntent.FLAG_UPDATE_CURRENT);
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), AlarmManager.INTERVAL_DAY, dailyPendingIntent);
			//Log.i("Mouflon", "set 15 min alarm");
		}
		else {
			//Log.d("Mouflon", "alarm already exists");
		}
		
	}

	private void populateList() {
		ListView lv = (ListView) findViewById(R.id.listView1);
		ArrayList<String> options = new ArrayList<String>();
		
		options.add(getString(R.string.send));
		options.add(getString(R.string.viewStatistics));
		//options.add(getString(R.string.view));
		options.add(getString(R.string.clear));
		options.add(getString(R.string.search));
		options.add(getString(R.string.settings));
					//Done: Reorder: upload, view stats, clear, search,  
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.text_view_only, options);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				Intent i = null;
				
				switch(position) {
				case 0:
					//Upload
					i = new Intent(MainScreen.this, UploadFile.class);
					i.putExtra("edu.ncsu.asbransc.mouflon.recorder.ManualUpload", true);
					startService(i);
					break;
				case 1:
					//Stats
					startActivity(new Intent(MainScreen.this, StatsActivity.class));
					break;
				//case 2:
					//View Logs
					//i = new Intent(MainScreen.this, StoredLogViewer.class);
					//startActivity(i);
					//break;
				case 2:
					//Clear
					//TODO update this to be a dialog fragment
					showDialog(DIALOG_CLEAR_ID);
					break;
				case 3:
					//Search
					startActivity(new Intent(MainScreen.this, SQLActivity.class));
					break;	
				case 4:
					startActivity(new Intent(MainScreen.this, MouflonPreferences.class));
					break;
				}
				
			}
			
		});
	}
	
	public static UUID getOrCreateUUID(Context context)  {
		UUID uuid = null;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String uuidString = prefs.getString(MainScreen.UUID_KEY, "");
		if (uuidString.equals("")) {
			uuid = UUID.randomUUID();
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(MainScreen.UUID_KEY, uuid.toString());
			editor.commit();
		}
		else {
			uuid = UUID.fromString(uuidString);
			
		}
		return uuid;
	}
	
	/*public class GetAuthDialogFragment extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			LayoutInflater inflater = getActivity().getLayoutInflater();
			
			builder.setView(inflater.inflate(R.layout.auth_dialog, null))
				   .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
					}
				})
				   .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//Do nothing.
						GetAuthDialogFragment.this.getDialog().cancel();
						
					}
				});
			return builder.create();
		}
		
	}*/

}
