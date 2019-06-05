package com.example.woles.myapplication;

import android.app.ProgressDialog;
import android.content.Context;
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
import org.chromium.content.browser.ThreadUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class NewTrackActivity extends AppCompatActivity {

    EditText nameInput;
    Button saveBtn;
    AlertDialog alertDialog;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_track);
        saveBtn = (Button) findViewById(R.id.saveBtn);
        nameInput = (EditText) findViewById(R.id.nameInput);

        alertDialog = new AlertDialog.Builder(this).create();
        progressDialog = new ProgressDialog(this);

    }

    public void saveBtn_OnClick(View view) {
        if(!nameInput.getText().toString().equals("")) {
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setTitle("Zapisywanie");
            progressDialog.setMessage("Czekaj...");
            progressDialog.setIndeterminate(true);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
            new Thread(new Runnable() {
                public void run() {
                    createTrack(nameInput.getText().toString());
                }
            }).start();

        }
    }

    private void createTrack(String trackName) {

        SharedPreferences sharedPref = UserInfo.getPref(this);
        SharedPreferences.Editor editor = sharedPref.edit();

        JSONObject json = new JSONObject();
        try {
            json.put("name", trackName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String body = json.toString();

        Log.e("LOG", ""+body);

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://"+MapsActivity.serverIP+":8080/api/track/create_track").openConnection();
            conn.setDoOutput(true);
            //conn.setFixedLengthStreamingMode(body.length());
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

            if(conn.getResponseCode() == 201){

                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
                StringBuilder sb = new StringBuilder();
                String output;
                while ((output = br.readLine()) != null) {
                    sb.append(output);
                }
                JSONObject jsonBody = new JSONObject(sb.toString());
                JSONObject data = jsonBody.getJSONObject("data");

                String trackID = data.getString("_id");
                String name = data.getString("name");

                editor.putString("trackID", trackID);
                editor.putString("trackName", name);
                editor.commit();
                UserInfo.loadInfo(this);
                MapsActivity.gpsManager.getFacade().setTrackID(trackID);

                alertDialog.setTitle("");
                alertDialog.setMessage("Trasa została utworzona");
                alertDialog.setCancelable(false);
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();
                                Intent intent = new Intent(NewTrackActivity.this, MenuActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            }
                        });

                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        alertDialog.show();
                    }
                });


            }
            else {
                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getErrorStream())));
                StringBuilder sb = new StringBuilder();
                String output;
                while ((output = br.readLine()) != null) {
                    sb.append(output);
                }
                JSONObject jsonBody = new JSONObject(sb.toString());
                String errorMsg = jsonBody.getString("message");
                showErrorMessage(errorMsg);

            }


        } catch (IOException e) {
            e.printStackTrace();
            showErrorMessage(e.getMessage());
        } catch (JSONException e) {
            e.printStackTrace();
            showErrorMessage(e.getMessage());
        }

    }

    private void showErrorMessage(String message) {

        alertDialog.setTitle("Błąd");
        alertDialog.setMessage(message);
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
                progressDialog.dismiss();
                alertDialog.show();
            }
        });
    }

}