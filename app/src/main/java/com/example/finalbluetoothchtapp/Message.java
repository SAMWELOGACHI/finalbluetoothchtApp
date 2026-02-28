package com.example.finalbluetoothchtapp;

public class Message {
    private final String text; // message content
    private final String imageUri; // image URI (null if text)
    private final boolean isMe; // true if sent by "Me", false if from peer
    private final long timestamp; // time in milliseconds
    private final String status; // optional: "sent", "delivered", "seen"

    // Basic text constructor
    public Message(String text, boolean isMe) {
        this(text, null, isMe, System.currentTimeMillis(), "sent");
    }

    // Basic image constructor
    public Message(String text, String imageUri, boolean isMe) {
        this(text, imageUri, isMe, System.currentTimeMillis(), "sent");
    }

    // Full constructor
    public Message(String text, String imageUri, boolean isMe, long timestamp, String status) {
        this.text = text;
        this.imageUri = imageUri;
        this.isMe = isMe;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getText() {
        return text;
    }

    public String getImageUri() {
        return imageUri;
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

    public boolean isImage() {
        return imageUri != null;
    }
}
