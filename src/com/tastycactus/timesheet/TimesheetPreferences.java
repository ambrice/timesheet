/***
 * Copyright (c) 2009 Tasty Cactus Software, LLC
 * 
 * All rights reserved.
 */

package com.tastycactus.timesheet;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class TimesheetPreferences extends PreferenceActivity {
   @Override
   public void onCreate(Bundle savedInstanceState) {
     super.onCreate(savedInstanceState);
     addPreferencesFromResource(R.xml.preferences);
   }
}

