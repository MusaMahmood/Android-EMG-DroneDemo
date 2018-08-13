/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *  Presenter. Android Client to remote control a presentation.          *
 *  Copyright (C) 2017 Felix Wohlfrom                                    *
 *                                                                       *
 *  This program is free software: you can redistribute it and/or modify *
 *  it under the terms of the GNU General Public License as published by *
 *  the Free Software Foundation, either version 3 of the License, or    *
 *  (at your option) any later version.                                  *
 *                                                                       *
 *  This program is distributed in the hope that it will be useful,      *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of       *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        *
 *  GNU General Public License for more details.                         *
 *                                                                       *
 *  You should have received a copy of the GNU General Public License    *
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.*
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package com.yeolabgt.mahmoodms.emgdronedemo.connectors.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.yeolabgt.mahmoodms.emgdronedemo.BuildConfig;
import com.yeolabgt.mahmoodms.emgdronedemo.connectors.RemoteControl;

/**
 * This class handles the bluetooth connection. It initiates the connection to a device and can be
 * used to transmit data to the other device.
 */
public class BluetoothPresenterControl extends RemoteControl {
    // Debugging
    private static final String TAG = "BluetoothControl";

    // Unique UUID for this application
    private static final UUID SERVICE_UUID =
            UUID.fromString("be71c255-8349-4d86-b09e-7983c035a191");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    /**
     * Constructor. Prepares a new bluetooth presenter control session.
     *
     * @param handler A handler to receive connection results
     */
    public BluetoothPresenterControl(Handler handler) {
        super(handler);
        mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Start the presenter service. Called by the Activity onResume()
     */
    public synchronized void start() {
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "connect to: " + device);
        }

        // Cancel any thread attempting to make a connection
        if (mState == ServiceState.CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, this, device);
        mConnectedThread.start();
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        mState = ServiceState.NONE;

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    @Override
    public void sendMessage(PresenterMessage message) {
        write((message.toString() + "\n\n").getBytes());
    }

    /**
     * Write output to the connected device. If no device is connected, no data is written.
     *
     * @param out The bytes to write
     */
    private void write(byte[] out) {
        // Create temporary object
        ConnectedThread connectedThread;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState == ServiceState.NONE) {
                return;
            }
            connectedThread = mConnectedThread;
        }
        // Perform the write unsynchronized
        connectedThread.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(ServiceState.CONNECTED.ordinal());
        Bundle bundle = new Bundle();
        bundle.putBoolean(RESULT_VALUES[0], false);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = ServiceState.NONE;
    }

    /**
     * Indicate that the connection was lost and notify callback handler.
     */
    private void connectionLost() {
        // Send a failure message back to the handler
        Message msg = mHandler.obtainMessage(ServiceState.NONE.ordinal());
        Bundle bundle = new Bundle();
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = ServiceState.NONE;
    }

    @Override
    public void disconnect() {
        // This will signal the reader thread to stop reading
        mState = ServiceState.NONE;
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        /**
         * Creates the connection thread that will connect to a given device.
         *
         * @param device The device to connect to
         */
        ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(SERVICE_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create socket failed", e);
            }
            mmSocket = tmp;
            mState = ServiceState.CONNECTING;

            // Notifiy the user that we are now connecting
            android.os.Message userNotification
                    = mHandler.obtainMessage(ServiceState.CONNECTING.ordinal());
            mHandler.sendMessage(userNotification);
        }

        /**
         * Initiate the connection.
         */
        public void run() {
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a successful connection
                // or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothPresenterControl.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        /**
         * Cancel connection to the device.
         */
        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;
        private final RemoteControl mRemoteControl;
        private final BluetoothDevice mDevice;
        private final StringBuffer mMessageBuffer;

        /**
         * Initiate the transmission using a given socket.
         *
         * @param socket The socket to connect to
         * @param control The parent remote control object
         * @param device The BluetoothDevice that has been connected
         */
        ConnectedThread(BluetoothSocket socket, RemoteControl control, BluetoothDevice device) {
            mSocket = socket;
            mRemoteControl = control;
            mDevice = device;
            mMessageBuffer = new StringBuffer();
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mInStream = tmpIn;
            mOutStream = tmpOut;
            // State will be set to connected once the version information is exchanged and we
            // found a common protocol version set to use.
        }

        /**
         * Try reading from input stream to wait until connection is lost.
         */
        public void run() {
            byte[] buffer = new byte[100];

            // Keep listening to the InputStream while connected
            while (mState != ServiceState.NONE) {
                try {
                    int readBytes = mInStream.read(buffer);
                    for (int i = 0; i < readBytes; i++) {
                        mMessageBuffer.append((char)buffer[i]);
                    }

                    if (mMessageBuffer.toString().contains("\n\n")) {
                        mRemoteControl.handleMessage(mDevice.getName(),
                                mMessageBuffer.substring(0, mMessageBuffer.indexOf("\n\n")));

                        mMessageBuffer.delete(0, mMessageBuffer.indexOf("\n\n") + "\n\n".length());
                    }
                } catch (IOException e) {
                    // Ignore exception if we already recognized that we are disconnected
                    if (mState != ServiceState.NONE) {
                        Log.e(TAG, "disconnected", e);
                        connectionLost();
                    }
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        void write(byte[] buffer) {
            try {
                mOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        /**
         * Cancel connection to other device.
         */
        void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "closing of connect socket failed", e);
            }
        }
    }

}