package com.stfalcon.chatkit.sample.features.main;

import android.app.Application;

import com.stfalcon.chatkit.messages.MessagesListAdapter;

public class BluetoothXApplication extends Application {
    protected BluetoothChatService mBluetoothChatService;
    protected MessagesListAdapter mMessagesAdapter;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    protected void setBluetoothChatService(BluetoothChatService service) {
        mBluetoothChatService = service;
    }

    protected BluetoothChatService getBluetoothChatService() {
        return mBluetoothChatService;
    }

    protected void setMessagesAdapter(MessagesListAdapter adapter) {
        mMessagesAdapter = adapter;
    }

    protected MessagesListAdapter getMessagesAdapter() {
        return mMessagesAdapter;
    }
}
