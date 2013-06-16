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

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;

import android.preference.PreferenceManager;

import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.tastycactus.timesheet.TimesheetAppWidgetProvider;
import com.tastycactus.timesheet.TimesheetDatabase;

public class TimesheetActivity extends ListActivity {
    TimesheetDatabase m_db;
    Cursor m_task_cursor;
    SimpleCursorAdapter m_ca;

    public static final int ADD_TASK_MENU_ITEM     = Menu.FIRST;
    public static final int DELETE_TASK_MENU_ITEM  = Menu.FIRST + 1;
    public static final int LIST_ENTRIES_MENU_ITEM = Menu.FIRST + 2;
    public static final int EDIT_TASK_MENU_ITEM    = Menu.FIRST + 3;
    public static final int PREFERENCES_MENU_ITEM  = Menu.FIRST + 4;

    private static final int ACTIVITY_CREATE = 0;
    private static final int ACTIVITY_EDIT   = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);

        m_db = new TimesheetDatabase(this);
        m_task_cursor = m_db.getTasks(prefs.getBoolean("alphabetise_tasks", false));
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

        // Set preference defaults if they haven't been set
        if (!prefs.contains("alphabetise_tasks")) {
            prefs.edit().putBoolean("alphabetise_tasks", false);
        }
        if (!prefs.contains("weekly_billable_only")) {
            prefs.edit().putBoolean("weekly_billable_only", true);
        }
        if (!prefs.contains("week_start")) {
            prefs.edit().putString("week_start", "2");
        }
        if (!prefs.contains("default_email")) {
            prefs.edit().putString("default_email", "");
        }
        prefs.edit().commit();

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
        menu.add(Menu.NONE, ADD_TASK_MENU_ITEM, Menu.NONE, "Add Task")
            .setIcon(android.R.drawable.ic_menu_add);
        menu.add(Menu.NONE, LIST_ENTRIES_MENU_ITEM, Menu.NONE, "List Entries")
            .setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(Menu.NONE, PREFERENCES_MENU_ITEM, Menu.NONE, "Preferences")
            .setIcon(android.R.drawable.ic_menu_preferences);
        return result;
    }

    @Override
    public void onListItemClick(ListView lv, View v, int position, long id) {
        if (id == m_db.getCurrentTaskId()) {
            m_db.completeCurrentTask();
            getListView().clearChoices();
            getListView().requestLayout();
        } else {
            m_db.changeTask(id);
        }
        // Update the App Widget
        startService(new Intent(this, TimesheetAppWidgetProvider.UpdateService.class));
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
            case PREFERENCES_MENU_ITEM:
                preferences();
                return true;
        }
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.NONE, EDIT_TASK_MENU_ITEM, Menu.NONE, "Edit Task");
        menu.add(Menu.NONE, DELETE_TASK_MENU_ITEM, Menu.NONE, "Delete Task");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case EDIT_TASK_MENU_ITEM:
                Intent i = new Intent(this, TaskEditActivity.class);
                i.putExtra("_id", info.id);
                startActivityForResult(i, ACTIVITY_EDIT);
                return true;
            case DELETE_TASK_MENU_ITEM:
                m_db.deleteTask(info.id);
                m_task_cursor.requery();
                // Update the App Widget, if necessary
                startService(new Intent(this, TimesheetAppWidgetProvider.UpdateService.class));
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

    private void preferences() {
        Intent i = new Intent(this, TimesheetPreferences.class);
        startActivity(i);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            m_task_cursor.requery();
            // Update the App Widget, if necessary
            startService(new Intent(this, TimesheetAppWidgetProvider.UpdateService.class));
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
