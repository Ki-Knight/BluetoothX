package com.stfalcon.chatkit.sample.features.main.group;

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
import com.stfalcon.chatkit.sample.features.main.BluetoothChatProtocol;
import com.stfalcon.chatkit.sample.features.main.BluetoothChatService;
import com.stfalcon.chatkit.sample.features.main.BluetoothXApplication;
import com.stfalcon.chatkit.sample.features.main.DeviceListActivity;
import com.stfalcon.chatkit.sample.features.main.MessageHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public class ChatServiceGroup {
    // Activity Tag for logger
    private static final String TAG = "ChatServiceGroup";
    // String constants for RF comm socket connection
    private static final String NAME_SECURE = "DeviceListActivitySecure";

    // UUID for bluetooth service
    private static final UUID MY_UUID = UUID.fromString("86b82faa-570a-4f77-aca2-f5e3e0708537");
    private static final int MAX_CONNECTION = 5;

    // Message list view adapter
    protected MessagesListAdapter<Message> mMessagesAdapter;
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
    private User mUserSelf;
    private Context mcontext;

    // Socket manager
    private Map<String, ConnectedThread> mSockets;
    private LinkedList<String> mSocketsQueue;
    private Handler mHandler;
    private BluetoothChatProtocol mProtocol;

    private BluetoothXApplication mApp;

    protected ChatServiceGroup(BluetoothXApplication app, BluetoothAdapter btadapter,
                                   Handler handler) {
        mBluetoothAdapter = btadapter;

        mState = SERVER_CONNECTING;

        // Initialize socket manager
        mSockets = new HashMap<String, ConnectedThread>();
        mSocketsQueue = new LinkedList<>();
        mHandler = handler;
        mProtocol = new BluetoothChatProtocol();
        mApp = app;
    }

    private synchronized void connect(BluetoothSocket socket) {
        BluetoothDevice device = socket.getRemoteDevice();
        ConnectedThread connection = new ConnectedThread(socket, device);
        connection.start();

        String address = device.getAddress();
        mSockets.put(address, connection);
    }

    protected synchronized void start() {
        Log.d(TAG, "Bluetooth chat service started!");

        // Set the user information of local bluetooth device
        User temp = new User(
                "0",
                mBluetoothAdapter.getName(),
                null,
                true
        );
        mUserSelf = temp;

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
        if (mSockets.containsKey(device.getAddress())) { return true; }
        if (mConnecter != null) { mConnecter.cancel(); mConnecter = null; }

        mAcceptThread.cancel();
        mAcceptThread = null;

        mConnecter = new Connecter(device);
        boolean result =  mConnecter.run();

        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
        return result;
    }

    protected synchronized void clearRedundantConnection() {
        while (mSockets.size() >= MAX_CONNECTION) {
            String address = mSocketsQueue.getLast();
            mSocketsQueue.removeLast();

            ConnectedThread thread = mSockets.get(address);
            thread.cancel();
            thread = null;

            mSockets.remove(address);
        }
    }

    protected synchronized void socketQueueReorder(String address) {
        int index = mSocketsQueue.indexOf(address);

        if (index == -1) {
            Log.d(TAG, "socketQueueReorder() target connection does not exist in queue!");
        }
        else {
            mSocketsQueue.remove(index);
            mSocketsQueue.addFirst(address);
        }
    }

    protected synchronized void onTextMessageSubmit(String content) {
        Date date = Calendar.getInstance().getTime();
        Message message = new Message(
                "all",
                new User(
                        "0",
                        mUserSelf.getName(),
                        null,
                        true
                ),
                content,
                date);
        byte[] msgb = mProtocol.getByteArrayFromTextMessage(message);

        for (Map.Entry<String, ConnectedThread> entry : mSockets.entrySet()) {
            ConnectedThread thread = entry.getValue();
            thread.write(msgb);
        }

        mMessagesAdapter = mApp.getMessagesAdapter();
        mMessagesAdapter.addToStart(message, true);
    }

    protected synchronized void onAttachmentMessageSubmit(Message message) {
        byte[] msgb = mProtocol.getByteArrayFromImageMwssage(message);
        for (Map.Entry<String, ConnectedThread> entry : mSockets.entrySet()) {
            ConnectedThread thread = entry.getValue();
            thread.write(msgb);
        }

        mMessagesAdapter.addToStart(message, true);
    }

    protected synchronized void onMessageReceived(Message message) {
        String address = message.getId();

        mMessagesAdapter = mApp.getMessagesAdapter();
        mMessagesAdapter.addToStart(message, true);
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
                android.os.Message message = mHandler.obtainMessage(DeviceListGroup.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString(DeviceListGroup.TOAST, "Failed to connect remote device!");
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
            synchronized (ChatServiceGroup.this) {
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
                        android.os.Message msg = mHandler.obtainMessage(DeviceListGroup.MESSAGE_READ);
                        Bundle bundle = new Bundle();
                        bundle.putByteArray(DeviceListGroup.BUFFER, buffer);
                        bundle.putString(DeviceListGroup.ADDRESS, mmDevice.getAddress());
                        msg.setData(bundle);

                        mHandler.sendMessage(msg);
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
                mHandler.obtainMessage(DeviceListGroup.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.d(TAG, "ConnectedThread write() exception during write");
                android.os.Message msg = mHandler.obtainMessage(DeviceListGroup.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString(DeviceListGroup.TOAST, "Message send failed!");
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
