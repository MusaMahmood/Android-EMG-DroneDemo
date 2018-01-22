package com.yeolabgt.mahmoodms.ssvepinterfacetf

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.NavUtils
import android.support.v4.content.FileProvider
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.*

import com.androidplot.util.Redrawer
import com.yeolabgt.mahmoodms.actblelibrary.ActBle

import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * Created by mahmoodms on 5/31/2016.
 * Android Activity for Controlling Bluetooth LE Device Connectivity
 */

class DeviceControlActivity : Activity(), ActBle.ActBleListener {
    // Graphing Variables:
    private var mGraphInitializedBoolean = false
    private var mGraphAdapterCh1: GraphAdapter? = null
    private var mGraphAdapterCh2: GraphAdapter? = null
    private var mGraphAdapterCh3: GraphAdapter? = null
    private var mTimeDomainPlotAdapterCh1: XYPlotAdapter? = null
    private var mTimeDomainPlotAdapterCh2: XYPlotAdapter? = null
    private var mTimeDomainPlotAdapterCh3: XYPlotAdapter? = null
    private var mCh1: DataChannel? = null
    private var mCh2: DataChannel? = null
    private var mCh3: DataChannel? = null
    //Device Information
    private var mBleInitializedBoolean = false
    private lateinit var mBluetoothGattArray: Array<BluetoothGatt?>
    private var mActBle: ActBle? = null
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null
    private var mConnected: Boolean = false
    private var mMSBFirst = false
    //Connecting to Multiple Devices
    private var deviceMacAddresses: Array<String>? = null
    private var mEEGConfigGattService: BluetoothGattService? = null
    private var mWheelchairGattIndex: Int = 0
    private var mEEGConfigGattIndex: Int = 0
    private var mEEGConnectedAllChannels = false
    // Classification
    private var mNumber2ChPackets = -1
    private var mRunTrainingBool: Boolean = false
    //UI Elements - TextViews, Buttons, etc
    private var mBatteryLevel: TextView? = null
    private var mDataRate: TextView? = null
    private var mTogglePlots: ToggleButton? = null
    private var menu: Menu? = null
    //Data throughput counter
    private var mLastTime: Long = 0
    private var points = 0
    private val mTimerHandler = Handler()
    private var mTimerEnabled = false
    //Data Variables:
    private val batteryWarning = 20//
    private var dataRate: Double = 0.toDouble()
    private var mStimulusDelaySeconds = 0.0
    //Play Sound:
    private lateinit var mMediaBeep: MediaPlayer
    //Tensorflow:

    private val timeStamp: String
        get() = SimpleDateFormat("yyyy.MM.dd_HH.mm.ss", Locale.US).format(Date())

    // Native Interface Function Handler:
    private val mNativeInterface = NativeInterfaceClass()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)
        //Set orientation of device based on screen type/size:
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        //Receive Intents:
        val intent = intent
        deviceMacAddresses = intent.getStringArrayExtra(MainActivity.INTENT_DEVICES_KEY)
        val deviceDisplayNames = intent.getStringArrayExtra(MainActivity.INTENT_DEVICES_NAMES)
        val intentStimulusClass = intent.getStringArrayExtra(MainActivity.INTENT_DELAY_VALUE_SECONDS)
        if (intent.extras != null)
            mRunTrainingBool = intent.extras!!.getBoolean(MainActivity.INTENT_TRAIN_BOOLEAN)
        else
            Log.e(TAG, "ERROR: intent.getExtras = null")

        mStimulusDelaySeconds = Integer.valueOf(intentStimulusClass[0])!!.toDouble()
        mDeviceName = deviceDisplayNames[0]
        mDeviceAddress = deviceMacAddresses!![0]
        Log.d(TAG, "Device Names: " + Arrays.toString(deviceDisplayNames))
        Log.d(TAG, "Device MAC Addresses: " + Arrays.toString(deviceMacAddresses))
        Log.d(TAG, Arrays.toString(deviceMacAddresses))
        //Set up action bar:
        if (actionBar != null) {
            actionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        val actionBar = actionBar
        actionBar!!.setBackgroundDrawable(ColorDrawable(Color.parseColor("#6078ef")))
        //Flag to keep screen on (stay-awake):
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        //Set up TextViews
        val mExportButton = findViewById<Button>(R.id.button_export)
        mBatteryLevel = findViewById(R.id.batteryText)
        mDataRate = findViewById(R.id.dataRate)
        mDataRate!!.text = "..."
        val ab = getActionBar()
        ab!!.title = mDeviceName
        ab.subtitle = mDeviceAddress
        //Initialize Bluetooth
        if (!mBleInitializedBoolean) initializeBluetoothArray()
        mMediaBeep = MediaPlayer.create(this, R.raw.beep_01a)
        mLastTime = System.currentTimeMillis()
        mTogglePlots = findViewById(R.id.toggleButtonCh1)
        mTogglePlots!!.setOnCheckedChangeListener { _, b ->
            if (!b) {
                mGraphAdapterCh3?.clearPlot()
                mGraphAdapterCh2?.clearPlot()
                mGraphAdapterCh1?.clearPlot()
            }
            mGraphAdapterCh1!!.plotData = b
            mGraphAdapterCh2!!.plotData = b
            mGraphAdapterCh3!!.plotData = b
        }
        mExportButton.setOnClickListener { exportData() }
    }

    private fun exportData() {
        try {
            terminateDataFileWriter()
        } catch (e: IOException) {
            Log.e(TAG, "IOException in saveDataFile")
            e.printStackTrace()
        }
        val context = applicationContext
        val uii = FileProvider.getUriForFile(context, context.packageName + ".provider", mPrimarySaveDataFile!!.file)
        val exportData = Intent(Intent.ACTION_SEND)
        exportData.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        exportData.putExtra(Intent.EXTRA_SUBJECT, "Sensor Data Export Details")
        exportData.putExtra(Intent.EXTRA_STREAM, uii)
        exportData.type = "text/html"
        startActivity(exportData)
    }

    @Throws(IOException::class)
    private fun terminateDataFileWriter() {
        mPrimarySaveDataFile?.terminateDataFileWriter()
    }

    public override fun onResume() {
        mNativeInterface.jmainInitialization(false)
        if (mRedrawer != null) {
            mRedrawer!!.start()
        }
        super.onResume()
    }

    override fun onPause() {
        if (mRedrawer != null) mRedrawer!!.pause()
        super.onPause()
    }

    private fun initializeBluetoothArray() {
        val mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val mBluetoothDeviceArray = arrayOfNulls<BluetoothDevice>(deviceMacAddresses!!.size)
        Log.d(TAG, "Device Addresses: " + Arrays.toString(deviceMacAddresses))
        if (deviceMacAddresses != null) {
            for (i in deviceMacAddresses!!.indices) {
                mBluetoothDeviceArray[i] = mBluetoothManager.adapter.getRemoteDevice(deviceMacAddresses!![i])
            }
        } else {
            Log.e(TAG, "No Devices Queued, Restart!")
            Toast.makeText(this, "No Devices Queued, Restart!", Toast.LENGTH_SHORT).show()
        }
        mActBle = ActBle(this, mBluetoothManager, this)
        mBluetoothGattArray = Array(deviceMacAddresses!!.size, { i -> mActBle!!.connect(mBluetoothDeviceArray[i]) })
        for (i in mBluetoothDeviceArray.indices) {
            Log.e(TAG, "Connecting to Device: Name: " + (mBluetoothDeviceArray[i]!!.name + " \nMAC:" + mBluetoothDeviceArray[i]!!.address))
            if ("WheelchairControl" == mBluetoothDeviceArray[i]!!.name) {
                mWheelchairGattIndex = i
                Log.e(TAG, "mWheelchairGattIndex: " + mWheelchairGattIndex)
                continue //we are done initializing
            } else {
                mEEGConfigGattIndex = i
            }

            val btDeviceName = mBluetoothDeviceArray[i]?.name?.toLowerCase()
            mMSBFirst = when {
                btDeviceName == null -> false
                btDeviceName.contains("EMG 250Hz") -> false
                btDeviceName.contains("nrf52") -> true
                else -> false
            }
            mSampleRate = when {
                btDeviceName == null -> 250
                btDeviceName.contains("8k") -> 8000
                btDeviceName.contains("4k") -> 4000
                btDeviceName.contains("2k") -> 2000
                btDeviceName.contains("1k") -> 1000
                btDeviceName.contains("500") -> 500
                else -> 250
            }
            mPacketBuffer = mSampleRate / 250
            Log.e(TAG, "mSampleRate: " + mSampleRate + "Hz")
            if (!mGraphInitializedBoolean) setupGraph()

            mGraphAdapterCh1!!.setxAxisIncrementFromSampleRate(mSampleRate)
            mGraphAdapterCh2!!.setxAxisIncrementFromSampleRate(mSampleRate)
            mGraphAdapterCh3?.setxAxisIncrementFromSampleRate(mSampleRate)

            mGraphAdapterCh1!!.setSeriesHistoryDataPoints(250 * 5)
            mGraphAdapterCh2!!.setSeriesHistoryDataPoints(250 * 5)
            mGraphAdapterCh3?.setSeriesHistoryDataPoints(250 * 5)
            val fileNameTimeStamped = "EOG_VergenceData_" + timeStamp + "_" + mSampleRate.toString() + "Hz"
            Log.e(TAG, "fileTimeStamp: " + fileNameTimeStamped)
            try {
                mPrimarySaveDataFile = SaveDataFile("/EOGData", fileNameTimeStamped,
                        24, 1.toDouble() / mSampleRate)
            } catch (e: IOException) {
                Log.e(TAG, "initializeBluetoothArray: IOException", e)
            }

            mPrimarySaveDataFile!!.setSaveTimestamps(false)
            mPrimarySaveDataFile!!.setFpPrecision(64)
            mPrimarySaveDataFile!!.setIncludeClass(false)
        }
        mBleInitializedBoolean = true
    }

    private fun setupGraph() {
        // Initialize our XYPlot reference:
        mGraphAdapterCh1 = GraphAdapter(mSampleRate * 4, "EOG Data Ch 1", false, Color.BLUE)
        mGraphAdapterCh2 = GraphAdapter(mSampleRate * 4, "EOG Data Ch 2", false, Color.RED)
        mGraphAdapterCh3 = GraphAdapter(mSampleRate * 4, "EOG Data Ch 3", false, Color.GREEN)

        //PLOT By default
        mGraphAdapterCh1!!.plotData = true
        mGraphAdapterCh2!!.plotData = true
        mGraphAdapterCh3!!.plotData = true
        mGraphAdapterCh1!!.setPointWidth(2.toFloat())
        mGraphAdapterCh2!!.setPointWidth(2.toFloat())
        mGraphAdapterCh3!!.setPointWidth(2.toFloat())

        mTimeDomainPlotAdapterCh1 = XYPlotAdapter(findViewById(R.id.eegTimeDomainXYPlot), false, 1000)
        if (mTimeDomainPlotAdapterCh1!!.xyPlot != null) {
            mTimeDomainPlotAdapterCh1!!.xyPlot!!.addSeries(mGraphAdapterCh1!!.series, mGraphAdapterCh1!!.lineAndPointFormatter)
        }
        mTimeDomainPlotAdapterCh2 = XYPlotAdapter(findViewById(R.id.frequencyAnalysisXYPlot), false, 1000)
        if (mTimeDomainPlotAdapterCh2!!.xyPlot != null) {
            mTimeDomainPlotAdapterCh2!!.xyPlot!!.addSeries(mGraphAdapterCh2!!.series, mGraphAdapterCh2!!.lineAndPointFormatter)
        }
        mTimeDomainPlotAdapterCh3 = XYPlotAdapter(findViewById(R.id.eogCh3XYPlot), false, 1000)
        if (mTimeDomainPlotAdapterCh3!!.xyPlot != null) {
            mTimeDomainPlotAdapterCh3?.xyPlot?.addSeries(mGraphAdapterCh3?.series, mGraphAdapterCh3?.lineAndPointFormatter)
        }

        val xyPlotList = listOf(mTimeDomainPlotAdapterCh1!!.xyPlot, mTimeDomainPlotAdapterCh2!!.xyPlot, mTimeDomainPlotAdapterCh3?.xyPlot)
        mRedrawer = Redrawer(xyPlotList, 30f, false)
        mRedrawer!!.start()
        mGraphInitializedBoolean = true
    }

    private fun setNameAddress(name_action: String?, address_action: String?) {
        val name = menu!!.findItem(R.id.action_title)
        val address = menu!!.findItem(R.id.action_address)
        name.title = name_action
        address.title = address_action
        invalidateOptionsMenu()
    }

    override fun onDestroy() {
        mRedrawer?.finish()
        disconnectAllBLE()
        try {
            terminateDataFileWriter()
        } catch (e: IOException) {
            Log.e(TAG, "IOException in saveDataFile")
            e.printStackTrace()
        }

        stopMonitoringRssiValue()
        mNativeInterface.jmainInitialization(true) //Just a technicality, doesn't actually do anything
        super.onDestroy()
    }

    private fun disconnectAllBLE() {
        if (mActBle != null) {
            for (bluetoothGatt in mBluetoothGattArray) {
                mActBle!!.disconnect(bluetoothGatt!!)
                mConnected = false
                resetMenuBar()
            }
        }
    }

    private fun resetMenuBar() {
        runOnUiThread {
            if (menu != null) {
                menu!!.findItem(R.id.menu_connect).isVisible = true
                menu!!.findItem(R.id.menu_disconnect).isVisible = false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_device_control, menu)
        menuInflater.inflate(R.menu.actionbar_item, menu)
        if (mConnected) {
            menu.findItem(R.id.menu_connect).isVisible = false
            menu.findItem(R.id.menu_disconnect).isVisible = true
        } else {
            menu.findItem(R.id.menu_connect).isVisible = true
            menu.findItem(R.id.menu_disconnect).isVisible = false
        }
        this.menu = menu
        setNameAddress(mDeviceName, mDeviceAddress)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_connect -> {
                if (mActBle != null) {
                    initializeBluetoothArray()
                }
                connect()
                return true
            }
            R.id.menu_disconnect -> {
                if (mActBle != null) {
                    disconnectAllBLE()
                }
                return true
            }
            android.R.id.home -> {
                if (mActBle != null) {
                    disconnectAllBLE()
                }
                NavUtils.navigateUpFromSameTask(this)
                onBackPressed()
                return true
            }
            R.id.action_settings -> {
                launchSettingsMenu()
                return true
            }
            R.id.action_export -> {
                exportData()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1) {
            val context = applicationContext
            //File Save Stuff
            val saveTimestamps = PreferencesFragment.saveTimestamps(context)
            val precision = (if (PreferencesFragment.setBitPrecision(context)) 64 else 32).toShort()
            val saveClass = PreferencesFragment.saveClass(context)
            mPrimarySaveDataFile!!.setSaveTimestamps(saveTimestamps)
            mPrimarySaveDataFile!!.setFpPrecision(precision)
            mPrimarySaveDataFile!!.setIncludeClass(saveClass)
            val filterData = PreferencesFragment.setFilterData(context)
            //TODO: for now just ch1:
            if (mGraphAdapterCh1 != null) {
                mFilterData = filterData
            }
            /**
             * Settings for ADS1299:
             */
            val registerConfigBytes = Arrays.copyOf(ADS1299_DEFAULT_BYTE_CONFIG, ADS1299_DEFAULT_BYTE_CONFIG.size)
            when (PreferencesFragment.setSampleRate(context)) {
                0 -> {
                    registerConfigBytes[0] = 0x96.toByte()
                }
                1 -> {
                    registerConfigBytes[0] = 0x95.toByte()
                }
                2 -> {
                    registerConfigBytes[0] = 0x94.toByte()
                }
                3 -> {
                    registerConfigBytes[0] = 0x93.toByte()
                }
                4 -> {
                    registerConfigBytes[0] = 0x92.toByte()
                }
            }
            val numChEnabled = PreferencesFragment.setNumberChannelsEnabled(context)
            Log.e(TAG, "numChEnabled: " + numChEnabled.toString())
            // Set
            when (numChEnabled) {
                1 -> {
                    registerConfigBytes[12] = 0b0000_0001
                    registerConfigBytes[13] = 0b0000_0001
                }
                2 -> {
                    registerConfigBytes[12] = 0b0000_0011
                    registerConfigBytes[13] = 0b0000_0011
                }
                3 -> {
                    registerConfigBytes[12] = 0b0000_0111
                    registerConfigBytes[13] = 0b0000_0111
                }
                4 -> {
                    registerConfigBytes[12] = 0b0000_1111
                    registerConfigBytes[13] = 0b0000_1111
                }
            }

            //Set all to disable.
            for (i in 4..7) registerConfigBytes[i] = 0xE1.toByte()
            Log.e(TAG, "SettingsNew0: " + DataChannel.byteArrayToHexString(registerConfigBytes))
            for (i in 4..(3 + numChEnabled)) {
                registerConfigBytes[i] = 0x00.toByte()
            }
            Log.e(TAG, "SettingsNew1: " + DataChannel.byteArrayToHexString(registerConfigBytes))
            val gain12 = PreferencesFragment.setGainCh12(context) //Check if ch enabled first
            for (i in 4..5) { //Checks first bit enabled on chs 1 & 2.
                registerConfigBytes[i] = when (registerConfigBytes[i] and 0x80.toByte()) {
                    0x80.toByte() -> registerConfigBytes[i] // do nothing if ch disabled
                    else -> registerConfigBytes[i] or (gain12 shl 4).toByte() // Shift gain into 0xxx_0000 position
                }
            }
            if (PreferencesFragment.setSRB1(context)) {
                registerConfigBytes[20] = 0x20.toByte()
                registerConfigBytes[13] = 0b0000_0000 // Turn off BIAS_SENSN with SRB 1
            } else {
                registerConfigBytes[20] = 0x00.toByte()
            }
            Log.e(TAG, "SettingsNew: " + DataChannel.byteArrayToHexString(registerConfigBytes))
            writeNewADS1299Settings(registerConfigBytes)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun writeNewADS1299Settings(bytes: ByteArray) {
        Log.e(TAG, "bytesOriginal: " + DataChannel.byteArrayToHexString(ADS1299_DEFAULT_BYTE_CONFIG))
        if (mEEGConfigGattService != null) {
            Log.e(TAG, "SendingCommand (byte): " + DataChannel.byteArrayToHexString(bytes))
            mActBle!!.writeCharacteristic(mBluetoothGattArray[mEEGConfigGattIndex]!!, mEEGConfigGattService!!.getCharacteristic(AppConstant.CHAR_EEG_CONFIG), bytes)
            //Should notify/update after writing
        }
    }

    private fun launchSettingsMenu() {
        val intent = Intent(applicationContext, SettingsActivity::class.java)
        startActivityForResult(intent, 1)
    }

    private fun connect() {
        runOnUiThread {
            val menuItem = menu!!.findItem(R.id.action_status)
            menuItem.title = "Connecting..."
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        Log.i(TAG, "onServicesDiscovered")
        if (status == BluetoothGatt.GATT_SUCCESS) {
            for (service in gatt.services) {
                if (service == null || service.uuid == null) {
                    continue
                }
                if (AppConstant.SERVICE_DEVICE_INFO == service.uuid) {
                    //Read the device serial number (if available)
                    if (service.getCharacteristic(AppConstant.CHAR_SERIAL_NUMBER) != null) {
                        mActBle!!.readCharacteristic(gatt, service.getCharacteristic(AppConstant.CHAR_SERIAL_NUMBER))
                    }
                    //Read the device software version (if available)
                    if (service.getCharacteristic(AppConstant.CHAR_SOFTWARE_REV) != null) {
                        mActBle!!.readCharacteristic(gatt, service.getCharacteristic(AppConstant.CHAR_SOFTWARE_REV))
                    }
                }

                if (AppConstant.SERVICE_EEG_SIGNAL == service.uuid) {
                    if (service.getCharacteristic(AppConstant.CHAR_EEG_CONFIG) != null) {
                        mEEGConfigGattService = service
                        mActBle!!.readCharacteristic(gatt, service.getCharacteristic(AppConstant.CHAR_EEG_CONFIG))
                        mActBle!!.setCharacteristicNotifications(gatt, service.getCharacteristic(AppConstant.CHAR_EEG_CONFIG), true)
                    }

                    if (service.getCharacteristic(AppConstant.CHAR_EEG_CH1_SIGNAL) != null) {
                        mActBle!!.setCharacteristicNotifications(gatt, service.getCharacteristic(AppConstant.CHAR_EEG_CH1_SIGNAL), true)
                        if (mCh1 == null) mCh1 = DataChannel(false, mMSBFirst, 4 * mSampleRate)
                    }
                    if (service.getCharacteristic(AppConstant.CHAR_EEG_CH2_SIGNAL) != null) {
                        mActBle!!.setCharacteristicNotifications(gatt, service.getCharacteristic(AppConstant.CHAR_EEG_CH2_SIGNAL), true)
                        if (mCh2 == null) mCh2 = DataChannel(false, mMSBFirst, 4 * mSampleRate)
                    }
                    if (service.getCharacteristic(AppConstant.CHAR_EEG_CH3_SIGNAL) != null) {
                        mActBle!!.setCharacteristicNotifications(gatt, service.getCharacteristic(AppConstant.CHAR_EEG_CH3_SIGNAL), true)
                        if (mCh3 == null) mCh3 = DataChannel(false, mMSBFirst, 4 * mSampleRate)
                    }
                    if (service.getCharacteristic(AppConstant.CHAR_EEG_CH4_SIGNAL) != null)
                        mActBle!!.setCharacteristicNotifications(gatt, service.getCharacteristic(AppConstant.CHAR_EEG_CH4_SIGNAL), true)
                    if (service.getCharacteristic(AppConstant.CHAR_EEG_CH5_SIGNAL) != null)
                        mActBle!!.setCharacteristicNotifications(gatt, service.getCharacteristic(AppConstant.CHAR_EEG_CH5_SIGNAL), true)
                    if (service.getCharacteristic(AppConstant.CHAR_EEG_CH6_SIGNAL) != null)
                        mActBle!!.setCharacteristicNotifications(gatt, service.getCharacteristic(AppConstant.CHAR_EEG_CH6_SIGNAL), true)
                    if (service.getCharacteristic(AppConstant.CHAR_EEG_CH7_SIGNAL) != null)
                        mActBle!!.setCharacteristicNotifications(gatt, service.getCharacteristic(AppConstant.CHAR_EEG_CH7_SIGNAL), true)
                    if (service.getCharacteristic(AppConstant.CHAR_EEG_CH8_SIGNAL) != null)
                        mActBle!!.setCharacteristicNotifications(gatt, service.getCharacteristic(AppConstant.CHAR_EEG_CH8_SIGNAL), true)
                }

                if (AppConstant.SERVICE_BATTERY_LEVEL == service.uuid) { //Read the device battery percentage
                    mActBle!!.readCharacteristic(gatt, service.getCharacteristic(AppConstant.CHAR_BATTERY_LEVEL))
                    mActBle!!.setCharacteristicNotifications(gatt, service.getCharacteristic(AppConstant.CHAR_BATTERY_LEVEL), true)
                }
            }
            //Run process only once:
            mActBle?.runProcess()
        }
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        Log.i(TAG, "onCharacteristicRead")
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (AppConstant.CHAR_BATTERY_LEVEL == characteristic.uuid) {
                if (characteristic.value != null) {
                    val batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                    updateBatteryStatus(batteryLevel)
                    Log.i(TAG, "Battery Level :: " + batteryLevel)
                }
            }
            //TODO: NEED TO CHANGE mSampleRate, DataChannel[], and GraphAdapter[] here.
            if (AppConstant.CHAR_EEG_CONFIG == characteristic.uuid) {
                if (characteristic.value != null) {
                    val readValue = characteristic.value
                    Log.e(TAG, "onCharacteriticRead: \n" +
                            "CHAR_EEG_CONFIG: " + DataChannel.byteArrayToHexString(readValue))
                }
            }
        } else {
            Log.e(TAG, "onCharacteristic Read Error" + status)
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        if (AppConstant.CHAR_EEG_CONFIG == characteristic.uuid) {
            if (characteristic.value != null) {
                val readValue = characteristic.value
                Log.e(TAG, "onCharacteriticChanged: \n" +
                        "CHAR_EEG_CONFIG: " + DataChannel.byteArrayToHexString(readValue))
                when (readValue[0] and 0x0F.toByte()) {
                    0x06.toByte() -> mSampleRate = 250
                    0x05.toByte() -> mSampleRate = 500
                    0x04.toByte() -> mSampleRate = 1000
                    0x03.toByte() -> mSampleRate = 2000
                    0x02.toByte() -> mSampleRate = 4000
                }
                //RESET mCH1 & mCH2:
                mCh1?.classificationBufferSize = 4 * mSampleRate
                mCh2?.classificationBufferSize = 4 * mSampleRate
                Log.e(TAG, "Updated Sample Rate: " + mSampleRate.toString())
            }
        }

        if (AppConstant.CHAR_BATTERY_LEVEL == characteristic.uuid) {
            val batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)!!
            updateBatteryStatus(batteryLevel)
        }

        if (AppConstant.CHAR_EEG_CH1_SIGNAL == characteristic.uuid) {
            val mNewEEGdataBytes = characteristic.value
            if (!mCh1!!.chEnabled) {
                mCh1!!.chEnabled = true
            }
            getDataRateBytes(mNewEEGdataBytes.size)
            if (mEEGConnectedAllChannels) {
                mCh1!!.handleNewData(mNewEEGdataBytes)
                if (mCh1!!.packetCounter.toInt() == mPacketBuffer) {
                    addToGraphBuffer(mCh1!!, mGraphAdapterCh1)
                }
            }
        }

        if (AppConstant.CHAR_EEG_CH2_SIGNAL == characteristic.uuid) {
            if (!mCh2!!.chEnabled) {
                mCh2!!.chEnabled = true
            }
            val mNewEEGdataBytes = characteristic.value
            val byteLength = mNewEEGdataBytes.size
            getDataRateBytes(byteLength)
            if (mEEGConnectedAllChannels) {
                mCh2!!.handleNewData(mNewEEGdataBytes)
                if (mCh2!!.packetCounter.toInt() == mPacketBuffer) {
                    addToGraphBuffer(mCh2!!, mGraphAdapterCh2)
                }
            }
        }

        if (AppConstant.CHAR_EEG_CH3_SIGNAL == characteristic.uuid) {
            if (!mCh3!!.chEnabled) {
                mCh3!!.chEnabled = true
            }
            val mNewEEGdataBytes = characteristic.value
            val byteLength = mNewEEGdataBytes.size
            getDataRateBytes(byteLength)
            if (mEEGConnectedAllChannels) {
                mCh3!!.handleNewData(mNewEEGdataBytes)
                if (mCh3!!.packetCounter.toInt() == mPacketBuffer) {
                    addToGraphBuffer(mCh3!!, mGraphAdapterCh3)
                }
            }
        }

        if (mCh1!!.chEnabled && mCh2!!.chEnabled && mCh3!!.chEnabled) {
            mNumber2ChPackets++
            mEEGConnectedAllChannels = true
            mCh1!!.chEnabled = false
            mCh2!!.chEnabled = false
            mCh3!!.chEnabled = false
            if (mCh1!!.characteristicDataPacketBytes != null &&
                    mCh2!!.characteristicDataPacketBytes != null &&
                    mCh3!!.characteristicDataPacketBytes != null) {
                mPrimarySaveDataFile!!.writeToDisk(mCh1?.characteristicDataPacketBytes,
                        mCh2?.characteristicDataPacketBytes, mCh3?.characteristicDataPacketBytes)
            }
        }
    }

    private fun addToGraphBuffer(dataChannel: DataChannel, graphAdapter: GraphAdapter?) {
        if (mFilterData && dataChannel.totalDataPointsReceived > 1000) {
            val filteredData = mNativeInterface.jSSVEPCfilter(dataChannel.classificationBuffer)
            graphAdapter!!.clearPlot()

            for (i in filteredData.indices) { // gA.addDataPointTimeDomain(y,x)
                graphAdapter.addDataPointTimeDomainAlt(filteredData[i].toDouble(),
                        dataChannel.totalDataPointsReceived - 999 + i)
            }
        } else {
            if (dataChannel.dataBuffer != null) {
                if (mPrimarySaveDataFile!!.resolutionBits == 24) {
                    var i = 0
                    while (i < dataChannel.dataBuffer!!.size / 3) {
                        graphAdapter!!.addDataPointTimeDomain(DataChannel.bytesToDouble(dataChannel.dataBuffer!![3 * i],
                                dataChannel.dataBuffer!![3 * i + 1], dataChannel.dataBuffer!![3 * i + 2]),
                                dataChannel.totalDataPointsReceived - dataChannel.dataBuffer!!.size / 3 + i)
                        i += graphAdapter.sampleRate / 250
                    }
                } else if (mPrimarySaveDataFile!!.resolutionBits == 16) {
                    var i = 0
                    while (i < dataChannel.dataBuffer!!.size / 2) {
                        graphAdapter!!.addDataPointTimeDomain(DataChannel.bytesToDouble(dataChannel.dataBuffer!![2 * i],
                                dataChannel.dataBuffer!![2 * i + 1]),
                                dataChannel.totalDataPointsReceived - dataChannel.dataBuffer!!.size / 2 + i)
                        i += graphAdapter.sampleRate / 250
                    }
                }
            }
        }

        dataChannel.dataBuffer = null
        dataChannel.packetCounter = 0.toShort()
    }

    private fun getDataRateBytes(bytes: Int) {
        val mCurrentTime = System.currentTimeMillis()
        points += bytes
        if (mCurrentTime > mLastTime + 5000) {
            dataRate = (points / 5).toDouble()
            points = 0
            mLastTime = mCurrentTime
            Log.e(" DataRate:", dataRate.toString() + " Bytes/s")
            runOnUiThread {
                val s = dataRate.toString() + " Bytes/s"
                mDataRate!!.text = s
            }
        }
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
        uiRssiUpdate(rssi)
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        when (newState) {
            BluetoothProfile.STATE_CONNECTED -> {
                mConnected = true
                runOnUiThread {
                    if (menu != null) {
                        menu!!.findItem(R.id.menu_connect).isVisible = false
                        menu!!.findItem(R.id.menu_disconnect).isVisible = true
                    }
                }
                Log.i(TAG, "Connected")
                updateConnectionState(getString(R.string.connected))
                invalidateOptionsMenu()
                runOnUiThread {
                    mDataRate!!.setTextColor(Color.BLACK)
                    mDataRate!!.setTypeface(null, Typeface.NORMAL)
                }
                //Start the service discovery:
                gatt.discoverServices()
                startMonitoringRssiValue()
            }
            BluetoothProfile.STATE_CONNECTING -> {
            }
            BluetoothProfile.STATE_DISCONNECTING -> {
            }
            BluetoothProfile.STATE_DISCONNECTED -> {
                mConnected = false
                runOnUiThread {
                    if (menu != null) {
                        menu!!.findItem(R.id.menu_connect).isVisible = true
                        menu!!.findItem(R.id.menu_disconnect).isVisible = false
                    }
                }
                Log.i(TAG, "Disconnected")
                runOnUiThread {
                    mDataRate!!.setTextColor(Color.RED)
                    mDataRate!!.setTypeface(null, Typeface.BOLD)
                    mDataRate!!.text = HZ
                }
                updateConnectionState(getString(R.string.disconnected))
                stopMonitoringRssiValue()
                invalidateOptionsMenu()
            }
            else -> {
            }
        }
    }

    private fun startMonitoringRssiValue() {
        readPeriodicallyRssiValue(true)
    }

    private fun stopMonitoringRssiValue() {
        readPeriodicallyRssiValue(false)
    }

    private fun readPeriodicallyRssiValue(repeat: Boolean) {
        mTimerEnabled = repeat
        // check if we should stop checking RSSI value
        if (!mConnected || !mTimerEnabled) {
            mTimerEnabled = false
            return
        }

        mTimerHandler.postDelayed(Runnable {
            if (!mConnected) {
                mTimerEnabled = false
                return@Runnable
            }
            // request RSSI value
            mBluetoothGattArray[0]!!.readRemoteRssi()
            // add call it once more in the future
            readPeriodicallyRssiValue(mTimerEnabled)
        }, RSSI_UPDATE_TIME_INTERVAL.toLong())
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        Log.i(TAG, "onCharacteristicWrite :: Status:: " + status)
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {}

    override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        Log.i(TAG, "onDescriptorRead :: Status:: " + status)
    }

    override fun onError(errorMessage: String) {
        Log.e(TAG, "Error:: " + errorMessage)
    }

    private fun updateConnectionState(status: String) {
        runOnUiThread {
            if (status == getString(R.string.connected)) {
                Toast.makeText(applicationContext, "Device Connected!", Toast.LENGTH_SHORT).show()
            } else if (status == getString(R.string.disconnected)) {
                Toast.makeText(applicationContext, "Device Disconnected!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateBatteryStatus(integerValue: Int) {
        val status: String
        val convertedBatteryVoltage = integerValue.toDouble() / 4096.0 * 7.20
        //Because TPS63001 dies below 1.8V, we need to set up a linear fit between 1.8-4.2V
        //Anything over 4.2V = 100%
        val finalPercent: Double = when {
            125.0 / 3.0 * convertedBatteryVoltage - 75.0 > 100.0 -> 100.0
            125.0 / 3.0 * convertedBatteryVoltage - 75.0 < 0.0 -> 0.0
            else -> 125.0 / 3.0 * convertedBatteryVoltage - 75.0
        }
        Log.e(TAG, "Battery Integer Value: " + integerValue.toString())
        Log.e(TAG, "ConvertedBatteryVoltage: " + String.format(Locale.US, "%.5f", convertedBatteryVoltage) + "V : " + String.format(Locale.US, "%.3f", finalPercent) + "%")
        status = String.format(Locale.US, "%.1f", finalPercent) + "%"
        runOnUiThread {
            if (finalPercent <= batteryWarning) {
                mBatteryLevel!!.setTextColor(Color.RED)
                mBatteryLevel!!.setTypeface(null, Typeface.BOLD)
                Toast.makeText(applicationContext, "Charge Battery, Battery Low " + status, Toast.LENGTH_SHORT).show()
            } else {
                mBatteryLevel!!.setTextColor(Color.GREEN)
                mBatteryLevel!!.setTypeface(null, Typeface.BOLD)
            }
            mBatteryLevel!!.text = status
        }
    }

    private fun uiRssiUpdate(rssi: Int) {
        runOnUiThread {
            val menuItem = menu!!.findItem(R.id.action_rssi)
            val statusActionItem = menu!!.findItem(R.id.action_status)
            val valueOfRSSI = rssi.toString() + " dB"
            menuItem.title = valueOfRSSI
            if (mConnected) {
                val newStatus = "Status: " + getString(R.string.connected)
                statusActionItem.title = newStatus
            } else {
                val newStatus = "Status: " + getString(R.string.disconnected)
                statusActionItem.title = newStatus
            }
        }
    }

    companion object {
        val HZ = "0 Hz"
        private val TAG = DeviceControlActivity::class.java.simpleName
        var mRedrawer: Redrawer? = null
        // Power Spectrum Graph Data:
        private var mSampleRate = 250
        var mSSVEPClass = 0.0
        //Data Channel Classes
        internal var mFilterData = false
        private var mPacketBuffer = 6
        //RSSI:
        private val RSSI_UPDATE_TIME_INTERVAL = 2000
        //Save Data File
        private var mPrimarySaveDataFile: SaveDataFile? = null
        //Tensorflow CONSTANTS:
        val ADS1299_DEFAULT_BYTE_CONFIG = byteArrayOf(
                0x96.toByte(), 0xD0.toByte(), 0xEC.toByte(), 0x00.toByte(), //CONFIG1-3, LOFF
                0x40.toByte(), 0x40.toByte(), 0xE1.toByte(), 0xE1.toByte(), //CHSET 1-4
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), //CHSET 5-8
                0x0F.toByte(), 0x0F.toByte(), // BIAS_SENSP/N
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // LOFF_P/N (IGNORE)
                0x0F.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()) //GPIO, MISC1 (0x20 for SRB1), MISC2, CONFIG4

        //Note for companion object: JNI call must include Companion in call: e.g. package_class_Companion_function(...).
        //TODO: Still does not work when I try to call from the companion object.
        init {
            System.loadLibrary("ssvep-lib")
        }
    }
}
