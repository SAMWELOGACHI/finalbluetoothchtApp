package com.example.finalbluetoothchtapp;

import android.bluetooth.BluetoothSocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class ChatConnection {
    private final BluetoothSocket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;

    public ChatConnection(BluetoothSocket socket) throws IOException {
        this.socket = socket;
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
    }

    public String readLine() throws IOException {
        return reader.readLine();
    }

    public void writeLine(String line) throws IOException {
        writer.println(line);
    }

    public void close() throws IOException {
        socket.close();
    }
}