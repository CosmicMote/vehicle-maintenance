package com.fowler.vehiclemaintenance;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;

import com.fowler.vehiclemaintenance.datamodel.Vehicle;
import com.fowler.vehiclemaintenance.datamodel.VehicleDataService;
import com.fowler.vehiclemaintenance.util.ListViewTouchListener;

import java.util.List;

public class VehicleListActivity extends AppCompatActivity {

    private static final int CREATE_EDIT_VEHICLE_REQUEST_CODE = 24601;

    private VehicleDataService vehiclesDataSvc;
    private ListView vehicleListView;
    private List<Vehicle> vehicles;
    private VehicleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        assert toolbar != null;
        toolbar.setLogo(R.drawable.ic_directions_car_darkgreen_24dp);
        toolbar.setTitle("Vehicles");
//        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        setSupportActionBar(toolbar);

        FloatingActionButton addFab = (FloatingActionButton) findViewById(R.id.addFab);
        assert addFab != null;
        addFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(VehicleListActivity.this, EditVehicleActivity.class);
                startActivityForResult(intent, CREATE_EDIT_VEHICLE_REQUEST_CODE);
            }
        });

        vehicleListView = (ListView)findViewById(R.id.vehicleListView);

        vehiclesDataSvc = new VehicleDataService(this);
        vehicles = vehiclesDataSvc.getVehicles();
        adapter = new VehicleAdapter(this, vehicles);

        assert vehicleListView != null;
        vehicleListView.setAdapter(adapter);

        ListViewTouchListener touchListener = new ListViewTouchListener(vehicleListView);
        touchListener.setClickListener(new ListViewTouchListener.ClickListener() {
            @Override
            public void onClick(View child, int childIdx) {
                Vehicle vehicle = vehicles.get(childIdx);
                Intent intent = new Intent(VehicleListActivity.this, VehicleDetailActivity.class);
                intent.putExtra(Constants.VEHICLE_EXTRA, vehicle);
                startActivity(intent);
            }
        });
        touchListener.setDismissListener(new ListViewTouchListener.DismissListener() {
            @Override
            public void onDismiss(View child, int childIdx) {
                Vehicle vehicle = vehicles.get(childIdx);
                vehiclesDataSvc.deleteVehicle(vehicle.getId());
                adapter.remove(vehicle);
            }
        });
        vehicleListView.setOnTouchListener(touchListener);

        NotificationManager.registerPeriodicNotifications(this);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshVehicleList();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CREATE_EDIT_VEHICLE_REQUEST_CODE && resultCode == RESULT_OK) {
            refreshVehicleList();
        }
    }

    private void refreshVehicleList() {
        vehicles.clear();
        vehicles.addAll(vehiclesDataSvc.getVehicles());
        adapter.notifyDataSetChanged();
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_vehicle_list, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
}
