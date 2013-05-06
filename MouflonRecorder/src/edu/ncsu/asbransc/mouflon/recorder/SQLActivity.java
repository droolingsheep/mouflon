package edu.ncsu.asbransc.mouflon.recorder;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SQLActivity extends Activity {
//Done Make this search for a string across all fields
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sql);
		Button submit = (Button) findViewById(R.id.submitButton);
		submit.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Editable searchEdit = ((EditText) findViewById(R.id.searchEdit)).getText();
				
				InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(((EditText) findViewById(R.id.searchEdit)).getWindowToken(), 0);
				setContentView(R.layout.reader);
				
				TextView logView = (TextView) findViewById(R.id.log);
				DbAdapter dba = new DbAdapter(SQLActivity.this);
		        dba.open();
		        Cursor allLogs = dba.fetchSelected(searchEdit);
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
		});
	}

}
