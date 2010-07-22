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

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.tastycactus.timesheet.TimesheetDatabase;

public class TaskEditActivity extends Activity {
    TimesheetDatabase m_db;
    long              m_row_id;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        m_db = new TimesheetDatabase(this);

        Bundle b = getIntent().getExtras();
        String title;
        boolean billable;

        if (b != null) {
            m_row_id = b.getLong("_id");
            Cursor entry = m_db.getTask(m_row_id);
            int billable_int = entry.getInt(entry.getColumnIndex("billable"));
            billable = (billable_int != 0);
            title = entry.getString(entry.getColumnIndex("title"));
            entry.close();
        } else {
            m_row_id = -1;
            billable = false;
            title = "";
        }

        setContentView(R.layout.task_edit);

        final EditText title_edit = (EditText) findViewById(R.id.task_title);
        final CheckBox billable_edit = (CheckBox) findViewById(R.id.task_billable);

        title_edit.setText(title);
        billable_edit.setChecked(billable);

        Button addButton = (Button) findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String title = title_edit.getText().toString();
                if (title.length() > 0) {
                    if (m_row_id == -1) {
                        m_db.newTask(title, billable_edit.isChecked());
                    } else {
                        m_db.updateTask(m_row_id, title, billable_edit.isChecked());
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
}
