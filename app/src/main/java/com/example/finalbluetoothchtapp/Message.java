package com.example.finalbluetoothchtapp;

public class Message {
    private final String text; // message content
    private final String imageUri; // image URI (null if text)
    private final String audioUri; // audio URI (null if not audio)
    private final boolean isMe; // true if sent by "Me", false if from peer
    private final long timestamp; // time in milliseconds
    private String status; // "sent", "delivered", "read"
    private String reaction; // emoji reaction (null if none)
    private final String replyToText; // quoted message text (null if none)
    private final String replyToSender; // quoted message sender (null if none)

    // Basic text constructor
    public Message(String text, boolean isMe) {
        this(text, null, null, isMe, System.currentTimeMillis(), "sent", null, null, null);
    }

    // Basic image constructor
    public Message(String text, String imageUri, boolean isMe) {
        this(text, imageUri, null, isMe, System.currentTimeMillis(), "sent", null, null, null);
    }

    // Basic audio constructor
    public Message(String text, String imageUri, String audioUri, boolean isMe) {
        this(text, imageUri, audioUri, isMe, System.currentTimeMillis(), "sent", null, null, null);
    }

    // Full constructor
    public Message(String text, String imageUri, String audioUri, boolean isMe, long timestamp, String status,
            String reaction, String replyToText, String replyToSender) {
        this.text = text;
        this.imageUri = imageUri;
        this.audioUri = audioUri;
        this.isMe = isMe;
        this.timestamp = timestamp;
        this.status = status;
        this.reaction = reaction;
        this.replyToText = replyToText;
        this.replyToSender = replyToSender;
    }

    public String getText() {
        return text;
    }

    public String getImageUri() {
        return imageUri;
    }

    public String getAudioUri() {
        return audioUri;
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

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReaction() {
        return reaction;
    }

    public void setReaction(String reaction) {
        this.reaction = reaction;
    }

    public String getReplyToText() {
        return replyToText;
    }

    public String getReplyToSender() {
        return replyToSender;
    }

    public boolean isImage() {
        return imageUri != null;
    }

    public boolean isAudio() {
        return audioUri != null;
    }

    public boolean isReply() {
        return replyToText != null;
    }
}
