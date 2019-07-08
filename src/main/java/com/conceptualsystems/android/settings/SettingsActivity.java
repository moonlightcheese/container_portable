package com.conceptualsystems.android.settings;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DoubleClickSpinner;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.conceptualsystems.R;
import com.conceptualsystems.android.Utility;
import com.conceptualsystems.android.color.ColorLayout;
import com.conceptualsystems.android.database.DbSchema;
import com.conceptualsystems.android.database.DbSingleton;
import com.conceptualsystems.android.dialog.DialogUtil;
import com.conceptualsystems.android.dropbox.DropboxClientFactory;
import com.conceptualsystems.android.dropbox.GetCurrentAccountTask;
import com.conceptualsystems.android.net.OutboundMessageManagerAndroid;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.users.FullAccount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A rewrite of the settings so that we don't depend on xml preference files.
 *
 * Created by moonl on 4/6/2018.
 */

public class SettingsActivity extends Activity {
    public static final String GENERIC_DROPBOX_LOGGED_IN_USERNAME = "<LOGGED IN>";

    DoubleClickSpinner mSiteSpinner = null;
    Button mNewSiteButton = null;
    Button mDeleteSiteButton = null;
    ListView mSettingsListView = null;

    SimpleAdapter mSettingsAdapter = null;
    SimpleCursorAdapter mSiteAdapter = null;

    SharedPreferences mPrefs = null;

    DialogUtil mDialogUtility = null;
    String mAccountName = "";

    public static final String[] SETTINGS_ITEMS = {
            "Site Label",
            "Server Address",
            "Server Port",
            "Activation Code",
            "Image Server Address",
            "Image Server Port",
            "Connection Timeout (milliseconds)",
            "Message Interval (milliseconds)",
            "Dropbox Account",
            "Dropbox Folder",
            "Listening Port"
    };

    protected Handler mNewSiteHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String input = msg.getData().getString("input");

            if(siteExists(input)) {
                mDialogUtility.createMessageDialog("A site with that name already exists!").show();
            } else {
                initSite(input);

                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putString(Utility.PREF_SITE, input);
                editor.commit();

                updateView();
            }
        }
    };

    protected Handler mEntryHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //get original data from Cursor
            Cursor siteCursor = getSiteCursor();
            setCursorToPreferenceValue(siteCursor);

            final String id = siteCursor.getString(siteCursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_LABEL));

            //get data from message
            String input = msg.getData().getString("input");
            String title = msg.getData().getString("title");

            //set content values to update in database.
            //it's a bit cheap to check the title of the dialog to determine this, but oh well.  less code!
            ContentValues cv = new ContentValues();
            if(title.equals(SETTINGS_ITEMS[0]))
                cv.put(DbSchema.SiteSchema.COLUMN_LABEL, input);
            if(title.equals(SETTINGS_ITEMS[1]))
                cv.put(DbSchema.SiteSchema.COLUMN_IP, input);
            if(title.equals(SETTINGS_ITEMS[2]))
                cv.put(DbSchema.SiteSchema.COLUMN_PORT, input);
            if(title.equals(SETTINGS_ITEMS[3]))
                cv.put(DbSchema.SiteSchema.COLUMN_ACTIVATION_CODE, input);
            if(title.equals(SETTINGS_ITEMS[4]))
                cv.put(DbSchema.SiteSchema.COLUMN_IMAGE_SERVER_IP, input);
            if(title.equals(SETTINGS_ITEMS[5]))
                cv.put(DbSchema.SiteSchema.COLUMN_IMAGE_SERVER_PORT, input);
            if(title.equals(SETTINGS_ITEMS[6]))
                cv.put(DbSchema.SiteSchema.COLUMN_TIMEOUT, Long.valueOf(input));
            if(title.equals(SETTINGS_ITEMS[7]))
                cv.put(DbSchema.SiteSchema.COLUMN_MESSAGE_INTERVAL, Long.valueOf(input));
            if(title.equals(SETTINGS_ITEMS[8]))
                cv.put(DbSchema.SiteSchema.COLUMN_DROPBOX_OAUTH, input);
            if(title.equals(SETTINGS_ITEMS[9]))
                cv.put(DbSchema.SiteSchema.COLUMN_DROPBOX_FOLDER, input);
            if(title.equals(SETTINGS_ITEMS[10]))
                cv.put(DbSchema.SiteSchema.COLUMN_PORT_LISTEN, input);

            //change label in database
            int returnValue = DbSingleton.getInstance().getDatabase(SettingsActivity.this).update(
                    DbSchema.SiteSchema.TABLE_NAME,
                    cv,
                    DbSchema.SiteSchema.COLUMN_LABEL + "=?",
                    new String[] { id }
            );

            //if we are changing the label, we need to also update the PREF_SITE value to the new label.
            if(title.equals(SETTINGS_ITEMS[0])) {
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putString(Utility.PREF_SITE, input);
                editor.commit();
            }

            //update screen
            updateView();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings);
        mSettingsListView = (ListView)findViewById(R.id.settings_list);
        mNewSiteButton = (Button)findViewById(R.id.new_site_btn);
        mSiteSpinner = (DoubleClickSpinner)findViewById(R.id.site_selection);
        mDeleteSiteButton = (Button)findViewById(R.id.delete_site_btn);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mDialogUtility = new DialogUtil(this);

        setScreen();
    }

    protected void setScreen() {
        Cursor siteCursor = getSiteCursor();

        if(siteCursor.getCount() < 1) {
            //no sites have been setup yet or one is not selected.
            //setup only the top item so that a site can be created.
            initSite("Default");

            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString(Utility.PREF_SITE, "Default");
            editor.commit();

            //requery for the new data.
            siteCursor = getSiteCursor();
        }

        setCursorToPreferenceValue(siteCursor);

        //save the original selection.
        int siteSelection = siteCursor.getPosition();

        mSiteAdapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_spinner_item,
                siteCursor,
                new String[] {
                        DbSchema.SiteSchema.COLUMN_LABEL
                },
                new int[] {
                        android.R.id.text1
                }
        );

        mSiteSpinner.setAdapter(mSiteAdapter);
        mSiteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSiteSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long itemId) {
                Cursor tempCursor = (Cursor) adapterView.getItemAtPosition(position);
                String site = tempCursor.getString(tempCursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_LABEL));

                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putString(Utility.PREF_SITE, site);
                editor.commit();

                updateView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mSiteSpinner.setSelection(siteSelection);

        mNewSiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDialogUtility.createTextEntryDialog(
                        true,
                        "",
                        "New Site",
                        new InputFilter[] {Utility.FILTER_ALPHA_NUM_CAPS},
                        mNewSiteHandler
                ).show();
            }
        });

        mDeleteSiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialogUtility.createYesNoDialog(
                        "Delete Site",
                        "Are you sure you want to delete this site?",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //delete the site
                                Cursor cursor = getSiteCursor();
                                setCursorToPreferenceValue(cursor);
                                deleteSite(cursor.getString(cursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_LABEL)));
                                if(getSiteCursor().getCount() <= 1) {
                                    initSite("Default");
                                }
                                updateView();
                            }
                        },
                        null
                ).show();
            }
        });
    }

    private int deleteSite(String siteLabel) {
        return DbSingleton.getInstance().getDatabase(this).delete(
                DbSchema.SiteSchema.TABLE_NAME,
                DbSchema.SiteSchema.COLUMN_LABEL + "=?",
                new String[]{siteLabel});
    }

    private void updateView() {
        Cursor siteCursor = getSiteCursor();
        setCursorToPreferenceValue(siteCursor);

        mSiteAdapter.getCursor().requery();

        mSettingsAdapter = new SimpleAdapter(
                this,
                generateSettingsList(siteCursor),
                android.R.layout.simple_list_item_2,
                new String[] {
                        "line1",
                        "line2"
                },
                new int[] {
                        android.R.id.text1,
                        android.R.id.text2
                }
        );

        mSettingsAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                //Log.v(SettingsActivity.this.getClass().getSimpleName(), "String: " + (String)data);
                try {
                    int resID = view.getId();
                    if(resID == -1) {
                        //Log.v(SettingsActivity.this.getClass().getSimpleName(), "resID was '" + resID + "', so there was no resource ID");
                    } else if(resID == android.R.id.text1) {
                        //Log.v(SettingsActivity.this.getClass().getSimpleName(), "resID was '" + resID + "', so text1");
                    } else if(resID == android.R.id.text2) {
                        //Log.v(SettingsActivity.this.getClass().getSimpleName(), "resID was '" + resID + "', so text2");
                    } else {
                        //Log.v(SettingsActivity.this.getClass().getSimpleName(), "resID was '" + resID + "', UNKNOWN");
                    }
                    TextView label = ((TextView)view);
                    label.setText((String)data);
//                    if(label == null) {
//                        label = ((TextView)view.findViewById(android.R.id.text2));
//                    }
                    if(mPrefs.getInt("color_mode", ColorLayout.LAYOUT_BRIGHT) == ColorLayout.LAYOUT_BRIGHT) {
                        label.setTextColor(mPrefs.getInt("bright_label_color", Color.BLACK));
                        //view.setBackgroundColor(mPrefs.getInt("bright_bg_color", Color.YELLOW));
                    }
                    if(mPrefs.getInt("color_mode", ColorLayout.LAYOUT_BRIGHT) == ColorLayout.LAYOUT_NORMAL) {
                        label.setTextColor(mPrefs.getInt("normal_label_color", Color.WHITE));
                        //view.setBackgroundColor(mPrefs.getInt("normal_bg_color", Color.BLACK));
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    return false;
                }

                return true;
            }
        });

        mSettingsListView.setAdapter(mSettingsAdapter);

        final String site = siteCursor.getString(siteCursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_LABEL));
        final String ip = siteCursor.getString(siteCursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_IP));
        final String port = siteCursor.getString(siteCursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_PORT));
        final String port_in = siteCursor.getString(siteCursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_PORT_LISTEN));
        final String activation = siteCursor.getString(siteCursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_ACTIVATION_CODE));
        final String img_ip = siteCursor.getString(siteCursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_IMAGE_SERVER_IP));
        final String img_port = siteCursor.getString(siteCursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_IMAGE_SERVER_PORT));
        final String timeout = siteCursor.getString(siteCursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_TIMEOUT));
        final String interval = siteCursor.getString(siteCursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_MESSAGE_INTERVAL));
        final String dropbox_oauth = siteCursor.getString(siteCursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_DROPBOX_OAUTH));
        final String dropbox_folder = siteCursor.getString(siteCursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_DROPBOX_FOLDER));

        mSettingsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                switch(i) {
                    case 0: {    //change label
                        mDialogUtility.createTextEntryDialog(
                                true,
                                site,
                                SETTINGS_ITEMS[0],
                                new InputFilter[] {Utility.FILTER_ALPHA_NUM_CAPS},
                                mEntryHandler
                        ).show();
                        break;
                    }
                    case 1: {    //change server ip address
                        mDialogUtility.createTextEntryDialog(
                                true,
                                ip,
                                SETTINGS_ITEMS[1],
                                new InputFilter[] {Utility.FILTER_IP_ADDRESS},
                                mEntryHandler
                        ).show();
                        break;
                    }
                    case 2: {    //change server port
                        mDialogUtility.createTextEntryDialog(
                                true,
                                port,
                                SETTINGS_ITEMS[2],
                                new InputFilter[] {Utility.FILTER_NUM},
                                mEntryHandler
                        ).show();
                        break;
                    }
                    case 3: {    //change activation code
                        mDialogUtility.createTextEntryDialog(
                                true,
                                activation,
                                SETTINGS_ITEMS[3],
                                new InputFilter[] {Utility.FILTER_ALPHA_NUM_CAPS},
                                mEntryHandler
                        ).show();
                        break;
                    }
                    case 4: {    //change image server ip address
                        mDialogUtility.createTextEntryDialog(
                                true,
                                img_ip,
                                SETTINGS_ITEMS[4],
                                new InputFilter[] {Utility.FILTER_IP_ADDRESS},
                                mEntryHandler
                        ).show();
                        break;
                    }
                    case 5: {    //change image server port
                        mDialogUtility.createTextEntryDialog(
                                true,
                                img_port,
                                SETTINGS_ITEMS[5],
                                new InputFilter[] {Utility.FILTER_NUM},
                                mEntryHandler
                        ).show();
                        break;
                    }
                    case 6: {
                        mDialogUtility.createTextEntryDialog(
                                true,
                                timeout,
                                SETTINGS_ITEMS[6],
                                new InputFilter[] {Utility.FILTER_NUM},
                                mEntryHandler
                        ).show();
                        break;
                    }
                    case 7: {
                        mDialogUtility.createTextEntryDialog(
                                true,
                                interval,
                                SETTINGS_ITEMS[7],
                                new InputFilter[] {Utility.FILTER_NUM},
                                mEntryHandler
                        ).show();
                        break;
                    }
                    case 8: {
                        /**
                         * TODO: create the dialog which allows the user to unset the current
                         * account oauth and setup.  If an account is not setup then we do not
                         * create a dialog, but go straight to the OAuth page which asks for
                         * the OAuth authorization.  If one is setup, we popup a dialog which asks
                         * if they want to unset the current account.
                         */
                        final SharedPreferences preferences = SettingsActivity.this.getSharedPreferences("dropbox-sample", Context.MODE_PRIVATE);
                        String accessToken = null;
                        if(preferences != null) {
                            Log.i(SettingsActivity.class.getSimpleName(), "Found preferences 'dropbox-sample'.");
                            accessToken = preferences.getString("access-token", null);
                        } else {
                            Auth.startOAuth2Authentication(SettingsActivity.this, getString(R.string.dropbox_app_key));
                            Log.i(SettingsActivity.class.getSimpleName(), "Did not find preferences 'dropbox-sample'.  No such SharedPreferences.");
                        }

                        if(accessToken != null) {
                            //we have an access token.  open a dialog which gives the option to unset.
                            mDialogUtility.createMessageDialog(
                                    SETTINGS_ITEMS[8],
                                    "Would you like to remove the current Dropbox account?",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if(preferences != null) {
                                                SharedPreferences.Editor edit = preferences.edit();
                                                edit.remove("access-token");
                                                edit.commit();
                                            } else {
                                                Log.w(SettingsActivity.class.getSimpleName(), "Cannot remove token.  Preference object was NULL!");
                                            }
                                            dialog.dismiss();
                                        }
                                    }).show();
                        } else {
                            Auth.startOAuth2Authentication(SettingsActivity.this, getString(R.string.dropbox_app_key));
                            Log.i(SettingsActivity.class.getSimpleName(), "Did not find 'access-token' in SharedPreferences 'dropbox-sample'.");
                        }
                        break;
                    }
                    case 9: {
                        mDialogUtility.createTextEntryDialog(
                                true,
                                dropbox_folder,
                                SETTINGS_ITEMS[9],
                                new InputFilter[] {Utility.FILTER_ALPHA_NUM_CAPS},
                                mEntryHandler
                        ).show();
                        break;
                    }
                    case 10: {
                        mDialogUtility.createTextEntryDialog(
                                true,
                                port_in,
                                SETTINGS_ITEMS[10],
                                new InputFilter[] {Utility.FILTER_ALPHA_NUM_CAPS},
                                mEntryHandler
                        ).show();
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        });
    }

    public void onResume() {
        super.onResume();
        if(mPrefs.getInt("color_mode", ColorLayout.LAYOUT_BRIGHT) == ColorLayout.LAYOUT_BRIGHT) {
            //mSiteSpinner.setBackgroundColor(mPrefs.getInt("bright_bg_color", Color.BLACK));
            //mSettingsListView.setBackgroundColor(mPrefs.getInt("bright_bg_color", Color.BLACK));
            //mNewSiteButton.setBackgroundColor(mPrefs.getInt("bright_bg_color", Color.BLACK));
            findViewById(R.id.root_view).setBackgroundColor(mPrefs.getInt("bright_bg_color", Color.YELLOW));
        }
        if(mPrefs.getInt("color_mode", ColorLayout.LAYOUT_BRIGHT) == ColorLayout.LAYOUT_NORMAL) {
            //mSiteSpinner.setBackgroundColor(mPrefs.getInt("normal_label_color", Color.WHITE));
            //mSettingsListView.setBackgroundColor(mPrefs.getInt("normal_label_color", Color.WHITE));
            //mNewSiteButton.setBackgroundColor(mPrefs.getInt("normal_label_color", Color.WHITE));
            findViewById(R.id.root_view).setBackgroundColor(mPrefs.getInt("normal_bg_color", Color.BLACK));
        }

        //save the access token, if we received one.
        String accessToken = Auth.getOAuth2Token();
        Cursor siteCursor = getSiteCursor();
        setCursorToPreferenceValue(siteCursor);
        if(accessToken != null) {
            Log.i(SettingsActivity.class.getSimpleName(), "onResume > Found access token in Auth:" + accessToken);

            final String id = siteCursor.getString(siteCursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_LABEL));

            ContentValues cv = new ContentValues();
            cv.put(DbSchema.SiteSchema.COLUMN_DROPBOX_OAUTH, accessToken);

            int returnValue = DbSingleton.getInstance().getDatabase(SettingsActivity.this).update(
                    DbSchema.SiteSchema.TABLE_NAME,
                    cv,
                    DbSchema.SiteSchema.COLUMN_LABEL + "=?",
                    new String[] { id }
            );

            DropboxClientFactory.init(accessToken);
            mAccountName = GENERIC_DROPBOX_LOGGED_IN_USERNAME;   //setting so it's not blank while we look up the account name from Dropbox
            updateView();
            getAccountInfoTask();
        } else {
            Log.v(SettingsActivity.class.getSimpleName(), "onResume > Did not find access token in Auth.");

            //maybe we already have an access token for this site?
            accessToken = siteCursor.getString(siteCursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_DROPBOX_OAUTH));

            if(accessToken != null && !accessToken.equals("")) {
                DropboxClientFactory.init(accessToken);
                mAccountName = GENERIC_DROPBOX_LOGGED_IN_USERNAME;   //setting so it's not blank while we look up the account name from Dropbox
                updateView();
                getAccountInfoTask();
            } else {
                Log.v(SettingsActivity.class.getSimpleName(), "onResume > Did not find access token in database.");
            }
        }
    }

    public Cursor getSiteCursor() {
        return DbSingleton.getInstance().getDatabase(this).query(
                DbSchema.SiteSchema.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    /**
     * creates an initial site
     * @param label
     */
    public void initSite(String label) {
        ContentValues cv = new ContentValues();
        cv.put(DbSchema.SiteSchema.COLUMN_LABEL, label);
        cv.put(DbSchema.SiteSchema.COLUMN_IP, "192.168.1.40");
        cv.put(DbSchema.SiteSchema.COLUMN_IMAGE_SERVER_IP, "192.168.1.40");
        cv.put(DbSchema.SiteSchema.COLUMN_PORT, "3005");
        cv.put(DbSchema.SiteSchema.COLUMN_IMAGE_SERVER_PORT, "3007");
        cv.put(DbSchema.SiteSchema.COLUMN_TIMEOUT, OutboundMessageManagerAndroid.DEFAULT_TIMEOUT);
        cv.put(DbSchema.SiteSchema.COLUMN_MESSAGE_INTERVAL, 200);
        cv.putNull(DbSchema.SiteSchema.COLUMN_DROPBOX_OAUTH);
        cv.put(DbSchema.SiteSchema.COLUMN_DROPBOX_FOLDER, "/_SMSX/SMSMobile/");
        cv.put(DbSchema.SiteSchema.COLUMN_PORT_LISTEN, "3006");
        DbSingleton.getInstance().getDatabase(this).insert(DbSchema.SiteSchema.TABLE_NAME, null, cv);
    }

    protected void setCursorToPreferenceValue(Cursor cursor) {
        //get the information for the selected site.
        if(cursor.getCount() < 1)
            return;

        cursor.moveToFirst();

        do {
            String curSite = cursor.getString(cursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_LABEL));
            if(curSite.equals(mPrefs.getString(Utility.PREF_SITE, null))) {
                break;
            }
        } while(cursor.moveToNext());
    }

    protected List<? extends Map<String, ?>> generateSettingsList(Cursor cursor) {
        List<HashMap<String, String>> settingsList = new ArrayList<HashMap<String, String>>();

        //get each setting from the Cursor which currently should point to the active site.
        String site = cursor.getString(cursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_LABEL));
        String ip = cursor.getString(cursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_IP));
        String port = cursor.getString(cursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_PORT));
        String port_in = cursor.getString(cursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_PORT_LISTEN));
        String activation = cursor.getString(cursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_ACTIVATION_CODE));
        String img_ip = cursor.getString(cursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_IMAGE_SERVER_IP));
        String img_port = cursor.getString(cursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_IMAGE_SERVER_PORT));
        String timeout = cursor.getString(cursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_TIMEOUT));
        String interval = cursor.getString(cursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_MESSAGE_INTERVAL));
        String dropboxFolder = cursor.getString(cursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_DROPBOX_FOLDER));

        HashMap<String, String> siteMap = new HashMap<String, String>();
        siteMap.put("line1", SETTINGS_ITEMS[0]);
        siteMap.put("line2", site);
        settingsList.add(siteMap);

        HashMap<String, String> ipMap = new HashMap<String, String>();
        ipMap.put("line1", SETTINGS_ITEMS[1]);
        ipMap.put("line2", ip);
        settingsList.add(ipMap);

        HashMap<String, String> portMap = new HashMap<String, String>();
        portMap.put("line1", SETTINGS_ITEMS[2]);
        portMap.put("line2", port);
        settingsList.add(portMap);

        HashMap<String, String> activationMap = new HashMap<String, String>();
        activationMap.put("line1", SETTINGS_ITEMS[3]);
        activationMap.put("line2", activation);
        settingsList.add(activationMap);

        HashMap<String, String> imgIpMap = new HashMap<String, String>();
        imgIpMap.put("line1", SETTINGS_ITEMS[4]);
        imgIpMap.put("line2", img_ip);
        settingsList.add(imgIpMap);

        HashMap<String, String> imgPortMap = new HashMap<String, String>();
        imgPortMap.put("line1", SETTINGS_ITEMS[5]);
        imgPortMap.put("line2", img_port);
        settingsList.add(imgPortMap);

        HashMap<String, String> timeoutMap = new HashMap<String, String>();
        timeoutMap.put("line1", SETTINGS_ITEMS[6]);
        timeoutMap.put("line2", timeout);
        settingsList.add(timeoutMap);

        HashMap<String, String> intervalMap = new HashMap<String, String>();
        intervalMap.put("line1", SETTINGS_ITEMS[7]);
        intervalMap.put("line2", interval);
        settingsList.add(intervalMap);

        HashMap<String, String> dropBoxAccountMap = new HashMap<>();
        dropBoxAccountMap.put("line1", SETTINGS_ITEMS[8]);
        dropBoxAccountMap.put("line2", mAccountName);
        settingsList.add(dropBoxAccountMap);

        HashMap<String, String> dropBoxFolderMap = new HashMap<>();
        dropBoxFolderMap.put("line1", SETTINGS_ITEMS[9]);
        dropBoxFolderMap.put("line2", dropboxFolder);
        settingsList.add(dropBoxFolderMap);

        HashMap<String, String> portInMap = new HashMap<>();
        portInMap.put("line1", SETTINGS_ITEMS[10]);
        portInMap.put("line2", port_in);
        settingsList.add(portInMap);

        return settingsList;
    }

    protected boolean siteExists(String siteID) {
        return siteExists(siteID, getSiteCursor());
    }

    protected boolean siteExists(String siteID, Cursor siteCursor) {
        if(siteID == null || siteID.equals("") || siteID.equals("<NONE>")) {
            return false;
        } else {
            siteCursor.requery();
            if(siteCursor.getCount()<1) {
                return false;
            } else {
                siteCursor.moveToFirst();
                do {
                    String curSite = siteCursor.getString(siteCursor.getColumnIndex(DbSchema.SiteSchema.COLUMN_LABEL));
                    if(curSite.equals(siteID)) {
                        return true;
                    }
                } while(siteCursor.moveToNext());
                return false;
            }
        }
    }

    protected void getAccountInfoTask() {
        new GetCurrentAccountTask(
                DropboxClientFactory.getClient(),
                new GetCurrentAccountTask.Callback() {
                    @Override
                    public void onComplete(FullAccount result) {
                        mAccountName = result.getName().getDisplayName();
                        updateView();
                        //((TextView) findViewById(R.id.email_text)).setText(result.getEmail());
                        //((TextView) findViewById(R.id.name_text)).setText(result.getName().getDisplayName());
                        //((TextView) findViewById(R.id.type_text)).setText(result.getAccountType().name());
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(getClass().getName(), "Failed to get account details.", e);
                    }
                }).execute();
    }
}
