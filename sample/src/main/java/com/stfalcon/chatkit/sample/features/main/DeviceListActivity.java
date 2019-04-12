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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.avast.android.dialogs.fragment.ProgressDialogFragment;
import com.avast.android.dialogs.fragment.SimpleDialogFragment;
import com.avast.android.dialogs.iface.ISimpleDialogCancelListener;
import com.avast.android.dialogs.iface.ISimpleDialogListener;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnItemClickListener;
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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.avast.android.dialogs.iface.ISimpleDialogCancelListener;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class DeviceListActivity extends DemoDialogsActivity {
    // Activity tag for logger
    private static final String TAG = "DeviceListActivity";

    protected static final String TOAST = "toast";
    protected static final String BUFFER = "buffer";
    protected static final String ADDRESS = "address";

    protected static final int CONNECT_RESULT = 0;
    protected static final int MESSAGE_WRITE = 1;
    protected static final int MESSAGE_READ = 2;
    protected static final int MESSAGE_TOAST = 4;

    // Request to enable bluetooth constant
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_DISCOVERABLE = 2;

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
    private MenuItem mScanItem;
    private BluetoothChatProtocol mProtocal;

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
                mDialogsAdapter.addItem(mDialogHandler.getDialogFromDevice(device));
                mDeviceMap.put(device.getAddress(), device);
                //mBluetoothChatService
            }
        }

        // Register remote bluetooth device found receiver
        mBroadcastReceiver = new BluetoothReceiver();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBroadcastReceiver, filter);

        // Register discovery finished receiver
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mBroadcastReceiver, filter);

        mBluetoothChatService = new BluetoothChatService(this, mBluetoothAdapter,
                mDialogsAdapter, mHandler);

        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                if (mBluetoothAdapter == null) {
                    SweetAlertDialog pDialog = new SweetAlertDialog(DeviceListActivity.this,
                            SweetAlertDialog.ERROR_TYPE);
                    pDialog.setTitleText("Error");
                    pDialog.setContentText("Bluetooth not available on this device!");
                    pDialog.setConfirmText("confirm");
                    pDialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {

                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            finish();
                        }
                    });
                    pDialog.show();
                }
                else {
                    // Make the device discoverable to remote bluetooth devices
                    makeDiscoverable();
                    mBluetoothChatService.start();
                }
            }
        }, 1000);
    }

    @Override
    public void onDialogClick(Dialog dialog) {
        // Display progress bar while connecting to remote device
        String address = dialog.getId();
        BluetoothDevice device = mDeviceMap.get(address);

        if (device == null) {
            Log.d(TAG, "Clicked device does not exist!");
            Toast.makeText(this, "Clicked device does not exist!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ConnectAsyncTask connectAsyncTask = new ConnectAsyncTask();
        connectAsyncTask.execute(device);
        // Get the instance of device of the clicked dialog
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.device_list_menu, menu);

        mScanItem = menu.findItem(R.id.scan);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan:
                mScanItem = item;
                if (item.getTitle() == getString(R.string.scan_option)) {
                    mScanItem.setTitle(R.string.stop_scan_option);
                    setProgressBarIndeterminateVisibility(true);
                    // Start discovery
                    doDiscovery();
                }
                else if (item.getTitle() == getString(R.string.stop_scan_option)) {
                    // Cancel discovery process
                    mBluetoothAdapter.cancelDiscovery();

                    // Dismiss progressbar, witch the title to scan
                    mScanItem.setTitle(getString(R.string.scan_option));
                }
                break;
            case R.id.about:
                Intent intent = new Intent(this, AboutPageActivity.class);
                startActivity(intent);
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
        mBluetoothChatService.stop();
    }

    private void initAdapter() {
        mDialogsAdapter = new DialogsListAdapter<>(imageLoader);
        //mDialogsAdapter.setItems(DialogsFixtures.getDialogs());
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
                    mDialogsAdapter.addItem(mDialogHandler.getDialogFromDevice(newDevice));
                    // Add new device to the list
                    mDeviceMap.put(newDevice.getAddress(), newDevice);
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mScanItem.setTitle(R.string.scan_option);
            }
        }
    }

    // Make the device discoverable to remote bluetooth devices
    protected void makeDiscoverable() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivityForResult(intent, REQUEST_DISCOVERABLE);
    }

    private void doDiscovery() {
        if (mBluetoothAdapter.isDiscovering()) { mBluetoothAdapter.cancelDiscovery(); }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBlutooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBlutooth, REQUEST_ENABLE_BT);
        }
        else {
            mBluetoothAdapter.startDiscovery();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                mBluetoothAdapter.startDiscovery();
                //setProgressBarVisibility(true);
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, "Bluetooth service not enabled!",
                        Toast.LENGTH_SHORT).show();
                mScanItem.setTitle(R.string.scan_option);
                setProgressBarIndeterminateVisibility(false);
            }
            break;
        case REQUEST_DISCOVERABLE:
            if (resultCode != Activity.RESULT_CANCELED) {
                Toast.makeText(this, "This device is now discoverable!",
                        Toast.LENGTH_SHORT).show();
                mBluetoothChatService.start();
            } else {
                Log.d(TAG, "Device not discoverable");
                Toast.makeText(this, "Request for discoverability denied!",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDialogLongClick(Dialog dialog) {
        AppUtils.showToast(
                this,
                dialog.getDialogName(),
                false);
    }

    private class ConnectAsyncTask extends AsyncTask<BluetoothDevice, Boolean, Void> {

        private SweetAlertDialog mmProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Display progress dialog
            mmProgressDialog = new SweetAlertDialog(DeviceListActivity.this,
                    SweetAlertDialog.PROGRESS_TYPE);
            mmProgressDialog.setTitleText("connecting");
            mmProgressDialog.setContentText("Please wait");
            mmProgressDialog.setCancelable(false);
            mmProgressDialog.getProgressHelper().setBarColor(R.color.material_blue_grey_80);
            mmProgressDialog.show();
//            mProgressDialog = ProgressDialogFragment.createBuilder(DeviceListActivity.this,
//                    getSupportFragmentManager())
//                    .setCancelableOnTouchOutside(false)
//                    .setTitle("Connecting")
//                    .setMessage("Please wait")
//                    .show();
        }

        @Override
        protected Void doInBackground(BluetoothDevice... device) {
            boolean result = mBluetoothChatService.onDialogsItemClicked(device[0]);
            onProgressUpdate(result);

            return null;
        }

        @Override
        protected void onProgressUpdate(Boolean... values) {
            mmProgressDialog.dismiss();

            if (values[0]) {
                MessagesActivity.open(DeviceListActivity.this);
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(android.os.Message msg) {
            switch(msg.what) {
            case MESSAGE_READ:
//                Bundle bundle = msg.getData();
//                byte[] readBuf = bundle.getByteArray(BUFFER);
//                String address = bundle.getString(ADDRESS);
//
//                Date date =
//
//                BluetoothDevice device = mDeviceMap.get(address);
//                Object msgo = mProtocal.getMessageFromByteArray(readBuf);
//                if (msgo instanceof String) {
//                    Message message = new Message(
//                            address,
//
//                    );
//                }
//                mBluetoothChatService.onMessageReceived(message, device);
                break;
            case MESSAGE_WRITE:
                break;
            case MESSAGE_TOAST:
                String content = msg.getData().getString(DeviceListActivity.TOAST);
                Toast.makeText(DeviceListActivity.this, content,
                        Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
}
