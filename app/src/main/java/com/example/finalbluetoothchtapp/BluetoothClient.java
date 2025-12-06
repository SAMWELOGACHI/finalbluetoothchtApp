package com.example.finalbluetoothchtapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothClient {
    private final BluetoothDevice device;
    private final BluetoothAdapter adapter;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    public BluetoothClient(BluetoothDevice device, BluetoothAdapter adapter) {
        this.device = device;
        this.adapter = adapter;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void connect(OnConnectedListener listener) {
        new Thread(() -> {
            try {
                // ✅ Use shared UUID
                socket = device.createRfcommSocketToServiceRecord(ChatHolder.APP_UUID);

                adapter.cancelDiscovery();
                socket.connect();
                Log.d("BluetoothClient", "Connected to server!");

                // Get streams
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                // Notify listener
                listener.onConnected(socket, inputStream, outputStream);

            } catch (IOException e) {
                Log.e("BluetoothClient", "Connection failed: " + e.getMessage(), e);
                close();
            }
        }).start();
    }

    // Send message to server
    public void sendMessage(String message) {
        if (outputStream != null) {
            try {
                outputStream.write(message.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                Log.e("BluetoothClient", "Error sending message", e);
            }
        }
    }

    // Close connection
    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            Log.e("BluetoothClient", "Error closing socket", e);
        }
    }

    // Listener interface
    public interface OnConnectedListener {
        void onConnected(BluetoothSocket socket, InputStream in, OutputStream out);
    }
}