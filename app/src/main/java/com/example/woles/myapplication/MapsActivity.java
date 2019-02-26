package com.example.woles.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.marianhello.bgloc.PluginDelegate;
import com.marianhello.bgloc.PluginException;
import com.marianhello.bgloc.WebSocketTrans;
import com.marianhello.bgloc.data.BackgroundActivity;
import com.marianhello.bgloc.data.BackgroundLocation;
import io.reactivex.disposables.CompositeDisposable;
import org.chromium.content.browser.ThreadUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import io.reactivex.disposables.Disposable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class MapsActivity extends FragmentActivity implements IGPSManager, PluginDelegate, OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;

    private Marker mainMarker;

    static public GPSManager gpsManager;
    private  Button startBtn;
    private boolean isRunning;
    private boolean isSubAll = false;
    private Button subBtn;
    private TextView connState;
    View infoBar;
    View moreInfoBar;

    public static String serverIP = "40.115.21.196";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UserInfo.loadInfo(this);
        if(!UserInfo.isLoggedIn()) {
            UserInfo.redirectToLogin(this);
            finish();
            return;
        }
        Log.e("LOG", ""+ UserInfo.getUserID());
        Log.e("LOG", ""+ UserInfo.getEmail());
        Log.e("LOG", ""+ UserInfo.getUsername());
        //Log.e("LOG", ""+ UserInfo.getToken());

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        gpsManager = new GPSManager(getApplicationContext(),this, this);
        setUsersAndOpenWebSocket();
        //gpsManager.getFacade().start();

        startBtn = findViewById(R.id.startBtn);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isRunning) {
                    isRunning = false;
                    startBtn.setText("Start");
                    gpsManager.getFacade().stop();
                }
                else {
                    startBtn.setText("Stop");
                    gpsManager.getFacade().start();
                    isRunning = true;
                }

            }
        });
        subBtn = (Button) findViewById(R.id.subBtn);
        connState = (TextView) findViewById(R.id.connectionInfo);
        infoBar  = findViewById(R.id.infoBar);
        moreInfoBar  = findViewById(R.id.moreInfoBar);
    }

    public void menuBtn_onClick(View view) {
        Intent intent = new Intent(this, MenuActivity.class);
        startActivity(intent);
    }

    public void subBtn_onClick(View view) {
        if(isSubAll) {
            gpsManager.getFacade().unSubAll();
            subBtn.setText("Obserwuj");
            isSubAll = false;
        }
        else {
            isSubAll = true;
            gpsManager.getFacade().subAll();
            subBtn.setText("Nie obserwuj");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(gpsManager != null) {
            gpsManager.getFacade().resume();
            gpsManager.getFacade().openWebSocket();
        }
        else {
            Log.e("onResume", "gpsManager is null");
        }

        Log.e("GPSAPP", "foreground");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(gpsManager != null)
            gpsManager.getFacade().pause();
        Log.e("GPSAPP", "background");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(gpsManager != null)
            gpsManager.getFacade().destroy();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the camera


        LatLng cord = new LatLng(50, 19);

        //TODO init after get curr location
        mainMarker = mMap.addMarker(new MarkerOptions().position(cord).title("Moja lokalizacja"));
        mainMarker.setTag("id1");
        mainMarker.showInfoWindow();
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cord, 16.0f));
        mMap.setOnMarkerClickListener(this);



       if(!gpsManager.getFacade().isRunning()) {
            if(gpsManager.getFacade().getAuthorizationStatus() == 1)
                getCurrentLocation();
        }


      if(gpsManager.getFacade().isRunning()) {
          isRunning = true;
          startBtn.setText("Stop");
          getCurrentLocation();
      }


    }

    private void getCurrentLocation() {
        if(gpsManager.getFacade().getAuthorizationStatus() == 1)
            gpsManager.getCurrentLocation();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        showInfoBar();
        if(marker.getTag() == null)
            return false;

        if (marker.getTag().equals("id1"))
        {
            //handle click here
            Log.e("", "clicked4");
        }
        marker.showInfoWindow();
        return false;
    }

    private void setCurrentLocation(BackgroundLocation location) {
        if(mainMarker == null)
            return;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mainMarker.setPosition(latLng);

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.0f));
    }

    private void setChangeLocation(BackgroundLocation location) {
        if(mainMarker == null)
            return;
        //Log.e("", ""+ location.getLatitude());
        mainMarker.setPosition( new LatLng(location.getLatitude(), location.getLongitude()));
    }




    @Override
    public void onCurrentLocation(final BackgroundLocation location) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setCurrentLocation(location);
            }
        });

    }


    @Override
    public void onLocationChanged(final BackgroundLocation location) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setChangeLocation(location);
            }
        });
    }

    @Override
    public void onStationaryChanged(BackgroundLocation location) {

    }

    @Override
    public void onActivityChanged(BackgroundActivity activity) {

    }

    @Override
    public void onServiceStatusChanged(int status) {

    }

    @Override
    public void onAbortRequested() {

    }

    @Override
    public void onHttpAuthorization() {

    }

    @Override
    public void onError(PluginException error) {

    }

    @Override
    public void onAuthorizationChanged(int authStatus) {
        if(authStatus == 1)
            gpsManager.getCurrentLocation();
    }

    private void setUsersAndOpenWebSocket() {

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("token", UserInfo.getToken());
            jsonObject.put("userID", UserInfo.getUserID());
            jsonObject.put("WS_URL", "ws://"+serverIP+":8080/connect-ws");
            jsonObject.put("usersURL", "http://"+serverIP+":8080/api/users");
            jsonObject.put("trackID", UserInfo.getTrackID());

        } catch (JSONException e) {
            e.printStackTrace();
        }

        gpsManager.getFacade().setUserInfoAndOpenWS(jsonObject.toString());

    }


    private ConcurrentHashMap<String, JSONObject> users = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Marker> markers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Polyline> polylines = new ConcurrentHashMap<>();


    @Override
    public void onUsers(String usersJson) {
        users = new ConcurrentHashMap<>();
        polylines = new ConcurrentHashMap<>();

        Log.e("LOG", "get users: " + usersJson);

        try {
            JSONArray array = new JSONArray(usersJson);

            for(int i = 0;i<array.length();i++) {
                JSONObject user = array.getJSONObject(i);
                users.put(user.getString("_id"), user);
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onWebSocketMessage(String payload) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                if(mMap == null)
                    return;

                JSONObject location = null;
                try {
                    location = new JSONObject(payload);

                    String userID = location.getString("userID");
                    if( users.get(location.getString("userID")) != null) {

                        LatLng latLng = new LatLng(Double.valueOf(location.getString("latitude")),
                                Double.valueOf(location.getString("longitude")));

                        if(markers.get(userID) != null ) {
                            ThreadUtils.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    markers.get(userID).setPosition(latLng);

                                }
                            });
                        }
                        else {
                            String username = users.get(location.getString("userID")).getString("username");
                            ThreadUtils.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(username));
                                    marker.showInfoWindow();
                                    markers.put(userID, marker);

                                }
                            });

                        }

                        Log.e("LOG", "User: " + users.get(location.getString("userID")).getString("username")
                                + " loc: " + location.getString("latitude")
                                + " " + location.getString("longitude"));
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }).start();

    }

    @Override
    public void clearMap() {
        for(String key : markers.keySet()) {
            markers.get(key).remove();
        }
        markers.clear();
        for(String key : polylines.keySet()) {
            polylines.get(key).remove();
        }
        polylines.clear();

    }

    @Override
    public void onWebSocketState(String state) {
        String prefix = "WS serwer: ";
        connState.setText(prefix + state);
    }


    boolean isInfoBarActive = true;
    boolean isMoreInfoBarActive = false;

    public void closeInfoBar(View view) {
        Log.e("LOG", "close info bar");
        if(!isInfoBarActive)
            return;

        infoBar.animate().translationY(-infoBar.getHeight());
        if(isMoreInfoBarActive)
            moreInfoBar.animate().translationY(-(moreInfoBar.getHeight() + moreInfoBar.getTranslationY()) + (-infoBar.getHeight()));
        else
            moreInfoBar.animate().translationY(-(moreInfoBar.getHeight() - 10f) + (-infoBar.getHeight()));

        isInfoBarActive = false;
        isMoreInfoBarActive = false;
    }

    public void showInfoBar() {
        Log.e("LOG", "close info bar");
        if(isInfoBarActive)
            return;

        infoBar.animate().translationY(0);
        if(isMoreInfoBarActive)
            moreInfoBar.animate().translationY(moreInfoBar.getHeight() + moreInfoBar.getTranslationY() + infoBar.getHeight() -20f);
        else
            moreInfoBar.animate().translationY(-moreInfoBar.getHeight());

        isInfoBarActive = true;
    }

    public void showMoreInfo(View view) {
        Log.e("LOG", "show more info");
        if(isMoreInfoBarActive) {
            moreInfoBar.animate().translationY(-moreInfoBar.getHeight());
        }
        else {
            moreInfoBar.animate().translationY(-10f);
        }
        isMoreInfoBarActive = !isMoreInfoBarActive;
    }
}

