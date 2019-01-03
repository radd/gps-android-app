package com.example.woles.myapplication;

import android.content.Context;
import com.marianhello.bgloc.BackgroundGeolocationFacade;
import com.marianhello.bgloc.Config;
import com.marianhello.bgloc.PluginDelegate;
import com.marianhello.bgloc.PluginException;
import com.marianhello.bgloc.data.BackgroundLocation;
import com.marianhello.bgloc.data.HashMapLocationTemplate;

import java.util.HashMap;


public class GPSManager {


    private BackgroundGeolocationFacade facade;
    private IGPSManager igpsManager;

    public GPSManager(Context context, IGPSManager igpsManager, PluginDelegate pluginDelegate) {
        this.igpsManager = igpsManager;
        facade = new BackgroundGeolocationFacade(context, pluginDelegate);

        Config config = new Config();
        config.setActivitiesInterval(4000);
        config.setDebugging(true);
        config.setFastestInterval(4000);
        config.setInterval(4000);
        config.setStopOnStillActivity(false);
        config.setLocationProvider(1);
        config.setDesiredAccuracy(1);
        config.setStopOnTerminate(false);
        //config.setUrl("http://192.168.1.43:8080/gpsloc/add");
        //config.setUrl("http://192.168.1.43:3000/api/gps");

        HashMap attrs = new HashMap<String, String>();
        attrs.put("lat", "@latitude");
        attrs.put("lon", "@longitude");
        attrs.put("speed", "@speed");

        config.setTemplate(new HashMapLocationTemplate(attrs));

        try {
            facade.configure(config);
        } catch (PluginException e) {
            e.printStackTrace();
        }

    }


    public BackgroundGeolocationFacade getFacade() {
        return facade;
    }


    public void  getCurrentLocation() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int timeout = Integer.MAX_VALUE;
                long maximumAge = Long.MAX_VALUE;
                boolean enableHighAccuracy = true;

                try {
                    BackgroundLocation location = facade.getCurrentLocation(timeout, maximumAge, enableHighAccuracy);
                    igpsManager.onCurrentLocation(location);
                } catch (PluginException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void saveURL(String url) {
        Config config = new Config();
        config.setUrl(url);

        try {
            facade.configure(config);
        } catch (PluginException e) {
            e.printStackTrace();
        }
    }
}
