package com.fowler.vehiclemaintenance;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.fowler.vehiclemaintenance.datamodel.MaintenanceItem;
import com.fowler.vehiclemaintenance.datamodel.MaintenanceType;
import com.fowler.vehiclemaintenance.datamodel.Vehicle;
import com.fowler.vehiclemaintenance.datamodel.VehicleDataService;

import java.util.List;

public class EditMaintenanceItemActivity extends AppCompatActivity {

    private Spinner spinner;
    private EditText intervalEditText;
    private EditText lastMileageEditText;

    private int vehicleId;
    private MaintenanceItem maintenanceItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_maintenance_item);

        Intent intent = getIntent();
        vehicleId = intent.getIntExtra(Constants.VEHICLE_ID_EXTRA, -1);
        maintenanceItem =
                (MaintenanceItem)intent.getSerializableExtra(Constants.MAINTENANCE_ITEM_EXTRA);

        VehicleDataService vehicleDataSvc = new VehicleDataService(this);
        Vehicle vehicle = vehicleDataSvc.getVehicle(vehicleId);

        List<MaintenanceType> maintenanceTypes = vehicle.getUnusedMaintenanceTypes();
        if(maintenanceItem != null) {
            maintenanceTypes.add(0, maintenanceItem.getType());
        }

        intervalEditText = (EditText)findViewById(R.id.intervalEditText);
        lastMileageEditText = (EditText)findViewById(R.id.lastMileageEditText);

        spinner = (Spinner)findViewById(R.id.maintenanceTypeSpinner);
        assert spinner != null;
        ArrayAdapter<MaintenanceType> spinnerAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, maintenanceTypes);
        spinner.setAdapter(spinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MaintenanceType maintenanceType = (MaintenanceType) spinner.getSelectedItem();
                intervalEditText.setText("" + maintenanceType.getDefaultMileageInterval());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                intervalEditText.setText("");
            }
        });

        if(maintenanceItem != null) {
            // If maintenance item is not null, we're editing an existing maintenance item
            spinner.setSelection(0);
            intervalEditText.setText("" + maintenanceItem.getMileageInterval());
            if(maintenanceItem.getLastMileageDone() != null)
                lastMileageEditText.setText("" + maintenanceItem.getLastMileageDone());
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        assert toolbar != null;
        toolbar.setTitle(maintenanceItem != null ? "Edit " + maintenanceItem.getType() : "Add Maintenance Item");
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

        Button okButton = (Button)findViewById(R.id.newMaintenanceItemOkButton);
        assert okButton != null;
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(intervalEditText.getText().toString().trim().isEmpty()) {
                    intervalEditText.setError("Interval is required.");
                    return;
                }

                MaintenanceType maintenanceType = (MaintenanceType) spinner.getSelectedItem();
                int interval = Integer.parseInt(intervalEditText.getText().toString());
                Integer lastMileageDone = null;
                Editable editable = lastMileageEditText.getText();
                if(editable != null && editable.length() > 0)
                    lastMileageDone = Integer.parseInt(editable.toString());

                if(maintenanceItem == null)
                    maintenanceItem = new MaintenanceItem();
                maintenanceItem.setType(maintenanceType);
                maintenanceItem.setMileageInterval(interval);
                if(lastMileageDone != null)
                    maintenanceItem.setLastMileageDone(lastMileageDone);

                VehicleDataService vehicleDataSvc = new VehicleDataService(EditMaintenanceItemActivity.this);
                int maintenanceItemId = vehicleDataSvc.persist(vehicleId, maintenanceItem);
                Intent intent = new Intent();
                intent.putExtra(Constants.VEHICLE_ID_EXTRA, vehicleId);
                intent.putExtra(Constants.MAINTENANCE_ITEM_ID_EXTRA, maintenanceItemId);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }
}
