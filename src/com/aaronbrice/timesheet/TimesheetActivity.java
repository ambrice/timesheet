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

public class TimesheetActivity extends Activity
{
    TimesheetDatabase m_db;
    Cursor m_task_cursor;

    public static final int ADD_TASK_MENU_ITEM    = Menu.FIRST;
    public static final int DELETE_TASK_MENU_ITEM = Menu.FIRST + 1;

    private static final int ACTIVITY_CREATE = 0;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        m_db = new TimesheetDatabase(this);
        m_task_cursor = m_db.getTasks();
        startManagingCursor(m_task_cursor);

        setContentView(R.layout.main);
        ListView task_list = (ListView) findViewById(R.id.task_list);
        SimpleCursorAdapter ca = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_single_choice, 
                m_task_cursor,
                new String[] {"title"},
                new int[] {android.R.id.text1});
        task_list.setAdapter(ca);
        task_list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        registerForContextMenu(task_list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, ADD_TASK_MENU_ITEM, Menu.NONE, "Add Task");
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case ADD_TASK_MENU_ITEM:
                addTask();
                return true;
        }
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.NONE, DELETE_TASK_MENU_ITEM, Menu.NONE, "Delete Task");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case DELETE_TASK_MENU_ITEM:
                m_db.deleteTask(info.id);
                m_task_cursor.requery();
                return true;
        }
        return false;
    }

    public void addTask()
    {
        Intent i = new Intent(this, TaskEditActivity.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case ACTIVITY_CREATE:
                if (resultCode == RESULT_OK) {
                    m_task_cursor.requery();
                }
                break;
        }
    }
}
