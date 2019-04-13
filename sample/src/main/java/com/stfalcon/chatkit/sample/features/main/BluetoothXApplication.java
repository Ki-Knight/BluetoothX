package com.stfalcon.chatkit.sample.features.main;

import android.app.Application;
import android.os.Handler;

import com.stfalcon.chatkit.dialogs.DialogsListAdapter;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

public class BluetoothXApplication extends Application {
    protected BluetoothChatService mBluetoothChatService;
    protected MessagesListAdapter mMessagesAdapter;
    protected DialogsListAdapter mDialogsAdapter;
    protected MessageHandler mMessageHandler;
    protected Handler mHandler;

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

    protected void setDialogsAdapter(DialogsListAdapter adapter) {
        mDialogsAdapter = adapter;
    }

    protected DialogsListAdapter geetDialogsAdapter() {
        return mDialogsAdapter;
    }

    protected void setMessageHandler (MessageHandler handler) {
        mMessageHandler = handler;
    }

    protected MessageHandler getMessageHandler () {
        return mMessageHandler;
    }

    protected void setHandler(Handler handler) {
        mHandler = handler;
    }

    protected Handler getHandler() {
        return mHandler;
    }
}
