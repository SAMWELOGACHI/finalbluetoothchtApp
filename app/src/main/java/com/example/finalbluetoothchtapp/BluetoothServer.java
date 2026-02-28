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
    private boolean isRunning = true;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothServer(Context context) throws IOException {
        this.context = context;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        // Use Insecure listener for better compatibility with non-paired devices
        serverSocket = adapter.listenUsingInsecureRfcommWithServiceRecord("ChatApp", ChatHolder.APP_UUID);
    }

    @Override
    public void run() {
        try {
            Log.d("BluetoothServer", "Waiting for client...");
            socket = serverSocket.accept(); // blocks until a client connects

            if (!isRunning) {
                close();
                return;
            }

            Log.d("BluetoothServer", "Client connected!");
            // ... (rest of the logic)
            ChatHolder.connection = new ChatConnection(socket);

            Intent serviceIntent = new Intent(context, BluetoothService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            Intent intent = new Intent(context, ChatActivity.class);
            if (socket.getRemoteDevice() != null) {
                intent.putExtra("device_name", socket.getRemoteDevice().getName());
                intent.putExtra("device_address", socket.getRemoteDevice().getAddress());
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

        } catch (IOException e) {
            if (isRunning) {
                Log.e("BluetoothServer", "Error accepting connection", e);
            }
            close();
        }
    }

    public void stopServer() {
        isRunning = false;
        close();
    }

    public void close() {
        try {
            if (socket != null)
                socket.close();
            if (serverSocket != null)
                serverSocket.close();
        } catch (IOException e) {
            Log.e("BluetoothServer", "Error closing socket", e);
        }
    }
}
