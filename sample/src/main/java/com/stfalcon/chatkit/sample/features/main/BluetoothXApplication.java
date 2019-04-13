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

    public BluetoothChatService getBluetoothChatService() {
        return mBluetoothChatService;
    }

    public void setMessagesAdapter(MessagesListAdapter adapter) {
        mMessagesAdapter = adapter;
    }

    public MessagesListAdapter getMessagesAdapter() {
        return mMessagesAdapter;
    }

    public void setDialogsAdapter(DialogsListAdapter adapter) {
        mDialogsAdapter = adapter;
    }

    public DialogsListAdapter geetDialogsAdapter() {
        return mDialogsAdapter;
    }

    public void setMessageHandler (MessageHandler handler) {
        mMessageHandler = handler;
    }

    public MessageHandler getMessageHandler () {
        return mMessageHandler;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public void clear() {
        mBluetoothChatService = null;
        mMessageHandler = null;
        mDialogsAdapter = null;
        mMessageHandler = null;
        mHandler = null;
    }
}
