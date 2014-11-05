package com.whoopeelab.autour;

import android.app.IntentService;
import android.content.Intent;

import com.whoopeelab.autour.utils.Constants;
import com.whoopeelab.autour.utils.HttpClientHelper;

import java.util.HashMap;
import java.util.Map;

public class PostFriendsListService extends IntentService {

    Map postDataMap;

    public PostFriendsListService(){
        super("PostFriendsListService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String usr = intent.getStringExtra("current_user_id");
        String friendsListStr = intent.getStringExtra("friends_list");
        String url = Constants.POST_FRIENDS_URI + usr;
        postDataMap = new HashMap();
        postDataMap.put("friends",friendsListStr);
        HttpClientHelper helper = new HttpClientHelper();
        boolean result = helper.post(url,postDataMap);
    }
}