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
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.stfalcon.chatkit.dialogs.DialogsListAdapter;
import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.stfalcon.chatkit.sample.common.data.model.Dialog;
import com.stfalcon.chatkit.sample.common.data.model.Message;
import com.stfalcon.chatkit.sample.common.data.model.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public class BluetoothChatService {
    // Activity Tag for logger
    private static final String TAG = "BluetoothChatService";
    // String constants for RF comm socket connection
    private static final String NAME_SECURE = "DeviceListActivitySecure";

    // UUID for bluetooth service
    private static final UUID MY_UUID = UUID.fromString("ec12ea6a-2b7a-458e-8563-5789a296b833");
    private static final int MAX_CONNECTION = 5;

    // Dialog list view adapter
    private DialogsListAdapter<Dialog> mDialogsAdapter;
    // Message list view adapter
    protected MessagesListAdapter<Message> mMessagesAdapter;
    // Chat message history handler
    private MessageHandler mMessageHandler;
    // Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter;

    // Server bluetooth service listener thread
    private AcceptThread mAcceptThread;
    // Client bluetooth service thread
    private Connecter mConnecter = null;

    private final int mState;

    private static final int CLIENT_CONNECTING = 0;
    private static final int SERVER_CONNECTING = 1;

    // The address of now interacting device
    private String mDeviceAddressNow = null;
    private final User mUserSelf;
    private Context mcontext;

    // Socket manager
    private Map<String, ConnectedThread> mSockets;
    private LinkedList<String> mSocketsQueue;
    private Handler mHandler;
    private BluetoothChatProtocol mProtocol;

    protected BluetoothChatService(Context context, BluetoothAdapter btadapter,
                                   DialogsListAdapter<Dialog> adapter, Handler handler) {
        mBluetoothAdapter = btadapter;
        mDialogsAdapter = adapter;

        mcontext = context;
        //mAcceptThread = new AcceptThread();
        //mConnectThread = new ConnectThread();

        // Start server connection listener service
        mState = SERVER_CONNECTING;

        // Set the user information of local bluetooth device
        User temp = new User(
                btadapter.getAddress(),
                btadapter.getName(),
                null,
                true
        );
        mUserSelf = temp;

        // Initialize socket manager
        mSockets = new HashMap<String, ConnectedThread>();
        mSocketsQueue = new LinkedList<>();
        mHandler = handler;
        mProtocol = new BluetoothChatProtocol();
    }

    private synchronized void connect(BluetoothSocket socket) {
        BluetoothDevice device = socket.getRemoteDevice();
        ConnectedThread connection = new ConnectedThread(socket, device);

        String address = device.getAddress();
        mSockets.put(address, connection);
    }

    protected synchronized void start() {
        Log.d(TAG, "Bluetooth chat service started!");
        if (mConnecter != null) { mConnecter.cancel(); mConnecter = null; }
        for (Map.Entry<String, ConnectedThread> entry: mSockets.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().cancel();
                entry.setValue(null);
            }
        }
        mSockets.clear();

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    protected synchronized void stop() {
        Log.d(TAG, "Bluetooth chat service stopped!");
        if (mConnecter != null) { mConnecter.cancel(); mConnecter = null; }
        if (mAcceptThread != null) { mAcceptThread.cancel(); mAcceptThread = null; }

        for (Map.Entry<String, ConnectedThread> entry: mSockets.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().cancel();
                entry.setValue(null);
            }
        }
        mSockets.clear();
    }

    protected boolean onDialogsItemClicked(BluetoothDevice device) {
        if (mConnecter != null) { mConnecter.cancel(); mConnecter = null; }

        mAcceptThread.cancel();
        mAcceptThread = null;

        mConnecter = new Connecter(device);
        boolean result =  mConnecter.run();

        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
        mDeviceAddressNow = device.getAddress();
        return result;
    }

    protected void clearRedundantConnection() {
        while (mSockets.size() >= MAX_CONNECTION) {
            String address = mSocketsQueue.getLast();
            mSocketsQueue.removeLast();

            ConnectedThread thread = mSockets.get(address);
            thread.cancel();
            thread = null;

            mSockets.remove(address);
        }
    }

    private void socketQueueReorder(String address) {
        int index = mSocketsQueue.indexOf(address);

        if (index == -1) {
            Log.d(TAG, "socketQueueReorder() target connection does not exist in queue!");
        }
        else {
            mSocketsQueue.remove(index);
            mSocketsQueue.addFirst(address);
        }
    }

    protected synchronized void onTextMessageSubmit(Message message) {
        ConnectedThread thread = mSockets.get(mDeviceAddressNow);
        if (thread == null) {
            Log.d(TAG,"onTextMessageSubmit() connection do not exist!");

            android.os.Message msg = mHandler.obtainMessage(DeviceListActivity.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(DeviceListActivity.TOAST, "Connection do not exist!");
            msg.setData(bundle);

            mHandler.sendMessage(msg);
        }

        byte[] msgb = mProtocol.getByteArrayFromTextMessage(message);
        thread.write(msgb);

        mDialogsAdapter.updateDialogWithMessage(mDeviceAddressNow, message);
        mMessagesAdapter.addToStart(message, true);
        mMessageHandler.addHistory(mDeviceAddressNow, message);
    }

    protected synchronized void onAttachmentMessageSubmit(Message message) {
        ConnectedThread thread = mSockets.get(mDeviceAddressNow);
        if (thread == null) {
            Log.d(TAG,"onAttachmentMessageSubmit() connection do not exist!");

            android.os.Message msg = mHandler.obtainMessage(DeviceListActivity.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(DeviceListActivity.TOAST, "Connection do not exist!");
            msg.setData(bundle);

            mHandler.sendMessage(msg);
        }

        byte[] msgb = mProtocol.getByteArrayFromImageMwssage(message);
        thread.write(msgb);

        mDialogsAdapter.updateDialogWithMessage(mDeviceAddressNow, message);
        mMessagesAdapter.addToStart(message, true);
        mMessageHandler.addHistory(mDeviceAddressNow, message);
    }

    protected synchronized void onMessageReceived(Message message) {
        String address = message.getId();

        mDialogsAdapter.updateDialogWithMessage(address, message);
        mMessageHandler.addHistory(address, message);
        if (address.equals(mDeviceAddressNow)) {
            mMessagesAdapter.addToStart(message, true);
        }
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
                Log.d(TAG, "listen() failed", e);
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
                    Log.d(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    connect(socket);
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "close() of server failed", e);
            }
        }
    }

    protected class Connecter {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public Connecter(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.d(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public boolean run() {
            Log.i(TAG, "BEGIN mConnectThread");

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                android.os.Message message = mHandler.obtainMessage(DeviceListActivity.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString(DeviceListActivity.TOAST, "Failed to connect remote device!");
                message.setData(bundle);

                mHandler.sendMessage(message);

                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.d(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                //BluetoothChatService.this.start();
                return false;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothChatService.this) {
                mConnecter = null;
            }

            // Start the connected thread
            connect(mmSocket);
            return true;
        }

        private void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        // Bluetooth input and output streams
        private final BluetoothSocket mmSocket;
        private final InputStream mmStreamIn;
        private final OutputStream mmStreamOut;

        private final BluetoothDevice mmDevice;

        public ConnectedThread(BluetoothSocket socket, BluetoothDevice device) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
                //mChatService.streamExceptionHandler();
            }

            mmStreamIn = tmpIn;
            mmStreamOut = tmpOut;
            mmDevice = device;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmStreamIn.read(buffer);
                    if (bytes != 0) {
                        // Cast binary array to message instance
                        //Message message = mChatService.castByte2Messsage(buffer);
                        //mChatService.updateMessages(mmDevice.getAddress(), message);
                        mHandler.obtainMessage(DeviceListActivity.MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    //mChatService.lostExceptionHandler();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer){
            try {
                mmStreamOut.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(DeviceListActivity.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.d(TAG, "ConnectedThread write() exception during write");
                android.os.Message msg = mHandler.obtainMessage(DeviceListActivity.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString(DeviceListActivity.TOAST, "Message send failed!");
                msg.setData(bundle);

                mHandler.sendMessage(msg);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
                //mChatService.cancelExceptionHadnler();
            }
        }
    }
}
