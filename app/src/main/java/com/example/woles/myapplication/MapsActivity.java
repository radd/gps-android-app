package com.example.woles.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.marianhello.bgloc.PluginDelegate;
import com.marianhello.bgloc.PluginException;
import com.marianhello.bgloc.WebSocketTrans;
import com.marianhello.bgloc.data.BackgroundActivity;
import com.marianhello.bgloc.data.BackgroundLocation;
import io.reactivex.disposables.CompositeDisposable;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import io.reactivex.disposables.Disposable;

import java.util.HashMap;
import java.util.Map;


public class MapsActivity extends FragmentActivity implements IGPSManager, PluginDelegate, OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;

    private Marker mainMarker;

    static public GPSManager gpsManager;
    private  Button startBtn;
    private boolean isRunning;

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
        //openWebSocket();
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
        Intent intent = new Intent(this, MenuActivity.class);
        startActivity(intent);
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

        if (marker.getTag().equals("id1"))
        {
            //handle click here
            Log.e("", "clicked4");
        }

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

    StompClient mStompClient;

   /* private void openWebSocket() {
        gpsManager.getFacade().setToken(UserInfo.getToken());
        gpsManager.getFacade().setUserID(UserInfo.getUserID());
        gpsManager.getFacade().setServerIP("192.168.1.41");


        gpsManager.getFacade().openWebSocket(new WebSocketTrans() {
            @Override
            public void onWebSocketOpened(StompClient stompClient) {
                mStompClient = stompClient;
            }
        });
    }*/



}

