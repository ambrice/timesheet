/***
 * Copyright (c) 2009 Tasty Cactus Software, LLC
 * 
 * All rights reserved.
 */

package com.tastycactus.timesheet;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.tastycactus.timesheet.TimesheetDatabase;

public class TaskEditActivity extends Activity {
    TimesheetDatabase m_db;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        m_db = new TimesheetDatabase(this);

        setContentView(R.layout.task_edit);

        final EditText title_edit = (EditText) findViewById(R.id.task_title);
        final CheckBox billable_edit = (CheckBox) findViewById(R.id.task_billable);

        Button addButton = (Button) findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String title = title_edit.getText().toString();
                if (title.length() > 0) {
                    m_db.newTask(title, billable_edit.isChecked());
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
