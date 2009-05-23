package com.aaronbrice.timesheet;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TimePicker;

import java.util.Calendar;

import com.aaronbrice.timesheet.TimesheetDatabase;


public class TimeEntryEditActivity extends Activity {
    Button m_start_date_button, m_end_date_button;
    Button m_start_time_button, m_end_time_button;

    TimesheetDatabase m_db;
    String m_start_date, m_end_date;
    String m_start_time, m_end_time;

    static final int START_DATE_DIALOG_ID = 0;
    static final int START_TIME_DIALOG_ID = 1;
    static final int END_DATE_DIALOG_ID   = 2;
    static final int END_TIME_DIALOG_ID   = 3;

    private DatePickerDialog.OnDateSetListener m_start_date_listener =
        new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int month, int day) {
                m_start_date = formatDate(year, month, day);
                updateDisplay();
            }
        };

    private DatePickerDialog.OnDateSetListener m_end_date_listener =
        new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int month, int day) {
                m_end_date = formatDate(year, month, day);
                updateDisplay();
            }
        };

    private TimePickerDialog.OnTimeSetListener m_start_time_listener =
        new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int hour, int minute) {
                m_start_time = formatTime(hour, minute);
                updateDisplay();
            }
        };

    private TimePickerDialog.OnTimeSetListener m_end_time_listener =
        new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int hour, int minute) {
                m_end_time = formatTime(hour, minute);
                updateDisplay();
            }
        };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        m_db = new TimesheetDatabase(this);
        Cursor task_cursor = m_db.getTasks();

        setContentView(R.layout.time_entry_edit);

        final Spinner task_edit = (Spinner) findViewById(R.id.time_entry_task);
        SimpleCursorAdapter ca = new SimpleCursorAdapter(this,
                android.R.layout.simple_spinner_item, 
                task_cursor,
                new String[] {"title"},
                new int[] {android.R.id.text1});
        ca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        task_edit.setAdapter(ca);

        final Calendar c = Calendar.getInstance();
        m_start_date = formatDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        m_start_time = formatTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        m_end_date = m_start_date;
        m_end_time = m_start_time;

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

        updateDisplay();

        Button addButton = (Button) findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                long task_id = task_edit.getSelectedItemId();

                if (task_id != Spinner.INVALID_ROW_ID) {
                    m_db.newTimeEntry(task_id, m_start_date + " " + m_start_time, m_end_date + " " + m_end_time);
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
    protected Dialog onCreateDialog(int id) 
    {
        final Calendar c = Calendar.getInstance();
        switch (id) {
            case START_DATE_DIALOG_ID:
                return new DatePickerDialog(this, m_start_date_listener, 
                        c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            case START_TIME_DIALOG_ID:
                return new TimePickerDialog(this, m_start_time_listener, 
                        c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
            case END_DATE_DIALOG_ID:
                return new DatePickerDialog(this, m_end_date_listener, 
                        c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            case END_TIME_DIALOG_ID:
                return new TimePickerDialog(this, m_end_time_listener,
                        c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
        }
        return null;
    }


    private void updateDisplay() 
    {
        m_start_date_button.setText(m_start_date);
        m_start_time_button.setText(m_start_time);
        m_end_date_button.setText(m_end_date);
        m_end_time_button.setText(m_end_time);
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

