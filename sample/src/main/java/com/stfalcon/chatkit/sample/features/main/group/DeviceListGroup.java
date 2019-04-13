package com.stfalcon.chatkit.sample.features.main.group;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.dialogs.DialogsList;
import com.stfalcon.chatkit.dialogs.DialogsListAdapter;
import com.stfalcon.chatkit.sample.R;
import com.stfalcon.chatkit.sample.common.data.model.Dialog;
import com.stfalcon.chatkit.sample.features.demo.DemoDialogsActivity;
import com.stfalcon.chatkit.sample.features.main.AboutPageActivity;
import com.stfalcon.chatkit.sample.features.main.BluetoothChatProtocol;
import com.stfalcon.chatkit.sample.features.main.BluetoothXApplication;
import com.stfalcon.chatkit.sample.features.main.DeviceListActivity;
import com.stfalcon.chatkit.sample.features.main.DialogHandler;
import com.stfalcon.chatkit.sample.features.main.MessagesActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class DeviceListGroup extends DemoDialogsActivity {

    private static final String TAG = "DeviceListGroup";

    protected static final String TOAST = "toast";
    protected static final String BUFFER = "buffer";
    protected static final String ADDRESS = "address";

    protected static final int NEW_CONNECTION = 0;
    protected static final int MESSAGE_WRITE = 1;
    protected static final int MESSAGE_READ = 2;
    protected static final int MESSAGE_TOAST = 4;
    protected static final int DIALOG_DISMISS = 5;

    // Request to enable bluetooth constant
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_DISCOVERABLE = 2;

    private MenuItem mScanItem;
    private BluetoothAdapter mBluetoothAdapter;
    private DialogsListAdapter<Dialog> mDialogsAdapter;
    private DialogsList mDialogsList;
    private ImageLoader imageLoader;
    private Map<String, BluetoothDevice> mDeviceMap;
    private ChatServiceGroup mBluetoothChatService;
    private BluetoothXApplication mApp;
    private Handler mHandler;
    private DialogHandler mDialogHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_list);

        imageLoader = new ImageLoader() {

            @Override
            public void loadImage(ImageView imageView, String url, Object payload) {
                Picasso.with(DeviceListGroup.this).load(url).into(imageView);
            }
        };

        mDialogHandler = new DialogHandler();
        mDialogsList = (DialogsList) findViewById(R.id.devicelist);
        initAdapter();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mDeviceMap = new HashMap<String,BluetoothDevice>();
        if (mBluetoothAdapter != null) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                mDialogsAdapter.addItem(mDialogHandler.getDialogFromDevice(device));
                mDeviceMap.put(device.getAddress(), device);
                //mBluetoothChatService
            }
        }

        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                if (mBluetoothAdapter == null) {
                    SweetAlertDialog pDialog = new SweetAlertDialog(DeviceListGroup.this,
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
        mApp.setHandler(mHandler);
    }

    @Override
    public void onDialogClick(Dialog dialog) {
        // Display progress bar while connecting to remote device
        String address = dialog.getId();
        dialog.setUnreadCount(0);
        mDialogsAdapter.updateItemById(dialog);
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
                } else if (item.getTitle() == getString(R.string.stop_scan_option)) {
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

    private void initAdapter() {
        mDialogsAdapter = new DialogsListAdapter<>(imageLoader);
        //mDialogsAdapter.setItems(DialogsFixtures.getDialogs());
        //onNewDialog(getDemoDialog());

        mDialogsAdapter.setOnDialogClickListener(this);
        mDialogsAdapter.setOnDialogLongClickListener(this);

        mDialogsList.setAdapter(mDialogsAdapter);
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

    private class ConnectAsyncTask extends AsyncTask<BluetoothDevice, Boolean, Void> {

        private SweetAlertDialog mmProgressDialog;
        private BluetoothDevice mmDevice;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Display progress dialog
            mmProgressDialog = new SweetAlertDialog(DeviceListGroup.this,
                    SweetAlertDialog.PROGRESS_TYPE);
            mmProgressDialog.setTitleText("connecting");
            mmProgressDialog.setContentText("Please wait");
            mmProgressDialog.setCancelable(false);
            mmProgressDialog.getProgressHelper().setBarColor(R.color.material_blue_grey_80);
            mmProgressDialog.show();
        }

        @Override
        protected Void doInBackground(BluetoothDevice... device) {
            mmDevice = device[0];
            boolean result = mBluetoothChatService.onDialogsItemClicked(device[0]);
            onProgressUpdate(result);

            return null;
        }

        @Override
        protected void onProgressUpdate(Boolean... values) {
            mmProgressDialog.dismiss();

            if (values[0]) {
                android.os.Message msg = mHandler.obtainMessage(NEW_CONNECTION);
                mHandler.sendMessage(msg);

                MessagesActivity.open(DeviceListGroup.this, mmDevice.getAddress());
            }
        }
    }
}
