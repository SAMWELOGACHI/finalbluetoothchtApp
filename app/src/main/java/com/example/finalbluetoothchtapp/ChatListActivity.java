package com.example.finalbluetoothchtapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalbluetoothchtapp.db.AppDatabase;
import com.example.finalbluetoothchtapp.db.MessageEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatListActivity extends AppCompatActivity {

    private ChatListAdapter adapter;
    private final List<ChatListAdapter.ChatSession> chatSessions = new ArrayList<>();
    private RecyclerView rvChats;
    private View llEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        rvChats = findViewById(R.id.rvChats);
        llEmptyState = findViewById(R.id.llEmptyState);

        rvChats.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatListAdapter(chatSessions, chat -> {
            Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
            intent.putExtra("device_name", chat.getName());
            intent.putExtra("device_address", chat.getAddress());
            startActivity(intent);
        });
        rvChats.setAdapter(adapter);

        findViewById(R.id.fabNewChat).setOnClickListener(v -> {
            Intent intent = new Intent(ChatListActivity.this, MainActivity.class);
            startActivity(intent);
        });
        
        findViewById(R.id.btnProfile).setOnClickListener(v -> {
            Intent intent = new Intent(ChatListActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentChats();
    }

    private void loadRecentChats() {
        new Thread(() -> {
            List<MessageEntity> recentMessages = AppDatabase.getInstance(this).messageDao().getRecentChats();
            List<ChatListAdapter.ChatSession> loadedSessions = new ArrayList<>();

            ChatConnection activeConn = ChatHolder.connection;
            String activeAddress = null;
            if (activeConn != null && activeConn.getDevice() != null) {
                try {
                    activeAddress = activeConn.getDevice().getAddress();
                } catch (SecurityException ignored) {}
            }

            for (MessageEntity entity : recentMessages) {
                String lastMsgPreview = "";
                if (entity.content != null) {
                    lastMsgPreview = entity.content;
                } else if (entity.imageUri != null) {
                    lastMsgPreview = "📷 Photo";
                } else if (entity.audioUri != null) {
                    lastMsgPreview = "🎙️ Voice Message";
                }

                String formattedTime = formatTime(entity.timestamp);
                boolean isOnline = entity.senderAddress != null && entity.senderAddress.equals(activeAddress);
                
                String name = entity.senderName;
                if (name == null || name.isEmpty() || name.equalsIgnoreCase("Me")) {
                    name = "Bluetooth Device";
                }

                String avatarBase64 = "";
                if (entity.senderAddress != null) {
                    android.content.SharedPreferences prefs = getSharedPreferences("PeerProfiles", MODE_PRIVATE);
                    String savedName = prefs.getString(entity.senderAddress + "_name", "");
                    if (!savedName.isEmpty()) {
                        name = savedName;
                    }
                    avatarBase64 = prefs.getString(entity.senderAddress + "_avatar", "");
                }

                loadedSessions.add(new ChatListAdapter.ChatSession(
                        name,
                        entity.senderAddress,
                        lastMsgPreview,
                        formattedTime,
                        0, // Unread badge placeholder
                        isOnline,
                        avatarBase64
                ));
            }

            runOnUiThread(() -> {
                chatSessions.clear();
                chatSessions.addAll(loadedSessions);
                adapter.notifyDataSetChanged();

                if (chatSessions.isEmpty()) {
                    llEmptyState.setVisibility(View.VISIBLE);
                    rvChats.setVisibility(View.GONE);
                } else {
                    llEmptyState.setVisibility(View.GONE);
                    rvChats.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    private String formatTime(long timestamp) {
        if (android.text.format.DateUtils.isToday(timestamp)) {
            return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(timestamp));
        } else {
            return new SimpleDateFormat("MMM dd", Locale.getDefault()).format(new Date(timestamp));
        }
    }
}