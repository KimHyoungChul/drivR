package com.pxlweavr.drivr;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.support.v4.app.FragmentTransaction;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends FragmentActivity implements DeviceSelectScreen.BluetoothController, StreamController {
    DeviceSelectScreen deviceSelectFragment;
    DashboardScreen dashboardFragment;
    GraphScreen graphFragment;

    private TextView statusLabel;

    private TabsPagerAdapter tabAdapter;
    private ViewPager viewPager;
    private TabLayout tabLayout;

    private ArrayList<DataStream> data;
    private DataStreamDbHelper dataStreamDb;

    /**
     * Method called when the app initially starts up.  Configures UI
     * @param savedInstanceState Initial device state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        data = new ArrayList<DataStream>();
        dataStreamDb = new DataStreamDbHelper(this);
        Cursor cursor = dataStreamDb.getDataStreams();
        cursor.moveToFirst();

        //Iterate through the db and get datastreams
        while (!cursor.isAfterLast()) {
            data.add(dataStreamDb.getDataStreamAtCursor(cursor));
            cursor.moveToNext();
        }

        statusLabel = (TextView) findViewById(R.id.status_label);

        viewPager = (ViewPager) findViewById(R.id.pager);
        tabLayout = (TabLayout) findViewById(R.id.tab_layout);

        deviceSelectFragment = new DeviceSelectScreen();
        dashboardFragment = new DashboardScreen();
        graphFragment = new GraphScreen();

        tabAdapter = new TabsPagerAdapter(getSupportFragmentManager(), deviceSelectFragment, dashboardFragment, graphFragment);

        viewPager.setAdapter(tabAdapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    public DataStream createStream() {
        DataStream stream = new DataStream();
        data.add(stream);
        dataStreamDb.insertDataStream(stream);
        graphFragment.addData(stream);
        return stream;
    }

    public void selectStream(DataStream ds) {
        dashboardFragment.selectStream(ds);
    }

    public void deleteStream(DataStream ds) {
        dataStreamDb.deleteDataStream(ds);
        data.remove(ds);
        graphFragment.removeData(ds);
    }

    public void updateStream(DataStream ds) {
        dataStreamDb.insertDataStream(ds);
    }

    public ArrayList<DataStream> getStreams() {
        return data;
    }

    /**
     * Pause the app by unregistering from our Bluetooth handler
     */
    @Override
    protected void onPause() {
        // Unregister since the activity is paused.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dataReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeBT();
    }

    /**
     * When we resume, reconnect to the Bluetooth handler
     */
    @Override
    protected void onResume() {
        //dashboardFragment.addInstrument(data.get(0));
        //Register to receive Messages from our BluetoothService
        LocalBroadcastManager.getInstance(this).registerReceiver(dataReceiver, new IntentFilter("OBD_Data"));
        super.onResume();
    }

    /**
     * Connect to Bluetooth device, and start BluetoothService service
     */
    public void openBT(BluetoothDevice bd) {
        if (bd != null) {
            Intent intent = new Intent(this, BluetoothService.class);
            intent.putExtra("device", bd);
            startService(intent);

            statusLabel.setText("Bluetooth Opened");
        } else {
            showError("No Device Selected");
        }
    }

    /**
     * Stop the BluetoothService service
     */
    public void closeBT() {
        stopService(new Intent(this, BluetoothService.class));
        statusLabel.setText("Bluetooth Closed");
    }

    /**
     * The receiver for data coming from bluetooth.
     */
    private BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("receiver", "Got message");

            //Get data included in the Intent
            ArrayList<Integer> rawData = intent.getIntegerArrayListExtra("data");
            //Get timestamp
            Long time = intent.getLongExtra("time", -1);

            for (DataStream stream : data) {
                stream.addData(rawData, time);
            }

            dashboardFragment.refreshInstruments();
        }
    };

    /**
     * Display simple error message to the user
     * @param msg The string to show to the user
     */
     @Override
     public void showError(String msg) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }
}