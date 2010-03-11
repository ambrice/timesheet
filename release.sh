jarsigner -keystore ~/tastycactus.keystore bin/TimesheetActivity-unsigned.apk release
rm bin/timesheet.apk
zipalign -v 4 bin/TimesheetActivity-unsigned.apk bin/timesheet.apk
adb install -r bin/timesheet.apk
