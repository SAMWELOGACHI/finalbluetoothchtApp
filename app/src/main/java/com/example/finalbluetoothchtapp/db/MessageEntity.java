package com.example.finalbluetoothchtapp.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "messages")
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String senderAddress;
    public String senderName;
    public String content;
    public String imageUri;
    public String audioUri;
    public String status; // "sent", "delivered", "read"
    public String reaction;
    public String replyToText;
    public String replyToSender;
    public long timestamp;
    public boolean isMe;

    public MessageEntity() {
    }

    @Ignore
    public MessageEntity(String senderAddress, String senderName, String content, String imageUri, long timestamp,
            boolean isMe) {
        this.senderAddress = senderAddress;
        this.senderName = senderName;
        this.content = content;
        this.imageUri = imageUri;
        this.audioUri = null;
        this.status = "sent";
        this.reaction = null;
        this.replyToText = null;
        this.replyToSender = null;
        this.timestamp = timestamp;
        this.isMe = isMe;
    }

    @Ignore
    public MessageEntity(String senderAddress, String senderName, String content, String imageUri, String audioUri,
            String status, String reaction, String replyToText, String replyToSender, long timestamp,
            boolean isMe) {
        this.senderAddress = senderAddress;
        this.senderName = senderName;
        this.content = content;
        this.imageUri = imageUri;
        this.audioUri = audioUri;
        this.status = status;
        this.reaction = reaction;
        this.replyToText = replyToText;
        this.replyToSender = replyToSender;
        this.timestamp = timestamp;
        this.isMe = isMe;
    }
}
