/*
 * Copyright (c) 2018-2019 Shrowshoo Young All Rights Reserved
 *
 * This programme is developed as free software for the investigation of human
 * pose estimation using RF signals. Redistribution or modification of it is
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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothChatService {
    // Activity Tag for logger
    private static final String TAG = "BluetoothChatService";
    // String constants for RF comm socket connection
    private static final String NAME_SECURE = "DeviceListActivitySecure";

    // UUID for bluetooth service
    private static final UUID MY_UUID = UUID.fromString("ec12ea6a-2b7a-458e-8563-5789a296b833");

    // Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter;

    // Server bluetooth service listener thread
    private final AcceptThread mAcceptThread;
    // Client bluetooth service thread
    //private final ConnectThread mConnectThread;
    // Connected bluetooth RF comm socket thread
    //private final ConnectedThread mConnectedThread;

    protected BluetoothChatService(Context context, BluetoothAdapter adapter) {
        mBluetoothAdapter = adapter;
        mAcceptThread = new AcceptThread();
        //mConnectThread = new ConnectThread();
        //mConnectedThread = new ConnectedThread();

        BluetoothSocket mSocket;
    }

    private class AcceptThread extends Thread {
        // Bluetooth server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary variable to get target BluetoothServerSocket and then
            // assign to mmServerSocket, because it is a final.
            BluetoothServerSocket temp = null;

            // Get bluetooth server socket
            try {
                temp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID);
            } catch (IOException e) {
                Log.d(TAG,  "Accept thread listen failed.", e);
            }

            mmServerSocket = temp;
        }

        public void run() {

            try {
                // This is a blocking call return only on a successful connection or
                // an exception
                mmServerSocket.accept();
            } catch (IOException e) {
                Log.d(TAG, "Accept thread accept failed.", e);
            }
        }

        public void cancel() {
            // Cancel listening procedure of local bluetooth device
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "Accept thread close of server failed.", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        // Bluetooth socket and device
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            // Temporary socket that will assign to mmSocket
            BluetoothSocket temp = null;

            // Get client bluetooth service socket
            try {
                temp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.d(TAG, "Accept thread connect failed.", e);
            }

            mmSocket = temp;
        }

        public void run() {
            try {
                // This is a blocking call, only return on a successful connection or
                // an exception
                mmSocket.connect();
            } catch (IOException e) {
                Log.d(TAG, "Accept thread connect failed.", e);
            }
        }

        public void cancel() {
            // Terminate connect procedure
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "Accept thread close of connect failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        // Bluetooth input and output streams
        private final BluetoothSocket mmSocket;
        private final InputStream mmStreamIn;
        private final OutputStream mmStreamOut;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            // Temporary input and output stream
            InputStream tempIn = null;
            OutputStream tempOut = null;

            // Get input and output stream from connection socket
            try {
                tempIn = mmSocket.getInputStream();
                tempOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                Log.d(TAG, "Input and output stream create failed", e);
            }

            mmStreamIn = tempIn;
            mmStreamOut = tempOut;
        }
    }
}
