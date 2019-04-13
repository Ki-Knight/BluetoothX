package com.stfalcon.chatkit.sample.features.main.group;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.stfalcon.chatkit.sample.R;
import com.stfalcon.chatkit.sample.common.data.fixtures.MessagesFixtures;
import com.stfalcon.chatkit.sample.common.data.model.Message;
import com.stfalcon.chatkit.sample.features.demo.DemoMessagesActivity;
import com.stfalcon.chatkit.sample.features.main.BluetoothChatProtocol;
import com.stfalcon.chatkit.sample.features.main.DialogHandler;
import com.stfalcon.chatkit.sample.features.main.MessagesActivity;
import com.stfalcon.chatkit.sample.utils.AppUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class BluetoothChatGroup extends DemoMessagesActivity implements MessageInput.InputListener,
        MessageInput.AttachmentsListener,
        MessageInput.TypingListener {

    private DialogHandler mDialogHandler;
    private BluetoothChatProtocol mProtocol;
    private MessagesList messagesList;
    // Bluetooth chat helper
    private ChatServiceGroup mBluetoothChatService;
    private ArrayList<Message> mMessageHistory;
    private Map<String, BluetoothDevice> mDeviceMap;

    public static void open(Context context, String address) {
        Intent intent = new Intent(context, MessagesActivity.class);
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

        mDialogHandler = new DialogHandler();
        mProtocol = new BluetoothChatProtocol();
        mDeviceMap = new HashMap<>();
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

    private void loadMessageHistory() {
        for (Message message : mMessageHistory) {
            messagesAdapter.addToStart(message, false);
        }
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
            }
        }
    };
}
