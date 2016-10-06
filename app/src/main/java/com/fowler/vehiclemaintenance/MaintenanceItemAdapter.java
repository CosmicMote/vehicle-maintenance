package com.fowler.vehiclemaintenance;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.fowler.vehiclemaintenance.datamodel.MaintenanceItem;
import com.fowler.vehiclemaintenance.datamodel.MaintenanceStatus;

import java.util.List;

public class MaintenanceItemAdapter extends ArrayAdapter<MaintenanceItem> {
    private static class ViewHolder {
        TextView typeTextView;
        TextView statusTextView;
        ImageView statusImageView;
    }

    public MaintenanceItemAdapter(Context context, List<MaintenanceItem> maintenanceItems) {
        // Just pass in 0 for resource id, since we're overridding getView() and it therefore won't be used
        super(context, 0, maintenanceItems);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MaintenanceItem maintenanceItem = getItem(position);
        ViewHolder viewHolder;
        if(convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.maintenance_item_list_view_layout, parent, false);
            viewHolder.typeTextView = (TextView)convertView.findViewById(R.id.maintenanceItemTypeTextView);
            viewHolder.statusTextView = (TextView)convertView.findViewById(R.id.maintenanceItemStatusTextView);
            // Store default text colors for status text view in tag so it can be restored later
            viewHolder.statusTextView.setTag(viewHolder.statusTextView.getTextColors());
            viewHolder.statusImageView = (ImageView)convertView.findViewById(R.id.maintenanceItemStatusImageView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        viewHolder.typeTextView.setText(maintenanceItem.getType().toString());
        MaintenanceStatus maintenanceStatus = maintenanceItem.getMaintenanceStatus();
        viewHolder.statusTextView.setText(maintenanceStatus.toString(maintenanceItem.getMileageDue()));
        Integer color = maintenanceStatus.getColor();
        if(color != null) {
            viewHolder.statusTextView.setTextColor(color);
        } else {
            // When the TextView was created above, we stored the original text colors in the tag
            ColorStateList textColors = (ColorStateList) viewHolder.statusTextView.getTag();
            viewHolder.statusTextView.setTextColor(textColors);
        }
        viewHolder.statusImageView.setImageResource(maintenanceStatus.getImageResource());

        return convertView;
    }
}
