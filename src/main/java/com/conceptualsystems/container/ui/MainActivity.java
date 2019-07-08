package com.conceptualsystems.container.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.conceptualsystems.R;
import com.conceptualsystems.android.color.ColorLayout;
import com.conceptualsystems.android.database.DbSingleton;
import com.conceptualsystems.android.net.InboundMessageManagerAndroid;
import com.conceptualsystems.container.database.ContainerOpenHelper;
import com.conceptualsystems.container.database.DbSchemaContainer;
import com.conceptualsystems.container.gps.GPSManager;
import com.conceptualsystems.container.net.InboundMessageProcessorContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainActivity extends Activity {
    public static final int REQUEST_FINE_LOCATION_PERMISSION = 30;

    protected Logger mLogger = LoggerFactory.getLogger(this.getClass());
    protected LayoutInflater mInflater;
    private SharedPreferences mPrefs;

    public class ManifestAdapter extends CursorAdapter {

        public ManifestAdapter(Context context, Cursor cur) {
            super(context, cur, true);
        }

        public void bindView(View view, Context context, Cursor cursor) {
            try {
                //log all items in the list view
                mLogger.info("Label: " + cursor.getString(cursor.getColumnIndex(DbSchemaContainer.ManifestSchema.COLUMN_LABEL)));
                mLogger.info("Address:   " + cursor.getString(cursor.getColumnIndex(DbSchemaContainer.ManifestSchema.COLUMN_ADDRESS)));
                mLogger.info("Latitude:  " + cursor.getString(cursor.getColumnIndex(DbSchemaContainer.ManifestSchema.COLUMN_LATITUDE)));
                mLogger.info("Longitude: " + cursor.getString(cursor.getColumnIndex(DbSchemaContainer.ManifestSchema.COLUMN_LONGITUDE)));
            } catch(Exception e) {

            }

            try {
                String label = cursor.getString(cursor.getColumnIndex(DbSchemaContainer.ManifestSchema.COLUMN_LABEL));
                String address = cursor.getString(cursor.getColumnIndex(DbSchemaContainer.ManifestSchema.COLUMN_ADDRESS));
                Long timestamp = cursor.getLong(cursor.getColumnIndex(DbSchemaContainer.ManifestSchema.COLUMN_TIME));
                Integer type = cursor.getInt(cursor.getColumnIndex(DbSchemaContainer.ManifestSchema.COLUMN_TYPE));
                Double latitude = cursor.getDouble(cursor.getColumnIndex(DbSchemaContainer.ManifestSchema.COLUMN_LATITUDE));
                Double longitude = cursor.getDouble(cursor.getColumnIndex(DbSchemaContainer.ManifestSchema.COLUMN_LONGITUDE));
                if(label != null && !label.equals("")) {
                    ((TextView)view.findViewById(R.id.label)).setText(label);
                } else if(address != null && !address.equals("")) {
                    ((TextView)view.findViewById(R.id.label)).setText(address);
                } else {
                    ((TextView)view.findViewById(R.id.label)).setText("");
                }

                if(mPrefs.getInt("color_mode", ColorLayout.LAYOUT_BRIGHT) == ColorLayout.LAYOUT_BRIGHT) {
                    ((TextView)view.findViewById(R.id.label)).setTextColor(mPrefs.getInt("bright_label_color", Color.BLACK));
                    ((TextView)view.findViewById(R.id.time)).setTextColor(mPrefs.getInt("bright_label_color", Color.BLACK));
                    view.setBackgroundColor(mPrefs.getInt("bright_bg_color", Color.YELLOW));
                }
                if(mPrefs.getInt("color_mode", ColorLayout.LAYOUT_BRIGHT) == ColorLayout.LAYOUT_NORMAL) {
                    ((TextView)view.findViewById(R.id.label)).setTextColor(mPrefs.getInt("normal_label_color", Color.WHITE));
                    ((TextView)view.findViewById(R.id.time)).setTextColor(mPrefs.getInt("normal_label_color", Color.WHITE));
                    view.setBackgroundColor(mPrefs.getInt("normal_bg_color", Color.BLACK));
                }
            } catch(Exception e) {
                mLogger.warn("Error in adapter!", e);
            }
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.manifest_list_item, parent, false);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //setWifiIndicator();
                    GPSManager.getInstance(this);
                } else {
                    Toast.makeText(this, "You must enable location permissions!", Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION_PERMISSION);
                }
                return;
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        DbSingleton.getInstance().getHelper(this, new ContainerOpenHelper(this));
        InboundMessageManagerAndroid.getInstance(this).setProcessor(new InboundMessageProcessorContainer(this));
        GPSManager.getInstance(this);

        setContentView(R.layout.manifest);

        ManifestAdapter adapter = new ManifestAdapter(
                this,
                DbSingleton.getInstance().getDatabase(this).rawQuery(
                    "SELECT * FROM " + DbSchemaContainer.ManifestSchema.TABLE_NAME,
                        null
        ));

        ListView manifest = ((ListView)findViewById(R.id.manifest_list));
        manifest.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION_PERMISSION);
        }
    }
}
