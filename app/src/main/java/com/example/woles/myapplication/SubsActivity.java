package com.example.woles.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;

public class SubsActivity extends AppCompatActivity {

    private ListView list ;
    private ArrayAdapter<String> adapter ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subs);




        list = (ListView) findViewById(R.id.usersListView);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?>adapter,View v, int position, long id){

            }
        });

       // adapter = new ArrayAdapter<String>(this, R.layout.users_list_view_item, carL);

        list.setAdapter(adapter);
    }
    public void clearMap(View view) {
        //MapsActivity.gpsManager.clearMap();
        finish();
    }



}