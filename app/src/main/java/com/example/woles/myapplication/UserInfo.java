package com.example.woles.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class UserInfo {

    static String userID;
    static String email;
    static String username;
    static String token;
    static String trackID;
    static boolean isActive;
    static SharedPreferences sharedPref;


    public static SharedPreferences getPref(Context context) {
        if(sharedPref == null)
            sharedPref = context.getSharedPreferences("authInfo", Context.MODE_PRIVATE);

        return sharedPref;
    }

    public static void loadInfo(Context context) {
        SharedPreferences sharedPref = getPref(context);

        userID = sharedPref.getString("userID", "");
        trackID = sharedPref.getString("trackID", "");
        token = sharedPref.getString("token", "");
        email = sharedPref.getString("userEmail", "");
        username = sharedPref.getString("username", "");
        isActive = sharedPref.getBoolean("userActive", false);
    }

    public static boolean isLoggedIn() {
        if(!isActive || token.isEmpty())
            return false;
        else
            return true;
    }

    public static void redirectToLogin(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void logout() {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("token", "");
        editor.putBoolean("userActive", false);
        editor.commit();
    }


    public static String getUserID() {
        return userID;
    }

    public static String getEmail() {
        return email;
    }

    public static String getUsername() {
        return username;
    }

    public static String getToken() {
        return token;
    }

    public static boolean isActive() {
        return isActive;
    }

    public static String getTrackID() {
        return trackID;
    }
}
