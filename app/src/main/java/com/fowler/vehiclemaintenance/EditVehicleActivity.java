package com.fowler.vehiclemaintenance;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.fowler.vehiclemaintenance.datamodel.Vehicle;
import com.fowler.vehiclemaintenance.datamodel.VehicleDataService;

public class EditVehicleActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText currentMileageEditText;
    private EditText milesPerMonthEditText;

    private Vehicle vehicle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_vehicle);

        nameEditText = (EditText)findViewById(R.id.vehicleNameEditText);
        currentMileageEditText = (EditText)findViewById(R.id.vehicleCurrentMileageEditText);
        milesPerMonthEditText = (EditText)findViewById(R.id.vehicleMilesPerMonthEditText);

        Intent intent = getIntent();
        vehicle = (Vehicle)intent.getSerializableExtra(Constants.VEHICLE_EXTRA);
        if(vehicle != null) {
            // If vehicle is not null, then we are editing an existing vehicle
            nameEditText.setText(vehicle.getName());
            currentMileageEditText.setText("" + vehicle.getEstimatedCurrentMileage());
            if(vehicle.getMilesPerMonth() != null)
                milesPerMonthEditText.setText("" + vehicle.getMilesPerMonth());
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        assert toolbar != null;
        toolbar.setTitle(vehicle != null ? "Edit " + vehicle.getName() : "Add Vehicle");
        toolbar.setLogo(R.drawable.ic_directions_car_darkgreen_24dp);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        setSupportActionBar(toolbar);
        // The navigation on-click listener must be set AFTER the call to setSupportActionBar,
        // or it will never fire.
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button okButton = (Button) findViewById(R.id.newVehicleOkButton);
        assert okButton != null;
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(nameEditText.getText().toString().trim().isEmpty()) {
                    nameEditText.setError("Name is required.");
                    return;
                }
                if(currentMileageEditText.getText().toString().trim().isEmpty()) {
                    currentMileageEditText.setError("Current mileage is required.");
                    return;
                }

                String name = nameEditText.getText().toString();
                int currentMileage = Integer.parseInt(currentMileageEditText.getText().toString());
                String milesPerMonthString = milesPerMonthEditText.getText().toString();
                Integer milesPerMonth = null;
                if(!milesPerMonthString.isEmpty()) {
                    milesPerMonth = Integer.parseInt(milesPerMonthString);
                }

                if(vehicle == null) {
                    vehicle = new Vehicle(name, currentMileage, milesPerMonth);
                } else {
                    vehicle.setName(name);
                    vehicle.setCurrentMileage(currentMileage);
                    vehicle.setMilesPerMonth(milesPerMonth);
                }
                VehicleDataService vehicleDataSvc = new VehicleDataService(EditVehicleActivity.this);
                int vehicleId = vehicleDataSvc.persist(vehicle);
                Intent intent = new Intent();
                intent.putExtra(Constants.VEHICLE_ID_EXTRA, vehicleId);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }
}
