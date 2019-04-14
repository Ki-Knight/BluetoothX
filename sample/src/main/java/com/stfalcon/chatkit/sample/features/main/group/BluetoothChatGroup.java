package com.stfalcon.chatkit.sample.features.main.group;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.stfalcon.chatkit.sample.R;
import com.stfalcon.chatkit.sample.common.data.fixtures.MessagesFixtures;
import com.stfalcon.chatkit.sample.common.data.model.Message;
import com.stfalcon.chatkit.sample.features.demo.DemoMessagesActivity;
import com.stfalcon.chatkit.sample.features.main.AboutPageActivity;
import com.stfalcon.chatkit.sample.features.main.BluetoothChatProtocol;
import com.stfalcon.chatkit.sample.features.main.BluetoothXApplication;
import com.stfalcon.chatkit.sample.features.main.DeviceListActivity;
import com.stfalcon.chatkit.sample.features.main.DialogHandler;
import com.stfalcon.chatkit.sample.features.main.MessagesActivity;
import com.stfalcon.chatkit.sample.utils.AppUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;
import mehdi.sakout.aboutpage.AboutPage;

public class BluetoothChatGroup extends DemoMessagesActivity implements MessageInput.InputListener,
        MessageInput.AttachmentsListener,
        MessageInput.TypingListener {

    private static final String TAG = "BluetoothChatGroup";

    private DialogHandler mDialogHandler;
    private BluetoothChatProtocol mProtocol;
    private MessagesList messagesList;
    // Bluetooth chat helper
    private ChatServiceGroup mBluetoothChatService;
    private ArrayList<Message> mMessageHistory;
    private Map<String, BluetoothDevice> mDeviceMap;
    private BluetoothXApplication mApp;
    private BluetoothAdapter mBluetoothAdapter;

    public static void open(Context context) {
        Intent intent = new Intent(context, BluetoothChatGroup.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messages_activity);

        this.messagesList = (MessagesList) findViewById(R.id.messagesList);
        initAdapter();

        MessageInput input = (MessageInput) findViewById(R.id.input);
        input.setInputListener(this);
        input.setTypingListener(this);
        input.setAttachmentsListener(this);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                if (mBluetoothAdapter == null) {
                    SweetAlertDialog pDialog = new SweetAlertDialog(BluetoothChatGroup.this,
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

        mApp = (BluetoothXApplication) getApplication();
        mDialogHandler = new DialogHandler();
        mProtocol = new BluetoothChatProtocol();
        mDeviceMap = new HashMap<>();
        mBluetoothChatService = new ChatServiceGroup(mApp, mBluetoothAdapter, mHandler);

        mApp.setHandler(mHandler);
        mApp.setChatServiceGroup(mBluetoothChatService);
        mApp.setMessagesAdapter(messagesAdapter);
    }

    @Override
    public boolean onSubmit(CharSequence input) {
        String content = input.toString();
        mBluetoothChatService.onTextMessageSubmit(content);
//        super.messagesAdapter.addToStart(
//                MessagesFixtures.getTextMessage(input.toString()), true);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.group_chat_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_device:
                startActivity(new Intent(this, DeviceListGroup.class));
                break;
            case R.id.about_group_chat:
                startActivity(new Intent(this, AboutPageActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAddAttachments() {
        super.messagesAdapter.addToStart(
                MessagesFixtures.getImageMessage(), true);
    }

    private void initAdapter() {
        super.messagesAdapter = new MessagesListAdapter<>(super.senderId, super.imageLoader);
        super.messagesAdapter.enableSelectionMode(this);
        super.messagesAdapter.setLoadMoreListener(this);
        super.messagesAdapter.registerViewClickListener(R.id.messageUserAvatar,
                new MessagesListAdapter.OnMessageViewClickListener<Message>() {

                    @Override
                    public void onMessageViewClick(View view, Message message) {
                        AppUtils.showToast(BluetoothChatGroup.this,
                                message.getUser().getName() + " avatar click",
                                false);
                    }
                });
        this.messagesList.setAdapter(super.messagesAdapter);
    }

    @Override
    public void onStartTyping() {
        Log.v("Typing listener", getString(R.string.start_typing_status));
    }

    @Override
    public void onStopTyping() {
        Log.v("Typing listener", getString(R.string.stop_typing_status));
    }

    @SuppressLint("HandlerLeak")
    protected Handler mHandler = new Handler() {

        @Override
        public void handleMessage(android.os.Message msg) {
            switch(msg.what) {
            case DeviceListGroup.NEW_CONNECTION:
                mBluetoothChatService.clearRedundantConnection();
                break;
            case DeviceListGroup.MESSAGE_READ:
                Bundle bundle = msg.getData();
                byte[] readBuf = bundle.getByteArray(DeviceListGroup.BUFFER);
                String address = bundle.getString(DeviceListGroup.ADDRESS);

                Date date = Calendar.getInstance().getTime();

                BluetoothDevice device = mDeviceMap.get(address);
                Object msgo = mProtocol.getMessageFromByteArray(readBuf);
                if (msgo instanceof String) {
                    Message message = mDialogHandler.getTextMessageFromString((String) msgo,
                            device.getAddress(),
                            mDialogHandler.getUsersFromDevice(device, false), date);
                    mBluetoothChatService.onMessageReceived(message);
                }
                mBluetoothChatService.socketQueueReorder(address);
                break;
            case DeviceListGroup.MESSAGE_WRITE:
                break;
            case DeviceListGroup.MESSAGE_TOAST:
                String content = msg.getData().getString(DeviceListGroup.TOAST);
                Toast.makeText(BluetoothChatGroup.this, content,
                        Toast.LENGTH_SHORT).show();
                break;
            case DeviceListGroup.NEW_DEVICE:
                BluetoothDevice newDevice = msg.getData().getParcelable(DeviceListGroup.DEVICE);
                mDeviceMap.put(newDevice.getAddress(), newDevice);
                break;
            }
        }
    };

    protected void makeDiscoverable() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivityForResult(intent, DeviceListGroup.REQUEST_DISCOVERABLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case DeviceListGroup.REQUEST_ENABLE_BT:
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
            }
            break;
        case DeviceListGroup.REQUEST_DISCOVERABLE:
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
}
