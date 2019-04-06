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

import android.bluetooth.BluetoothDevice;

import com.stfalcon.chatkit.sample.common.data.model.Dialog;
import com.stfalcon.chatkit.sample.common.data.model.Message;
import com.stfalcon.chatkit.sample.common.data.model.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class DialogHandler {
    // Class tag for logger output
    private static final String TAG = "DialogHandler";
    public void DialogHandler() {}

    // Calender instance for dialog message time display
    private Calendar mCalendar;

    public Dialog getDialogFromDevice(BluetoothDevice device) {
        // Get calendar instance and the date now
        mCalendar = Calendar.getInstance();
        Date date = mCalendar.getTime();

        // Get corresponding user information from bluetooth device
        ArrayList<User> users = getUsersFromDevice(device);
        // Get empty message
        Message emptyMessage = getEmptyMessage(date, users, device);
        return new Dialog(
                device.getAddress(),
                device.getName(),
                "no photo",
                users,
                emptyMessage,
                0
                );
    }

    private ArrayList<User> getUsersFromDevice(BluetoothDevice device) {
        ArrayList<User> arrayList = new ArrayList<>();
        arrayList.add(new User(
                device.getAddress(),
                device.getName(),
                "no avatar",
                true
        ));

        return arrayList;
    }

    private Message getEmptyMessage(Date date, ArrayList<User> users, BluetoothDevice device) {
        return new Message(
                device.getAddress(),
                users.get(0),
                "",
                date
        );
    }
}
