package edu.ncsu.asbransc.mouflon.recorder;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class MouflonPreferences extends PreferenceActivity {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
	
	//TODO make upload automatically and notify mutually exclusive
}
