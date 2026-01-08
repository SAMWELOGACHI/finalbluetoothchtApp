package com.example.finalbluetoothchtapp;

public class Message {
    private final String text;       // message content
    private final boolean isMe;      // true if sent by "Me", false if from peer
    private final long timestamp;    // time in milliseconds
    private final String status;     // optional: "sent", "delivered", "seen"

    // Basic constructor (default status = "sent")
    public Message(String text, boolean isMe) {
        this(text, isMe, System.currentTimeMillis(), "sent");
    }

    // Full constructor
    public Message(String text, boolean isMe, long timestamp, String status) {
        this.text = text;
        this.isMe = isMe;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getText() {
        return text;
    }

    public boolean isMe() {
        return isMe;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }
}
