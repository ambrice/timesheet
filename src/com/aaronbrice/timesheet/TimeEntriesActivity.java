package com.aaronbrice.timesheet;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.aaronbrice.timesheet.TimesheetDatabase;

public class TimeEntriesActivity extends Activity
{
    TimesheetDatabase m_db;
    Cursor m_time_entry_cursor;

    public static final int ADD_TIME_ENTRY_MENU_ITEM    = Menu.FIRST;
    public static final int DELETE_TIME_ENTRY_MENU_ITEM = Menu.FIRST + 1;
    public static final int EDIT_TIME_ENTRY_MENU_ITEM   = Menu.FIRST + 2;
    public static final int LIST_TASKS_MENU_ITEM        = Menu.FIRST + 3;

    private static final int ACTIVITY_CREATE = 0;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        m_db = new TimesheetDatabase(this);
        m_time_entry_cursor = m_db.getTimeEntries();
        startManagingCursor(m_time_entry_cursor);

        setContentView(R.layout.entries);
        ListView time_entry_list = (ListView) findViewById(R.id.entries_list);
        SimpleCursorAdapter ca = new SimpleCursorAdapter(this,
                R.layout.time_entry, 
                m_time_entry_cursor,
                new String[] {"title", "start_time", "end_time"},
                new int[] {R.id.time_entry_title, R.id.time_entry_start, R.id.time_entry_end});
        time_entry_list.setAdapter(ca);
        time_entry_list.setChoiceMode(ListView.CHOICE_MODE_NONE);

        registerForContextMenu(time_entry_list);
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
                m_time_entry_cursor.requery();
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
                    m_time_entry_cursor.requery();
                }
                break;
        }
    }
}
