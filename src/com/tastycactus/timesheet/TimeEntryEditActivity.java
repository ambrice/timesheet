/*
 * Copyright (c) 2009-2010 Tasty Cactus Software, LLC
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
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TimePicker;

import java.util.Calendar;

import com.tastycactus.timesheet.TimesheetDatabase;

public class TimeEntryEditActivity extends Activity {
    class TimeEntryData {
        String m_start_date, m_end_date;
        String m_start_time, m_end_time;
        String m_comment;
        long m_task_id, m_row_id;

        public TimeEntryData() {
            final Calendar c = Calendar.getInstance();
            m_start_date = formatDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            m_start_time = formatTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
            m_end_date = m_start_date;
            m_end_time = m_start_time;
            m_comment = "";
            m_task_id = -1;
            m_row_id = -1;
        }

        public TimeEntryData(Cursor c, long row_id) {
            m_start_date = c.getString(c.getColumnIndex("start_date"));
            m_start_time = c.getString(c.getColumnIndex("start_time"));
            m_end_date = c.getString(c.getColumnIndex("end_date"));
            m_end_time = c.getString(c.getColumnIndex("end_time"));
            m_task_id = c.getLong(c.getColumnIndex("task_id"));
            m_comment = c.getString(c.getColumnIndex("comment"));
            m_row_id = row_id;
        }

        public long task_id() {
            return m_task_id;
        }

        public void set_start_date(int year, int month, int day) {
            m_start_date = formatDate(year, month, day);
        }

        public void set_start_time(int hour, int minute) {
            m_start_time = formatTime(hour, minute);
        }

        public void set_end_date(int year, int month, int day) {
            m_end_date = formatDate(year, month, day);
        }

        public void set_end_time(int hour, int minute) {
            m_end_time = formatTime(hour, minute);
        }

        public String start_date() {
            return m_start_date;
        }

        public String start_time() {
            return m_start_time;
        }

        public String end_date() {
            return m_end_date;
        }

        public String end_time() {
            return m_end_time;
        }

        public String comment() {
            return m_comment;
        }

        public long row() {
            return m_row_id;
        }

        private String formatDate(int year, int month, int day)
        {
            return String.format("%04d-%02d-%02d", year, month+1, day);
        }

        private String formatTime(int hour, int minute)
        {
            return String.format("%02d:%02d", hour, minute);
        }
    }

    TimeEntryData m_data;
    Button m_start_date_button, m_end_date_button;
    Button m_start_time_button, m_end_time_button;
    EditText m_comment_edit;

    TimesheetDatabase m_db;

    static final int START_DATE_DIALOG_ID = 0;
    static final int START_TIME_DIALOG_ID = 1;
    static final int END_DATE_DIALOG_ID   = 2;
    static final int END_TIME_DIALOG_ID   = 3;

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

    private TimePickerDialog.OnTimeSetListener m_start_time_listener =
        new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int hour, int minute) {
                m_data.set_start_time(hour, minute);
                updateDisplay();
            }
        };

    private TimePickerDialog.OnTimeSetListener m_end_time_listener =
        new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int hour, int minute) {
                m_data.set_end_time(hour, minute);
                updateDisplay();
            }
        };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);

        m_db = new TimesheetDatabase(this);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            long row_id = b.getLong("_id");
            Cursor entry = m_db.getTimeEntry(row_id);
            m_data = new TimeEntryData(entry, row_id);
            entry.close();
        } else {
            m_data = new TimeEntryData();
        }

        Cursor task_cursor = m_db.getTasks(prefs.getBoolean("alphabetise_tasks", false));

        setContentView(R.layout.time_entry_edit);

        final Spinner task_edit = (Spinner) findViewById(R.id.time_entry_task);
        SimpleCursorAdapter ca = new SimpleCursorAdapter(this,
                android.R.layout.simple_spinner_item, 
                task_cursor,
                new String[] {"title"},
                new int[] {android.R.id.text1});
        ca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        task_edit.setAdapter(ca);
        if (m_data.task_id() != -1) {
            for (int i = 0; i < task_edit.getCount(); ++i) {
                if (m_data.task_id() == task_edit.getItemIdAtPosition(i)) {
                    task_edit.setSelection(i);
                }
            }
        }

        m_start_date_button = (Button) findViewById(R.id.time_entry_start_date);
        m_start_date_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(START_DATE_DIALOG_ID);
            }
        });

        m_start_time_button = (Button) findViewById(R.id.time_entry_start_time);
        m_start_time_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(START_TIME_DIALOG_ID);
            }
        });

        m_end_date_button = (Button) findViewById(R.id.time_entry_end_date);
        m_end_date_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(END_DATE_DIALOG_ID);
            }
        });

        m_end_time_button = (Button) findViewById(R.id.time_entry_end_time);
        m_end_time_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(END_TIME_DIALOG_ID);
            }
        });

        m_comment_edit = (EditText) findViewById(R.id.time_entry_comment);
        m_comment_edit.setText(m_data.comment());

        if (m_data.row() == m_db.getCurrentId()) {
            // For the current task, hide the end date and end time buttons,
            // only start date and time are edittable
            LinearLayout end_layout = (LinearLayout) findViewById(R.id.end_layout);
            end_layout.setVisibility(View.GONE);
        }

        updateDisplay();

        Button addButton = (Button) findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                long task_id = task_edit.getSelectedItemId();

                if (task_id != Spinner.INVALID_ROW_ID) {
                    String start = m_data.start_date() + " " + m_data.start_time();
                    String end = m_data.end_date() + " " + m_data.end_time();
                    String comment = m_comment_edit.getText().toString();
                    if (m_data.row() == -1) {
                        m_db.newTimeEntry(task_id, comment, start, end);
                    } else if (m_data.row() == m_db.getCurrentId()) {
                        m_db.updateTimeEntry(m_data.row(), task_id, comment, start);
                    } else {
                        m_db.updateTimeEntry(m_data.row(), task_id, comment, start, end);
                    }
                    setResult(RESULT_OK);
                } else {
                    setResult(RESULT_CANCELED);
                }
                finish();
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
            case START_TIME_DIALOG_ID:
                items = m_data.start_time().split(":");
                return new TimePickerDialog(this, m_start_time_listener, 
                        Integer.parseInt(items[0]), Integer.parseInt(items[1]), true);
            case END_DATE_DIALOG_ID:
                items = m_data.end_date().split("-");
                return new DatePickerDialog(this, m_end_date_listener, 
                        Integer.parseInt(items[0]), Integer.parseInt(items[1]) - 1, Integer.parseInt(items[2]));
            case END_TIME_DIALOG_ID:
                items = m_data.end_time().split(":");
                return new TimePickerDialog(this, m_end_time_listener,
                        Integer.parseInt(items[0]), Integer.parseInt(items[1]), true);
        }
        return null;
    }

    private void updateDisplay() 
    {
        m_start_date_button.setText(m_data.start_date());
        m_start_time_button.setText(m_data.start_time());
        m_end_date_button.setText(m_data.end_date());
        m_end_time_button.setText(m_data.end_time());
    }
}

