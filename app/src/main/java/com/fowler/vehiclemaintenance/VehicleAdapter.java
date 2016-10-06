package com.fowler.vehiclemaintenance;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.fowler.vehiclemaintenance.datamodel.MaintenanceStatus;
import com.fowler.vehiclemaintenance.datamodel.Vehicle;

import java.util.List;

public class VehicleAdapter extends ArrayAdapter<Vehicle> {

    private static class ViewHolder {
        TextView nameTextView;
        ImageView statusImageView;
        TextView statusTextView;
    }

    public VehicleAdapter(Context context, List<Vehicle> vehicles) {
        // resource id can be 0 since we're overriding getView()
        super(context, 0, vehicles);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Vehicle vehicle = getItem(position);
        ViewHolder viewHolder;
        if(convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.vehicle_list_view_layout, parent, false);
            viewHolder.nameTextView = (TextView)convertView.findViewById(R.id.vehicleNameTextView);
            viewHolder.statusImageView = (ImageView)convertView.findViewById(R.id.vehicleStatusImageView);
            viewHolder.statusTextView = (TextView)convertView.findViewById(R.id.vehicleStatusTextView);
            // Store default text colors for status text view in tag so it can be restored later
            viewHolder.statusTextView.setTag(viewHolder.statusTextView.getTextColors());
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        viewHolder.nameTextView.setText(vehicle.getName());

        if(!vehicle.getMaintenanceItems().isEmpty()) {
            MaintenanceStatus maintenanceStatus = vehicle.getMaintenanceStatus();
            viewHolder.statusImageView.setImageResource(maintenanceStatus.getImageResource());
            viewHolder.statusTextView.setText("Maintenance " + maintenanceStatus);
            Integer color = maintenanceStatus.getColor();
            if (color != null) {
                viewHolder.statusTextView.setTextColor(color);
            } else {
                // When the TextView was created above, we stored the original text colors in the tag
                ColorStateList textColors = (ColorStateList) viewHolder.statusTextView.getTag();
                viewHolder.statusTextView.setTextColor(textColors);
            }
        } else {
            viewHolder.statusImageView.setVisibility(ImageView.INVISIBLE);
            viewHolder.statusTextView.setText("No Maintenance Items");
            // When the TextView was created above, we stored the original text colors in the tag
            ColorStateList textColors = (ColorStateList) viewHolder.statusTextView.getTag();
            viewHolder.statusTextView.setTextColor(textColors);
        }

        return convertView;
    }
}
