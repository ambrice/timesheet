package com.tastycactus.timesheet;

import android.test.ActivityInstrumentationTestCase;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.tastycactus.timesheet.TimesheetActivityTest \
 * com.tastycactus.timesheet.tests/android.test.InstrumentationTestRunner
 */
public class TimesheetActivityTest extends ActivityInstrumentationTestCase<TimesheetActivity> {

    public TimesheetActivityTest() {
        super("com.tastycactus.timesheet", TimesheetActivity.class);
    }

}
