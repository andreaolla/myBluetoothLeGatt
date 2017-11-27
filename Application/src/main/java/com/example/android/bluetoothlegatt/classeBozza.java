package com.example.android.bluetoothlegatt;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.List;
import java.util.UUID;

import static java.lang.Thread.sleep;

/**
 * Created by andre on 22/11/2017.
 */

public class classeBozza extends Thread{

    Handler wHandler;

    public classeBozza(Handler wHandler){
        this.wHandler = wHandler;
    }

    private BluetoothLeService mBluetoothLeService;

    /* EMG Service UUID */
    public static UUID EMG_SERVICE = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
    /* Mandatory Current Value Information Characteristic */
    public static UUID CURRENT_VALUE    = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb");

    public void run() {
        while (true){
            Log.i("Thread acquisizione", "is running");
            try {sleep(50);} catch (InterruptedException e) {e.printStackTrace();}
            // Send the Message to the Main activity.
            Message readMsg = wHandler.obtainMessage();
            wHandler.sendMessage(readMsg);

        }
    }
}
