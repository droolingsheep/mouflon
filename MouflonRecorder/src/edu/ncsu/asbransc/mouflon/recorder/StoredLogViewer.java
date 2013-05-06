package edu.ncsu.asbransc.mouflon.recorder;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;

public class StoredLogViewer extends Activity {
    /** Called when the activity is first created. */
	//Colors? 
	//label as raw data
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader);
        TextView logView = (TextView) findViewById(R.id.log);
        DbAdapter dba = new DbAdapter(this);
        dba.open();
        Cursor allLogs = dba.fetchAll();
        StringBuilder sb = new StringBuilder();
        allLogs.moveToFirst();
        while (!allLogs.isAfterLast()) {
        	sb.append(allLogs.getString(allLogs.getColumnIndex(DbAdapter.KEY_TIME)));
        	sb.append(":::");
        	sb.append(allLogs.getString(allLogs.getColumnIndex(DbAdapter.KEY_PROCESSTAG)));
        	sb.append(":::");
        	sb.append(allLogs.getString(allLogs.getColumnIndex(DbAdapter.KEY_EXTRA_1)));
        	sb.append(":::");
        	sb.append(allLogs.getString(allLogs.getColumnIndex(DbAdapter.KEY_EXTRA_2)));
        	sb.append(":::");
        	sb.append(allLogs.getString(allLogs.getColumnIndex(DbAdapter.KEY_EXTRA_3)));
        	sb.append(":::");
        	sb.append(allLogs.getString(allLogs.getColumnIndex(DbAdapter.KEY_EXTRA_4)));
        	sb.append("\n");
        	allLogs.moveToNext();
        }
        dba.close();
        logView.setText(sb);
        
    }
}