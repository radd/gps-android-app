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

public class SignUpActivity extends AppCompatActivity {

    EditText emailInput;
    EditText nameInput;
    EditText passwordInput;
    EditText password2Input;
    AlertDialog alertDialog;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        emailInput = (EditText) findViewById(R.id.email);
        nameInput = (EditText) findViewById(R.id.name);
        passwordInput = (EditText) findViewById(R.id.password);
        password2Input = (EditText) findViewById(R.id.password2);

        alertDialog = new AlertDialog.Builder(this).create();
        progressDialog = new ProgressDialog(this);

    }

    public void signUpBtn_onClick(View view) {
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle("Tworzenie konta");
        progressDialog.setMessage("Czekaj...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        new Thread(new Runnable() {
            public void run() {
                signUp();
            }
        }).start();
    }

    private void signUp() {

        JSONObject jsonForm = new JSONObject();
        try {
            jsonForm.put("email", emailInput.getText());
            jsonForm.put("username", nameInput.getText());
            jsonForm.put("password", passwordInput.getText());
            jsonForm.put("password2", password2Input.getText());
        } catch (JSONException e) {
            e.printStackTrace();
        }


        SharedPreferences sharedPref = UserInfo.getPref(this);
        SharedPreferences.Editor editor = sharedPref.edit();


        String body = jsonForm.toString();
        Log.e("LOG", ""+body);

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://192.168.1.43:8080/api/auth/sign-up").openConnection();
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(body.length());
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

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

            if(conn.getResponseCode() == 201){ //created

                editor.putString("userEmail",jsonForm.getString("email"));
                editor.commit();


                alertDialog.setTitle("");
                alertDialog.setMessage("Konto zostało założone");
                alertDialog.setCancelable(false);
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Zaloguj się",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();
                                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
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

        SignUpActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.dismiss();
                alertDialog.show();
            }
        });
    }
}