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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;
import android.os.PatternMatcher;

public class RecorderService extends Service {

	private final IBinder mBinder = new RecorderBinder();
	
	public class RecorderBinder extends Binder {
		RecorderService getService() {
			return RecorderService.this;
		}
	}
	
	private Runnable mRecordTask = new Runnable() {

		@Override
		public void run() {
			
			java.lang.Process logcatProc = null;
			DbAdapter dba = new DbAdapter(RecorderService.this);
	        try {
				logcatProc = Runtime.getRuntime().exec(new String[] {"logcat", "-d", "-v", "time", "ActivityManager:I", "power:I", "*:S"});
	        	//logcatProc = Runtime.getRuntime().exec(new String[] {"su", "-c", "logcat -d -v time ActivityManager:I power:I *:S"});
				BufferedReader reader = new BufferedReader(new InputStreamReader(logcatProc.getInputStream()));
				String line;
//				while (true) {
//					try {
//						logcatProc.exitValue();
//						break;
//					}
//					catch (IllegalThreadStateException e) {
//						
//					}
//					
//				}
//				Log.d("Mouflon", "LogcatFinished");
				//PatternMatcher pm = new PatternMatcher(".*Start proc.*", PatternMatcher.PATTERN_SIMPLE_GLOB);
				PatternMatcher pm2 = new PatternMatcher(".*set_screen_state.*", PatternMatcher.PATTERN_SIMPLE_GLOB);
				PatternMatcher pm3 = new PatternMatcher(".*Starting: Intent.*", PatternMatcher.PATTERN_SIMPLE_GLOB);
				PatternMatcher pm4 = new PatternMatcher(".*START.*", PatternMatcher.PATTERN_SIMPLE_GLOB);
				Pattern datePat = Pattern.compile("\\s[IWEDV]/");
				Pattern tagPat = Pattern.compile("\\s*\\([\\s\\d]*\\): ");
				Pattern redactPat = Pattern.compile("http(s)?://\\S*");
				//cmp act cat
				PatternMatcher pmCmp = new PatternMatcher(".*cmp=.*", PatternMatcher.PATTERN_SIMPLE_GLOB);
				PatternMatcher pmAct = new PatternMatcher(".*act=.*", PatternMatcher.PATTERN_SIMPLE_GLOB);
				PatternMatcher pmCat = new PatternMatcher(".*cat=.*", PatternMatcher.PATTERN_SIMPLE_GLOB);
				Pattern parsePat = Pattern.compile("[\\s}{]");
				//Pattern skipPat = Pattern.compile(".*-{6,}.*");
				
				SimpleDateFormat dateParser = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);
				dba.open();
				Cursor c = dba.fetchAll();
				c.moveToLast();
				String lastDateString = c.isBeforeFirst() ? "01-01 00:00:00.000" : c.getString(c.getColumnIndex(DbAdapter.KEY_TIME));
				//lastDateString = "01-01 00:00:00.000";
				Date lastDate = null;
				Date thisDate = null;
				try {
					lastDate = dateParser.parse(lastDateString);
				} catch (ParseException e) {
					//Log.e("Mouflon", "could not parse date " + lastDateString);
				}
				//Log.i("about to read","about to read");
				while ((line = reader.readLine()) != null) {
					//if (skipPat.matcher(line).matches())
					//	continue;
					//Log.i("line", line);
					String[] temp = datePat.split(line);
					String date = temp[0];
					
					try {
						thisDate = dateParser.parse(date);
					} catch (ParseException e) {
						//Log.d("Mouflon", "could not parse date " + date);
						continue;
					}
					//Log.d("Mouflon", thisDate.toLocaleString() +" after " + lastDate.toLocaleString() + " is " + Boolean.toString(thisDate.after(lastDate)));
					boolean power = pm2.match(line);
					boolean startGB = pm3.match(line);
					boolean startICS = pm4.match(line);
					if (thisDate.after(lastDate) && (/*pm.match(line) ||*/ power || startGB || startICS)) {
						temp = tagPat.split(temp[1]);
						String tag = temp[0];
						temp = redactPat.split(temp[1]);
						String data = temp[0];
						for (int i = 1; i < temp.length; i ++ ) {
							data += "http://redacted" + temp[i];
						}
						temp = parsePat.split(data);
						String extra1 = "";
						String extra2 = "";
						String extra3 = "";
						String extra4 = "";
						if (power) {
							extra1 = temp[2];
						}
						else if (startGB || startICS) {
							extra1 = "START";
							for (int i = 0; i < temp.length; i++) {
								if (pmCmp.match(temp[i])) 
									extra2 = temp[i];
								else if (pmAct.match(temp[i]))
									extra3 = temp[i];
								else if (pmCat.match(temp[i]))
									extra4 = temp[i];
							}
						}
						dba.addEntry(date, tag , extra1, extra2, extra3, extra4);  
					}
				}
				
				
				
			} catch (IOException e) {
				//Toast.makeText(RecorderService.this, "Mouflon: error reading logs", Toast.LENGTH_LONG).show();
				e.printStackTrace();
				// I believe Android/Dalvik will send this to /dev/null
			}
	        dba.close();
	        //Toast.makeText(RecorderService.this, "Mouflon: finished recording in db, quitting", Toast.LENGTH_LONG).show();
	        //Log.i("Mouflon", "Finished recording logs");
	        RecorderService.this.stopSelf();
		}
		
	};

	@Override
	public IBinder onBind(Intent intent) {
		
		return mBinder;
	}

	@Override
	public void onCreate() {
		
		super.onCreate();
		//Toast.makeText(RecorderService.this, "Mouflon: starting service", Toast.LENGTH_LONG).show();
		//Log.i("Mouflon", "Starting thread to save logs");
		Thread thr = new Thread(null, mRecordTask, "MouflonRecorderService");
		thr.start();
	}
	

}
