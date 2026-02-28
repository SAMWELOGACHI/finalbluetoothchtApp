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
    public BluetoothSocket connect() {
        // Step 1: Try Secure RFCOMM
        try {
            Log.d("BluetoothClient", "Attempting Secure RFCOMM connection...");
            socket = device.createRfcommSocketToServiceRecord(ChatHolder.APP_UUID);
            adapter.cancelDiscovery();
            socket.connect();
            return initStreams();
        } catch (IOException e) {
            Log.w("BluetoothClient", "Secure connection failed, trying Insecure...", e);
        }

        // Step 2: Try Insecure RFCOMM (bypasses pairing on many devices)
        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(ChatHolder.APP_UUID);
            socket.connect();
            return initStreams();
        } catch (IOException e) {
            Log.w("BluetoothClient", "Insecure connection failed, trying Reflection...", e);
        }

        // Step 3: Reflection Fallback (last resort)
        try {
            socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] { int.class })
                    .invoke(device, 1);
            socket.connect();
            return initStreams();
        } catch (Exception e) {
            Log.e("BluetoothClient", "All connection attempts failed: " + e.getMessage());
            close();
            return null;
        }
    }

    private BluetoothSocket initStreams() throws IOException {
        Log.d("BluetoothClient", "Connected! Initializing streams...");
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
        return socket;
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
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            Log.e("BluetoothClient", "Error closing socket", e);
        }
    }

    // Listener interface (optional if you want async callbacks)
    public interface OnConnectedListener {
        void onConnected(BluetoothSocket socket, InputStream in, OutputStream out);
    }
}