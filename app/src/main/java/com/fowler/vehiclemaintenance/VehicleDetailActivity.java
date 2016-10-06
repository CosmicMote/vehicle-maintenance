package com.fowler.vehiclemaintenance;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fowler.vehiclemaintenance.datamodel.MaintenanceItem;
import com.fowler.vehiclemaintenance.datamodel.Vehicle;
import com.fowler.vehiclemaintenance.datamodel.VehicleDataService;
import com.fowler.vehiclemaintenance.util.InputDialog;
import com.fowler.vehiclemaintenance.util.ListViewTouchListener;
import com.fowler.vehiclemaintenance.util.OkClickListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressLint("SimpleDateFormat")
public class VehicleDetailActivity extends AppCompatActivity {

    private static final String TAG = VehicleDetailActivity.class.getSimpleName();

    private static final int CREATE_EDIT_MAINTENANCE_ITEM_REQUEST_CODE = 24602;
    private static final int EDIT_VEHICLE_REQUEST_CODE = 24603;

    private VehicleDataService vehiclesDataSvc;

    private Vehicle vehicle;
    private MaintenanceItemAdapter adapter;
    private ListView maintenanceItemListView;
    private TextView currentMileageTextView;
    private TextView milesPerMonthTextView;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_detail);

        Intent intent = getIntent();
        vehicle = (Vehicle)intent.getSerializableExtra(Constants.VEHICLE_EXTRA);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        assert toolbar != null;
        toolbar.setTitle(vehicle.getName());
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

        FloatingActionButton addMaintenanceItemFab = (FloatingActionButton) findViewById(R.id.addFab);
        assert addMaintenanceItemFab != null;
        addMaintenanceItemFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!vehicle.getUnusedMaintenanceTypes().isEmpty()) {
                    Intent intent = new Intent(VehicleDetailActivity.this, EditMaintenanceItemActivity.class);
                    intent.putExtra(Constants.VEHICLE_ID_EXTRA, vehicle.getId());
                    startActivityForResult(intent, CREATE_EDIT_MAINTENANCE_ITEM_REQUEST_CODE);
                } else {
                    Toast.makeText(VehicleDetailActivity.this, "All maintenance types have been created.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        FloatingActionButton editVehicleFab = (FloatingActionButton) findViewById(R.id.editFab);
        assert editVehicleFab != null;
        editVehicleFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(VehicleDetailActivity.this, EditVehicleActivity.class);
                intent.putExtra(Constants.VEHICLE_EXTRA, vehicle);
                startActivityForResult(intent, EDIT_VEHICLE_REQUEST_CODE);
            }
        });

        currentMileageTextView = (TextView)findViewById(R.id.currentMileageTextView);
        NumberFormat numberFormat = NumberFormat.getInstance();
        currentMileageTextView.setText(numberFormat.format(vehicle.getEstimatedCurrentMileage()));
        milesPerMonthTextView = (TextView)findViewById(R.id.milesPerMonthTextView);
        if(vehicle.getMilesPerMonth() != null)
            milesPerMonthTextView.setText(numberFormat.format(vehicle.getMilesPerMonth()));

        Button currentMileageButton = (Button)findViewById(R.id.currentMileageButton);
        assert currentMileageButton != null;
        currentMileageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputCurrentMileage();
            }
        });

        maintenanceItemListView = (ListView)findViewById(R.id.maintenanceItemListView);
        adapter = new MaintenanceItemAdapter(this, vehicle.getMaintenanceItems());
        assert maintenanceItemListView != null;
        maintenanceItemListView.setAdapter(adapter);

        vehiclesDataSvc = new VehicleDataService(this);

        ListViewTouchListener touchListener = new ListViewTouchListener(maintenanceItemListView);
        touchListener.setClickListener(new ListViewTouchListener.ClickListener() {
            @Override
            public void onClick(View child, int childIdx) {
                Intent intent = new Intent(VehicleDetailActivity.this, MaintenanceItemDetailActivity.class);
                MaintenanceItem maintenanceItem = vehicle.getMaintenanceItems().get(childIdx);
                intent.putExtra(Constants.MAINTENANCE_ITEM_EXTRA, maintenanceItem);
                startActivity(intent);
            }
        });
        touchListener.setDismissListener(new ListViewTouchListener.DismissListener() {
            @Override
            public void onDismiss(View child, int childIdx) {
                MaintenanceItem maintenanceItem = vehicle.getMaintenanceItems().get(childIdx);
                vehiclesDataSvc.deleteMaintenanceItem(maintenanceItem.getId());
                refreshData();
            }
        });
        maintenanceItemListView.setOnTouchListener(touchListener);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshData();
    }

    private void inputCurrentMileage() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
        String title = vehicle.getName() + " Mileage as of " + sdf.format(new Date());
        InputDialog inputDialog = new InputDialog(this, title, InputType.TYPE_CLASS_NUMBER);
        inputDialog.setOkClickListener(new OkClickListener() {
            @Override
            public void okClicked(String value) {
                if(value != null && !value.isEmpty()) {
                    int currentMileage = Integer.parseInt(value);
                    vehicle.setCurrentMileage(currentMileage);
                    int vehicleId = vehiclesDataSvc.persist(vehicle);
                    refreshData(vehicleId);
                }
            }
        });

        inputDialog.show();
    }

    private void refreshData() {
        refreshData(vehicle.getId());
    }

    private void refreshData(int vehicleId) {
        vehicle = vehiclesDataSvc.getVehicle(vehicleId);
        toolbar.setTitle(vehicle.getName());
        NumberFormat numberFormat = NumberFormat.getInstance();
        currentMileageTextView.setText(numberFormat.format(vehicle.getEstimatedCurrentMileage()));
        if(vehicle.getMilesPerMonth() != null)
            milesPerMonthTextView.setText(numberFormat.format(vehicle.getMilesPerMonth()));
        adapter.clear();
        adapter.addAll(vehicle.getMaintenanceItems());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {

            switch(requestCode) {

                case EDIT_VEHICLE_REQUEST_CODE:
                    int vehicleId = data.getIntExtra(Constants.VEHICLE_ID_EXTRA, -1);
                    if(vehicleId != -1)
                        refreshData(vehicleId);
                    else
                        Log.wtf(TAG, "No vehicle ID extra present");
                    break;

                case CREATE_EDIT_MAINTENANCE_ITEM_REQUEST_CODE:
                    refreshData();
                    break;

                default:
                    Log.wtf(TAG, "Unknown request code: " + requestCode);
            }
        }
    }
}
