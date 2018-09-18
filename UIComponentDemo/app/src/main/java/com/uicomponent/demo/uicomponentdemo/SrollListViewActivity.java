package com.uicomponent.demo.uicomponentdemo;


import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SrollListViewActivity extends Activity {
    private SimpleListViewAdapter  m_SimpleListViewAdapter;
    private ArrayList<HashMap<String, String>> m_TextList = new ArrayList<HashMap<String, String>>();
    private ListView mCallLogListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sroll_list_view);
        mCallLogListView = (ListView) findViewById(R.id.contacts_listview);
        m_SimpleListViewAdapter = new SimpleListViewAdapter(this,m_TextList,R.layout.layout_listview_item,new String[] {
                "FROM"},new int[] {R.id.list_item_phonebook_name});

        m_TextList.add(new HashMap<String, String>(){
            {
                put("name", "June");
                put("FROM", "wocao");
            }
        });
        m_SimpleListViewAdapter.notifyDataSetChanged();
    }
}
