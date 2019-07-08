package com.conceptualsystems.container.gps;

import android.content.Context;

public class GPSManager {
    protected GPSThread mGPSThread;

    private static GPSManager INSTANCE;

    private GPSManager(Context context) {
        mGPSThread = new GPSThread(context);
        mGPSThread.start();
    }

    public static GPSManager getInstance(Context context) {
        if(INSTANCE == null) {
            INSTANCE = new GPSManager(context);
        }

        return INSTANCE;
    }
}
