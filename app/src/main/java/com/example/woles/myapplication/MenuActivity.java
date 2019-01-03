package com.example.woles.myapplication;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

public class MenuActivity extends AppCompatActivity {

    EditText mURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        mURL = (EditText) findViewById(R.id.URL);

    }

    public void save(View view) {
        String url = mURL.getText().toString();
        MapsActivity.gpsManager.saveURL(url);

        finish();
    }

}