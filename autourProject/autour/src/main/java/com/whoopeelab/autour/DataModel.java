package com.whoopeelab.autour;

public class DataModel {

    private String name;
    private String imgUri;
    private long timeStamp;
    private String location;

    public DataModel(String name, String imgUri, long timeStamp, String location){
        this.name = name;
        this.imgUri = imgUri;
        this.timeStamp = timeStamp;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImgUri() {
        return imgUri;
    }

    public void setImgUri(String imgUri) {
        this.imgUri = imgUri;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public DataModel(String serialized) {
        String [] param = serialized.split(";");
        if(param.length == 4) {
            this.name = param[0];
            this.imgUri = param[1];
            this.timeStamp = Long.valueOf(param[2]);
            this.location = param[3];
        }
    }

    public String serialize() {
        return name + ";" + imgUri + ";" + String.valueOf(timeStamp) + ";" + location;
    }
}