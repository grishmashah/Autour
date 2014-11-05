package com.whoopeelab.autour.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.whoopeelab.autour.RowItem;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

public class Utilities {

    public static String getHumanReadableTime(long timeInSecsSinceEpoch) {
        if(timeInSecsSinceEpoch == 0) {
            return "NA";
        }
        long currentEpochTime = (new Date()).getTime();
        long epochTimeDiff = currentEpochTime - (timeInSecsSinceEpoch * 1000);
        String dateFormat = "MM/dd/yy";

        // more than a day
        if(epochTimeDiff <= 86400000) {
            dateFormat = "hh:mm a";
        }

        DateFormat format = new SimpleDateFormat(dateFormat);
        format.setTimeZone(TimeZone.getDefault());
        return format.format(new Date(timeInSecsSinceEpoch*1000));
    }

    public static String getDistanceBetweenGpsLocations(String myGpsLoc, String friendGpsLoc) {
        if(myGpsLoc == null || friendGpsLoc == null || myGpsLoc.equals("null") || friendGpsLoc.equals("null"))
            return "NA";

        double lat1 = Double.parseDouble(myGpsLoc.split(",")[0]);
        double lng1 = Double.parseDouble(myGpsLoc.split(",")[1]);
        double lat2 = Double.parseDouble(friendGpsLoc.split(",")[0]);
        double lng2 = Double.parseDouble(friendGpsLoc.split(",")[1]);

        int r = 6371; // average radius of the earth in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return String.valueOf((int)(r * c * 1000)) + "m";
    }

    public static SharedPreferences getAutourSharedPreference(Context context) {
      return context.getSharedPreferences(Constants.AUTOUR_SHARED_PREF, Context.MODE_PRIVATE);
    }

    public static class DistanceComparator implements Comparator<RowItem> {
        @Override
        public int compare(RowItem o1, RowItem o2) {
            String d1 = o1.getDistance();
            String d2 = o2.getDistance();

            // if d1 is NA then the distance is unknown
            // so let d1 be greater than d2.
            if (d1.equals("NA")) {
                return 1;
            }

            // if d2 is NA then the distance is unknown but d1's distance is known
            // so let d1 be less than d2.
            if (d2.equals("NA")) {
                return -1;
            }

            int i1 = Integer.parseInt(d1.replace("m",""));
            int i2 = Integer.parseInt(d2.replace("m",""));

            return Integer.valueOf(i1).compareTo(Integer.valueOf(i2));
        }
    }
}