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
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothServer extends Thread {
    private final BluetoothServerSocket serverSocket;
    private final Context context;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

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

            // Get streams
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            // Store connection globally if needed
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

            // Start reading messages
            readMessages();

        } catch (IOException e) {
            Log.e("BluetoothServer", "Error accepting connection", e);
            close();
        }
    }

    // Send message to client
    public void sendMessage(String message) {
        if (outputStream != null) {
            try {
                outputStream.write(message.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                Log.e("BluetoothServer", "Error sending message", e);
            }
        }
    }

    // Read incoming messages
    private void readMessages() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            try {
                while ((bytes = inputStream.read(buffer)) != -1) {
                    String received = new String(buffer, 0, bytes);
                    Log.d("BluetoothServer", "Received: " + received);
                    // TODO: update UI with received message
                }
            } catch (IOException e) {
                Log.e("BluetoothServer", "Error reading", e);
            }
        }).start();
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