package com.conceptualsystems.android.settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.conceptualsystems.R;
import com.conceptualsystems.android.color.ColorLayout;
import com.conceptualsystems.android.dialog.ColorPickerDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ColorActivity extends Activity
{
	//other members
	LayoutInflater mInflater;
	ListView mRootView;
	ListItemAdapter mAdapter;
	SharedPreferences prefs;
	ArrayList<Map<String, String>> mList;
	
	//CONSTANTS
	public final static int DIALOG_COLOR = 1;
	
	private class ListItemAdapter extends ArrayAdapter<Map<String, String>> implements ListAdapter {
		private ArrayList<Map<String, String>> items;
		private Context mContext;
		private Integer mMode;
		
		public ListItemAdapter(Context ctx, int layoutResourceId, ArrayList<Map<String, String>> items, int mode) {
			//init array list
			super(ctx, layoutResourceId, items);
			this.mContext = ctx;
			this.mMode = mode;
			this.items = items;
		}
		
		public void setMode(int mode) {
			this.mMode=mode;
		}
		
		public int getMode() {
			return this.mMode;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView == null) {
				convertView = mInflater.inflate(R.layout.color_pick_list_item, null);
			}
			//define list view items View here
			try {	//alleviates index out of bounds errors
				String label = items.get(position).get("label");
				String color = items.get(position).get("color");
				
				((TextView)convertView.findViewById(R.id.field_name)).setText(label);
				if(color!=null && !color.equals("")) {
					((TextView)convertView.findViewById(R.id.hex_color)).setText("#"+Integer.toHexString(new Integer(color)).toUpperCase());
					((ImageView)convertView.findViewById(R.id.swatch)).setBackgroundColor(new Integer(color));
				} else {
					((TextView)convertView.findViewById(R.id.hex_color)).setText("#00000000");
					((ImageView)convertView.findViewById(R.id.swatch)).setBackgroundColor(Color.BLACK);
				}
				
			} catch(Exception e) {
				Log.d("nfe", e.getMessage());
			}
			
			return convertView;
		}
	}

	public static ArrayList<Map<String, String>> initList(Context ctx) {
		ArrayList<Map<String, String>> list = new ArrayList<Map<String, String>>();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		//label_color = position(0)
		list.add(new HashMap<String, String>());
		list.get(0).put("label", "Labels");
		if(prefs.getInt("color_mode", ColorLayout.LAYOUT_BRIGHT) == ColorLayout.LAYOUT_BRIGHT) {//bright mode
			//list.get(0).put("hex", Integer.toHexString(prefs.getInt("bright_label_color", Color.BLACK)).toUpperCase());
			list.get(0).put("color", Integer.toString(prefs.getInt("bright_label_color", Color.BLACK)));
		}
		if(prefs.getInt("color_mode", ColorLayout.LAYOUT_BRIGHT) == ColorLayout.LAYOUT_NORMAL) {//normal mode
			//list.get(0).put("hex", Integer.toHexString(prefs.getInt("normal_label_color", Color.WHITE)).toUpperCase());
			list.get(0).put("color", Integer.toString(prefs.getInt("normal_label_color", Color.WHITE)));
		}
		//bg_color = position(1)
		list.add(new HashMap<String, String>());
		list.get(1).put("label", "Background");
		if(prefs.getInt("color_mode", ColorLayout.LAYOUT_BRIGHT) == ColorLayout.LAYOUT_BRIGHT) {//bright mode
			//list.get(1).put("hex", Integer.toHexString(prefs.getInt("bright_bg_color", Color.YELLOW)).toUpperCase());
			list.get(1).put("color", Integer.toString(prefs.getInt("bright_bg_color", Color.YELLOW)));
		}
		if(prefs.getInt("color_mode", ColorLayout.LAYOUT_BRIGHT) == ColorLayout.LAYOUT_NORMAL) {//normal mode
			//list.get(1).put("hex", Integer.toHexString(prefs.getInt("normal_bg_color", Color.BLACK)).toUpperCase());
			list.get(1).put("color", Integer.toString(prefs.getInt("normal_bg_color", Color.BLACK)));
		}
		//highlight color = position(2)
		list.add(new HashMap<String, String>());
		list.get(2).put("label", "Highlight");
		if(prefs.getInt("color_mode", ColorLayout.LAYOUT_BRIGHT) == ColorLayout.LAYOUT_BRIGHT) {//bright mode
			list.get(2).put("color", Integer.toString(prefs.getInt("bright_highlight_color", Color.RED)));
		}
		if(prefs.getInt("color_mode", ColorLayout.LAYOUT_BRIGHT) == ColorLayout.LAYOUT_NORMAL) {//normal mode
			list.get(2).put("color", Integer.toString(prefs.getInt("normal_highlight_color", Color.RED)));
		}
		return list;
	}
	
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		mInflater = (LayoutInflater) this.getSystemService(this.LAYOUT_INFLATER_SERVICE);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mRootView = new ListView(this);
		LinearLayout mainLayout = new LinearLayout(this);
		ViewGroup.LayoutParams wc = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ViewGroup.LayoutParams mp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		mainLayout.setLayoutParams(mp);
		mainLayout.setOrientation(LinearLayout.VERTICAL);
		TextView modeIndication = new TextView(this);
		if(prefs.getInt("color_mode", ColorLayout.LAYOUT_BRIGHT) == ColorLayout.LAYOUT_BRIGHT) {
			//modeIndication.setTextColor(prefs.getInt("bright_label_color", Color.BLACK));
			modeIndication.setText("BRIGHT MODE SETTINGS");
		}
		if(prefs.getInt("color_mode", ColorLayout.LAYOUT_BRIGHT) == ColorLayout.LAYOUT_NORMAL) {
			//modeIndication.setTextColor(prefs.getInt("normal_label_color", Color.WHITE));  //this is an error, dont' do this
			modeIndication.setText("NORMAL MODE SETTINGS");
		}
		modeIndication.setTextSize(24);
		modeIndication.setGravity(Gravity.CENTER_HORIZONTAL);
		mainLayout.addView(modeIndication, wc);
		mainLayout.addView(mRootView, mp);
		setContentView(mainLayout);
		mList = initList(this);
		mAdapter = new ListItemAdapter(
			this,
			R.layout.color_pick_list_item,
			mList,
			prefs.getInt("color_mode", ColorLayout.LAYOUT_BRIGHT)
		);
		mRootView.setAdapter(mAdapter);
		mRootView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
					Bundle b = new Bundle();
					b.putInt("resourceId", R.id.swatch);
					b.putInt("position", position);
					int colorMode = prefs.getInt("color_mode", ColorLayout.LAYOUT_BRIGHT);
					b.putInt("layoutId", colorMode);
					if(colorMode == ColorLayout.LAYOUT_BRIGHT) {
						if(position==0) { //label
							b.putInt("color", prefs.getInt("bright_label_color", Color.BLACK));
						}
						if(position==1) {
							b.putInt("color", prefs.getInt("bright_bg_color", Color.YELLOW));
						}
						if(position==2) {
							b.putInt("color", prefs.getInt("bright_highlight_color", Color.RED));
						}
					}
					if(colorMode == ColorLayout.LAYOUT_NORMAL) {
						if(position==0) { //label
							b.putInt("color", prefs.getInt("normal_label_color", Color.WHITE));
						}
						if(position==1) {
							b.putInt("color", prefs.getInt("normal_bg_color", Color.BLACK));
						}
						if(position==2) {
							b.putInt("color", prefs.getInt("normal_highlight_color", Color.RED));
						}
					}
					removeDialog(DIALOG_COLOR);
					showDialog(DIALOG_COLOR, b);
					
				}
			});
	}
	
	public Dialog createColorChangeDialog(Bundle b, final View colorPreviewView) {
        if(b!=null && b.containsKey("resourceId") && b.containsKey("color") && b.containsKey("layoutId") && b.containsKey("position")) {
            final int resource = b.getInt("resourceId", 0);		//the View resource affected by this color change (always R.id.swatch for ListView implementation)
            final int oldColor = b.getInt("color", 0);			//the old color
            final int layoutId = b.getInt("layoutId", 0);		//the layout type "LAYOUT_BRIGHT" or "LAYOUT_NORMAL"
            final int position = b.getInt("position", 0);

            final SharedPreferences.Editor edit = prefs.edit();
            ColorPickerDialog dialog = new ColorPickerDialog(this, new ColorPickerDialog.OnColorChangedListener() {
                public void colorChanged(int a, int r, int g, int b) {
                    switch(layoutId) {
                        case ColorLayout.LAYOUT_BRIGHT:
                        {
                            (colorPreviewView.findViewById(resource)).setBackgroundColor(Color.argb(a, r, g, b)); //R.id.swatch update color
                            if(position==0) { //label=0
                                edit.putInt("bright_label_color", Color.argb(a,r,g,b));
                                edit.commit();
                            }
                            if(position==1) { //bg=1
                                edit.putInt("bright_bg_color", Color.argb(a,r,g,b));
                                edit.commit();
                            }
                            if(position==2) {
                                edit.putInt("bright_highlight_color", Color.argb(a,r,g,b));
                                edit.commit();
                            }
                            break;
                        }
                        case ColorLayout.LAYOUT_NORMAL:
                        {
                            (colorPreviewView.findViewById(resource)).setBackgroundColor(Color.argb(a, r, g, b)); //R.id.swatch update color
                            if(position==0) {
                                edit.putInt("normal_label_color", Color.argb(a,r,g,b));
                                edit.commit();
                            }
                            if(position==1) {
                                edit.putInt("normal_bg_color", Color.argb(a,r,g,b));
                                edit.commit();
                            }
                            if(position==2) {
                                edit.putInt("normal_highlight_color", Color.argb(a,r,g,b));
                                edit.commit();
                            }
                            break;
                        }
                        default:
                        {
                            break;
                        }
                    }
                }
            },
                    oldColor);
            return dialog;
        } else {
            return null;
        }
    }
	
	// DIALOG STUFF
	///////////////
	@Override
	protected Dialog onCreateDialog(int id, Bundle b) {
		switch(id) {
			case DIALOG_COLOR:
			{
				final int position = b.getInt("position", 0);

				return createColorChangeDialog(b, mRootView.getChildAt(position));
			}
			default:
			{
				return null;
			}
		}
	}
}