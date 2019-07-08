package com.conceptualsystems.container.gps;

import android.content.Context;
import android.location.Geocoder;

import java.util.Locale;

public class GPSGeocoder  {
    protected final Geocoder mGeocoder;

    public GPSGeocoder(Context context) {
        this(context, Locale.getDefault());
    }

    public GPSGeocoder(Context context, Locale locale) {
        if(locale == null)
            mGeocoder = new Geocoder(context, Locale.getDefault());
        else
            mGeocoder = new Geocoder(context, locale);
    }

    public Geocoder getGeocoder() {
        return mGeocoder;
    }
}
