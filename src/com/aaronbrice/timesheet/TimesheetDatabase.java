package com.aaronbrice.timesheet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Calendar;

public class TimesheetDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Timesheet";
    private static final int DATABASE_VERSION = 1;

    public TimesheetDatabase(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) throws SQLException {
        String[] sqls = new String[] {
            "CREATE TABLE tasks (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, billable INTEGER)",
            "CREATE TABLE time_entries (_id INTEGER PRIMARY KEY AUTOINCREMENT, task_id INTEGER, start_time TEXT NOT NULL, end_time TEXT)"
        };
        db.beginTransaction();
        try {
            for( String sql : sqls )
                db.execSQL(sql);
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e("Error creating Timesheet database tables", e.toString());
            throw e;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int old_version, int new_version) {
        String[] sqls = new String[] {
            "DROP TABLE tasks",
            "DROP TABLE time_entries",
            "CREATE TABLE tasks (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, billable INTEGER)",
            "CREATE TABLE time_entries (_id INTEGER PRIMARY KEY AUTOINCREMENT, task_id INTEGER, start_time TEXT NOT NULL, end_time TEXT)"
        };
        db.beginTransaction();
        try {
            for( String sql : sqls )
                db.execSQL(sql);
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e("Error upgrading Timesheet database tables", e.toString());
            throw e;
        } finally {
            db.endTransaction();
        }
    }

    public Cursor getTasks() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("tasks", new String[] {"_id", "title", "billable"}, null, null, null, null, "billable DESC, _id ASC");
        c.moveToFirst();
        return c;
    }

    public void newTask(String title, boolean billable) {
        ContentValues cv = new ContentValues();
        cv.put("title", title);
        cv.put("billable", billable);
        try {
            getWritableDatabase().insert("tasks", null, cv);
        } catch (SQLException e) {
            Log.e("Error adding new task", e.toString());
        }
    }

    public void deleteTask(long task_id) {
        try {
            getWritableDatabase().delete("tasks", "_id=?", new String[] {Long.toString(task_id)});
        } catch (SQLException e) {
            Log.e("Error deleting task", e.toString());
        }
    }

    public Cursor getTimeEntry(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT time_entries._id, title, strftime('%H:%M', start_time) AS start_time, strftime('%H:%M', end_time) AS end_time FROM time_entries, tasks"
                + " WHERE tasks._id = time_entries.task_id AND time_entries._id = ? ORDER BY start_time ASC", 
                new String[] {Long.toString(id)}
        );
        c.moveToFirst();
        return c;
    }

    public Cursor getTimeEntries() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT time_entries._id, title, strftime('%H:%M', start_time) AS start_time, strftime('%H:%M', end_time) AS end_time FROM time_entries, tasks"
                + " WHERE tasks._id = time_entries.task_id AND date(start_time) = ? ORDER BY start_time ASC", 
                new String[] {getSqlDate()}
        );
        c.moveToFirst();
        return c;
    }

    public Cursor getTimeEntries(int year, int month, int day) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT time_entries._id, title, strftime('%H:%M', start_time) AS start_time, strftime('%H:%M', end_time) AS end_time FROM time_entries, tasks"
                + " WHERE tasks._id = time_entries.task_id AND date(start_time) = ? ORDER BY start_time ASC", 
                new String[] {String.format("%04d-%02d-%02d", year, month, day)}
        );
        c.moveToFirst();
        return c;
    }

    public void newTimeEntry(long task_id, String start_time, String end_time) {
        ContentValues cv = new ContentValues();
        cv.put("task_id", task_id);
        cv.put("start_time", start_time);
        cv.put("end_time", end_time);
        try {
            getWritableDatabase().insert("time_entries", null, cv);
        } catch (SQLException e) {
            Log.e("Error adding new time entry", e.toString());
        }
    }

    public Cursor getWeekEntries() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT time_entries._id AS _id, title, date(start_time) AS start_date,"
                + " sum((strftime('%s', end_time) - strftime('%s', start_time)) / 3600.0) AS duration"
                + " FROM time_entries, tasks"
                + " WHERE tasks._id = time_entries.task_id AND date(start_time) = ?"
                + " GROUP BY title, start_date ORDER BY start_date ASC",
                new String[] {getSqlDate()}
        );
        c.moveToFirst();
        return c;
    }

    public Cursor getWeekEntries(int year, int month, int day) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT time_entries._id AS _id, title, date(start_time) AS start_date,"
                + " sum((strftime('%s', end_time) - strftime('%s', start_time)) / 3600.0) AS duration"
                + " FROM time_entries, tasks"
                + " WHERE tasks._id = time_entries.task_id AND date(start_time) = ?"
                + " GROUP BY title, start_date ORDER BY start_date ASC",
                new String[] {String.format("%04d-%02d-%02d", year, month, day)}
        );
        c.moveToFirst();
        return c;
    }

    public void deleteTimeEntry(long time_entry_id) {
        try {
            getWritableDatabase().delete("time_entries", "_id=?", new String[] {Long.toString(time_entry_id)});
        } catch (SQLException e) {
            Log.e("Error deleting time entry", e.toString());
        }
    }

    public void completeTask(long id) {
        String time = getSqlTime();
        ContentValues cv = new ContentValues();
        cv.put("end_time", time);
        try {
            getWritableDatabase().update("time_entries", cv, "_id=?", new String[] {Long.toString(id)});
        } catch (SQLException e) {
            Log.e("Error updating time entry", e.toString());
            return;
        }
    }

    public void completeCurrentTask() {
        long current_id = getCurrentId();
        if (current_id == 0) {
            return;
        }
        completeTask(current_id);
    }

    public void changeTask(long id) {
        completeCurrentTask();
        newTimeEntry(id, getSqlTime(), null);
    }

    public long getCurrentId() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(true, "time_entries", new String[] {"_id"}, "end_time IS NULL", null, null, null, null, null);
        if (c.getCount() == 0) {
            return 0;
        }
        c.moveToFirst();
        return c.getLong(0);
    }

    public long getCurrentTaskId() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(true, "time_entries", new String[] {"task_id"}, "end_time IS NULL", null, null, null, null, null);
        if (c.getCount() == 0) {
            return 0;
        }
        c.moveToFirst();
        return c.getLong(0);
    }

    public static String getSqlDate() {
        final Calendar c = Calendar.getInstance();
        return String.format("%04d-%02d-%02d", 
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static String getSqlTime() {
        final Calendar c = Calendar.getInstance();
        return String.format("%04d-%02d-%02d %02d:%02d", 
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), 
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }
}
