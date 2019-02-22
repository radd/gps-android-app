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
import android.widget.EditText;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    EditText emailInput;
    EditText passwordInput;
    AlertDialog alertDialog;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        alertDialog = new AlertDialog.Builder(this).create();
        progressDialog = new ProgressDialog(this);

        //SharedPreferences sharedPref = this.getSharedPreferences("authInfo", Context.MODE_PRIVATE);
        SharedPreferences sharedPref = UserInfo.getPref(this);
        String token = sharedPref.getString("token", "");
        String email = sharedPref.getString("userEmail", "");
        boolean userActive = sharedPref.getBoolean("userActive", false);

        /*if(userActive) {
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
            finish();
            return;
        }*/

        emailInput = (EditText) findViewById(R.id.email);
        passwordInput = (EditText) findViewById(R.id.password);

        emailInput.setText(email);


        if(token.isEmpty())
            Log.e("LOG", "token null");
        else
            Log.e("LOG", "token:" + token);
    }

    public void loginBtn_onClick(View view) {
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle("Logowanie");
        progressDialog.setMessage("Czekaj...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        new Thread(new Runnable() {
            public void run() {
                signIn();
            }
        }).start();



    }

    private void signIn() {
        JSONObject json = new JSONObject();



        SharedPreferences sharedPref = this.getSharedPreferences("authInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();


        try {
            json.put("email", emailInput.getText());
            json.put("password", passwordInput.getText());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String body = json.toString();
        Log.e("LOG", ""+body);

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://"+MapsActivity.serverIP+":8080/api/auth/sign-in").openConnection();
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(body.length());
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            /*Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> pair = it.next();
                conn.setRequestProperty(pair.getKey(), pair.getValue());
            }*/

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

            Log.e("LOG", ""+ conn.getResponseCode());

            if(conn.getResponseCode() == 200){

                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
                StringBuilder sb = new StringBuilder();
                String output;
                while ((output = br.readLine()) != null) {
                    sb.append(output);
                }
                JSONObject jsonBody = new JSONObject(sb.toString());
                JSONObject data = jsonBody.getJSONObject("data");
                String token = data.getString("accessToken");
                JSONObject user = data.getJSONObject("user");
                String userID = user.getString("_id");
                String userEmail = user.getString("email");
                String username = user.getString("username");

                editor.putString("token", token);
                editor.putString("userID", userID);
                editor.putString("userEmail", userEmail);
                editor.putString("username", username);
                editor.putBoolean("userActive", true);
                editor.commit();

                progressDialog.dismiss();
                Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();

            }
            else {
                if(conn.getResponseCode() == 401) {
                    showErrorMessage("Nieprawidłowy email lub hasło");
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

        LoginActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.dismiss();
                alertDialog.show();
            }
        });
    }

    public void signUpBtn_onClick(View view) {
        Log.e("LOG", "nowe konto");

        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }



}