package com.example.finalbluetoothchtapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    void insert(MessageEntity message);

    @Query("SELECT * FROM messages WHERE senderAddress = :address ORDER BY timestamp ASC")
    List<MessageEntity> getMessagesForDevice(String address);

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    List<MessageEntity> getAllMessages();

    @Query("SELECT * FROM messages WHERE id IN (SELECT MAX(id) FROM messages GROUP BY senderAddress) ORDER BY timestamp DESC")
    List<MessageEntity> getRecentChats();

    @Query("DELETE FROM messages")
    void deleteAll();

    @Query("UPDATE messages SET status = :status WHERE timestamp = :timestamp AND isMe = :isMe")
    void updateMessageStatus(long timestamp, boolean isMe, String status);

    @Query("UPDATE messages SET reaction = :reaction WHERE timestamp = :timestamp")
    void updateMessageReaction(long timestamp, String reaction);

    @Query("UPDATE messages SET content = :newContent WHERE timestamp = :timestamp")
    void updateMessageContent(long timestamp, String newContent);

    @Query("UPDATE messages SET content = :deletedPlaceholder, imageUri = null, audioUri = null WHERE timestamp = :timestamp")
    void deleteMessageContent(long timestamp, String deletedPlaceholder);
}
