package com.example.finalbluetoothchtapp;

public class Message {
    private final String text;     // message content
    private final boolean isMe;    // true if sent by "Me", false if from peer

    public Message(String text, boolean isMe) {
        this.text = text;
        this.isMe = isMe;
    }

    public String getText() {
        return text;
    }

    public boolean isMe() {
        return isMe;
    }
}