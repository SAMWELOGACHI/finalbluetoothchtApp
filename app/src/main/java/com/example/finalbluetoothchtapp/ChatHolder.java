package com.example.finalbluetoothchtapp;

import java.util.UUID;

public class ChatHolder {
    // Shared UUID for both server and client
    public static final UUID APP_UUID =
            UUID.fromString("6f58c0bb-2f6a-4f26-9b8b-7f2c9b6c9b11");

    // Global connection reference
    static ChatConnection connection;

    // Optional: store connected device name
    private static String connectedDeviceName;

    public static ChatConnection getConnection() {
        return connection;
    }

    public static void setConnection(ChatConnection conn) {
        connection = conn;
    }

    public static String getConnectedDeviceName() {
        return connectedDeviceName;
    }

    public static void setConnectedDeviceName(String name) {
        connectedDeviceName = name;
    }
}
