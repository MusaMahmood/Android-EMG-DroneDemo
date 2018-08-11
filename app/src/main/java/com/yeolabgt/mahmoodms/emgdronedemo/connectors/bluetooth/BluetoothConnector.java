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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.Toast;

import com.yeolabgt.mahmoodms.emgdronedemo.Connecting;
import com.yeolabgt.mahmoodms.emgdronedemo.Presenter;
import com.yeolabgt.mahmoodms.emgdronedemo.R;
import com.yeolabgt.mahmoodms.emgdronedemo.Settings;
import com.yeolabgt.mahmoodms.emgdronedemo.connectors.Command;
import com.yeolabgt.mahmoodms.emgdronedemo.connectors.RemoteControl;

/**
 * The bluetooth connector activity is used to handle the complete presenter control using
 * a bluetooth connection.
 * It shows the bluetooth device selector and afterwards the presenter fragment.
 */
public class BluetoothConnector extends Activity
        implements DeviceSelector.DeviceListResultListener,
        Presenter.PresenterListener {

    /**
     * This request code is used to verify that the activity result is really our request to
     * enable bt.
     */
    private static final int REQUEST_ENABLE_BT = 1;

    /**
     * Local Bluetooth adapter.
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the presenter control service.
     */
    private BluetoothPresenterControl mPresenterControl = null;

    /**
     * Stores if the presenter fragment is visible or not.
     */
    private boolean mPresenterVisible = false;

    /**
     * Stores if the current visibility state of the bluetooth connector.
     */
    private boolean mBluetoothConnectorVisible = false;

    /**
     * The settings instance
     */
    private Settings mSettings;

    /**
     * The BroadcastReceiver that listens for bluetooth broadcasts
     */
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)
                    && intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        == BluetoothAdapter.STATE_OFF) {
                //Device has disconnected
                Toast.makeText(BluetoothConnector.this, R.string.bluetooth_required_leaving,
                        Toast.LENGTH_LONG).show();
                BluetoothConnector.this.finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = new Settings(this);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_available, Toast.LENGTH_LONG).show();
            this.finish();
            return;
        }

        IntentFilter disconnectFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(mReceiver, disconnectFilter);

        setContentView(R.layout.activity_bluetooth_connector);
    }

    @Override
    public void onDestroy() {
        this.unregisterReceiver(mReceiver);

        super.onDestroy();
        if (mPresenterControl != null) {
            mPresenterControl.stop();
        }
    }

    @Override
    public void onPause() {
        mBluetoothConnectorVisible = false;

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenterVisible = false;
        mBluetoothConnectorVisible = true;

        // If BT is not on, request that it be enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            setTitle("");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

        } else if (mPresenterControl == null) {
            // Initialize the BluetoothPresenterControl to perform bluetooth connections
            mPresenterControl = new BluetoothPresenterControl(mHandler);
        }

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mPresenterControl != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mPresenterControl.getState() == RemoteControl.ServiceState.NONE) {
                // initialize presenter control service
                mPresenterControl.start();
            }

            if (mPresenterControl.getState() == RemoteControl.ServiceState.CONNECTED) {
                // show presenter fragment
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                Fragment fragment = new Presenter();
                transaction.replace(R.id.connector_content, fragment);
                transaction.addToBackStack(null);
                transaction.commit();

                mPresenterVisible = true;
            } else if (mPresenterControl.getState() == RemoteControl.ServiceState.CONNECTING) {
                // show "connecting" fragment
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                Fragment fragment = new Connecting();
                transaction.replace(R.id.connector_content, fragment);
                transaction.addToBackStack(null);
                transaction.commit();

            } else {
                // show device selector
                setTitle(R.string.title_device_selector);
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                Fragment fragment = new DeviceSelector();
                transaction.replace(R.id.connector_content, fragment);
                transaction.commit();
            }
        }
    }

    /**
     * The handler reacts on status changes of our service.
     */
    @SuppressLint("HandlerLeak") // We don't leak any handlers here
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == RemoteControl.ServiceState.CONNECTED.ordinal()) {
                if (msg.getData().getBoolean(RemoteControl.RESULT_VALUES[0])) {
                    // If connection succeeded
                    Toast.makeText(BluetoothConnector.this,
                            BluetoothConnector.this.getString(R.string.bluetooth_connected,
                                    msg.getData().getString(
                                            BluetoothPresenterControl.RESULT_VALUES[1])),
                            Toast.LENGTH_SHORT).show();

                    // Remove "connecting" fragment
                    if (getFragmentManager().getBackStackEntryCount() > 0) {
                        getFragmentManager().popBackStack();
                    }

                    // show presenter fragment
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    Fragment fragment = new Presenter();
                    transaction.replace(R.id.connector_content, fragment);
                    transaction.addToBackStack(null);
                    transaction.commit();

                    mPresenterVisible = true;
                    return;

                } else {
                    Toast.makeText(BluetoothConnector.this,
                            getString(R.string.bluetooth_not_connected),
                            Toast.LENGTH_SHORT).show();
                }

            } else if (msg.what == RemoteControl.ServiceState.CONNECTING.ordinal()) {
                // show "connecting" fragment
                setTitle(R.string.connecting_to_service);
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                Fragment fragment = new Connecting();
                transaction.replace(R.id.connector_content, fragment);
                transaction.addToBackStack(null);
                transaction.commit();

                return;

            } else if (msg.what == RemoteControl.ServiceState.ERROR.ordinal()) {
                RemoteControl.ERROR_TYPES error_type = RemoteControl.ERROR_TYPES.valueOf(
                        msg.getData().getString(RemoteControl.RESULT_VALUES[2]));

                String errorMessage = "";

                switch (error_type) {
                    case VERSION:
                        errorMessage = getString(R.string.incompatible_server_version);
                        break;
                    case PARSING:
                        errorMessage = getString(R.string.parsing_error);
                        break;
                }

                Toast.makeText(BluetoothConnector.this, errorMessage, Toast.LENGTH_LONG).show();

            } else if (msg.what == RemoteControl.ServiceState.NONE.ordinal()) {
                Toast.makeText(BluetoothConnector.this,
                        BluetoothConnector.this.getString(R.string.connection_lost),
                        Toast.LENGTH_LONG).show();
            }

            if (getFragmentManager().getBackStackEntryCount() > 0) {
                if (mBluetoothConnectorVisible) {
                    getFragmentManager().popBackStack();
                }

                mPresenterVisible = false;
            }

            setTitle(R.string.title_device_selector);
        }
    };

    /**
     * Handles answers of our request to enable bluetooth.
     *
     * @param requestCode The request code to identify our request.
     *                    Should always be REQUEST_ENABLE_BT
     * @param resultCode  The result code of the request
     * @param data        Additional data, unused.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode != Activity.RESULT_OK) {
            // User did not enable Bluetooth or an error occurred
            Toast.makeText(this, R.string.bluetooth_required_leaving,
                    Toast.LENGTH_LONG).show();
            this.finish();
        }
    }

    @Override
    public void onDeviceSelected(String address) {
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mPresenterControl.connect(device);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Ignore volume key events if volume keys are used for navigation and
        // presenter fragment is active
        return (mSettings.useVolumeKeysForNavigation() && mPresenterVisible
                    && ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                        || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)))
                || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Handle volume key usage for navigation
        if (mSettings.useVolumeKeysForNavigation() && mPresenterVisible) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                onNextSlide();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                onPrevSlide();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        mPresenterVisible = false;
        mPresenterControl.disconnect();
        setTitle(R.string.title_device_selector);
        super.onBackPressed();
    }

    @Override
    public void onPrevSlide() {
        mPresenterControl.sendCommand(Command.PREV_SLIDE);
    }

    @Override
    public void onNextSlide() {
        mPresenterControl.sendCommand(Command.NEXT_SLIDE);
    }

    @Override
    public void endPresentation() {
        mPresenterControl.sendCommand(Command.ESCAPE);
    }

    @Override
    public void beginPresentation() {
        mPresenterControl.sendCommand(Command.BEGIN);
    }
}
