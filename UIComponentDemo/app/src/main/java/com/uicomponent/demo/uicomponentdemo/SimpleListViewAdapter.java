package com.uicomponent.demo.uicomponentdemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/9/15.
 */

public class SimpleListViewAdapter extends SimpleAdapter {

    private Context mContext;
    private LayoutInflater mInflater;
    private int mSelectIdx;
    private ArrayList<HashMap<String, String>> mlist;
    /**
     * Constructor
     *
     * @param context  The context where the View associated with this SimpleAdapter is running
     * @param data     A List of Maps. Each entry in the List corresponds to one row in the list. The
     *                 Maps contain the data for each row, and should include all the entries specified in
     *                 "from"
     * @param resource Resource identifier of a view layout that defines the views for this list
     *                 item. The layout file should include at least those named views defined in "to"
     * @param from     A list of column names that will be added to the Map associated with each
     *                 item.
     * @param to       The views that should display column in the "from" parameter. These should all be
     *                 TextViews. The first N views in this list are given the values of the first N columns
     */
    public SimpleListViewAdapter(Context context, ArrayList<HashMap<String, String>> data, int resource, String[] from, int[] to) {
        super(context, data, resource, from, to);

        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        mSelectIdx = 0;
        mlist = data;
    }
    public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {

            convertView = mInflater.inflate(R.layout.layout_listview_item, null);
            holder.rl_bg = (RelativeLayout) convertView.findViewById(R.id.rl_phonebook_bg);
            holder.name = (TextView) convertView.findViewById(R.id.list_item_phonebook_name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }
        String pbname = "";
        try{
            pbname = (String) mlist.get(position).get("FROM");

        }finally{
            ;
        }
        holder.name.setText(pbname);
        return convertView;
    }
    public final class ViewHolder {
        public TextView name;
        public RelativeLayout rl_bg;
    }
}
