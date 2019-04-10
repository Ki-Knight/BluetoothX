/*
 * Copyright (c) 2018-2019 Shrowshoo Young All Rights Reserved
 *
 * This programme is developed as free software for the Development of
 * bluetooth chat application. Redistribution or modification of it is
 * allowed under the terms of the GNU General Public Licence published by the
 * Free Software Foundation, either version 3 or later version.
 *
 * Redistribution and use in source or executable programme, with or without
 * modification is permitted provided that the following conditions are met:
 *
 * 1. Redistribution in the form of source code with the copyright notice
 *    above, the conditions and following disclaimer retained.
 *
 * 2. Redistribution in the form of executable programme must reproduce the
 *    copyright notice, conditions and following disclaimer in the
 *    documentation and\or other literal materials provided in the distribution.
 *
 * This is an unoptimized software designed to meet the requirements of the
 * processing pipeline. No further technical support is guaranteed.
 * */
package com.stfalcon.chatkit.sample.features.main;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.avast.android.dialogs.fragment.ProgressDialogFragment;
import com.avast.android.dialogs.fragment.SimpleDialogFragment;
import com.squareup.picasso.Picasso;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.dialogs.DialogsList;
import com.stfalcon.chatkit.dialogs.DialogsListAdapter;
import com.stfalcon.chatkit.sample.R;
import com.stfalcon.chatkit.sample.common.data.fixtures.DialogsFixtures;
import com.stfalcon.chatkit.sample.common.data.model.Dialog;
import com.stfalcon.chatkit.sample.common.data.model.Message;
import com.stfalcon.chatkit.sample.features.demo.DemoDialogsActivity;
import com.stfalcon.chatkit.sample.utils.AppUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DeviceListActivity extends DemoDialogsActivity {
    // Activity tag for logger
    private static final String TAG = "DeviceListActivity";

    // Request to enable bluetooth constant
    private static final int REQUEST_ENABLE_BT = 1;

    // Bluetooth default adapter
    private BluetoothAdapter mBluetoothAdapter;
    // Scan broadcast listener
    private BluetoothReceiver mBroadcastReceiver;

    protected ImageLoader imageLoader;
    // Dialog list view
    private DialogsList mDialogsListView;
    // Dialog Adapter
    private DialogsListAdapter<Dialog> mDialogsAdapter;

    // Dialog generator
    private DialogHandler mDialogHandler;

    // Bluetooth chat interface
    private BluetoothChatService mBluetoothChatService;

    private Map<String, BluetoothDevice> mDeviceMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_list);

        imageLoader = new ImageLoader() {

            @Override
            public void loadImage(ImageView imageView, String url, Object payload) {
                Picasso.with(DeviceListActivity.this).load(url).into(imageView);
            }
        };

        // Get instance of DialogHandler
        mDialogHandler = new DialogHandler();
        // Get instance of device list
        mDeviceMap = new HashMap<String, BluetoothDevice>();

        // Set dialogs list view
        mDialogsListView = (DialogsList) findViewById(R.id.devicelist);
        initAdapter();

        // Assign default bluetooth adapter and start discovery
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Add paired devices to dialog list adapter
        if (mBluetoothAdapter != null) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                onNewDialog(mDialogHandler.getDialogFromDevice(device));
                mDeviceMap.put(device.getAddress(), device);
            }
        }

        // Register remote bluetooth device found receiver
        mBroadcastReceiver = new BluetoothReceiver();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBroadcastReceiver, filter);

        // Register discovery finished receiver
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mBroadcastReceiver, filter);

        // Get instance of bluetooth chat service
        mBluetoothChatService = new BluetoothChatService(mBluetoothAdapter, mDialogsAdapter);
    }

    @Override
    public void onDialogClick(Dialog dialog) {
        // Display progress bar while connecting to remote device
        android.support.v4.app.DialogFragment progressBar =
                ProgressDialogFragment.createBuilder(DeviceListActivity.this,
                        getSupportFragmentManager())
                .setCancelableOnTouchOutside(true)
                .setMessage("Connecting")
                .show();

        // Get the instance of device of the clicked dialog
        String address = dialog.getId();
        BluetoothDevice device = mDeviceMap.get(address);

        try {
            mBluetoothChatService.connect(device);
        } catch (Exception e) {
            // Connection failed, display connection fail notice to user
            progressBar.dismiss();
            Toast.makeText(this, "Connection failed, please try later!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Connection succeeded
        progressBar.dismiss();
        MessagesActivity.open(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.device_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case R.id.scan:
                if (item.getTitle() == getString(R.string.scan_option)) {
                    if (mBluetoothAdapter == null) {
                        // Display alert dialog
                        SimpleDialogFragment.createBuilder(this,
                                getSupportFragmentManager())
                                .setTitle("Error")
                                .setMessage("Bluetooth service is not available!")
                                .setNegativeButtonText("Confirm")
                                .show();
                        Log.d(TAG, "Bluetooth service is not available");
                        // Dismiss the main activity
                        break;
                    }

                    // Make the device discoverable to remote bluetooth devices
                    makeDiscoverable();
                    // Start discovery
                    runDiscovery();

                    // Display progressbar and switch the title of option to stop
                    setProgressBarVisibility(true);
                    item.setTitle(getString(R.string.stop_scan_option));
                }
                else if (item.getTitle() == getString(R.string.stop_scan_option)) {
                    // Cancel discovery process
                    mBluetoothAdapter.cancelDiscovery();

                    // Dismiss progressbar, witch the title to scan
                    setProgressBarVisibility(false);
                    item.setTitle(getString(R.string.scan_option));
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Cancel preceeding discovering
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // unregister broadcast receiver
        unregisterReceiver(mBroadcastReceiver);
    }

    private void initAdapter() {
        mDialogsAdapter = new DialogsListAdapter<>(imageLoader);
        mDialogsAdapter.setItems(DialogsFixtures.getDialogs());
        //onNewDialog(getDemoDialog());

        mDialogsAdapter.setOnDialogClickListener(this);
        mDialogsAdapter.setOnDialogLongClickListener(this);

        mDialogsListView.setAdapter(mDialogsAdapter);
    }

    //for example
    private void onNewMessage(String dialogId, Message message) {
        boolean isUpdated = mDialogsAdapter.updateDialogWithMessage(dialogId, message);
        if (!isUpdated) {
            //Dialog with this ID doesn't exist, so you can create new Dialog or update all dialogs list
        }
    }

    //for example
    private void onNewDialog(Dialog dialog) {
        mDialogsAdapter.addItem(dialog);
    }

    // Bluetooth broadcast receiver
    protected class BluetoothReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Inform user that a remote device has been discovered
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Display remote device information
                BluetoothDevice newDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Add a new dialog if new device is found
                if (newDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                    onNewDialog(mDialogHandler.getDialogFromDevice(newDevice));
                    // Add new device to the list
                    mDeviceMap.put(newDevice.getAddress(), newDevice);
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            }
        }
    }

    // Make the device discoverable to remote bluetooth devices
    protected void makeDiscoverable() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        startActivity(intent);
    }

    // Bluetooth adapter discovery caller
    private void runDiscovery() {
        // This device support bluetooth service, but is disabled
        if (mBluetoothAdapter.isEnabled()) {
            Intent enableBlutooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBlutooth, REQUEST_ENABLE_BT);
        }

        Toast.makeText(DeviceListActivity.this,
                "scanning", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Start scanning");

        // Cancel proceeding discovery procedure
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        mBluetoothAdapter.startDiscovery();
    }

    @Override
    public void onDialogLongClick(Dialog dialog) {
        AppUtils.showToast(
                this,
                dialog.getDialogName(),
                false);
    }
}
