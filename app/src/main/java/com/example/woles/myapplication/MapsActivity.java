package com.example.woles.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
        openWebSocket();
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
        subBtn = findViewById(R.id.subBtn);
     /*   StompClient mStompClient;

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + UserInfo.getToken());

        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP,
                "ws://192.168.1.41:8080/send", headers);
        mStompClient.connect();


        Disposable dispLifecycle = mStompClient.lifecycle().subscribe(lifecycleEvent -> {
            switch (lifecycleEvent.getType()) {

                case OPENED:
                    Log.e("WEBSOCKET", "Stomp connection opened");
                    break;

                case ERROR:
                    Log.e("WEBSOCKET", "Error", lifecycleEvent.getException());
                    break;

                case CLOSED:
                    Log.e("WEBSOCKET", "Stomp connection closed");
                    break;
            }
        });



        Disposable dispTopic = mStompClient.topic("/topic/get/5c5d6da932bee21b60fca64b").subscribe(topicMessage -> {
            Log.e("WEBSOCKET", topicMessage.getPayload());
           // mStompClient.send("/ws/send/5c5d6da932bee21b60fca64", "My first STOMP message!").subscribe();
        }, error ->{
            Log.e("WEBSOCKET", error.getMessage());
        });
*/

/*        Disposable dispTopic2 = mStompClient.topic("/topic/get/5c5d6da932bee21b60fca64").subscribe(topicMessage -> {
            Log.e("WEBSOCKET", topicMessage.getPayload());
        }, error ->{
            Log.e("WEBSOCKET", error.getMessage());
        });*/




//        new Thread(new Runnable() {
//            public void run() {
//
//
//
//            }
//        }).start();

    }

    public void menuBtn_onClick(View view) {
        //Intent intent = new Intent(this, SubsActivity.class);
        //startActivity(intent);
        if(isSubAll) {
            gpsManager.getFacade().unSubAll();
            subBtn.setText("Obesrwuj");
            isSubAll = false;
        }
        else {
            isSubAll = true;
            gpsManager.getFacade().subAll();
            subBtn.setText("Nie obesrwuj");
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        if(gpsManager != null)
            gpsManager.getFacade().resume();
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

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0f));
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

    private void openWebSocket() {




        JSONObject jsonObject = new JSONObject();
        String serverIP = "192.168.1.41";
        try {
            jsonObject.put("token", UserInfo.getToken());
            jsonObject.put("userID", UserInfo.getUserID());
            jsonObject.put("WS_URL", "ws://"+serverIP+":8080/send");
            jsonObject.put("usersURL", "http://"+serverIP+":8080/api/test/users");
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

}

