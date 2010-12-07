/*
 * Copyright (c) 2010 Tasty Cactus Software, LLC
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Aaron Brice <aaron@tastycactus.com>
 *
 */

package com.tastycactus.timesheet;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;

import android.preference.PreferenceManager;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Calendar;

import com.tastycactus.timesheet.TimesheetDatabase;

public class ExportActivity extends Activity {
    class ExportData {
        String m_start_date, m_end_date;

        public ExportData() {
            final Calendar c = Calendar.getInstance();
            m_start_date = formatDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            m_end_date = m_start_date;
        }

        public ExportData(Cursor c, long row_id) {
            m_start_date = c.getString(c.getColumnIndex("start_date"));
            m_end_date = c.getString(c.getColumnIndex("end_date"));
        }

        /**
         *
         */
        public void set_start_date(int year, int month, int day) {
            m_start_date = formatDate(year, month, day);
        }

        public void set_end_date(int year, int month, int day) {
            m_end_date = formatDate(year, month, day);
        }

        public String start_date() {
            return m_start_date;
        }

        public String end_date() {
            return m_end_date;
        }

        private String formatDate(int year, int month, int day)
        {
            return String.format("%04d-%02d-%02d", year, month+1, day);
        }
    }

    ExportData m_data;
    Button m_start_date_button, m_end_date_button;
    boolean m_export_billable_only;

    TimesheetDatabase m_db;

    static final int START_DATE_DIALOG_ID = 0;
    static final int END_DATE_DIALOG_ID   = 1;

    private DatePickerDialog.OnDateSetListener m_start_date_listener =
        new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int month, int day) {
                m_data.set_start_date(year, month, day);
                updateDisplay();
            }
        };

    private DatePickerDialog.OnDateSetListener m_end_date_listener =
        new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int month, int day) {
                m_data.set_end_date(year, month, day);
                updateDisplay();
            }
        };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        m_db = new TimesheetDatabase(this);
        m_data = new ExportData();

        setContentView(R.layout.export);

        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);
        m_export_billable_only = prefs.getBoolean("weekly_billable_only", true);

        m_start_date_button = (Button) findViewById(R.id.export_start_date);
        m_start_date_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(START_DATE_DIALOG_ID);
            }
        });

        m_end_date_button = (Button) findViewById(R.id.export_end_date);
        m_end_date_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(END_DATE_DIALOG_ID);
            }
        });

        updateDisplay();

        Button exportButton = (Button) findViewById(R.id.export_button);
        final Context ctx = this;
        exportButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Cursor c = m_db.getTimeEntries(m_data.start_date(), m_data.end_date());
                try {
                    File root = Environment.getExternalStorageDirectory();
                    if (root.canWrite()) {
                        File tdir = new File(root, "timesheets");
                        if (!tdir.exists()) {
                            tdir.mkdir();
                        }
                        String filename = "timesheet " + m_data.start_date() + " to " + m_data.end_date() + ".csv";
                        File csvfile = new File(tdir, filename);
                        FileWriter csvwriter = new FileWriter(csvfile);
                        BufferedWriter out = new BufferedWriter(csvwriter);
                        out.write("Task,Comment,Start Time,End Time,Duration\n");
                        while (!c.isAfterLast()) {
                            int billable = c.getInt(c.getColumnIndex("billable"));
                            if (billable == 1 || !m_export_billable_only) {
                                out.write(c.getString(c.getColumnIndex("title")) + ",");
                                out.write(c.getString(c.getColumnIndex("comment")) + ",");
                                out.write(c.getString(c.getColumnIndex("start_time")) + ",");
                                out.write(c.getString(c.getColumnIndex("end_time")) + ",");
                                out.write(c.getString(c.getColumnIndex("duration")) + "\n");
                            }
                            c.moveToNext();
                        }
                        Toast.makeText(ctx, "Exported time entries to file timesheets/" + filename, Toast.LENGTH_LONG).show();
                        out.close();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(ctx, "Could not write CSV file to SD Card", Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    Log.e("Timesheet", "Could not write CSV file: " + e.getMessage());
                    Toast.makeText(ctx, "Could not write CSV file to SD Card", Toast.LENGTH_LONG).show();
                }
            }
        });

        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy()
    {
        m_db.close();
        super.onDestroy();
    }

    @Override
    protected Dialog onCreateDialog(int id) 
    {
        String[] items;
        switch (id) {
            case START_DATE_DIALOG_ID:
                items = m_data.start_date().split("-");
                return new DatePickerDialog(this, m_start_date_listener, 
                        Integer.parseInt(items[0]), Integer.parseInt(items[1]) - 1, Integer.parseInt(items[2]));
            case END_DATE_DIALOG_ID:
                items = m_data.end_date().split("-");
                return new DatePickerDialog(this, m_end_date_listener, 
                        Integer.parseInt(items[0]), Integer.parseInt(items[1]) - 1, Integer.parseInt(items[2]));
        }
        return null;
    }

    private void updateDisplay() 
    {
        m_start_date_button.setText(m_data.start_date());
        m_end_date_button.setText(m_data.end_date());
    }
}

