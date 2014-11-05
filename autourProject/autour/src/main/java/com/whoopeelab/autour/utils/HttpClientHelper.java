package com.whoopeelab.autour.utils;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Map;

public class HttpClientHelper {

  HttpClient client = new DefaultHttpClient();
  public HttpClientHelper() {}

  public JSONObject get(String url) {
     //Log.v(getClass().getName(),"Getting URL "+ url);
    StringBuilder builder = new StringBuilder();
    JSONObject jsonObject = null;
    HttpGet httpGet = new HttpGet(url);
    try {
      HttpResponse response = client.execute(httpGet);
      StatusLine statusLine = response.getStatusLine();

      if(statusLine.getStatusCode() == 200) {
        HttpEntity entity = response.getEntity();
        InputStream content = entity.getContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(content));
        String line;
        while((line = reader.readLine()) != null) {
          builder.append(line);
        }

        try {
          jsonObject = new JSONObject(builder.toString());
        } catch (JSONException e) {
          Log.e(getClass().getName(), e.getMessage());
          return null;
        }

      } else {
        return null;
      }
    } catch (IOException e) {
      Log.e(getClass().getName(), e.getMessage());
      return null;
    }
    return jsonObject;
  }

  public boolean post(String url, Map<String,String> postDataMap) {
    Log.v(getClass().getName(),"Posting URL "+ url);
    HttpPost httpPost = new HttpPost(url);
    JSONObject postData = new JSONObject(postDataMap);

    try {
      httpPost.setEntity(new StringEntity(postData.toString()));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("Content-type", "application/json");

    try {
      HttpResponse response = client.execute(httpPost);
      StatusLine statusLine = response.getStatusLine();
      if (statusLine.getStatusCode() == 201) {
        return true;
      }
    } catch (IOException e) {
      Log.e(getClass().getName(), e.getMessage());
      return false;
    }

    return false;
  }
}