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

import android.app.IntentService;
import android.app.PendingIntent;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.database.Cursor;

import android.preference.PreferenceManager;

import android.widget.RemoteViews;

import com.tastycactus.timesheet.TimesheetDatabase;

public class TimesheetAppWidgetProvider extends AppWidgetProvider 
{
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction()==null) {
            context.startService(new Intent(context, UpdateService.class));
        }
        else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        context.startService(new Intent(context, UpdateService.class));
    }

    public static class UpdateService extends IntentService
    {
        TimesheetDatabase m_db;
        SharedPreferences m_prefs=null;

        public UpdateService() {
            super("TimesheetAppWidgetProvider$UpdateService");
        }

        @Override
        public void onCreate() {
            super.onCreate();
            m_db = new TimesheetDatabase(this);
            m_prefs = PreferenceManager.getDefaultSharedPreferences(this);
        }

        @Override
        public void onDestroy() {
            m_db.close();
            super.onDestroy();
        }

        @Override
        public void onHandleIntent(Intent intent) {
            ComponentName me = new ComponentName(this, TimesheetAppWidgetProvider.class);
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);
            mgr.updateAppWidget(me, buildUpdate(this));
        }

        private RemoteViews buildUpdate(Context context) {
            long task_id = m_prefs.getLong("app_task", -1);
            long current_id = m_db.getCurrentTaskId();

            // task_id could be an id that has since been deleted
            if (task_id == -1 || !m_db.isValidTask(task_id)) {
                if (current_id == 0) {
                    task_id = m_db.getFirstTaskId(m_prefs.getBoolean("alphabetise_tasks", false));
                } else {
                    task_id = current_id;
                }
                SharedPreferences.Editor edit = m_prefs.edit();
                edit.putLong("app_task", task_id);
                edit.commit();
            }

            RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.app_widget);

            if (task_id == current_id && task_id > 0) {
                updateViews.setImageViewResource(R.id.select_task, R.drawable.vert_toggle_on);
            } else {
                updateViews.setImageViewResource(R.id.select_task, R.drawable.vert_toggle_off);
            }

            updateViews.setTextViewText(R.id.current_task, m_db.getTaskName(task_id));

            Intent intent = new Intent(context, ToggleActiveService.class);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
            updateViews.setOnClickPendingIntent(R.id.select_task, pendingIntent);

            intent = new Intent(context, NextTaskService.class);
            pendingIntent = PendingIntent.getService(context, 0, intent, 0);
            updateViews.setOnClickPendingIntent(R.id.next_task, pendingIntent);

            return updateViews;
        }
    }

    public static class ToggleActiveService extends IntentService
    {
        TimesheetDatabase m_db;
        SharedPreferences m_prefs=null;

        public ToggleActiveService() {
            super("TimesheetAppWidgetProvider$ToggleActiveService");
        }

        @Override
        public void onCreate() {
            super.onCreate();
            m_db = new TimesheetDatabase(this);
            m_prefs = PreferenceManager.getDefaultSharedPreferences(this);
        }

        @Override
        public void onDestroy() {
            m_db.close();
            super.onDestroy();
        }

        @Override
        public void onHandleIntent(Intent intent) {
            // If the currently selected task is the active task, clear the 
            // current task.  Otherwise, set this to the current task
            long task_id = m_prefs.getLong("app_task", -1);
            long current_id = m_db.getCurrentTaskId();
            if (task_id == current_id && task_id > 0) {
                m_db.completeCurrentTask();
            } else if (task_id > 0) {
                m_db.changeTask(task_id);
            }

            // Update the GUI
            startService(new Intent(this, UpdateService.class));
        }
    }

    public static class NextTaskService extends IntentService
    {
        TimesheetDatabase m_db;
        SharedPreferences m_prefs=null;

        public NextTaskService() {
            super("TimesheetAppWidgetProvider$NextTaskService");
        }

        @Override
        public void onCreate() {
            super.onCreate();
            m_db = new TimesheetDatabase(this);
            m_prefs = PreferenceManager.getDefaultSharedPreferences(this);
        }

        @Override
        public void onDestroy() {
            m_db.close();
            super.onDestroy();
        }

        @Override
        public void onHandleIntent(Intent intent) {
            long task_id = m_prefs.getLong("app_task", -1);
            long next_task_id = -1;
            Cursor c = m_db.getTasks(m_prefs.getBoolean("alphabetise_tasks", false));
            while (!c.isAfterLast()) {
                long c_task_id = c.getLong(c.getColumnIndex("_id"));
                if (c_task_id == task_id) {
                    // Found the current task, find the next one
                    if (c.isLast()) {
                        c.moveToFirst();
                        next_task_id = c.getLong(c.getColumnIndex("_id"));
                    } else {
                        c.moveToNext();
                        next_task_id = c.getLong(c.getColumnIndex("_id"));
                    }
                    break;
                }
                c.moveToNext();
            }
            SharedPreferences.Editor edit = m_prefs.edit();
            edit.putLong("app_task", next_task_id);
            edit.commit();

            // Update the GUI
            startService(new Intent(this, UpdateService.class));
        }
    }
}
