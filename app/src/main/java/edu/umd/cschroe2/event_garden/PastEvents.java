package edu.umd.cschroe2.event_garden;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.Fragment;
import android.app.ListActivity;
import android.app.ListFragment;
import android.app.Notification;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import snow.skittles.BaseSkittle;
import snow.skittles.SkittleBuilder;
import snow.skittles.SkittleLayout;
import snow.skittles.TextSkittle;

public class PastEvents extends AppCompatActivity {

    int ADD_EVENT_REQUEST=1;
    FragmentTabHost tabHost;
    public ArrayList<String> attending;
    Filter filter;
    public int filter_distance= 10;
    private ListView mDrawerList;
    private ArrayAdapter<String> drawerAdapter;

    public DatabaseHelper eventGardenDatabase;
    //https://github.com/mikepenz/Android-Iconics
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        //http://stackoverflow.com/questions/12246388/remove-shadow-below-actionbar
        getSupportActionBar().setElevation(0);
        //getSupportActionBar().setIcon(R.drawable.plant_icon);
        // Setup database
        eventGardenDatabase = DatabaseHelper.getInstance(this);
        eventGardenDatabase.onCreate(eventGardenDatabase.getWritableDatabase());
        //eventGardenDatabase.onUpgrade(eventGardenDatabase.getWritableDatabase(),0,1);
        attending = new ArrayList<String>();
        filter=new Filter();

        //https://www.youtube.com/watch?v=QutovPrajXs
        tabHost = (FragmentTabHost) findViewById(R.id.tabhost);
        //tabHost.setup();
        tabHost.setup(this, getSupportFragmentManager(), android.R.id.tabcontent);

        Bundle b = new Bundle();
        b.putInt("Filter_distance", 10);

        tabHost.addTab(
                tabHost.newTabSpec("tab1").setIndicator("List", null),
                List_Fragment.class, null);

        tabHost.addTab(
                tabHost.newTabSpec("tab2").setIndicator("Map", null),
                Map_Fragment.class, b);

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {

            @Override
            public void onTabChanged(String tabId) {
                setTabColor();
            }
        }); //try setting all the points up here

        //initial tab color set
        setTabColor();

        android.support.design.widget.FloatingActionButton add = (android.support.design.widget.FloatingActionButton) findViewById(R.id.add_button);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PastEvents.this, AddEvent.class);
                startActivityForResult(intent, ADD_EVENT_REQUEST);
            }
        });

        final DrawerLayout drawerLayout = (DrawerLayout)findViewById(R.id.drawer);
        /*Calendar calendar = Calendar.getInstance();
        int d = calendar.get(Calendar.DAY_OF_MONTH);
        int m = calendar.get(Calendar.MONTH);
        String mon = "" + m;
        String da = "" + d;
        final EditText month = (EditText) drawerLayout.findViewById(R.id.month);
        month.setText(mon);
        final EditText day = (EditText) drawerLayout.findViewById(R.id.day);
        day.setText(da);*/
        final EditText radius = (EditText)drawerLayout.findViewById(R.id.rad);

        CheckBox enviro = (CheckBox) drawerLayout.findViewById(R.id.checkbox_enviro);
        CheckBox rec = (CheckBox) drawerLayout.findViewById(R.id.checkbox_rec);
        CheckBox arts = (CheckBox) drawerLayout.findViewById(R.id.checkbox_arts);
        CheckBox animals = (CheckBox) drawerLayout.findViewById(R.id.checkbox_animals);
        CheckBox social = (CheckBox) drawerLayout.findViewById(R.id.checkbox_social);
        enviro.setChecked(filter.filter_categories.get("Environmental"));
        rec.setChecked(filter.filter_categories.get("Recreation"));
        arts.setChecked(filter.filter_categories.get("Arts"));
        animals.setChecked(filter.filter_categories.get("Environmental"));
        social.setChecked(filter.filter_categories.get("Environmental"));


        Button saveButton = (Button) drawerLayout.findViewById(R.id.save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter.setRadius(radius.getText().toString());
                filter_checkboxes(drawerLayout);

                List_Fragment list = (List_Fragment) getSupportFragmentManager().findFragmentByTag("tab1");
                Map_Fragment map = (Map_Fragment) getSupportFragmentManager().findFragmentByTag("tab2");
                if(list != null) {
                    list.applyFilters(filter);
                }
                if(map !=null){

                }
                drawerLayout.closeDrawers();
            }
        });

    }

    public void filter_checkboxes( DrawerLayout dialog){
        CheckBox enviro = (CheckBox) dialog.findViewById(R.id.checkbox_enviro);
        CheckBox rec = (CheckBox) dialog.findViewById(R.id.checkbox_rec);
        CheckBox arts = (CheckBox) dialog.findViewById(R.id.checkbox_arts);
        CheckBox animals = (CheckBox) dialog.findViewById(R.id.checkbox_animals);
        CheckBox social = (CheckBox) dialog.findViewById(R.id.checkbox_social);
        if(enviro.isChecked()){
            filter.filter_categories.put("Environmental",true);
        }else{
            filter.filter_categories.put("Environmental",false);
        }

        if(rec.isChecked()){
            filter.filter_categories.put("Recreation",true);
        }else{
            filter.filter_categories.put("Recreation",false);
        }

        if(arts.isChecked()){
            filter.filter_categories.put("Arts", true);
        }else{
            filter.filter_categories.put("Arts", false);
        }

        if(animals.isChecked()){
            filter.filter_categories.put("Animals",true);
        }else{
            filter.filter_categories.put("Animals",false);
        }

        if(social.isChecked()){
            filter.filter_categories.put("Social",true);
        }else{
            filter.filter_categories.put("Social",false);
        }
    }


    //Change The Backgournd Color of Tabs
    //http://stackoverflow.com/questions/9356546/is-it-possible-to-change-the-color-of-selected-tab-in-android
    public void setTabColor() {

        for(int i=0;i<tabHost.getTabWidget().getChildCount();i++)
            tabHost.getTabWidget().getChildAt(i).setBackgroundColor(getResources().getColor(R.color.white)); //unselected

        if(tabHost.getCurrentTab()==0)
            tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab()).setBackgroundColor(getResources().getColor(R.color.colorAccent)); //1st tab selected
        else
            tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab()).setBackgroundColor(getResources().getColor(R.color.colorAccent)); //2nd tab selected
        setContentView(R.layout.past_events);
    }

    //http://stackoverflow.com/questions/2736389/how-to-pass-an-object-from-one-activity-to-another-on-android
    //http://stackoverflow.com/questions/23467612/communication-between-fragments-of-fragment-tab-host
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent i){
        if(requestCode == ADD_EVENT_REQUEST && resultCode == RESULT_OK){

            //add event object to SQL database
            Event event = (Event) i.getSerializableExtra("event");
            long eventID = eventGardenDatabase.insertEvent(event,1);
            if (eventID == -1){
                Toast.makeText(PastEvents.this, "Unable to access Sqlite database.", Toast.LENGTH_SHORT).show();
            }
            else{
                int id = eventGardenDatabase.getId(event);
                Log.i("setting id",id+" ");
                event.setId(id);
            }

            // Todo This is a debug statement for the sql database.
            Log.d("Events found", ""+eventGardenDatabase.getAllEvents().size());
            for (Event e : eventGardenDatabase.getAllEvents()){
                Log.d("EVENTNAME", e.event_name);
                Log.d("event date", e.date);
                Log.d("event time", e.time);
                Log.d("event location", e.location);
                Log.d("event description", e.description);
                for (String s : e.filters){
                    Log.d("event category", s);
                }
                Log.d("Event","Equipment");
                java.util.Iterator iter = e.equipment.entrySet().iterator();
                while (iter.hasNext()){
                    Map.Entry pair = (Map.Entry) iter.next();
                    Log.d("Equipment Name",(String)pair.getKey());
                    Log.d("Equipment quantity",pair.getValue().toString());
                }
            }
            // Todo End debug for sqldatabase.

            // Add event to adapter.
            String curr_tab_tag = tabHost.getCurrentTabTag();
            //Log.i("current tab tag",curr_tab_tag);
            //List_Fragment f = (List_Fragment) getSupportFragmentManager().findFragmentByTag(curr_tab_tag);
            //f.addtoAdapt(event);
            List_Fragment list =(List_Fragment)getSupportFragmentManager().findFragmentByTag("tab1");
            Map_Fragment map =(Map_Fragment)getSupportFragmentManager().findFragmentByTag("tab2");
            if(map != null){ //add markers to array?
                //map.addMarker(event);
            }
            list.addtoAdapt(event);


        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.search:

                return true;
            case R.id.profile:
                Intent i = new Intent(PastEvents.this ,Profile.class );
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public Filter getFilter(){
        return filter;
    }

}