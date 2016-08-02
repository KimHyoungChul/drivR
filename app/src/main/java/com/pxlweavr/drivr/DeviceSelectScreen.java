package com.pxlweavr.drivr;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

public class DeviceSelectScreen extends Fragment {
    BluetoothController btCallback;

    /**
     * Interface to the Main activity to run Bluetooth
     */
    public interface BluetoothController {
        public void openBT(BluetoothDevice bd);
        public void closeBT();
        public void showError(String msg);
    }

    private ListView deviceSelector;
    private TextView statusLabel;
    ArrayList<BluetoothDevice> devicesList;

    /**
     * Called when the fragment attaches to an Activity
     * @param activity The activity we just attached to
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        btCallback = (BluetoothController) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Load the appropriate layout
        View rootView = inflater.inflate(R.layout.activity_device_select_screen, container, false);

        //SHow device list
        deviceSelector = (ListView) rootView.findViewById(R.id.device_selector);

        //Connect buttons to listeners
        Button connectButton = (Button) rootView.findViewById(R.id.connect_button);
        Button disconnectButton = (Button) rootView.findViewById(R.id.disconnect_button);
        Button scanButton = (Button) rootView.findViewById(R.id.scan_button);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Do we have something valid selected?
                if (deviceSelector.getCheckedItemCount() == 1) {
                    int devicePos = deviceSelector.getCheckedItemPosition();
                    BluetoothDevice device = devicesList.get(devicePos);

                    btCallback.closeBT();
                    btCallback.openBT(device);
                } else {
                    btCallback.showError("No Device Selected");
                }
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btCallback.closeBT();
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanForDevices();
            }
        });

        scanForDevices();

        return rootView;
    }

    /**
     * Scan for all paired Bluetooth Devices and show them on the device list
     */
    private void scanForDevices() {
        //Adapter we'll be scanning with
        BluetoothAdapter obdAdapter = BluetoothAdapter.getDefaultAdapter();

        //If we don't have any bluetooth, we are sad :(
        if (obdAdapter == null) {
            statusLabel.setText("ERROR: No Bluetooth Available");
        }

        //Enable bluetooth if it wasn't on already
        if (!obdAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        //Get list of devices
        Set<BluetoothDevice> devicesSet = obdAdapter.getBondedDevices();
        //COnvert to arraylist for display
        devicesList = new ArrayList<BluetoothDevice>();
        devicesList.addAll(devicesSet);

        BluetoothDeviceAdapter arrayAdapter = new BluetoothDeviceAdapter(getActivity(), devicesList);
        deviceSelector.setAdapter(arrayAdapter);
    }
}
