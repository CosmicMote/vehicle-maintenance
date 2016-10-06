package com.fowler.vehiclemaintenance;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.fowler.vehiclemaintenance.datamodel.MaintenanceItem;
import com.fowler.vehiclemaintenance.datamodel.MaintenanceStatus;
import com.fowler.vehiclemaintenance.datamodel.Vehicle;
import com.fowler.vehiclemaintenance.datamodel.VehicleDataService;
import com.fowler.vehiclemaintenance.util.InputDialog;
import com.fowler.vehiclemaintenance.util.OkClickListener;

@SuppressLint("DefaultLocale")
public class MaintenanceItemDetailActivity extends AppCompatActivity {

    private VehicleDataService vehiclesDataSvc;
    private MaintenanceItem maintenanceItem;
    private TextView lastDoneTextView;
    private TextView intervalTextView;
    private TextView maintenanceItemStatusTextView;
    private ImageView maintenanceItemStatusImageView;
    private Button markAsDoneAtCurrentMileageButton;
    private Button markAsDoneAtCustomMileageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance_item_detail);

        vehiclesDataSvc = new VehicleDataService(this);

        Intent intent = getIntent();
        maintenanceItem = (MaintenanceItem) intent.getSerializableExtra(Constants.MAINTENANCE_ITEM_EXTRA);
        final Vehicle vehicle = maintenanceItem.getVehicle();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        assert toolbar != null;
        toolbar.setTitle(maintenanceItem.toString());
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

        FloatingActionButton editFab = (FloatingActionButton) findViewById(R.id.editFab);
        assert editFab != null;
        editFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MaintenanceItemDetailActivity.this, EditMaintenanceItemActivity.class);
                intent.putExtra(Constants.VEHICLE_ID_EXTRA, vehicle.getId());
                intent.putExtra(Constants.MAINTENANCE_ITEM_EXTRA, maintenanceItem);
                startActivity(intent);
            }
        });

        intervalTextView = (TextView)findViewById(R.id.intervalTextView);
        assert intervalTextView != null;
        String intervalText = String.format("%,d miles", maintenanceItem.getMileageInterval());
        intervalTextView.setText(intervalText);

        lastDoneTextView = (TextView)findViewById(R.id.lastDoneTextView);
        assert lastDoneTextView != null;
        Integer lastMileageDone = maintenanceItem.getLastMileageDone();
        String lastDoneText = lastMileageDone != null ? String.format("%,d miles", lastMileageDone) : "Never";
        lastDoneTextView.setText(lastDoneText);

        MaintenanceStatus maintenanceStatus = maintenanceItem.getMaintenanceStatus();
        maintenanceItemStatusTextView = (TextView)findViewById(R.id.maintenanceItemStatusTextView);
        assert maintenanceItemStatusTextView != null;
        maintenanceItemStatusTextView.setText(maintenanceStatus.toString(maintenanceItem.getMileageDue()));
        maintenanceItemStatusTextView.setTag(maintenanceItemStatusTextView.getTextColors());
        Integer color = maintenanceStatus.getColor();
        if(color != null)
            maintenanceItemStatusTextView.setTextColor(color);
        maintenanceItemStatusImageView = (ImageView)findViewById(R.id.maintenanceItemStatusImageView);
        assert maintenanceItemStatusImageView != null;
        maintenanceItemStatusImageView.setImageResource(maintenanceStatus.getImageResource());

        markAsDoneAtCurrentMileageButton = (Button)findViewById(R.id.markAsDoneAtCurrentMileageButton);
        assert markAsDoneAtCurrentMileageButton != null;
        final int currentMileage = vehicle.getEstimatedCurrentMileage();
        markAsDoneAtCurrentMileageButton.setText(String.format("Mark as done at %,d", currentMileage));
        markAsDoneAtCurrentMileageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMileageDone(currentMileage);
            }
        });

        markAsDoneAtCustomMileageButton = (Button)findViewById(R.id.markAsDoneAtCustomMileageButton);
        assert markAsDoneAtCustomMileageButton != null;
        markAsDoneAtCustomMileageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputMileageDone();
            }
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshData();
    }

    private void refreshData() {
        maintenanceItem = vehiclesDataSvc.getMaintenanceItem(maintenanceItem.getId());
        setTitle(maintenanceItem.toString());

        String intervalText = String.format("%,d miles", maintenanceItem.getMileageInterval());
        intervalTextView.setText(intervalText);

        Integer lastMileageDone = maintenanceItem.getLastMileageDone();
        String lastDoneText = lastMileageDone != null ? String.format("%,d miles", lastMileageDone) : "Never";
        lastDoneTextView.setText(lastDoneText);

        MaintenanceStatus maintenanceStatus = maintenanceItem.getMaintenanceStatus();
        maintenanceItemStatusTextView.setText(maintenanceStatus.toString(maintenanceItem.getMileageDue()));
        Integer color = maintenanceStatus.getColor();
        if(color != null) {
            maintenanceItemStatusTextView.setTextColor(color);
        } else {
            // When this text view was created in onCreate, the original colors were stored in the tag
            ColorStateList colors = (ColorStateList)maintenanceItemStatusTextView.getTag();
            maintenanceItemStatusTextView.setTextColor(colors);
        }
        maintenanceItemStatusImageView.setImageResource(maintenanceStatus.getImageResource());

        Vehicle vehicle = maintenanceItem.getVehicle();
        final int currentMileage = vehicle.getEstimatedCurrentMileage();
        markAsDoneAtCurrentMileageButton.setText(String.format("Mark as done at %,d", currentMileage));
    }

    private void setMileageDone(int mileageDone) {
        vehiclesDataSvc.persist(maintenanceItem.getId(), mileageDone);
        maintenanceItem.setLastMileageDone(mileageDone);
        refreshData();
    }

    private void inputMileageDone() {
        InputDialog inputDialog =
                new InputDialog(this, maintenanceItem.getType() + " Mileage", InputType.TYPE_CLASS_NUMBER);
        inputDialog.setOkClickListener(new OkClickListener() {
            @Override
            public void okClicked(String value) {
                if(value != null && !value.isEmpty()) {
                    int mileageDone = Integer.parseInt(value);
                    setMileageDone(mileageDone);
                }
            }
        });

        inputDialog.show();
    }
}
