/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class WorkActivity extends Activity {

        /* *********************************************************************************************
    *                                      FIELDS
    ********************************************************************************************** */

    //Bluetooth Fields
    private final static String TAG = WorkActivity.class.getSimpleName();
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private Button mDataField;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    /* EMG Service UUID */
    public static UUID EMG_SERVICE = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
    /* Mandatory Current Value Information Characteristic */
    public static UUID CURRENT_VALUE = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb");


    //MyApp Fields
    /*

*/
    Float value1;
    Float value2;
    Float value3;


    /* *********************************************************************************************
    *                             ACTIVITY MANAGEMENT
    ********************************************************************************************** */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work);

        final Intent intent = getIntent();
        String mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        mDataField = (Button) findViewById(R.id.buttonGO);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        BluetoothHandler HND = new BluetoothHandler();
        final classeBozza Controller = new classeBozza(HND);

        mDataField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                // azioni da fare
                Controller.start();
            }


        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    /* *********************************************************************************************
    *                             CLASS
    ********************************************************************************************** */

    @SuppressLint("HandlerLeak")
    private class BluetoothHandler extends Handler {
        @SuppressLint({"SetTextI18n", "LongLogTag"})
        @Override
        public void handleMessage(Message msg) {

            List<BluetoothGattService> gattServices = mBluetoothLeService.getSupportedGattServices();

            for (BluetoothGattService gattService : gattServices) {
                UUID A = gattService.getUuid();
                if (A.equals(EMG_SERVICE)) {
                    List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        if (gattCharacteristic.getUuid().equals(CURRENT_VALUE)) {
                            Log.d("Uuid CHARACTERISTIC verification ", " YES");
                            int charaProp = gattCharacteristic.getProperties();

                            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                                // If there is an active notification on a characteristic, clear
                                // it first so it doesn't update the data field on the user interface.
                                if (mNotifyCharacteristic != null) {
                                    mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                                    mNotifyCharacteristic = null;
                                }
                                mBluetoothLeService.readCharacteristic(gattCharacteristic);

                                Log.d("Characteristic is    ", Arrays.toString(gattCharacteristic.getValue()));

                            }
                            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                mNotifyCharacteristic = gattCharacteristic;
                                mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                            }
                        }
                    }
                }
            }
        }
    }

    /* *********************************************************************************************
    *                             METHODS
    ********************************************************************************************** */

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);

/*
            Button btn1 = (Button) findViewById(R.id.muscle1);
            Button btn2 = (Button) findViewById(R.id.muscle2);
            Button btn3 = (Button) findViewById(R.id.muscle3);

            value1= value2 = value3 = Float.valueOf (data);
            selectButtonBackground(btn1,value1);
            selectButtonBackground(btn2,value2);
            selectButtonBackground(btn3,value3);*/

            Log.d("Display data ->    ", "data");
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void selectButtonBackground(Button button, Float value){
        String val = Float.toString(value);

        button.setText(val);

        if (value < -0.05) {
            button.setSelected(true);
            button.setActivated(false);
        } //   Cambia colore attraverso
        if (value >= -0.05 && value <= 0) {
            button.setSelected(true);
            button.setActivated(true);
        } //   selector_muscle_2
        if (value > 0) {
            button.setSelected(false);
            button.setActivated(true);
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };
}
