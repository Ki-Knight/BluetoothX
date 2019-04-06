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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class SocketManager {
    // Class tag for logger
    private static final String TAG = "SocketManager";

    // The map of connection. Key is the device address, in the form of String.
    // This map only includes the running connections. Closed or aborted
    // connections will be excluded form this map. It provides interface to
    // all the connected remote device.
    private Map<String, Connection> mConnectionMap;

    protected SocketManager() {
        mConnectionMap = new HashMap<String, Connection>();
    }

    public boolean sendMassageByDevice(String address, byte[] massage) {
        return true;
    }

    public int size() {
        return mConnectionMap.size();
    }

    public boolean add(BluetoothSocket socket) {
        // Get instance of connection
        Connection connection = new Connection(socket);
        // Get address of remote device
        String address = connection.mmRemoteDevice.getAddress();

        mConnectionMap.put(address, connection);
        return true;
    }

    public boolean remove(String address) {
        /* Cancel connection or anything*/
        mConnectionMap.remove(address);
        return true;
    }


    protected class Connection {
        // Instance of remote device that the local device is connecting to
        protected final BluetoothDevice mmRemoteDevice;
        // The connection listening thread
        protected final ConnectedThread mmConnectedThread;

        public Connection(BluetoothSocket socket) {
            // Get the instance of remote device, this is a temporary object
            // and will be assigned to mmRemoteDevice
            BluetoothDevice tempDevice = socket.getRemoteDevice();
            // Get the instance of connected thread, will be assigned to
            // mmConnectedThread
            ConnectedThread tempThread = new ConnectedThread(socket);

            mmRemoteDevice = tempDevice;
            mmConnectedThread = tempThread;
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
                Log.d(TAG, "Get input and output stream failed!");
            }

            mmStreamIn = tempIn;
            mmStreamOut = tempOut;
        }

    }
}
