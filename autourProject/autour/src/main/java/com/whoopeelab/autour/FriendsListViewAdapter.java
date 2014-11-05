package com.whoopeelab.autour;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class FriendsListViewAdapter extends ArrayAdapter<RowItem> {
    Context context;
    int resourceId;
    public FriendsListViewAdapter(Context context, int resourceId, List<RowItem> items) {
        super(context, resourceId, items);
        this.context = context;
        this.resourceId = resourceId;
    }

    private class MainListViewHolder {
        TextView txtTimeStamp;
        TextView txtDistance;
        ImageView imageView;
        TextView txtName;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        MainListViewHolder mainHolder = null;

        RowItem rowItem = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.fragment_main_list_item, null);
            mainHolder = new MainListViewHolder();
            mainHolder.imageView = (ImageView) convertView.findViewById(R.id.main_icon);
            mainHolder.txtTimeStamp = (TextView) convertView.findViewById(R.id.main_timeStamp);
            mainHolder.txtName = (TextView) convertView.findViewById(R.id.main_name);
            mainHolder.txtDistance = (TextView) convertView.findViewById(R.id.main_distance);
            convertView.setTag(mainHolder);
        } else {
            mainHolder = (MainListViewHolder) convertView.getTag();
        }

        mainHolder.txtDistance.setText(rowItem.getDistance());
        mainHolder.txtTimeStamp.setText(rowItem.getTimeStamp());
        mainHolder.txtName.setText(rowItem.getName());
        mainHolder.txtName.setTag(rowItem.getUserid());
        mainHolder.imageView.setImageBitmap(rowItem.getIcon());

        return convertView;
    }
}
