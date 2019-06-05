package com.example.woles.myapplication;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.chromium.content.browser.ThreadUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class MenuActivity extends AppCompatActivity {

    Button trackBtn;
    AlertDialog alertDialog;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        alertDialog = new AlertDialog.Builder(this).create();
        progressDialog = new ProgressDialog(this);

        trackBtn = (Button) findViewById(R.id.trackBtn);

        setUserInfo();

        setTrackButton();

    }


    @Override
    protected void onResume() {
        super.onResume();
        setTrackButton();
    }


    private void setUserInfo() {
        TextView username = (TextView) findViewById(R.id.username);
        TextView email = (TextView) findViewById(R.id.email);

        username.setText(UserInfo.getUsername());
        email.setText(UserInfo.getEmail());
    }

    private void setTrackButton() {
        SharedPreferences sharedPref = UserInfo.getPref(this);

        if(UserInfo.getTrackID()== null || UserInfo.getTrackID().isEmpty()) {
            trackBtn.setText("Utwórz nową trasę");
        }
        else {
            trackBtn.setText("Zakończ trasę: \"" + UserInfo.getTrackName() + "\"");
        }
    }

    public void logout(View view) {
        UserInfo.logout();
        UserInfo.redirectToLogin(this);
        finish();
    }

    public void trackBtn_OnClick(View view) {
        if(UserInfo.getTrackID()== null || UserInfo.getTrackID().isEmpty()) {
            createTrack();
        }
        else {

            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setTitle("Zakończ trasę");
            progressDialog.setMessage("Czekaj...");
            progressDialog.setIndeterminate(true);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            new Thread(new Runnable() {
                public void run() {
                    finishTrack();
                }
            }).start();

        }
    }


    private void createTrack() {
        Intent intent = new Intent(this, NewTrackActivity.class);
        startActivity(intent);
    }

    private void finishTrack() {
        SharedPreferences sharedPref = UserInfo.getPref(this);
        SharedPreferences.Editor editor = sharedPref.edit();

        JSONObject json = new JSONObject();
        try {
            json.put("trackID", UserInfo.getTrackID());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String body = json.toString();

        Log.e("LOG", ""+body);

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://"+MapsActivity.serverIP+":8080/api/track/finish_track").openConnection();
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(body.length());
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + UserInfo.getToken());

            OutputStreamWriter os = null;
            try {
                os = new OutputStreamWriter(conn.getOutputStream());
                os.write(body);

            } finally {
                if (os != null) {
                    os.flush();
                    os.close();
                }
            }

            //Log.e("LOG", ""+ conn.getResponseCode());

            if(conn.getResponseCode() == 200){
                editor.putString("trackID", "");
                editor.putString("trackName", "");
                editor.commit();
                UserInfo.loadInfo(this);


                alertDialog.setTitle("");
                alertDialog.setMessage("Trasa została zakończona");
                alertDialog.setCancelable(false);
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();

                            }
                        });

                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setTrackButton();
                        progressDialog.dismiss();
                        alertDialog.show();
                    }
                });

            }
            else {


            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void refresh_onClick(View view) {
        MapsActivity.gpsManager.getFacade().openWebSocket();
    }
}