package com.example.woles.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.gson.Gson;
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
    private String activeUserID = "";
    private String followUserID = "";
    private boolean isFollow = false;

    private TextView altitude;
    private TextView speed;
    private TextView accuracy;
    private TextView name;
    private TextView follow;
    private TextView zoom;
    private TextView trackName;
    private View trackInfo;

    public static String serverIP = "40.115.21.196";

    private Gson gson = new Gson();
    Typeface font;

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

        subBtn = (Button) findViewById(R.id.subBtn);
        connState = (TextView) findViewById(R.id.connectionInfo);
        infoBar  = findViewById(R.id.infoBar);
        moreInfoBar  = findViewById(R.id.moreInfoBar);
        speed  = (TextView) findViewById(R.id.speed);
        altitude  = (TextView) findViewById(R.id.altitude);
        accuracy  = (TextView) findViewById(R.id.accuracy);
        name  = (TextView) findViewById(R.id.name);
        follow  = (TextView) findViewById(R.id.follow);
        zoom  = (TextView) findViewById(R.id.zoom);
        trackInfo  = findViewById(R.id.trackInfo);
        trackName  = (TextView) findViewById(R.id.trackName);

        activeUserID = UserInfo.getUserID();


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
                    speed.setText("0");
                }
                else {
                    startBtn.setText("Stop");
                    gpsManager.getFacade().start();
                    isRunning = true;
                }

            }
        });

        checkTrack();

        font = Typeface.createFromAsset( getAssets(), "fontello.ttf" );
        //font = Typeface.createFromAsset( getAssets(), "fa-solid-900.ttf" );

        TextView zoomIcon = (TextView) findViewById(R.id.zoomIcon);
        TextView followIcon = (TextView) findViewById(R.id.followIcon);
        TextView closeInfo = (TextView) findViewById(R.id.closeInfo);
        zoomIcon.setTypeface(font);
        followIcon.setTypeface(font);
        closeInfo.setTypeface(font);

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

            activeUserID = UserInfo.getUserID();
            name.setText("Twoja lokalizacja");
            getCurrentLocation();
        }
        else {
            isSubAll = true;
            gpsManager.getFacade().subAll();
            subBtn.setText("Nie obserwuj");
        }

    }

    public void followUser_onClick(View view) {
        if(isFollow) {
            if(followUserID.equals(activeUserID))
                unfollowUser();
            else
                followUser();
        }
        else {
            followUser();
        }

    }

    public void zoom_onClick(View view) {

        LocationJson loc = lastLocations.get(activeUserID);
        if(loc == null)
            return;

        LatLng latLng = new LatLng(Double.valueOf(loc.getLatitude()),
                Double.valueOf(loc.getLongitude()));

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
    }

    private void followUser() {
        isFollow = true;
        followUserID = activeUserID;
        follow.setText("Zakończ");
    }

    private void unfollowUser() {
        isFollow = false;
        followUserID = "";
        follow.setText("Śledź");
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
        checkTrack();

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
        mainMarker = mMap.addMarker(new MarkerOptions().position(cord).title("Twoja lokalizacja"));
        mainMarker.setTag(UserInfo.getUserID());
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

        activeUserID = (String) marker.getTag();

        if(activeUserID.equals(UserInfo.getUserID()))
            name.setText("Twoja lokalizacja");
        else
            name.setText(users.get(activeUserID).getUsername());

        setUserInfo(lastLocations.get(activeUserID));
        marker.showInfoWindow();
        return false;
    }

    private void setCurrentLocation(BackgroundLocation location) {
        if(mainMarker == null)
            return;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mainMarker.setPosition(latLng);

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0f));

        setMainUserLocation(location);
    }

    private void setChangeLocation(BackgroundLocation location) {
        if(mainMarker == null)
            return;
        //Log.e("", ""+ location.getLatitude());
        mainMarker.setPosition( new LatLng(location.getLatitude(), location.getLongitude()));

        if(activeUserID == UserInfo.getUserID())
            setUserInfo(location);

        setMainUserLocation(location);
    }

    private void setMainUserLocation(BackgroundLocation location) {
        if(activeUserID.equals(UserInfo.getUserID()))
            setUserInfo(location);

        LocationJson loc = null;
        if((loc = lastLocations.get(UserInfo.getUserID())) == null) {
            loc = new LocationJson();
        }

        loc.setUserID(UserInfo.getUserID());
        loc.setTrackID(UserInfo.getTrackID());
        loc.setLatitude(String.valueOf(location.getLatitude()));
        loc.setLongitude(String.valueOf(location.getLongitude()));
        loc.setAltitude( (float) location.getAltitude());
        loc.setAccuracy(location.getAccuracy());
        loc.setSpeed(location.getSpeed());

        lastLocations.put(UserInfo.getUserID(), loc);
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


    private ConcurrentHashMap<String, UserJson> users = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Marker> markers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Polyline> polylines = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, LocationJson> lastLocations = new ConcurrentHashMap<>();


    @Override
    public void onUsers(String usersJson) {
        users = new ConcurrentHashMap<>();
        polylines = new ConcurrentHashMap<>();

        Log.e("LOG", "get users: " + usersJson);

        try {
            JSONArray array = new JSONArray(usersJson);

            for(int i = 0;i<array.length();i++) {
                UserJson user = gson.fromJson(array.getJSONObject(i).toString(), UserJson.class);
                users.put(user.getID(), user);
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

                LocationJson location = gson.fromJson(payload, LocationJson.class);
                if(location == null)
                    return;

                if( users.get(location.getUserID()) == null)
                    return;

                UserJson user = users.get(location.getUserID());

                LatLng latLng = new LatLng(Double.valueOf(location.getLatitude()),
                        Double.valueOf(location.getLongitude()));

                lastLocations.put(user.getID(), location);

                if(markers.get(user.getID()) != null ) {
                    ThreadUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            markers.get(user.getID()).setPosition(latLng);

                            if(isFollow && followUserID.equals(user.getID()))
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, mMap.getCameraPosition().zoom));

                            if(activeUserID.equals(user.getID()))
                                setUserInfo(location);
                        }
                    });
                }
                else {
                    ThreadUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(user.getUsername()));
                            marker.setTag(user.getID());
                            marker.showInfoWindow();
                            markers.put(user.getID(), marker);

                        }
                    });

                }

                Log.e("LOG", "User: " + user.getUsername()
                        + " loc: " + location.getLatitude()
                        + " " + location.getLongitude());



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


    // TODO use interface (BackgroundLocation and LocationJson)
    private void setUserInfo(BackgroundLocation location) {
        if(!isInfoBarActive || location == null)
            return;

        speed.setText(String.valueOf(prepareSpeed(location.getSpeed())));
        altitude.setText(prepareAltitude(location.getAltitude()));
        accuracy.setText(prepareAccuracy(location.getAccuracy()));

        setMoreInfo();
    }

    private void setUserInfo(LocationJson location) {
        if(!isInfoBarActive || location == null)
            return;

        speed.setText(String.valueOf(prepareSpeed(location.getSpeed())));
        altitude.setText(prepareAltitude(location.getAltitude()));
        accuracy.setText(prepareAccuracy(location.getAccuracy()));

        setMoreInfo();
    }

    private int prepareSpeed(float speed) {
        float speedInKMPerH = speed * 3600 / 1000;
        return Math.round(speedInKMPerH);
    }

    private String prepareAltitude(double altitude) {
        return Math.round(altitude) + "m";
    }

    private String prepareAccuracy(float accuracy) {
        return Math.round(accuracy) + "m";
    }

    private void setMoreInfo() {
        if(followUserID.equals(activeUserID))
            follow.setText("Zakończ");
        else
            follow.setText("Śledź");
    }


    private void checkTrack() {
        if(UserInfo.getTrackID().isEmpty()) {
            trackInfo.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0));
        }
        else {
            trackName.setText("Trasa: " + UserInfo.getTrackName());
            trackInfo.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }


}

