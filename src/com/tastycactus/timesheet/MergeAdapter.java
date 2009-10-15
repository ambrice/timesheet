/***
 * Copyright (c) 2009 Tasty Cactus Software, LLC
 * 
 * All rights reserved.
 */

package com.tastycactus.timesheet;

import android.content.Context;

import android.database.DataSetObservable;
import android.database.DataSetObserver;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Adapter;
import android.widget.ListAdapter;
import android.widget.TextView;

public class MergeAdapter implements ListAdapter
{
    DataSetObservable dso = new DataSetObservable();
    Adapter[] adapter_list;
    String[] header_list;
    int header_layout_id;
    int header_view_id;
    Context ctx;

    class AdapterSection {
        public Adapter adapter;
        public String header;
        public int index;
    }

    /**
     * Constructs a new instance.
     */
    public MergeAdapter(Context ctx, int header_layout_id, int header_view_id, Adapter[] adapter_list, String[] header_list)
    {
        this.ctx = ctx;
        this.header_layout_id = header_layout_id;
        this.header_view_id = header_view_id;
        this.adapter_list = adapter_list;
        this.header_list = header_list;
    }

    /**
     * {@inheritDoc}
     * @see ListAdapter#areAllItemsEnabled()
     */
    @Override
    public boolean areAllItemsEnabled()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     * @see ListAdapter#isEnabled(int)
     */
    @Override
    public boolean isEnabled(int position)
    {
        if (getAdapterSection(position).index == -1) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     * @see android.widget.Adapter#registerDataSetObserver(DataSetObserver)
     */
    @Override
    public void registerDataSetObserver(DataSetObserver observer)
    {
        dso.registerObserver(observer);
    }

    /**
     * {@inheritDoc}
     * @see android.widget.Adapter#unregisterDataSetObserver(DataSetObserver)
     */
    @Override
    public void unregisterDataSetObserver(DataSetObserver observer)
    {
        dso.unregisterObserver(observer);
    }

    /**
     * {@inheritDoc}
     * @see android.widget.Adapter#getCount()
     */
    @Override
    public int getCount()
    {
        int count = 0;
        // getCount() is the count of all adapters, plus one for each header
        for (Adapter a : adapter_list)
        {
            count += a.getCount() + 1;
        }
        return count;
    }

    /**
     * {@inheritDoc}
     * @see android.widget.Adapter#getItem(int)
     */
    @Override
    public Object getItem(int position)
    {
        AdapterSection section = getAdapterSection(position);
        if (section.index == -1) {
            return section.header;
        } else {
            return section.adapter.getItem(section.index);
        }
    }

    /**
     * {@inheritDoc}
     * @see android.widget.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(int position)
    {
        return position;
    }

    /**
     * {@inheritDoc}
     * @see android.widget.Adapter#hasStableIds()
     */
    @Override
    public boolean hasStableIds()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     * @see android.widget.Adapter#getView(int,View,ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        AdapterSection section = getAdapterSection(position);
        TextView tview;
        if (section.index == -1) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View v = inflater.inflate(header_layout_id, null);
                tview = (TextView) v.findViewById(header_view_id);
            } else {
                tview = (TextView) convertView;
            }
            tview.setText(section.header);
            return tview;
        } else {
            return section.adapter.getView(section.index, convertView, parent);
        }
    }

    /**
     * {@inheritDoc}
     * @see android.widget.Adapter#getItemViewType(int)
     */
    @Override
    public int getItemViewType(int position)
    {
        AdapterSection section = getAdapterSection(position);
        if (section.index == -1) {
            return 0;
        }
        int total = 1;
        for (Adapter a : adapter_list) {
            if (a == section.adapter) {
                return total + section.adapter.getItemViewType(section.index);
            }
            total += a.getViewTypeCount();
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     * @see android.widget.Adapter#getViewTypeCount()
     */
    @Override
    public int getViewTypeCount()
    {
        int total = 1;
        for (Adapter a : adapter_list) {
            total += a.getViewTypeCount();
        }
        return total;
    }

    /**
     * {@inheritDoc}
     * @see android.widget.Adapter#isEmpty()
     */
    @Override
    public boolean isEmpty()
    {
        if (adapter_list.length == 0) {
            return true;
        } else if (getCount() == 0) {
            return true;
        }
        return false;
    }

    public void notifyDataSetChanged()
    {
        dso.notifyChanged();
    }

    public void notifyDataSetInvalidated()
    {
        dso.notifyInvalidated();
    }

    protected AdapterSection getAdapterSection(int position) 
    {
        // Given a position, get the adapter and header at 
        // that position
        AdapterSection section = new AdapterSection();
        int p = 0;
        int list_idx = 0;
        for (Adapter a : adapter_list) {
            String h = header_list[list_idx];
            if (p == position) {
                section.adapter = a;
                section.header = h;
                section.index = -1;
                return section;
            }
            if (p + a.getCount() >= position) {
                section.adapter = a;
                section.header = h;
                section.index = position - p - 1;
                return section;
            }
            p += a.getCount() + 1;
            ++list_idx;
        }
        return null;
    }
}
