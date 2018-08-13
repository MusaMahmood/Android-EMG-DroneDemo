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

package com.yeolabgt.mahmoodms.emgdronedemo.connectors.bluetooth

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Fragment
import android.app.FragmentTransaction
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import com.yeolabgt.mahmoodms.emgdronedemo.*

import com.yeolabgt.mahmoodms.emgdronedemo.connectors.Command
import com.yeolabgt.mahmoodms.emgdronedemo.connectors.RemoteControl

/**
 * The bluetooth connector activity is used to handle the complete presenter control using
 * a bluetooth connection.
 * It shows the bluetooth device selector and afterwards the presenter fragment.
 */
class BluetoothConnector : Activity(), DeviceSelector.DeviceListResultListener {

    /**
     * Local Bluetooth adapter.
     */
    private var mBluetoothAdapter: BluetoothAdapter? = null

    /**
     * Member object for the presenter control service.
     */
    private var mPresenterControl: BluetoothPresenterControl? = null

    /**
     * Stores if the presenter fragment is visible or not.
     */
    private var mPresenterVisible = false

    /**
     * Stores if the current visibility state of the bluetooth connector.
     */
    private var mBluetoothConnectorVisible = false

    /**
     * The settings instance
     */
    private var mSettings: Settings? = null

    /**
     * The BroadcastReceiver that listens for bluetooth broadcasts
     */
    internal var mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (BluetoothAdapter.ACTION_STATE_CHANGED == action && intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) == BluetoothAdapter.STATE_OFF) {
                //Device has disconnected
                Toast.makeText(this@BluetoothConnector, R.string.bluetooth_required_leaving,
                        Toast.LENGTH_LONG).show()
                this@BluetoothConnector.finish()
            }
        }
    }

    /**
     * The handler reacts on status changes of our service.
     */
    @SuppressLint("HandlerLeak") // We don't leak any handlers here
    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == RemoteControl.ServiceState.CONNECTED.ordinal) {
                if (msg.data.getBoolean(RemoteControl.RESULT_VALUES[0])) {
                    // If connection succeeded
                    Toast.makeText(this@BluetoothConnector,
                            this@BluetoothConnector.getString(R.string.bluetooth_connected,
                                    msg.data.getString(
                                            BluetoothPresenterControl.RESULT_VALUES[1])),
                            Toast.LENGTH_SHORT).show()

                    // Remove "connecting" fragment
                    if (fragmentManager.backStackEntryCount > 0) {
                        fragmentManager.popBackStack()
                    }

                    // show presenter fragment
                    val transaction = fragmentManager.beginTransaction()
                    val fragment = Presenter()
                    transaction.replace(R.id.connector_content, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()

                    mPresenterVisible = true
                    return

                } else {
                    Toast.makeText(this@BluetoothConnector,
                            getString(R.string.bluetooth_not_connected),
                            Toast.LENGTH_SHORT).show()
                }

            } else if (msg.what == RemoteControl.ServiceState.CONNECTING.ordinal) {
                // show "connecting" fragment
                setTitle(R.string.connecting_to_service)
                val transaction = fragmentManager.beginTransaction()
                val fragment = Connecting()
                transaction.replace(R.id.connector_content, fragment)
                transaction.addToBackStack(null)
                transaction.commit()

                return

            } else if (msg.what == RemoteControl.ServiceState.ERROR.ordinal) {
                val error_type = RemoteControl.ERROR_TYPES.valueOf(
                        msg.data.getString(RemoteControl.RESULT_VALUES[2]))

                var errorMessage = ""

                when (error_type) {
                    RemoteControl.ERROR_TYPES.VERSION -> errorMessage = getString(R.string.incompatible_server_version)
                    RemoteControl.ERROR_TYPES.PARSING -> errorMessage = getString(R.string.parsing_error)
                }

                Toast.makeText(this@BluetoothConnector, errorMessage, Toast.LENGTH_LONG).show()

            } else if (msg.what == RemoteControl.ServiceState.NONE.ordinal) {
                Toast.makeText(this@BluetoothConnector,
                        this@BluetoothConnector.getString(R.string.connection_lost),
                        Toast.LENGTH_LONG).show()
            }

            if (fragmentManager.backStackEntryCount > 0) {
                if (mBluetoothConnectorVisible) {
                    fragmentManager.popBackStack()
                }

                mPresenterVisible = false
            }

            setTitle(R.string.title_device_selector)
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSettings = Settings(this)

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_available, Toast.LENGTH_LONG).show()
            this.finish()
            return
        }

        val disconnectFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        this.registerReceiver(mReceiver, disconnectFilter)

        setContentView(R.layout.activity_bluetooth_connector)
    }

    public override fun onDestroy() {
        this.unregisterReceiver(mReceiver)

        super.onDestroy()
        if (mPresenterControl != null) {
            mPresenterControl!!.stop()
        }
    }

    public override fun onPause() {
        mBluetoothConnectorVisible = false

        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        mPresenterVisible = false
        mBluetoothConnectorVisible = true

        // If BT is not on, request that it be enabled.
        if (!mBluetoothAdapter!!.isEnabled) {
            title = ""
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)

        } else if (mPresenterControl == null) {
            // Initialize the BluetoothPresenterControl to perform bluetooth connections
            mPresenterControl = BluetoothPresenterControl(mHandler)
        }

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mPresenterControl != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mPresenterControl!!.state == RemoteControl.ServiceState.NONE) {
                // initialize presenter control service
                mPresenterControl!!.start()
            }

            if (mPresenterControl!!.state == RemoteControl.ServiceState.CONNECTED) {
                // show presenter fragment TODO:
                // Intent to DCA:

                mPresenterVisible = true
            } else if (mPresenterControl!!.state == RemoteControl.ServiceState.CONNECTING) {
                // show "connecting" fragment
                val transaction = fragmentManager.beginTransaction()
                val fragment = Connecting()
                transaction.replace(R.id.connector_content, fragment)
                transaction.addToBackStack(null)
                transaction.commit()

            } else {
                // show device selector
                setTitle(R.string.title_device_selector)
                val transaction = fragmentManager.beginTransaction()
                val fragment = DeviceSelector()
                transaction.replace(R.id.connector_content, fragment)
                transaction.commit()
            }
        }
    }

    /**
     * Handles answers of our request to enable bluetooth.
     *
     * @param requestCode The request code to identify our request.
     * Should always be REQUEST_ENABLE_BT
     * @param resultCode  The result code of the request
     * @param data        Additional data, unused.
     */
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode != Activity.RESULT_OK) {
            // User did not enable Bluetooth or an error occurred
            Toast.makeText(this, R.string.bluetooth_required_leaving,
                    Toast.LENGTH_LONG).show()
            this.finish()
        }
    }

    override fun onDeviceSelected(address: String) {
        // Get the BluetoothDevice object
        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        // Attempt to connect to the device
        mPresenterControl!!.connect(device)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Ignore volume key events if volume keys are used for navigation and
        // presenter fragment is active
        return (mSettings!!.useVolumeKeysForNavigation() && mPresenterVisible
                && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        // Handle volume key usage for navigation
        if (mSettings!!.useVolumeKeysForNavigation() && mPresenterVisible) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
//                onNextSlide()
                return true
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
//                onPrevSlide()
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onBackPressed() {
        mPresenterVisible = false
        mPresenterControl!!.disconnect()
        setTitle(R.string.title_device_selector)
        super.onBackPressed()
    }

    //TODO;
//    override fun onPrevSlide() {
//        mPresenterControl!!.sendCommand(Command.PREV_SLIDE)
//    }
//
//    override fun onNextSlide() {
//        mPresenterControl!!.sendCommand(Command.NEXT_SLIDE)
//    }
//
//    override fun endPresentation() {
//        mPresenterControl!!.sendCommand(Command.ESCAPE)
//    }
//
//    override fun beginPresentation() {
//        mPresenterControl!!.sendCommand(Command.BEGIN)
//    }

    companion object {

        /**
         * This request code is used to verify that the activity result is really our request to
         * enable bt.
         */
        private val REQUEST_ENABLE_BT = 1
    }
}
