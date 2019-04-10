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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.avast.android.dialogs.fragment.ProgressDialogFragment;
import com.stfalcon.chatkit.dialogs.DialogsListAdapter;
import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.stfalcon.chatkit.sample.common.data.model.Dialog;
import com.stfalcon.chatkit.sample.common.data.model.Message;

import java.io.IOException;
import java.util.UUID;

public class BluetoothChatService {
    // Activity Tag for logger
    private static final String TAG = "BluetoothChatService";
    // String constants for RF comm socket connection
    private static final String NAME_SECURE = "DeviceListActivitySecure";

    // UUID for bluetooth service
    private static final UUID MY_UUID = UUID.fromString("ec12ea6a-2b7a-458e-8563-5789a296b833");

    // Connection socket manager
    protected SocketManager mSocketManager;
    // Dialog list view adapter
    protected DialogsListAdapter<Dialog> mDialogsAdapter;
    // Message list view adapter
    protected MessagesListAdapter<Message> mMessagesAdapter;
    // Chat message history handler
    protected MessageHandler mMessageHandler;
    // Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter;

    // Server bluetooth service listener thread
    private AcceptThread mAcceptThread;
    // Client bluetooth service thread
    private ConnectThread mConnectThread = null;

    private final int mState;

    private static final int CLIENT_CONNECTING = 0;
    private static final int SERVER_CONNECTING = 1;

    protected BluetoothChatService(BluetoothAdapter btadapter,
                                   DialogsListAdapter<Dialog> adapter) {
        mBluetoothAdapter = btadapter;
        mDialogsAdapter = adapter;
        //mAcceptThread = new AcceptThread();
        //mConnectThread = new ConnectThread();

        // Start server connection listener service
        mState = SERVER_CONNECTING;
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
    }

    public void updateMessages(String address, byte[] message) {

    }

    public void updateMessagesList() {

    }

    protected synchronized void connect(BluetoothDevice device) throws Exception{

        // Terminate now running accept thread
        if (mState == CLIENT_CONNECTING) { mConnectThread.cancel(); mConnectThread = null; }

        // Terminate now running connect thread
        if (mState == SERVER_CONNECTING) { mAcceptThread.cancel(); mAcceptThread = null; }

        // Build up new connect thread
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();


    }

    protected void onConnectionAccepted() {

    }

    protected void streamExceptionHandler() {

    }

    protected void lostExceptionHandler() {

    }

    protected void writeExceptionHandler() {

    }

    protected void cancelExceptionHadnler() {

    }

    protected void connectionExceptionHandler() {

    }

    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread");
            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != CLIENT_CONNECTING) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                connectionExceptionHandler();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                //BluetoothChatService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            //connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
