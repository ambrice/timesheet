/***
 * Copyright (c) 2009 Tasty Cactus Software, LLC
 * 
 * All rights reserved.
 */

package com.tastycactus.timesheet;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;

import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.tastycactus.timesheet.TimesheetDatabase;

public class TimesheetActivity extends ListActivity {
    TimesheetDatabase m_db;
    Cursor m_task_cursor;
    SimpleCursorAdapter m_ca;

    public static final int ADD_TASK_MENU_ITEM     = Menu.FIRST;
    public static final int DELETE_TASK_MENU_ITEM  = Menu.FIRST + 1;
    public static final int LIST_ENTRIES_MENU_ITEM = Menu.FIRST + 2;

    private static final int ACTIVITY_CREATE = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_db = new TimesheetDatabase(this);
        m_task_cursor = m_db.getTasks();
        startManagingCursor(m_task_cursor);

        setContentView(R.layout.main);
        m_ca = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_single_choice, 
                m_task_cursor,
                new String[] {"title"},
                new int[] {android.R.id.text1});

        m_ca.registerDataSetObserver(new DataSetObserver() {
            public void onChanged() {
                updateCheckedItem();
            }
        });

        setListAdapter(m_ca);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        registerForContextMenu(getListView());

        updateCheckedItem();
    }

    @Override
    protected void onDestroy()
    {
        m_task_cursor.close();
        m_db.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, ADD_TASK_MENU_ITEM, Menu.NONE, "Add Task").setIcon(android.R.drawable.ic_menu_add);
        menu.add(Menu.NONE, LIST_ENTRIES_MENU_ITEM, Menu.NONE, "List Entries").setIcon(android.R.drawable.ic_menu_info_details);
        return result;
    }

    @Override
    public void onListItemClick(ListView lv, View v, int position, long id) {
        if (id == m_db.getCurrentTaskId()) {
            m_db.completeCurrentTask();
            getListView().clearChoices();
        } else {
            m_db.changeTask(id);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case ADD_TASK_MENU_ITEM:
                addTask();
                return true;
            case LIST_ENTRIES_MENU_ITEM:
                listEntries();
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

    private void addTask() {
        Intent i = new Intent(this, TaskEditActivity.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }

    private void listEntries() {
        Intent i = new Intent(this, TimeEntriesActivity.class);
        startActivity(i);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case ACTIVITY_CREATE:
                if (resultCode == RESULT_OK) {
                    m_task_cursor.requery();
                }
                break;
        }
    }

    private void updateCheckedItem() {
        long current_id = m_db.getCurrentTaskId();
        if (current_id == 0) {
            getListView().clearChoices();
        } else {
            int count = getListView().getCount();
            for (int i=0; i < count; ++i) {
                if (m_ca.getItemId(i) == current_id) {
                    getListView().setItemChecked(i, true);
                }
            }
        }
    }
}
