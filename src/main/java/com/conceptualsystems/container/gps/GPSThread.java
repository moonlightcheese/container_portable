package com.conceptualsystems.container.gps;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Ask Android for GPS permissions before instantiating this class.  In Android, this thread
 * simply registers this class for location updates and then completes.  There is no loop for the
 * thread, and the locations are received in the location callbacks.
 */
public class GPSThread extends Thread implements LocationListener {
    LocationManager mLocationManager;
    Context mContext;
    Logger mLogger = LoggerFactory.getLogger(this.getClass());
    Long mLastGPSTimestamp;

    public GPSThread(Context context) {
        mContext = context;
        mLocationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void run() {
        Looper.prepare();

        if(mLocationManager == null) {
            mLogger.warn("Could not start thread [" + this.getClass().getSimpleName() + "] because LocationManager instance was NULL!");
            return;
        } else {
            try {
                mLogger.info("Registering thread [" + this.getClass().getSimpleName() + "] for location updates...");
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this, Looper.getMainLooper());
                mLogger.info("Thread [" + this.getClass().getSimpleName() + "] now listening for location updates!");
            } catch(SecurityException se) {
                mLogger.warn("Failed to register thread [" + this.getClass().getSimpleName() + "] for location updates!", se);
            } catch(Exception e) {
                mLogger.warn("Failed to register thread [" + this.getClass().getSimpleName() + "] for location updates!", e);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        //regardless of what's in the location object, since this callback was triggered, we set the GPS timestamp to now.
        mLogger.info("Setting GPS update timestamp: " + (new Date().toString()));
        mLastGPSTimestamp = System.currentTimeMillis();

        if(location == null) {
            mLogger.warn("onLocationChanged called but location was null!");
            return;
        } else {
            Bundle locationExtras = location.getExtras();

            if(locationExtras != null) {
                if(locationExtras.containsKey("satellites")) {
                    int satellites = locationExtras.getInt("satellites", 1000000);
                    mLogger.info("Number of satellites which gave us this GPS fix: " + satellites);
                } else {
                    mLogger.info("No satellite info in the extras for this GPS fix!");
                }
            } else {
                mLogger.info("Location extras not available for this GPS fix!");
                return;
            }

            //get info
            try {
                mLogger.info("Provider: " + location.getProvider());
                mLogger.info("Speed: " + location.getSpeed());
                mLogger.info("Accuracy: " + location.getAccuracy());
                mLogger.info("Altitude: " + location.getAltitude());
                mLogger.info("Bearing: " + location.getBearing());
                mLogger.info("Latitude: " + location.getLatitude());
                mLogger.info("Longitude: " + location.getLongitude());
            } catch(Exception e) {
                //just in case some data retrieval goes wrong, let's not crash the program.
                mLogger.warn("There was a problem getting GPS stats!", e);
            }

            if(location.getAccuracy() > 15d) {
                mLogger.info("GPS Accuracy radius is greater than 15 meters!  Discarding this GPS...");
                return;
            }

            /////////  DEBUGGING  //////////

        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
