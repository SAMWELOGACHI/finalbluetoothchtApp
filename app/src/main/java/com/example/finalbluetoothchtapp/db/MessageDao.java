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

    @Query("DELETE FROM messages")
    void deleteAll();
}
