package com.yeolabgt.mahmoodms.emgdronedemo;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.androidplot.Plot;
import com.androidplot.util.Redrawer;
import com.beele.BluetoothLe;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.yeolabgt.mahmoodms.emgdronedemo.ParrotDrone.MiniDrone;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by mahmoodms on 5/31/2016.
 * Android Activity for Controlling Bluetooth LE Device Connectivity
 */

public class DeviceControlActivity extends Activity implements BluetoothLe.BluetoothLeListener {
    public static final String HZ = "0 Hz";
    // Graphing Variables:
    private boolean mGraphInitializedBoolean = false;
    private GraphAdapter mGraphAdapterCh1;
    private GraphAdapter mGraphAdapterCh2;
    //    private GraphAdapter mGraphAdapterCh1PSDA;
//    private GraphAdapter mGraphAdapterCh2PSDA;
    public XYPlotAdapter mCh1PlotAdapter;
    public XYPlotAdapter mCh2PlotAdapter;
    private int mSampleRate = 250;
    public static Redrawer redrawer;
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    //Refactored Data Channel Classes
    DataChannel mCh1;
    DataChannel mCh2;
    static int mNumberChannels = 0;
    private boolean mChannelsConnected = false;
    int mCh1PacketCount = 0;
    int mCh2PacketCount = 0;
    boolean mMSBFirst = false;
    //LocalVars
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean mConnected;
    private boolean mRunTrainingBool;
    //Class instance variable
    private boolean mBleInitializedBoolean = false;
    private BluetoothLe mBluetoothLe;
    //Connecting to Multiple Devices
    private String[] deviceMacAddresses = null;
    private BluetoothGatt[] mBluetoothGattArray = null;
    //    private boolean mEEGConnected_2ch = false;
    // Classification
    private static int mPacketBuffer = 6;
    //Layout - TextViews and Buttons
    private TextView mBatteryLevel;
    private TextView mDataRate;
    private Button mExportButton;
    private long mLastTime;
    private int byteResolution = 3;
    private int points = 0;
    private Menu menu;
    //RSSI:
    private static final int RSSI_UPDATE_TIME_INTERVAL = 2000;
    private Handler mTimerHandler = new Handler();
    private boolean mTimerEnabled = false;
    //Data Variables:
    private int batteryWarning = 20;//
    private double dataRate;
    //Play Sound:
    MediaPlayer mMediaBeep;
    //File Save Stuff:
    private boolean fileSaveInitialized = false;
    private CSVWriter csvWriter;
    private File mSaveFile;
    //Drone Interface Stuff:
    private MiniDrone mMiniDrone;
    private boolean mDronePresent = false;
    private boolean mDroneControl = false; //Default classifier.
    private ProgressDialog mConnectionProgressDialog;
    private ProgressDialog mDownloadProgressDialog;
    private Button mTakeOffLandBt;
    private Button mConnectDroneButton;
    private boolean mDroneConnectionState = false;
    private TextView mBatteryLevelDrone;
    private int mNbMaxDownload;
    private int mCurrentDownloadIndex;

    //Classification:
    static ClassDataAnalysis TrainingData;
    private double mEMGClass = 0;
    private double mStimulusDelaySeconds = 0;
    private TextView mYfitTextView;
    private TextView mTrainingInstructions;
    private int mNumberOfClassifierCalls = 0;
    int fPSDStartIndex = 0;
    int fPSDEndIndex = 100;
    //Filestuffs:
    private static File trainingDataFile;
    private static double[] CUSTOM_KNN_PARAMS;
    private static boolean mUseCustomParams = false;
    private int[] mYfitArray = new int[10];
    private TextView mEMGClassText;
    ARDiscoveryDeviceService mARService;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
        //Set orientation of device based on screen type/size:
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //Receive Intents:
        final Intent intent = getIntent();
        deviceMacAddresses = intent.getStringArrayExtra(MainActivity.INTENT_DEVICES_KEY);
        String[] deviceDisplayNames = intent.getStringArrayExtra(MainActivity.INTENT_DEVICES_NAMES);
        String[] intentStimulusClass = intent.getStringArrayExtra(MainActivity.INTENT_DELAY_VALUE_SECONDS);
        if (intent.getExtras() != null)
            mRunTrainingBool = intent.getExtras().getBoolean(MainActivity.INTENT_TRAIN_BOOLEAN);
        else Log.e(TAG, "ERROR: intent.getExtras = null");

        mStimulusDelaySeconds = Integer.valueOf(intentStimulusClass[0]);
        mDeviceName = deviceDisplayNames[0];
        mDeviceAddress = deviceMacAddresses[0];
        //get Drone Info if present:
        Log.d(TAG, "Device Names: " + Arrays.toString(deviceDisplayNames));
        Log.d(TAG, "Device MAC Addresses: " + Arrays.toString(deviceMacAddresses));
        Log.d(TAG, Arrays.toString(deviceMacAddresses));
        //Set up action bar:
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        ActionBar actionBar = getActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#6078ef")));
        //Flag to keep screen on (stay-awake):
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //Set up TextViews
        mExportButton = findViewById(R.id.button_export);
        mBatteryLevel = findViewById(R.id.batteryText);
        mBatteryLevelDrone = findViewById(R.id.droneBatteryText);
        mTrainingInstructions = findViewById(R.id.trainingInstructions);
        mEMGClassText = findViewById(R.id.emgClassText);
        mConnectDroneButton = findViewById(R.id.droneConnectButton);
        updateTrainingView(mRunTrainingBool);
        mDataRate = findViewById(R.id.dataRate);
        mDataRate.setText("...");
        mYfitTextView = findViewById(R.id.textViewYfit);
        ActionBar ab = getActionBar();
        ab.setTitle(mDeviceName);
        ab.setSubtitle(mDeviceAddress);
        //Initialize Bluetooth
        if (!mBleInitializedBoolean) initializeBluetoothArray();
        mExportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    terminateDataFileWriter();
                } catch (IOException e) {
                    Log.e(TAG, "IOException in saveDataFile");
                    e.printStackTrace();
                }
                Context context = getApplicationContext();
                Uri uii = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", mSaveFile);
                Intent exportData = new Intent(Intent.ACTION_SEND);
                exportData.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                exportData.putExtra(Intent.EXTRA_SUBJECT, "Sensor Data Export Details");
                exportData.putExtra(Intent.EXTRA_STREAM, uii);
                exportData.setType("text/html");
                startActivity(exportData);
            }
        });
        mTakeOffLandBt = findViewById(R.id.buttonS);
        makeFilterSwitchVisible(false);
        mLastTime = System.currentTimeMillis();
        ToggleButton toggleButton1 = findViewById(R.id.toggleButtonDroneControl);
        toggleButton1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mDroneControl = b;
                makeFilterSwitchVisible(b);
            }
        });

        ToggleButton ch1 = findViewById(R.id.toggleButtonCh1);
        ch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mGraphAdapterCh1.setPlotData(b);

            }
        });
        ToggleButton ch2 = findViewById(R.id.toggleButtonCh2);
        ch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mGraphAdapterCh2.setPlotData(b);
            }
        });
        Button resetButton = findViewById(R.id.resetActivityButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                resetActivity();
                finish();
            }
        });
        mMediaBeep = MediaPlayer.create(this, R.raw.beep_01a);
        findViewById(R.id.buttonFwd).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mMiniDrone.setPitch((byte) 50);
                        mMiniDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        mMiniDrone.setPitch((byte) 0);
                        mMiniDrone.setFlag((byte) 0);
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        findViewById(R.id.buttonR).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mMiniDrone.setYaw((byte) 50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mMiniDrone.setYaw((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });
        mTakeOffLandBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (mMiniDrone.getFlyingState()) {
                    case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                        mMiniDrone.takeOff();
                        break;
                    case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        mMiniDrone.land();
                        break;
                    default:
                }
            }
        });
        mConnectDroneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mDroneConnectionState) {
                    if(connectDrone()) {
                        mDroneConnectionState = true;
                        String s = "Disconnect";
                        mConnectDroneButton.setText(s);
                    }
                } else {
                    if(disconnectDrone()) {
                        mDroneConnectionState = false;
                        String s = "Connect";
                        mConnectDroneButton.setText(s);
                    }
                }
                Log.d(TAG, "onClick: mDroneConnectionState: "+String.valueOf(mDroneConnectionState));
            }
        });
        mARService = intent.getParcelableExtra(MainActivity.EXTRA_DRONEDEVICE_SERVICE);
    }

    private boolean disconnectDrone() {
        if (mMiniDrone != null) {
            mConnectionProgressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
            mConnectionProgressDialog.setIndeterminate(true);
            mConnectionProgressDialog.setMessage("Disconnecting ...");
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();
            if (!mMiniDrone.disconnect()) {
                finish();
                return false;
            }
        }
        return true;
    }

    private boolean connectDrone() {
        if (mARService != null) {
            mBatteryLevelDrone.setVisibility(View.VISIBLE);
            mDronePresent = true;
            mMiniDrone = new MiniDrone(this, mARService);
            mMiniDrone.addListener(mMiniDroneListener);
            if ((mMiniDrone != null) && !(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mMiniDrone.getConnectionState()))) {
                mConnectionProgressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
                mConnectionProgressDialog.setIndeterminate(true);
                mConnectionProgressDialog.setMessage("Connecting ...");
                mConnectionProgressDialog.setCancelable(false);
                mConnectionProgressDialog.show();
                // if the connection to the MiniDrone fails, finish the activity
                if (!mMiniDrone.connect()) {
                    Toast.makeText(getApplicationContext(), "Failed to Connect Drone", Toast.LENGTH_LONG).show();
                    finish();
                    return false;
                }
                return true;
            } else {
                return false;
            }
        } else {
            Log.e(TAG, "Drone Service is Null");
            return false;
        }
    }

    private void sendDroneCommand(int command) {
        if (mDronePresent && mDroneControl) {
            switch (command) {
                case 0:
                    //Do nothing/Hover
                    if (mMiniDrone != null) {
                        //Reset conditions:
                        mMiniDrone.setYaw((byte) 0);
                        mMiniDrone.setPitch((byte) 0);
                        mMiniDrone.setFlag((byte) 0);
                    }
                    break;
                case 1:
                    //Move Fwd:
                    if (mMiniDrone != null) {
                        mMiniDrone.setPitch((byte) 50);
                        mMiniDrone.setFlag((byte) 1);
                    }
                    break;
                case 2:
                    //RotR
                    if (mMiniDrone != null) {
                        mMiniDrone.setYaw((byte) 50);
                    }
                    break;
                case 3:
                    // Take off or Land
                    if (mMiniDrone != null) {
                        switch (mMiniDrone.getFlyingState()) {
                            case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                                mMiniDrone.takeOff();
                                break;
                            case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                                //Do nothing.
                            case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                                mMiniDrone.land();
                                break;
                            default:
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public String getTimeStamp() {
        return new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss", Locale.US).format(new Date());
    }

    /**
     * @throws IOException some IO Error
     */
    public void terminateDataFileWriter() throws IOException {
        if (fileSaveInitialized) {
            csvWriter.flush();
            csvWriter.close();
            fileSaveInitialized = false;
        }
    }

    /**
     * Initializes CSVWriter For Saving Data.
     *
     * @throws IOException bc
     */
    public void saveDataFile() throws IOException {
        File root = Environment.getExternalStorageDirectory();
        String fileTimeStamp = "EMG_1CH_" + getTimeStamp() + "_" + String.valueOf(mSampleRate) + "Hz";
        Log.d(TAG, "fileTimeStamp: " + fileTimeStamp);
        if (root.canWrite()) {
            File dir = new File(root.getAbsolutePath() + "/EMGData");
            boolean resultMkdir = dir.mkdirs();
            if (!resultMkdir) {
                Log.d(TAG, "MKDIRS FAILED");
            }
            mSaveFile = new File(dir, fileTimeStamp + ".csv");
            if (mRunTrainingBool) trainingDataFile = mSaveFile;
            if (mSaveFile.exists() && !mSaveFile.isDirectory()) {
                Log.d(TAG, "File " + mSaveFile.toString() + " already exists - appending data");
                FileWriter fileWriter = new FileWriter(mSaveFile, true);
                csvWriter = new CSVWriter(fileWriter);
            } else {
                csvWriter = new CSVWriter(new FileWriter(mSaveFile));
            }
            fileSaveInitialized = true;
        }
    }

    public void exportFileWithClass(double eegData1) throws IOException {
        if (fileSaveInitialized) {
            String[] writeCSVValue = new String[2];
            writeCSVValue[0] = eegData1 + "";
            writeCSVValue[1] = mEMGClass + "";
            csvWriter.writeNext(writeCSVValue, false);
        }
    }

    public void exportFileWithClass(double eegData1, double eegData2) throws IOException {
        if (fileSaveInitialized) {
            String[] writeCSVValue = new String[3];
            writeCSVValue[0] = eegData1 + "";
            writeCSVValue[1] = eegData2 + "";
            writeCSVValue[2] = mEMGClass + "";
            csvWriter.writeNext(writeCSVValue, false);
        }
    }

    @Override
    public void onResume() {
        jmainInitialization();
        if (redrawer != null) {
            redrawer.start();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (redrawer != null) redrawer.pause();
        makeFilterSwitchVisible(false);
        super.onPause();
    }

    private void initializeBluetoothArray() {
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothDevice[] mBluetoothDeviceArray = new BluetoothDevice[deviceMacAddresses.length];
        mBluetoothGattArray = new BluetoothGatt[deviceMacAddresses.length];
        Log.d(TAG, "Device Addresses: " + Arrays.toString(deviceMacAddresses));
        if (deviceMacAddresses != null && mBluetoothManager != null) {
            for (int i = 0; i < deviceMacAddresses.length; i++) {
                mBluetoothDeviceArray[i] = mBluetoothManager.getAdapter().getRemoteDevice(deviceMacAddresses[i]);
            }
        } else {
            Log.e(TAG, "No Devices Queued, Restart!");
            Toast.makeText(this, "No Devices Queued, Restart!", Toast.LENGTH_SHORT).show();
        }
        mBluetoothLe = new BluetoothLe(this, mBluetoothManager, this);
        for (int i = 0; i < mBluetoothDeviceArray.length; i++) {
            mBluetoothGattArray[i] = mBluetoothLe.connect(mBluetoothDeviceArray[i], false);
            Log.e(TAG, "Connecting to Device: " + String.valueOf(mBluetoothDeviceArray[i].getName() + " " + mBluetoothDeviceArray[i].getAddress()));
            if ("EMG 250Hz".equals(mBluetoothDeviceArray[i].getName())) {
                mMSBFirst = false;
            } else if ("EMG 3CH 250Hz".equals(mBluetoothDeviceArray[i].getName())) {
                mMSBFirst = true;
            } else if (mBluetoothDeviceArray[i].getName().toLowerCase().contains("nRF52".toLowerCase())) {
                mMSBFirst = true;
            }
            if (mBluetoothDeviceArray[i].getName().toLowerCase().contains("8k".toLowerCase())) {
                mSampleRate = 8000;
                mPacketBuffer = 32;
                byteResolution = 2; //FOR ECG ONLY
            } else if (mBluetoothDeviceArray[i].getName().toLowerCase().contains("4k".toLowerCase())) {
                mSampleRate = 4000;
                mPacketBuffer = 16;
                byteResolution = 3;
            } else if (mBluetoothDeviceArray[i].getName().toLowerCase().contains("2k".toLowerCase())) {
                mSampleRate = 2000;
                mPacketBuffer = 8;
                byteResolution = 3;
            } else if (mBluetoothDeviceArray[i].getName().toLowerCase().contains("1k".toLowerCase())) {
                mSampleRate = 1000;
                mPacketBuffer = 4;
                byteResolution = 3;
            } else if (mBluetoothDeviceArray[i].getName().toLowerCase().contains("500".toLowerCase())) {
                mSampleRate = 500;
                mPacketBuffer = 2;
                byteResolution = 3;
            } else {
                mSampleRate = 250;
                mPacketBuffer = 1;
            }
            fPSDStartIndex = 16;
            fPSDEndIndex = 80;
            Log.e(TAG, "mSampleRate: " + mSampleRate + "Hz");

            if (!mGraphInitializedBoolean) setupGraph();

            mGraphAdapterCh1.setxAxisIncrementFromSampleRate(mSampleRate);
            mGraphAdapterCh2.setxAxisIncrementFromSampleRate(mSampleRate);

            mGraphAdapterCh1.setSeriesHistoryDataPoints(250 * 5);
            mGraphAdapterCh2.setSeriesHistoryDataPoints(250 * 5);

            if (!fileSaveInitialized) {
                try {
                    saveDataFile();
                } catch (IOException ex) {
                    Log.e("IOEXCEPTION:", ex.toString());
                }
            }
        }
        mBleInitializedBoolean = true;
    }

    private void setupGraph() {
        // Initialize our XYPlot reference:
        mGraphAdapterCh1 = new GraphAdapter(mSampleRate * 4, "EEG Data Ch 1", false, Color.BLUE); //Color.parseColor("#19B52C") also, RED, BLUE, etc.
        mGraphAdapterCh2 = new GraphAdapter(mSampleRate * 4, "EEG Data Ch 2", false, Color.RED); //Color.parseColor("#19B52C") also, RED, BLUE, etc.
        //PLOT CH1 By default
        mGraphAdapterCh1.plotData = true;
        mGraphAdapterCh1.setPointWidth((float) 2);
        mGraphAdapterCh2.setPointWidth((float) 2);
        mCh1PlotAdapter = new XYPlotAdapter(findViewById(R.id.eegTimeDomainXYPlot), false, 1000);
        mCh1PlotAdapter.xyPlot.addSeries(mGraphAdapterCh1.series, mGraphAdapterCh1.lineAndPointFormatter);
        mCh2PlotAdapter = new XYPlotAdapter(findViewById(R.id.frequencyAnalysisXYPlot), "Frequency (Hz)", "Power Density (W/Hz)", ((double) mSampleRate / 125.0));
        mCh2PlotAdapter.xyPlot.addSeries(mGraphAdapterCh2.series, mGraphAdapterCh2.lineAndPointFormatter);

        redrawer = new Redrawer(
                Arrays.asList(new Plot[]{mCh1PlotAdapter.xyPlot, mCh2PlotAdapter.xyPlot}), 60, false);
        redrawer.start();
        mGraphInitializedBoolean = true;
    }

    private void setNameAddress(String name_action, String address_action) {
        MenuItem name = menu.findItem(R.id.action_title);
        MenuItem address = menu.findItem(R.id.action_address);
        name.setTitle(name_action);
        address.setTitle(address_action);
        invalidateOptionsMenu();
    }

    @Override
    protected void onDestroy() {
        if (mMiniDrone != null) mMiniDrone.dispose();
        redrawer.finish();
        disconnectAllBLE();
        try {
            terminateDataFileWriter();
        } catch (IOException e) {
            Log.e(TAG, "IOException in saveDataFile");
            e.printStackTrace();
        }
        stopMonitoringRssiValue();
        super.onDestroy();
    }

    private void disconnectAllBLE() {
        disconnectDrone();
        if (mBluetoothLe != null) {
            for (BluetoothGatt bluetoothGatt : mBluetoothGattArray) {
                mBluetoothLe.disconnect(bluetoothGatt);
                mConnected = false;
                resetMenuBar();
            }
        }
    }

    private void resetMenuBar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (menu != null) {
                    menu.findItem(R.id.menu_connect).setVisible(true);
                    menu.findItem(R.id.menu_disconnect).setVisible(false);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_control, menu);
        getMenuInflater().inflate(R.menu.actionbar_item, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        this.menu = menu;
        setNameAddress(mDeviceName, mDeviceAddress);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                if (mBluetoothLe != null) {
                    initializeBluetoothArray();
                }
                connect();
                return true;
            case R.id.menu_disconnect:
                if (mBluetoothLe != null) {
                    disconnectAllBLE();
                }
                return true;
            case android.R.id.home:
                if (mBluetoothLe != null) {
                    disconnectAllBLE();
                }
                NavUtils.navigateUpFromSameTask(this);
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void connect() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MenuItem menuItem = menu.findItem(R.id.action_status);
                menuItem.setTitle("Connecting...");
            }
        });
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.i(TAG, "onServicesDiscovered");
        if (status == BluetoothGatt.GATT_SUCCESS) {
            for (BluetoothGattService service : gatt.getServices()) {
                if ((service == null) || (service.getUuid() == null)) {
                    continue;
                }
                if (AppConstant.SERVICE_DEVICE_INFO.equals(service.getUuid())) {
                    //Read the device serial number
                    mBluetoothLe.readCharacteristic(gatt, service.getCharacteristic(AppConstant.CHAR_SERIAL_NUMBER));
                    //Read the device software version
                    mBluetoothLe.readCharacteristic(gatt, service.getCharacteristic(AppConstant.CHAR_SOFTWARE_REV));
                }

                if (AppConstant.SERVICE_3CH_EMG_SIGNAL.equals(service.getUuid())) {
                    mBluetoothLe.setCharacteristicNotification(gatt, service.getCharacteristic(AppConstant.CHAR_3CH_EMG_SIGNAL_CH1), true);
                    mBluetoothLe.setCharacteristicNotification(gatt, service.getCharacteristic(AppConstant.CHAR_3CH_EMG_SIGNAL_CH2), true);
                    mBluetoothLe.setCharacteristicNotification(gatt, service.getCharacteristic(AppConstant.CHAR_3CH_EMG_SIGNAL_CH3), true);
                }

                if (AppConstant.SERVICE_EEG_SIGNAL.equals(service.getUuid())) {
                    if (service.getCharacteristic(AppConstant.CHAR_EEG_CH1_SIGNAL) != null) {
                        mBluetoothLe.setCharacteristicNotification(gatt, service.getCharacteristic(AppConstant.CHAR_EEG_CH1_SIGNAL), true);
                        //init mCh1:
                        if (mCh1 == null) {
                            mCh1 = new DataChannel(false, mMSBFirst, 2 * mSampleRate);
                        }
                        mNumberChannels = 1;
                    }
                    if (service.getCharacteristic(AppConstant.CHAR_EEG_CH2_SIGNAL) != null) {
                        mBluetoothLe.setCharacteristicNotification(gatt, service.getCharacteristic(AppConstant.CHAR_EEG_CH2_SIGNAL), true);
                        //init mCh2
                        if (mCh2 == null) {
                            mCh2 = new DataChannel(false, mMSBFirst, 2 * mSampleRate);
                        }
                        mNumberChannels = 2;
                    }
                    if (service.getCharacteristic(AppConstant.CHAR_EEG_CH3_SIGNAL) != null) {
                        mBluetoothLe.setCharacteristicNotification(gatt, service.getCharacteristic(AppConstant.CHAR_EEG_CH3_SIGNAL), true);
                    }
                    if (service.getCharacteristic(AppConstant.CHAR_EEG_CH4_SIGNAL) != null) {
                        mBluetoothLe.setCharacteristicNotification(gatt, service.getCharacteristic(AppConstant.CHAR_EEG_CH4_SIGNAL), true);
                    }
                }

                if (AppConstant.SERVICE_EOG_SIGNAL.equals(service.getUuid())) {
                    mBluetoothLe.setCharacteristicNotification(gatt, service.getCharacteristic(AppConstant.CHAR_EOG_CH1_SIGNAL), true);
                    mBluetoothLe.setCharacteristicNotification(gatt, service.getCharacteristic(AppConstant.CHAR_EOG_CH2_SIGNAL), true);
                    for (BluetoothGattCharacteristic c : service.getCharacteristics()) {
                        if (AppConstant.CHAR_EOG_CH3_SIGNAL.equals(c.getUuid())) {
                            mBluetoothLe.setCharacteristicNotification(gatt, service.getCharacteristic(AppConstant.CHAR_EOG_CH3_SIGNAL), true);
                        }
                    }
                }

                if (AppConstant.SERVICE_BATTERY_LEVEL.equals(service.getUuid())) { //Read the device battery percentage
                    mBluetoothLe.readCharacteristic(gatt, service.getCharacteristic(AppConstant.CHAR_BATTERY_LEVEL));
                    mBluetoothLe.setCharacteristicNotification(gatt, service.getCharacteristic(AppConstant.CHAR_BATTERY_LEVEL), true);
                }
            }
        }
    }

    private void makeFilterSwitchVisible(final boolean visible) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (visible) {
                    mExportButton.setVisibility(View.VISIBLE);
                } else {
                    mExportButton.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.i(TAG, "onCharacteristicRead");
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (AppConstant.CHAR_BATTERY_LEVEL.equals(characteristic.getUuid())) {
                if (characteristic.getValue().length > 1) {
                    int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    updateBatteryStatus(batteryLevel);
                    Log.i(TAG, "Battery Level :: " + batteryLevel);
                }
            }
        } else {
            Log.e(TAG, "onCharacteristic Read Error" + status);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (AppConstant.CHAR_BATTERY_LEVEL.equals(characteristic.getUuid())) {
            int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            updateBatteryStatus(batteryLevel);
        }

        if (AppConstant.CHAR_EEG_CH1_SIGNAL.equals(characteristic.getUuid())) {
            byte[] mNewEEGdataBytes = characteristic.getValue();
            if (!mCh1.chEnabled) {
                mCh1.chEnabled = true;
            }
            getDataRateBytes(mNewEEGdataBytes.length);
            mCh1.handleNewData(mNewEEGdataBytes);

            mCh1PacketCount++;
            if (mNumberChannels == 2 && mChannelsConnected) {
                if (mCh1.packetCounter == mPacketBuffer) {
                    addToGraphBuffer(mCh1, mGraphAdapterCh1, false);
                }
            } else {
                writeToDisk24(mCh1.characteristicDataPacketBytes);
                addToGraphBuffer(mCh1, mGraphAdapterCh1, true);
            }
        }

        if (AppConstant.CHAR_EEG_CH2_SIGNAL.equals(characteristic.getUuid())) {
            if (!mCh2.chEnabled) {
                mCh2.chEnabled = true;
            }
            byte[] mNewEEGdataBytes = characteristic.getValue();
            int byteLength = mNewEEGdataBytes.length;
            getDataRateBytes(byteLength);
            mCh2.handleNewData(mNewEEGdataBytes);
            if (mNumberChannels == 2 && mChannelsConnected) {
                if (mCh2.packetCounter == mPacketBuffer) {
                    addToGraphBuffer(mCh2, mGraphAdapterCh2, false);
                }
            }
        }

        if (mNumberChannels > 1 && mCh1.chEnabled && mCh2.chEnabled) {
            mCh2PacketCount++;
            mCh1.chEnabled = false;
            mCh2.chEnabled = false;
            mChannelsConnected = true;
            if (mCh2PacketCount % 10 == 0 && mCh2PacketCount > 120) {
                writeToDisk24(mCh1.characteristicDataPacketBytes, mCh2.characteristicDataPacketBytes);
                Thread thread = new Thread(mRunnableClassifyTaskThread);
                mNumberOfClassifierCalls++;
                thread.start();

            }
        } else {
            if (mCh1PacketCount % 10 == 0 && mCh1PacketCount > 120) {
                Log.i(TAG, "[" + String.valueOf(mNumberOfClassifierCalls + 1) + "] CALLING CLASSIFIER FUNCTION!");
                Thread t = new Thread(mRunnableClassifyTaskThread);
                mNumberOfClassifierCalls++;
                t.start();
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String concat = "C:[" + mEMGClass + "]";
                mEMGClassText.setText(concat);
            }
        });
    }

    void addToGraphBuffer(DataChannel dataChannel, GraphAdapter graphAdapter, boolean updateTrainingRoutine) {
        if (byteResolution == 3) {
            for (int i = 0; i < dataChannel.dataBuffer.length / 3; i += graphAdapter.sampleRate / 250) {
                graphAdapter.addDataPoint(DataChannel.bytesToDouble(dataChannel.dataBuffer[3 * i],
                        dataChannel.dataBuffer[3 * i + 1], dataChannel.dataBuffer[3 * i + 2]),
                        dataChannel.totalDataPointsReceived - dataChannel.dataBuffer.length / 3 + i);
                if (updateTrainingRoutine) {
                    for (int j = 0; j < graphAdapter.sampleRate / 250; j++) {
                        updateTrainingRoutine(dataChannel.totalDataPointsReceived - dataChannel.dataBuffer.length / 3 + i + j);
                    }
                }
            }
        } else if (byteResolution == 2) {
            for (int i = 0; i < dataChannel.dataBuffer.length / 2; i += graphAdapter.sampleRate / 250) {
                graphAdapter.addDataPoint(DataChannel.bytesToDouble(dataChannel.dataBuffer[2 * i],
                        dataChannel.dataBuffer[2 * i + 1]),
                        dataChannel.totalDataPointsReceived - dataChannel.dataBuffer.length / 2 + i);
                if (updateTrainingRoutine) {
                    for (int j = 0; j < graphAdapter.sampleRate / 250; j++) {
                        updateTrainingRoutine(dataChannel.totalDataPointsReceived - dataChannel.dataBuffer.length / 2 + i + j);
                    }
                }
            }
        }
        dataChannel.dataBuffer = null;
        dataChannel.packetCounter = 0;
    }

    private void updateTrainingRoutine(int dataPoints) {
        if (dataPoints % mSampleRate == 0 && mRunTrainingBool) {
            int second = dataPoints / mSampleRate;
            int mSDS = (int) mStimulusDelaySeconds;
            int eventSecondCountdown = 0;
            if (second >= 0 && second < mSDS) {
                eventSecondCountdown = mSDS - second;
                updateTrainingPrompt("Relax hand");
                updateTrainingPromptColor(Color.GREEN);
                mEMGClass = 0;
            } else if (second >= mSDS && second < 2 * mSDS) {
                eventSecondCountdown = 2 * mSDS - second;
                updateTrainingPrompt("Close Hand");
                mEMGClass = 1;
            } else if (second >= 2 * mSDS && second < 3 * mSDS) {
                eventSecondCountdown = 3 * mSDS - second;
                updateTrainingPrompt("Relax hand");
                updateTrainingPromptColor(Color.GREEN);
                mEMGClass = 0;
            } else if (second >= 3 * mSDS && second < 4 * mSDS) {
                eventSecondCountdown = 4 * mSDS - second;
                updateTrainingPrompt("Rotate Hand");
                mEMGClass = 2;
            } else if (second >= 4 * mSDS && second < 5 * mSDS) {
                eventSecondCountdown = 5 * mSDS - second;
                updateTrainingPrompt("Relax hand");
                updateTrainingPromptColor(Color.GREEN);
                mEMGClass = 0;
            } else if (second >= 5 * mSDS && second < 6 * mSDS) {
                eventSecondCountdown = 6 * mSDS - second;
                updateTrainingPrompt("Forwards & Backwards");
                updateTrainingPromptColor(Color.GREEN);
                mEMGClass = 3;
            } else if (second >= 6 * mSDS && second < 7 * mSDS) {
                eventSecondCountdown = 7 * mSDS - second;
                updateTrainingPrompt("Stop!");
                updateTrainingPromptColor(Color.RED);
                mEMGClass = 0;
            } else if (second >= 7 * mSDS && second < 8 * mSDS) {
                eventSecondCountdown = 8 * mSDS - second;
                updateTrainingPrompt("Stop!");
                updateTrainingPromptColor(Color.RED);
                updateTrainingView(false);
                TrainTask trainTask = new TrainTask();
                Log.e(TAG, "CALLING TRAINING FUNCTION - ASYNCTASK!");
                trainTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                if (mUseCustomParams) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Training Data Loaded", Toast.LENGTH_LONG).show();
                        }
                    });
                    mRunTrainingBool = false;
                } else {
                    if (TrainingData != null) {
                        if (TrainingData.ERROR) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "TrainingData.ERROR \n Failed to Load Training Data", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }
            }
            if (eventSecondCountdown == mSDS) {
                mMediaBeep.start();
            }
        }
    }

    private void updateTrainingPrompt(final String prompt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRunTrainingBool) {
                    mTrainingInstructions.setText(prompt);
                }
            }
        });
    }

    private void updateTrainingView(final boolean b) {
        final int visibility = (b) ? View.VISIBLE : View.GONE;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTrainingInstructions.setVisibility(visibility);
                mEMGClassText.setVisibility(visibility);
            }
        });
    }

    private void updateTrainingPromptColor(final int color) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRunTrainingBool) {
                    mTrainingInstructions.setTextColor(color);
                }
            }
        });
    }

    Runnable mRunnableClassifyTaskThread = new Runnable() {
        @Override
        public void run() {
            double Y = 0.0;
            if (mUseCustomParams && CUSTOM_KNN_PARAMS != null) {
                if (mNumberChannels == 1)
                    Y = jClassifyUsingKNN(mCh1.classificationBuffer, CUSTOM_KNN_PARAMS);
                else if (mNumberChannels == 2) {
                    // TODO: 10/27/2017 Implement 2ch classification here.
                    Y = 0.0;
                }

            }
            processClassifiedData(Y);
        }
    };

    private void processClassifiedData(final double Y) {
        int output;
        //Shift backwards:
        System.arraycopy(mYfitArray, 1, mYfitArray, 0, mYfitArray.length-1);
        //Add to end;
        mYfitArray[mYfitArray.length-1] = (int)Y;
        //Analyze:
        Log.i(TAG, " YfitArray: " + Arrays.toString(mYfitArray));
        final boolean checkLastThreeMatches = lastThreeMatches(mYfitArray);
        if (checkLastThreeMatches) {
            // TODO: Control Drone: Tweaks?
            if((int)Y==3) {
                if(allMembersMatch(mYfitArray)) {
                    Log.e(TAG, "Found fit: " + String.valueOf(mYfitArray[mYfitArray.length-1]));
                    output = 3;
                } else {
                    output = 0;
                }
            } else {
                output = (int)Y;
            }
        } else {
            output = 0;
        }
        //Update UI on every change
        sendDroneCommand(output);
        final String s = Arrays.toString(mYfitArray)+"-[" + String.valueOf(output) + "]";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mYfitTextView.setText(s);
            }
        });
    }

    private boolean allMembersMatch(int[] mArray) {
        short count = 0;
        int compare = mArray[mArray.length-1];
        for (int d: mArray) {
            if(d==compare) count++;
        }
        return (count==mArray.length);
    }

    private boolean lastThreeMatches(int[] mArray) {
        boolean b0 = false;
        boolean b1 = false;
        if (mArray[mArray.length-1] != 0) {
            b0 = (mArray[mArray.length-1] == mArray[mArray.length-2]);
            b1 = (mArray[mArray.length-2] == mArray[mArray.length-3]);
        }
        return b0 && b1;
    }

    public static void readFromTrainingFile(File f) {
        try {
            CSVReader csvReader = new CSVReader(new FileReader(f), ',');
            List<String[]> strings = csvReader.readAll();
            Log.e(TAG, "strings.length = " + String.valueOf(strings.size()));
            TrainingData = new ClassDataAnalysis(strings, mNumberChannels, 15000);
            double[] trainingDataRawAll = ClassDataAnalysis.concatAll();
            if (!TrainingData.ERROR && trainingDataRawAll != null) {
                Log.e(TAG, "trainingDataAll.length = " + String.valueOf(trainingDataRawAll.length));
                if(mNumberChannels==1) {
                    CUSTOM_KNN_PARAMS = jTrainingRoutine(trainingDataRawAll);
                    Log.d(TAG, "CUSTOM_KNN_PARAMS length: " + String.valueOf(CUSTOM_KNN_PARAMS.length));
                    mUseCustomParams = true;
                } else if (mNumberChannels==2) {
                    // TODO: 10/27/2017 UPDATE TRAINING FOR 2 CHANNELS.
                    CUSTOM_KNN_PARAMS = null;
                    Log.e(TAG, "CUSTOM_KNN_PARAMS" + "IS NULL");
                }
                Log.e(TAG, "Custom Training Data Loaded");
            } else {
                Log.e(TAG, "Unable to Load Training Data! :: Please Run Training Session Again");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class TrainTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            readFromTrainingFile(trainingDataFile);
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private void writeToDisk24(byte[] ch1Bytes) {
        if (byteResolution == 3) {
            for (int i = 0; i < ch1Bytes.length / 3; i++) {
                try {
                    exportFileWithClass(DataChannel.bytesToDouble(ch1Bytes[3 * i], ch1Bytes[3 * i + 1], ch1Bytes[3 * i + 2]));
                } catch (IOException e) {
                    Log.e("IOException", e.toString());
                }
            }
        } else if (byteResolution == 2) {
            for (int i = 0; i < ch1Bytes.length / 2; i++) {
                try {
                    exportFileWithClass(DataChannel.bytesToDouble(ch1Bytes[2 * i], ch1Bytes[2 * i + 1]));
                } catch (IOException e) {
                    Log.e("IOException", e.toString());
                }
            }
        }
    }

    private void writeToDisk24(byte[] ch1Bytes, byte[] ch2Bytes) {
        if (byteResolution == 3) {
            for (int i = 0; i < ch1Bytes.length / 3; i++) {
                try {
                    exportFileWithClass(DataChannel.bytesToDouble(ch1Bytes[3 * i], ch1Bytes[3 * i + 1], ch1Bytes[3 * i + 2]),
                            DataChannel.bytesToDouble(ch2Bytes[3 * i], ch2Bytes[3 * i + 1], ch2Bytes[3 * i + 2]));
                } catch (IOException e) {
                    Log.e("IOException", e.toString());
                }
            }
        } else if (byteResolution == 2) {
            for (int i = 0; i < ch1Bytes.length / 2; i++) {
                try {
                    exportFileWithClass(DataChannel.bytesToDouble(ch1Bytes[2 * i], ch1Bytes[2 * i + 1]),
                            DataChannel.bytesToDouble(ch2Bytes[2 * i], ch2Bytes[2 * i + 1]));
                } catch (IOException e) {
                    Log.e("IOException", e.toString());
                }
            }
        }
    }

    private void getDataRateBytes(int bytes) {
        long mCurrentTime = System.currentTimeMillis();
        points += bytes;
        if (mCurrentTime > (mLastTime + 5000)) {
            dataRate = (points / 5);
            points = 0;
            mLastTime = mCurrentTime;
            Log.e(" DataRate:", String.valueOf(dataRate) + " Bytes/s");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String s = String.valueOf(dataRate) + " Bytes/s";
                    mDataRate.setText(s);
                }
            });
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        uiRssiUpdate(rssi);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        switch (newState) {
            case BluetoothProfile.STATE_CONNECTED:
                mConnected = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (menu != null) {
                            menu.findItem(R.id.menu_connect).setVisible(false);
                            menu.findItem(R.id.menu_disconnect).setVisible(true);
                        }
                    }
                });
                Log.i(TAG, "Connected");
                updateConnectionState(getString(R.string.connected));
                invalidateOptionsMenu();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDataRate.setTextColor(Color.BLACK);
                        mDataRate.setTypeface(null, Typeface.NORMAL);
                    }
                });
                //Start the service discovery:
                gatt.discoverServices();
                startMonitoringRssiValue();
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                mConnected = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (menu != null) {
                            menu.findItem(R.id.menu_connect).setVisible(true);
                            menu.findItem(R.id.menu_disconnect).setVisible(false);
                        }
                    }
                });
                Log.i(TAG, "Disconnected");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDataRate.setTextColor(Color.RED);
                        mDataRate.setTypeface(null, Typeface.BOLD);
                        mDataRate.setText(HZ);
                    }
                });
                updateConnectionState(getString(R.string.disconnected));
                stopMonitoringRssiValue();
                invalidateOptionsMenu();
                break;
            default:
                break;
        }
    }

    public void startMonitoringRssiValue() {
        readPeriodicallyRssiValue(true);
    }

    public void stopMonitoringRssiValue() {
        readPeriodicallyRssiValue(false);
    }

    public void readPeriodicallyRssiValue(final boolean repeat) {
        mTimerEnabled = repeat;
        // check if we should stop checking RSSI value
        if (!mConnected || mBluetoothGattArray == null || !mTimerEnabled) {
            mTimerEnabled = false;
            return;
        }

        mTimerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mBluetoothGattArray == null || !mConnected) {
                    mTimerEnabled = false;
                    return;
                }
                // request RSSI value
                mBluetoothGattArray[0].readRemoteRssi();
                // add call it once more in the future
                readPeriodicallyRssiValue(mTimerEnabled);
            }
        }, RSSI_UPDATE_TIME_INTERVAL);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic
            characteristic, int status) {
        Log.i(TAG, "onCharacteristicWrite :: Status:: " + status);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        Log.i(TAG, "onDescriptorRead :: Status:: " + status);
    }

    @Override
    public void onError(String errorMessage) {
        Log.e(TAG, "Error:: " + errorMessage);
    }

    private void updateConnectionState(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (status.equals(getString(R.string.connected))) {
                    Toast.makeText(getApplicationContext(), "Device Connected!", Toast.LENGTH_SHORT).show();
                } else if (status.equals(getString(R.string.disconnected))) {
                    Toast.makeText(getApplicationContext(), "Device Disconnected!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateBatteryStatus(final int integerValue) {
        final String status;
//        double convertedBatteryVoltage = ((double) integerValue / (4096.0)) * 7.20;
        //Because TPS63001 dies below 1.8V, we need to set up a linear fit between 1.8-4.2V
        //Anything over 4.2V = 100%
//        final double finalPercent;
//        if (((125.0 / 3.0) * convertedBatteryVoltage - 75.0) > 100.0) {
//            finalPercent = 100.0;
//        } else if (((125.0 / 3.0) * convertedBatteryVoltage - 75.0) < 0) {
//            finalPercent = 0;
//        } else {
//            finalPercent = (125.0 / 3.0) * convertedBatteryVoltage - 75.0;
//        }
        Log.e(TAG, "Battery Integer Value: " + String.valueOf(integerValue));
        Log.e(TAG, "ConvertedBatteryVoltage: " + String.format(Locale.US, "%.1d", integerValue)+"%");
        status = "EMG Battery: "+String.format(Locale.US, "%.1d", integerValue) + "%";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (integerValue <= batteryWarning) {
                    mBatteryLevel.setTextColor(Color.RED);
                    mBatteryLevel.setTypeface(null, Typeface.BOLD);
                    Toast.makeText(getApplicationContext(), "Charge Battery, Battery Low " + status, Toast.LENGTH_SHORT).show();
                } else {
                    mBatteryLevel.setTextColor(Color.GREEN);
                    mBatteryLevel.setTypeface(null, Typeface.BOLD);
                }
                mBatteryLevel.setText(status);
            }
        });
    }

    private void uiRssiUpdate(final int rssi) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MenuItem menuItem = menu.findItem(R.id.action_rssi);
                MenuItem status_action_item = menu.findItem(R.id.action_status);
                final String valueOfRSSI = String.valueOf(rssi) + " dB";
                menuItem.setTitle(valueOfRSSI);
                if (mConnected) {
                    String newStatus = "Status: " + getString(R.string.connected);
                    status_action_item.setTitle(newStatus);
                } else {
                    String newStatus = "Status: " + getString(R.string.disconnected);
                    status_action_item.setTitle(newStatus);
                }
            }
        });
    }

    private final MiniDrone.Listener mMiniDroneListener = new MiniDrone.Listener() {
        @Override
        public void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
            switch (state) {
                case ARCONTROLLER_DEVICE_STATE_RUNNING:
                    mConnectionProgressDialog.dismiss();
                    break;

                case ARCONTROLLER_DEVICE_STATE_STOPPED:
                    // if the deviceController is stopped, go back to the previous activity
                    mConnectionProgressDialog.dismiss();
                    finish();
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onBatteryChargeChanged(int batteryPercentage) {
            String batteryFormatted = String.format(Locale.US, "Drone Battery: [%d%%]", batteryPercentage);
            mBatteryLevelDrone.setText(batteryFormatted);
        }

        @Override
        public void onPilotingStateChanged(ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
            switch (state) {
                case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                    String to = "Take off";
                    mTakeOffLandBt.setText(to);
                    mTakeOffLandBt.setEnabled(true);
//                    mDownloadBt.setEnabled(true);
                    break;
                case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                    String ld = "Land";
                    mTakeOffLandBt.setText(ld);
                    mTakeOffLandBt.setEnabled(true);
//                    mDownloadBt.setEnabled(false);
                    break;
                default:
                    mTakeOffLandBt.setEnabled(false);
//                    mDownloadBt.setEnabled(false);
            }
        }

        @Override
        public void onPictureTaken(ARCOMMANDS_MINIDRONE_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error) {
            Log.i(TAG, "Picture has been taken");
        }

        @Override
        public void configureDecoder(ARControllerCodec codec) {
//            mVideoView.configureDecoder(codec);
        }

        @Override
        public void onFrameReceived(ARFrame frame) {
//            mVideoView.displayFrame(frame);
        }

        @Override
        public void onMatchingMediasFound(int nbMedias) {
            mDownloadProgressDialog.dismiss();
            mNbMaxDownload = nbMedias;
            mCurrentDownloadIndex = 1;
            if (nbMedias > 0) {
                mDownloadProgressDialog = new ProgressDialog(DeviceControlActivity.this, R.style.AppCompatAlertDialogStyle);
                mDownloadProgressDialog.setIndeterminate(false);
                mDownloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mDownloadProgressDialog.setMessage("Downloading medias");
                mDownloadProgressDialog.setMax(mNbMaxDownload * 100);
                mDownloadProgressDialog.setSecondaryProgress(mCurrentDownloadIndex * 100);
                mDownloadProgressDialog.setProgress(0);
                mDownloadProgressDialog.setCancelable(false);
                mDownloadProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mMiniDrone.cancelGetLastFlightMedias();
                    }
                });
                mDownloadProgressDialog.show();
            }
        }

        @Override
        public void onDownloadProgressed(String mediaName, int progress) {
            mDownloadProgressDialog.setProgress(((mCurrentDownloadIndex - 1) * 100) + progress);
        }

        @Override
        public void onDownloadComplete(String mediaName) {
            mCurrentDownloadIndex++;
            mDownloadProgressDialog.setSecondaryProgress(mCurrentDownloadIndex * 100);

            if (mCurrentDownloadIndex > mNbMaxDownload) {
                mDownloadProgressDialog.dismiss();
                mDownloadProgressDialog = null;
            }
        }
    };

    /*
    * Application of JNI code:
    */
    static {
        System.loadLibrary("emg1c-lib");
    }

    public native int jmainInitialization();

    /**
     * 1CH Classification using KNN
     *
     * @param a samples
     * @param b labeled features.
     * @return Y output
     */
    public native double jClassifyUsingKNN(double[] a, double[] b);

    /**
     * 1CH Training data extraction
     *
     * @param data all data
     * @return features[]
     */
    public static native double[] jTrainingRoutine(double[] data);

}
