package com.example.finalbluetoothchtapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.RequiresPermission;

import java.io.IOException;

public class BluetoothServer extends Thread {
    private final BluetoothServerSocket serverSocket;
    private final Context context;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public BluetoothServer(Context context) throws IOException {
        this.context = context;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        // ✅ Use shared UUID
        serverSocket = adapter.listenUsingRfcommWithServiceRecord("ChatApp", ChatHolder.APP_UUID);
    }

    @Override
    public void run() {
        BluetoothSocket socket;
        while (true) {
            try {
                socket = serverSocket.accept(); // blocks until a client connects
            } catch (IOException e) {
                break;
            }
            if (socket != null) {
                try {
                    ChatConnection conn = new ChatConnection(socket);
                    ChatHolder.connection = conn;

                    // Launch ChatActivity
                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break; // stop after one connection
            }
        }
    }

    public void cancel() {
        try { serverSocket.close(); } catch (IOException ignored) {}
    }
}