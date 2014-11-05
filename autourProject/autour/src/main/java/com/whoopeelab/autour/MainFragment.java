package com.whoopeelab.autour;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.whoopeelab.autour.location.GetFriendsLocService;
import com.whoopeelab.autour.location.LocationUtils;
import com.whoopeelab.autour.route.MapActivity;
import com.whoopeelab.autour.utils.Constants;
import com.whoopeelab.autour.utils.HttpClientHelper;
import com.whoopeelab.autour.utils.Utilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainFragment extends Fragment implements AdapterView.OnItemClickListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener, CompoundButton.OnCheckedChangeListener {

    LocationRequest mLocationRequest;
    LocationClient mLocationClient;

    ListView mainListView;
    List<RowItem> mainRowItems;
    FriendsListViewAdapter mainListAdapter;
    Switch schUpdateLoc;
    TextView noNetConnTxtView;
    Intent postIdServiceIntent;
    String current_user_id;
    String current_user_name;
    boolean servicesStarted = false;
    int friendsCount = 0;
    boolean locBtnChecked = false;
    Timer getFriendsLocTimer;
    Timer postFriendsDataTimer;
    UiLifecycleHelper uiHelper;
    Intent postLocServiceIntent = null;
    SharedPreferences sharedPrefFrmPreferences;

    private Session.StatusCallback sessionCallback = new Session.StatusCallback() {
        @Override
        public void call(final Session session, final SessionState state, final Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        noNetConnTxtView = (TextView) view.findViewById(R.id.conn_warning);
        schUpdateLoc = (Switch) view.findViewById(R.id.on_off_switch);
        schUpdateLoc.setOnCheckedChangeListener(this);

        //set the UI list
        mainRowItems = new ArrayList<RowItem>();
        mainListView = (ListView) view.findViewById(R.id.main_list);
        mainListAdapter = new FriendsListViewAdapter(getActivity(), R.layout.fragment_main_list_item, mainRowItems);
        mainListView.setAdapter(mainListAdapter);
        mainListView.setOnItemClickListener(this);
        registerForContextMenu(mainListView);

        return view;
    }



    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
        menu.setHeaderTitle(R.string.title);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.msg_friend:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, "Let's Meet");
                intent.putExtra(Intent.EXTRA_TEXT, "Hey I am in the same area as you are");
                startActivity(Intent.createChooser(intent, "Send Message"));
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void initializeServices() {
        postFriendsData();
        getFriendsLoc();
    }

    private void setupLocationRequestParams(){
        mLocationRequest = LocationRequest.create();

        //Set the update interval
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        mLocationRequest.setSmallestDisplacement(LocationUtils.SMALLEST_DISPLACEMENT_METERS);

        mLocationClient = new LocationClient(getActivity(), this, this);
    }

//    private boolean playServicesConnected() {
//
//        // Check that Google Play services is available
//        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
//
//        // If Google Play services is available
//        return ConnectionResult.SUCCESS == resultCode;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(getActivity(), sessionCallback);
        uiHelper.onCreate(savedInstanceState);
        setupLocationRequestParams();
        setHasOptionsMenu(true);
        sharedPrefFrmPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()){
            case R.id.about:
                return true;
            case R.id.action_settings:
                getActivity().startActivityForResult(new Intent(getActivity(), SettingsActivity.class), 1);
                return true;
            case R.id.sign_out:
                fbSignOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (schUpdateLoc.isChecked()) {
            Log.v(getClass().getName(),"in onconnected and publish location is enabled so request loc update");
            mLocationClient.requestLocationUpdates(mLocationRequest, this);
        } else {
            Log.v(getClass().getName(),"in onconnected and publish location is disabled");
        }
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.v(getClass().getName(),"Coming to new onLocationChanged");
        final String loc = LocationUtils.getLatLng(getActivity(), location);
        final SharedPreferences preferences = Utilities.getAutourSharedPreference(getActivity());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("current_user_location", loc);
        editor.commit();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.v(getClass().getName(),"sending location to server "+ loc);
                HashMap<String, String> postDataMap = new HashMap();
                postDataMap.put("loc", loc);
                postDataMap.put("name", preferences.getString("current_user_name",""));
                HttpClientHelper helper = new HttpClientHelper();
                boolean result = helper.post(Constants.POST_LOC_URI + preferences.getString("current_user_id",""),postDataMap);
                return;

            }
        }).start();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if(isChecked) {
            LocationManager locationManager = (LocationManager) getActivity().getSystemService(getActivity().LOCATION_SERVICE);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                if (mLocationClient.isConnected()) {
                    mLocationClient.requestLocationUpdates(mLocationRequest, this);
                } else {
                    mLocationClient.connect();
                }
            } else {
                showGPSDisabledAlertToUser();
            }
        } else {
            if (mLocationClient.isConnected()) {
                mLocationClient.removeLocationUpdates(this);
            }
        }
    }

    public void fbSignOut() {
        final Session openSession = Session.getActiveSession();
        if (openSession != null && openSession.isOpened()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.signing_out)
                    .setCancelable(true)
                    .setPositiveButton(R.string.sign_out, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            openSession.closeAndClearTokenInformation();
                            MainActivity mainActivity = (MainActivity)getActivity();
                            mainActivity.showFragment(mainActivity.LOGIN, false);
                            servicesStarted = false;
                            getFriendsLocTimer.cancel();
                            postFriendsDataTimer.cancel();
                            if(postLocServiceIntent != null) {
                              getActivity().stopService(postLocServiceIntent);
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, null);
            builder.create().show();
        }
    }

    @Override
    public void onStart(){
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        uiHelper.onResume();

        if(schUpdateLoc.isChecked()) {
            if(mLocationClient.isConnected()) {
                Log.i(getClass().getName(), "onresume publish location enabled and location client is also connected, just requesting update");
                mLocationClient.requestLocationUpdates(mLocationRequest, this);
            } else {
                Log.i(getClass().getName(), "onresume publish location enabled and location client is not connected");
                mLocationClient.connect();
            }
        } else {
            if(!mLocationClient.isConnected()) {
                Log.i(getClass().getName(), "onresume publish location disabled and location client is not connected");
                mLocationClient.connect();
            } else {
                Log.i(getClass().getName(), "onresume publish location disabled and location client is also connected");
            }
        }
    }

    public void initializeFragment(){
        SharedPreferences sharedPref = Utilities.getAutourSharedPreference(getActivity());
        current_user_id = sharedPref.getString("current_user_id", null);
        current_user_name = sharedPref.getString("current_user_name", null);
        if(current_user_id != null) {
            if(!servicesStarted) {
                initializeServices();
                servicesStarted = true;
            }
            Toast t;
            if(checkConnection()) {
                noNetConnTxtView.setVisibility(View.GONE);
                t = Toast.makeText(getActivity(),R.string.loading_msg, 30000);
                t.setGravity(Gravity.CENTER, 0, 0);
                t.show();
            } else {
                noNetConnTxtView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
        if (mLocationClient.isConnected()) {
            Log.v(getClass().getName(),"in onpause location client is connected, so removing location req update");
            mLocationClient.removeLocationUpdates(this);
        } else {
            Log.v(getClass().getName(),"in onpause location client is not connected");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        uiHelper.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getFriendsLocTimer.cancel();
        postFriendsDataTimer.cancel();
        if(postLocServiceIntent != null) {
           getActivity().stopService(postLocServiceIntent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
        if (mLocationClient.isConnected()) {
            mLocationClient.removeLocationUpdates(this);
        }
        mLocationClient.disconnect();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
    }

    public boolean checkConnection() {
        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(getActivity().CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        connected = networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
        return connected;
    }

    // method to get facebook friends who have logged in to the application
    private void makeMyFriendsRequest(final Session session) {
        Request request = Request.newMyFriendsRequest(session, new Request.GraphUserListCallback() {
            @Override
            public void onCompleted(List<GraphUser> users, Response response) {
                if(getActivity() == null) {
                    return;
                }

                StringBuilder ids = new StringBuilder();
                JSONObject g = response.getGraphObject().getInnerJSONObject();
                if(g!=null){
                    try {
                        JSONArray jsonArray = g.getJSONArray("data");
                        SharedPreferences sharedPref = Utilities.getAutourSharedPreference(getActivity());
                        SharedPreferences.Editor editor = sharedPref.edit();
                        for (int i=0; i< jsonArray.length(); i++) {
                            JSONObject json_obj = jsonArray.getJSONObject(i);
                            String url = json_obj.getJSONObject("picture").getJSONObject("data").getString("url");
                            String friend_id = "UF" + json_obj.getString("id");
                            DataModel dataModel;
                            if(sharedPref.contains(friend_id)) {
                                dataModel = new DataModel(sharedPref.getString(friend_id, null));
                                dataModel.setName(json_obj.getString("name"));
                                dataModel.setImgUri(url);
                            } else {
                              dataModel = new DataModel(json_obj.getString("name"),url, 0, null);
                            }

                            ids.append(json_obj.getString("id"));
                            ids.append(",");
                            editor.putString(friend_id, dataModel.serialize());
                        }

                        // call load listview for the very first time of this app
                        if(!sharedPref.getBoolean("HAS_FRIENDS", false)) {
                            new LoadListView().execute();
                        }

                        if(jsonArray.length() > 0) {
                            editor.putBoolean("HAS_FRIENDS", true);
                            editor.commit();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // now call the service if new friends have been added
                    if(friendsCount < ids.length()) {
                        friendsCount = ids.length();
                        postIdServiceIntent = new Intent(getActivity(), PostFriendsListService.class);
                        postIdServiceIntent.putExtra("current_user_id",current_user_id);
                        postIdServiceIntent.putExtra("friends_list", ids.toString());
                        getActivity().startService(postIdServiceIntent);
                    }
                }
            }
        });

        Bundle params = new Bundle();
        params.putString("fields", "id,name,picture");
        request.setParameters(params);
        request.executeAsync();
    }

    private void postFriendsData(){
        final Handler handler = new Handler();
        postFriendsDataTimer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            if(checkConnection()){
                                Session session = Session.getActiveSession();
                                if (session != null && session.isOpened()) {
                                    makeMyFriendsRequest(session);
                                }
                            }
                        } catch (Exception e) {
                        }
                    }
                });
            }
        };
        postFriendsDataTimer.schedule(doAsynchronousTask, 0, 30000); //execute in every 30 secs
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Switch s = (Switch) getActivity().findViewById(R.id.on_off_switch);
        if(s.isChecked()){
            if(checkConnection()){
                TextView distanceView = (TextView) view.findViewById(R.id.main_distance);
                if(distanceView.getText().equals("NA")){
                    TextView nameView = (TextView) view.findViewById(R.id.main_name);
                    Toast t = Toast.makeText(getActivity(), nameView.getText() + "'s latest location is not available", 3000);
                    t.setGravity(Gravity.CENTER, 0, 0);
                    t.show();
                }else{
                    Intent intent = new Intent(getActivity(),MapActivity.class);
                    TextView v = (TextView) view.findViewById(R.id.main_name);
                    intent.putExtra("friend_user_id", v.getTag().toString());
                    startActivity(intent);
                }
            }else{
                Toast t = Toast.makeText(getActivity(), R.string.enable_connection, 5000);
                t.setGravity(Gravity.CENTER, 0, 0);
                t.show();
            }
        }else{
            Toast t = Toast.makeText(getActivity(), R.string.enable_gps_loc, 5000);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
        }
    }

    //method to load the data periodically got from server
    private class LoadListView extends AsyncTask<Void,Void,List<RowItem>>{

        @Override
        protected List<RowItem> doInBackground(Void... voids) {
            if(getActivity() == null){
                return null;
            }

            RowItem item = null;
            Bitmap bitmap =null;
            SharedPreferences sharedPref = Utilities.getAutourSharedPreference(getActivity());
            List<RowItem> newRowItems = new ArrayList<RowItem>();
            Map<String,?> map = sharedPref.getAll();
            for(Map.Entry<String, ?> entry : map.entrySet()) {
                String id = entry.getKey();
                if(entry.getKey()!=null && entry.getKey().contains("UF")){
                    DataModel model = new DataModel(entry.getValue().toString());
                    try {
                        URL imgUrl = new URL(model.getImgUri());
                        bitmap = BitmapFactory.decodeStream(imgUrl.openConnection().getInputStream());
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e){
                        bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.com_facebook_profile_default_icon);
                    }

                    item = new RowItem(id, model.getName(), bitmap,
                            Utilities.getHumanReadableTime(model.getTimeStamp()),
                            Utilities.getDistanceBetweenGpsLocations(sharedPref.getString("current_user_location", null), model.getLocation()));
                    newRowItems.add(item);
                }

                // sort by smallest distance first.
                if(sharedPrefFrmPreferences!=null && sharedPrefFrmPreferences.getBoolean("pref_key_sort_dist",false)){
                    Collections.sort(newRowItems, new Utilities.DistanceComparator());
                }
            }
            return newRowItems;
        }

        @Override
        protected void onPostExecute(List<RowItem> list) {
            if(list != null){
                mainRowItems.clear();
                mainRowItems.addAll(list);
                mainListAdapter.notifyDataSetChanged();
            }
        }
    }

    private void getFriendsLoc() {
        final Handler handler = new Handler();
        getFriendsLocTimer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            new LoadListView().execute();
                            if(checkConnection()){
                                Intent i = new Intent(getActivity(), GetFriendsLocService.class);
                                i.putExtra("current_user_id",current_user_id);
                                getActivity().startService(i);
                                noNetConnTxtView.setVisibility(View.GONE);
                            }else{
                                noNetConnTxtView.setVisibility(View.VISIBLE);
                            }
                        } catch (Exception e) {
                        }
                    }
                });
            }
        };

        getFriendsLocTimer.schedule(doAsynchronousTask, 0, 3000); //5 secs
    }

    private void showGPSDisabledAlertToUser(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setMessage(R.string.gps_disabled)
                .setCancelable(false)
                .setPositiveButton("Settings",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                getActivity().startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

}