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
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.stfalcon.chatkit.sample.common.data.model.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class SocketManager {
    // Class tag for logger
    private static final String TAG = "SocketManager";

    private static final int MAX_CONNECTION = 5;

    // The map of connection. Key is the device address, in the form of String.
    // This map only includes the running connections. Closed or aborted
    // connections will be excluded form this map. It provides interface to
    // all the connected remote device.
    private Map<String, ConnectedThread> mConnectionMap;
    private Queue<String> mConnectionQueue;
    private BluetoothChatService mChatService;

    private int mSize;

    protected SocketManager(BluetoothChatService chatService) {
        mChatService = chatService;
        mConnectionMap = new HashMap<String, ConnectedThread>();
        mConnectionQueue = new LinkedList<String>();

        mSize = 0;
    }

    public void sendMassageByDevice(String address, byte[] message) throws Exception {
        // Get target socket
        ConnectedThread connection = mConnectionMap.get(address);
        if (connection == null) {
            throw new Exception("Target socket does not exist!");
        }

        // Send message if target socket exist
        connection.write(message);
    }

    public int size() {
        return mConnectionMap.size();
    }

    public boolean add(BluetoothSocket socket) {
        // Get instance of connection
        BluetoothDevice device = socket.getRemoteDevice();
        ConnectedThread connection = new ConnectedThread(socket, device);
        // Get address of remote device
        String address = socket.getRemoteDevice().getAddress();

        mConnectionMap.put(address, connection);
        return true;
    }

    public void cancelConnection(String address) throws Exception {
        // Get target connection
        ConnectedThread connection = mConnectionMap.get(address);
        if (connection == null) {
            throw new Exception("The connection to be removed does not exist!");
        }

        // Cancel connection socket
        connection.cancel();
        // Remove reference of the connection so that the memory can be recycled
        mConnectionMap.remove(address);

    }

    public void releaseRedundantConnection() {
        while (mSize >= MAX_CONNECTION) {
            String address = mConnectionQueue.remove();
            try {
                cancelConnection(address);
            } catch (Exception e) {
                Log.d(TAG, "Redundant connection cancel failed!");
            }
            --mSize;
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
                mChatService.streamExceptionHandler();
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
                        Message message = mChatService.castByte2Messsage(buffer);
                        mChatService.updateMessages(mmDevice.getAddress(), message);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    mChatService.lostExceptionHandler();
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
                //mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1, buffer)
                //        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
                mChatService.writeExceptionHandler();
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
                mChatService.cancelExceptionHadnler();
            }
        }
    }
}
