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

import android.util.Base64;

import com.stfalcon.chatkit.sample.common.data.model.Message;

public class BluetoothChatProtocol {
    private static final String TAG = "BluetoothChatProtocol";

    private static final int MESSAGE_TYPE_TEXT = 0;
    private static final int MESSAGE_TYPE_IMAGE = 1;

    protected BluetoothChatProtocol() { }

    private int byteArray2Integer(byte[] buffer, int index) {
        return  buffer[index + 3] & 0xFF |
                (buffer[index + 2] & 0xFF) << 8 |
                (buffer[index + 1] & 0xFF) << 16 |
                (buffer[index] & 0xFF) << 24;
    }

    private void integer2ByteArray(int value, byte[] buffer, int index) {
        for (int i = 0; i < 4; ++i) {
            buffer[index + i] = (byte)((value >> (24 - 8*i)) & 0XFF);
        }
    }

    private byte[] integer2ByteArray(int value) {
        return new byte[]{
                (byte)((value >> 24) & 0XFF),
                (byte)((value >> 16) & 0XFF),
                (byte)((value >> 8) & 0XFF),
                (byte)((value) & 0XFF),
        };
    }

    private String getStringFromByteArray(byte[] buffer, int index) {
        int length = byteArray2Integer(buffer, index);
        index += 4;

        return Base64.encodeToString(buffer, index, length, Base64.DEFAULT);
    }

    protected void getByteArrayFromString(String string, byte[] buffer, int index) {
        int length = string.length();
        integer2ByteArray(length, buffer, index);
        index += 4;

        byte[] code = string.getBytes();
        System.arraycopy(code, 0, buffer, index, code.length);
    }

    private byte[] getByteArrayFromString(String string) {
        int length = string.length();
        byte[] lb = integer2ByteArray(length);

        byte[] sb = string.getBytes();
        byte[] buffer = new byte[lb.length + sb.length];
        System.arraycopy(lb, 0, buffer, 0, lb.length);
        System.arraycopy(sb, 0, buffer, lb.length, sb.length);

        return buffer;
    }

    protected byte[] getByteArrayFromTextMessage(Message message) {
        String string = message.getText();

        byte[] sb = getByteArrayFromString(string);
        byte[] tb = integer2ByteArray(MESSAGE_TYPE_TEXT);

        byte[] buffer = new byte[sb.length + tb.length];
        System.arraycopy(tb, 0, buffer, 0, tb.length);
        System.arraycopy(sb, 0, buffer, tb.length, sb.length);

        return buffer;
    }

    protected byte[] getByteArrayFromImageMwssage(Message message) {
        return null;
    }

    protected Object getMessageFromByteArray(byte[] buffer) {
        int index = 0;
        int dataType = byteArray2Integer(buffer, index);
        index += 4;

        switch(dataType) {
        case MESSAGE_TYPE_TEXT:
            return  getStringFromByteArray(buffer, index);
        case MESSAGE_TYPE_IMAGE:
            return null;
        default:
            return null;
        }
    }
}
