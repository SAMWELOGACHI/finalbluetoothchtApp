package com.example.finalbluetoothchtapp.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String senderAddress;
    public String senderName;
    public String content;
    public String imageUri;
    public long timestamp;
    public boolean isMe;

    public MessageEntity(String senderAddress, String senderName, String content, String imageUri, long timestamp,
            boolean isMe) {
        this.senderAddress = senderAddress;
        this.senderName = senderName;
        this.content = content;
        this.imageUri = imageUri;
        this.timestamp = timestamp;
        this.isMe = isMe;
    }
}
