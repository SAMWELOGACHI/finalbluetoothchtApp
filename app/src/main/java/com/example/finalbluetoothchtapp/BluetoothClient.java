package com.example.finalbluetoothchtapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import androidx.annotation.RequiresPermission;

import java.io.IOException;

public class BluetoothClient {
    private final BluetoothDevice device;
    private final BluetoothAdapter adapter;

    public BluetoothClient(BluetoothDevice device, BluetoothAdapter adapter) {
        this.device = device;
        this.adapter = adapter;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void connect(OnConnectedListener listener) {
        new Thread(() -> {
            try {
                // ✅ Use shared UUID
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(ChatHolder.APP_UUID);
                adapter.cancelDiscovery();
                socket.connect();
                listener.onConnected(socket);
            } catch (IOException e) {
                e.printStackTrace();
                android.util.Log.e("BluetoothClient", "Connection failed: " + e.getMessage());
            }
        }).start();
    }

    public interface OnConnectedListener {
        void onConnected(BluetoothSocket socket);
    }
}