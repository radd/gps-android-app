package com.example.woles.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    EditText mURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //mURL = (EditText) findViewById(R.id.URL);

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String token = sharedPref.getString(getString(R.string.token), null);

        if(token == null)
            Log.e("LOG", "token null");
        else
            Log.e("LOG", "token:" + token);
    }

    public void loginBtn_onClick(View view) {

        JSONObject json = new JSONObject();

        EditText emailInput = (EditText) findViewById(R.id.email);
        EditText passwordInput = (EditText) findViewById(R.id.password);

        try {
            json.put("email", emailInput.getText());
            json.put("password", passwordInput.getText());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String body = json.toString();
        Log.e("LOG", ""+body);

       /* try {
            HttpURLConnection conn = (HttpURLConnection) new URL("").openConnection();
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(body.length());
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> pair = it.next();
                conn.setRequestProperty(pair.getKey(), pair.getValue());
            }

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


        } catch (IOException e) {
            e.printStackTrace();
        }


        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.token), );
        editor.commit();*/


    }

    public void signUpBtn_onClick(View view) {
        Log.e("LOG", "nowe konto");

    }



}