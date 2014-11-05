package com.whoopeelab.autour.route;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.whoopeelab.autour.utils.Utilities;
import com.whoopeelab.autour.DataModel;
import com.whoopeelab.autour.R;

public class MapActivity extends Activity {

    String friend_user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent receivedIntent = getIntent();
        friend_user_id = receivedIntent.getStringExtra("friend_user_id");
        setContentView(R.layout.map);

        SharedPreferences sharedPref = Utilities.getAutourSharedPreference(this);
        DataModel d = new DataModel(sharedPref.getString(friend_user_id, null));

        GoogleMap map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

        if(map != null) {
            double friend_lat = Double.parseDouble(d.getLocation().split(",")[0]);
            double friend_lng = Double.parseDouble(d.getLocation().split(",")[1]);
            LatLng friend_loc = new LatLng(friend_lat, friend_lng);
            map.setMyLocationEnabled(true);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(friend_loc, 18));
            map.addMarker(new MarkerOptions().title(d.getName()).position(friend_loc));
        }
    }
}