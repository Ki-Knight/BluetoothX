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

import android.support.annotation.Nullable;
import android.util.Log;

import com.stfalcon.chatkit.sample.common.data.model.Message;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MessageHandler {
    // Activity tag for logger output
    private static final String TAG = "MessageHandler";

    private Map<String, ArrayList<Message>> mMessageHistory;

    protected MessageHandler() {
        // Get instance of mMessageHistory
        mMessageHistory = new HashMap<String, ArrayList<Message>>();
    }

    protected void addHistory(String address, Message message) {
        // Get target message history
        ArrayList<Message> messages = mMessageHistory.get(address);
        if (messages == null) {
            Log.d(TAG, "Target message history does not exist!");
            return;
        }

        // Add a new message item to message history
        messages.add(message);
    }

    public ArrayList<Message> getHistory(String address) {
        // Get target message history
        ArrayList<Message> messages = mMessageHistory.get(address);
        // If no target matches corresponding key
        if (messages == null) {
            Log.d(TAG, "Target message history does not exist!");
            return null;
        }

        return messages;
    }

    protected void addNewHistory(String address, @Nullable Message message) {
        // Check whether history already in the map
        if (mMessageHistory.containsKey(address)) {
            Log.d(TAG, "Target device already in the history!");
            return;
        }
        // Add a new history
        mMessageHistory.put(address, new ArrayList<Message>());

        if (message != null) {
            ArrayList<Message> history = mMessageHistory.get(address);
            history.add(message);
        }
    }
}
