package com.aaronbrice.timesheet;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TabHost;

import java.util.Calendar;

import com.aaronbrice.timesheet.TimesheetDatabase;

public class TimeEntriesActivity extends TabActivity
{
    TimesheetDatabase m_db;
    Cursor m_day_cursor, m_week_cursor;
    TabHost m_tab_host;
    SimpleCursorAdapter m_day_ca, m_week_ca;
    Button m_day_button, m_week_button;

    public static final int ADD_TIME_ENTRY_MENU_ITEM    = Menu.FIRST;
    public static final int DELETE_TIME_ENTRY_MENU_ITEM = Menu.FIRST + 1;
    public static final int EDIT_TIME_ENTRY_MENU_ITEM   = Menu.FIRST + 2;
    public static final int LIST_TASKS_MENU_ITEM        = Menu.FIRST + 3;

    private static final int SELECT_DAY_DIALOG_ID = 0;
    private static final int SELECT_WEEK_DIALOG_ID = 1;

    private static final int ACTIVITY_CREATE = 0;

    private DatePickerDialog.OnDateSetListener m_day_listener =
        new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int month, int day) {
                m_day_cursor.close();
                stopManagingCursor(m_day_cursor);
                m_day_cursor = m_db.getTimeEntries(year, month + 1, day);
                startManagingCursor(m_day_cursor);
                m_day_ca.changeCursor(m_day_cursor);
                m_day_button.setText(String.format("%04d-%02d-%02d", year, month + 1, day));
            }
        };

    private DatePickerDialog.OnDateSetListener m_week_listener =
        new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int month, int week) {
                m_week_cursor.close();
                stopManagingCursor(m_week_cursor);
                m_week_cursor = m_db.getWeekEntries(year, month + 1, week);
                startManagingCursor(m_week_cursor);
                m_week_ca.changeCursor(m_week_cursor);
                m_week_button.setText(String.format("%04d-%02d-%02d", year, month + 1, week));
            }
        };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entries);

        m_tab_host = getTabHost();
        m_tab_host.addTab(m_tab_host.newTabSpec("tab_byday").setIndicator("By Day").setContent(R.id.byday_content));
        m_tab_host.addTab(m_tab_host.newTabSpec("tab_byweek").setIndicator("By Week").setContent(R.id.byweek_content));
        m_tab_host.setCurrentTab(0);

        m_db = new TimesheetDatabase(this);

        setupDayTab();
        setupWeekTab();
    }

    protected void setupDayTab() 
    {
        m_day_cursor = m_db.getTimeEntries();
        startManagingCursor(m_day_cursor);

        ListView time_entry_list = (ListView) findViewById(R.id.entries_byday);
        m_day_ca = new SimpleCursorAdapter(this,
                R.layout.time_entry, 
                m_day_cursor,
                new String[] {"title", "start_time", "end_time"},
                new int[] {R.id.time_entry_title, R.id.time_entry_start, R.id.time_entry_end});
        time_entry_list.setAdapter(m_day_ca);
        time_entry_list.setChoiceMode(ListView.CHOICE_MODE_NONE);

        m_day_button = (Button) findViewById(R.id.day_selection_button);
        m_day_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(SELECT_DAY_DIALOG_ID);
            }
        });
        final Calendar c = Calendar.getInstance();
        m_day_button.setText(String.format("%04d-%02d-%02d", 
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)));

        registerForContextMenu(time_entry_list);
    }

    protected void setupWeekTab() 
    {
        m_week_cursor = m_db.getWeekEntries();
        startManagingCursor(m_week_cursor);

        ListView week_entry_list = (ListView) findViewById(R.id.entries_byweek);
        m_week_ca = new SimpleCursorAdapter(this,
                R.layout.week_entry, 
                m_week_cursor,
                new String[] {"title", "start_date", "duration"},
                new int[] {R.id.week_entry_title, R.id.week_entry_start, R.id.week_entry_duration});
        week_entry_list.setAdapter(m_week_ca);
        week_entry_list.setChoiceMode(ListView.CHOICE_MODE_NONE);

        m_week_button = (Button) findViewById(R.id.week_selection_button);
        m_week_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(SELECT_WEEK_DIALOG_ID);
            }
        });
        final Calendar c = Calendar.getInstance();
        m_week_button.setText(String.format("%04d-%02d-%02d", 
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)));

        registerForContextMenu(week_entry_list);
    }

    @Override
    protected Dialog onCreateDialog(int id) 
    {
        final Calendar c = Calendar.getInstance();
        switch (id) {
            case SELECT_DAY_DIALOG_ID:
                return new DatePickerDialog(this, m_day_listener, 
                        c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            case SELECT_WEEK_DIALOG_ID:
                return new DatePickerDialog(this, m_week_listener, 
                        c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, ADD_TIME_ENTRY_MENU_ITEM, Menu.NONE, "Add Time Entry");
        menu.add(Menu.NONE, LIST_TASKS_MENU_ITEM, Menu.NONE, "List Tasks");
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        switch (item.getItemId()) {
            case ADD_TIME_ENTRY_MENU_ITEM:
                addTimeEntry();
                return true;
            case LIST_TASKS_MENU_ITEM:
                listTasks();
                return true;
        }
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) 
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.NONE, DELETE_TIME_ENTRY_MENU_ITEM, Menu.NONE, "Delete Time Entry");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) 
    {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case DELETE_TIME_ENTRY_MENU_ITEM:
                m_db.deleteTimeEntry(info.id);
                m_day_cursor.requery();
                return true;
        }
        return false;
    }

    public void addTimeEntry()
    {
        Intent i = new Intent(this, TimeEntryEditActivity.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }

    private void listTasks()
    {
        Intent i = new Intent(this, TimesheetActivity.class);
        startActivity(i);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case ACTIVITY_CREATE:
                if (resultCode == RESULT_OK) {
                    m_day_cursor.requery();
                }
                break;
        }
    }
}
