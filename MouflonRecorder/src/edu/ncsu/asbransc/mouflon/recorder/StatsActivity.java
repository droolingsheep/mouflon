package edu.ncsu.asbransc.mouflon.recorder;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;

public class StatsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		//startService(new Intent(this, NotificationService.class));
		setContentView(R.layout.stats);
		DbAdapter dba = new DbAdapter(this);
        dba.open();
        //find power on events
        //find START events
        //find power off events
        //Done names of unique apps
        //DONE make the list scrollable -- need to test
        int c1 = dba.fetchAll().getCount();
        int c2 = dba.fetchPowerOn().getCount();
        int c3 = dba.fetchPowerOff().getCount();
        int c4 = dba.fetchStart().getCount();
        Cursor c5 = dba.fetchApps();
        StringBuilder sb = new StringBuilder();
        sb.append("Total Events: ");
        sb.append(c1);
        sb.append("\n");
        sb.append("Power On Events: ");
        sb.append(c2);
        sb.append("\n");
        sb.append("Power Off Events: ");
        sb.append(c3);
        sb.append("\n");
        sb.append("App Start Events: ");
        sb.append(c4);
        sb.append("\n");
        sb.append("Names of Unique Apps: \n");
        c5.moveToFirst();
        while (!c5.isAfterLast()) {
        	String entry = c5.getString(c5.getColumnIndex(DbAdapter.KEY_EXTRA_2));
        	
        	if (entry.matches("cmp=.*")) {
        		entry = entry.substring(4);
        	}
        	sb.append(entry);
        	sb.append("\n");
        	c5.moveToNext();
        }
        dba.close();
        TextView tv = (TextView) findViewById(R.id.statsText);
        tv.setText(sb.toString());
        
	}

}
