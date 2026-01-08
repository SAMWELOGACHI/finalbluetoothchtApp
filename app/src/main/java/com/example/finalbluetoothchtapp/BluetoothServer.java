package com.example.finalbluetoothchtapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.io.IOException;

public class BluetoothServer extends Thread {
    private final BluetoothServerSocket serverSocket;
    private final Context context;
    private BluetoothSocket socket;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothServer(Context context) throws IOException {
        this.context = context;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        // ✅ Use shared UUID
        serverSocket = adapter.listenUsingRfcommWithServiceRecord("ChatApp", ChatHolder.APP_UUID);
    }

    @Override
    public void run() {
        try {
            Log.d("BluetoothServer", "Waiting for client...");
            socket = serverSocket.accept(); // blocks until a client connects
            Log.d("BluetoothServer", "Client connected!");

            // Store connection globally
            ChatHolder.connection = new ChatConnection(socket);

            // Launch ChatActivity
            Intent intent = new Intent(context, ChatActivity.class);
            if (socket.getRemoteDevice() != null) {
                try {
                    intent.putExtra("device_name", socket.getRemoteDevice().getName());
                } catch (SecurityException e) {
                    // Ignore if permission missing
                }
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            // We do NOT read messages here anymore.
            // ChatActivity will read from ChatHolder.connection

        } catch (IOException e) {
            Log.e("BluetoothServer", "Error accepting connection", e);
            close();
        }
    }

    // Close connection
    public void close() {
        try {
            if (socket != null) socket.close();
            serverSocket.close();
        } catch (IOException e) {
            Log.e("BluetoothServer", "Error closing socket", e);
        }
    }
}
