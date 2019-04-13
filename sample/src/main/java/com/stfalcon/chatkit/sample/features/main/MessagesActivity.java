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
import com.stfalcon.chatkit.sample.utils.AppUtils;

import java.util.ArrayList;

public class MessagesActivity extends DemoMessagesActivity
        implements MessageInput.InputListener,
        MessageInput.AttachmentsListener,
        MessageInput.TypingListener {

    public static void open(Context context, String address) {
        Intent intent = new Intent(context, MessagesActivity.class);
        intent.putExtra("address", address);
        context.startActivity(intent);
    }

    private MessagesList messagesList;
    // Bluetooth chat helper
    private BluetoothChatService mBluetoothChatService;
    private ArrayList<Message> mMessageHistory;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messages_activity);

        Intent intent = getIntent();
        String address = intent.getStringExtra("address");

        this.messagesList = (MessagesList) findViewById(R.id.messagesList);
        initAdapter();

        MessageInput input = (MessageInput) findViewById(R.id.input);
        input.setInputListener(this);
        input.setTypingListener(this);
        input.setAttachmentsListener(this);

        BluetoothXApplication app = (BluetoothXApplication)getApplication();
        mBluetoothChatService = app.getBluetoothChatService();
        app.setMessagesAdapter(messagesAdapter);

        mHandler = app.getHandler();

        mMessageHistory = app.getMessageHandler().getHistory(address);
        if (mMessageHistory != null) {
            loadMessageHistory();
        }
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
                    AppUtils.showToast(MessagesActivity.this,
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        android.os.Message msg = mHandler.obtainMessage(DeviceListActivity.DIALOG_DISMISS);
        mHandler.sendMessage(msg);
    }

    private void loadMessageHistory() {
        for (Message message : mMessageHistory) {
            messagesAdapter.addToStart(message, false);
        }
    }
}
