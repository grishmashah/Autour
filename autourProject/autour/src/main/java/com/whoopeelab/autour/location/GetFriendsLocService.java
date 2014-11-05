package com.whoopeelab.autour.location;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;

import com.whoopeelab.autour.DataModel;
import com.whoopeelab.autour.utils.Constants;
import com.whoopeelab.autour.utils.HttpClientHelper;
import com.whoopeelab.autour.utils.Utilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class GetFriendsLocService extends IntentService {

    public GetFriendsLocService() {
        super("GetFriendsLocService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPref = Utilities.getAutourSharedPreference(this);
        String current_user_id = intent.getStringExtra("current_user_id");
        SharedPreferences.Editor editor = sharedPref.edit();
        HttpClientHelper helper = new HttpClientHelper();
        JSONObject jsonObject = helper.get(Constants.GET_FRIENDS_LOC_URI + current_user_id + "?all=true");
        if(jsonObject!=null){
            int count = 0;
            Map<String,?> map = sharedPref.getAll();
            for(Map.Entry<String, ?> entry : map.entrySet()) {
                String id = entry.getKey().replaceFirst("UF","");
                if(entry.getKey()!=null && entry.getKey().contains("UF")){
                    DataModel model = new DataModel(entry.getValue().toString());
                    try {
                        if(jsonObject.getJSONObject(id).has("gps_loc")){
                            model.setLocation(jsonObject.getJSONObject(id).getString("gps_loc"));
                        }
                        if(jsonObject.getJSONObject(id).has("gps_loc_ts")){
                            model.setTimeStamp(jsonObject.getJSONObject(id).getLong("gps_loc_ts"));
                        }
                        editor.putString(entry.getKey(), model.serialize());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    count++;
                }
            }
            editor.commit();
        }
    }
}