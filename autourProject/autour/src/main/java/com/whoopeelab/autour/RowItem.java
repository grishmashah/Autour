package com.whoopeelab.autour;

import android.graphics.Bitmap;

public class RowItem {
    private String timeStamp;
    private String name;
    private Bitmap icon;
    private String distance;
    private String userid;

    public RowItem(String userid, String name, Bitmap icon, String timeStamp, String distance) {
        this.userid = userid;
        this.name = name;
        this.icon = icon;
        this.timeStamp = timeStamp;
        this.distance = distance;
    }

    public Bitmap getIcon() {
        return icon;
    }
    public void setIcon(Bitmap icon) {
        this.icon = icon;
    }
    public String getTimeStamp() {
        return timeStamp;
    }
    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }
    public void setUserid(String userid) {this.userid = userid;}
    public String getUserid() {return userid;}
}
