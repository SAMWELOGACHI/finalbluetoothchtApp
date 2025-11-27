package com.example.finalbluetoothchtapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ChatListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        RecyclerView rvChats = findViewById(R.id.rvChats);
        rvChats.setLayoutManager(new LinearLayoutManager(this));

        // Dummy data for production demo
        java.util.List<ChatListAdapter.ChatSession> chats = new java.util.ArrayList<>();
        chats.add(new ChatListAdapter.ChatSession("Alice", "Hey, are you free to test this out?", "10:45 AM", 2, true));
        chats.add(new ChatListAdapter.ChatSession("Bob", "Okay, I'm connected now.", "9:30 AM", 0, true));
        chats.add(new ChatListAdapter.ChatSession("Charlie", "Let's sync up tomorrow.", "Yesterday", 0, false));
        chats.add(new ChatListAdapter.ChatSession("Diana", "Sounds good!", "3/15/24", 0, false));

        ChatListAdapter adapter = new ChatListAdapter(chats, chat -> {
            // Handle chat click - for now just toast or open chat activity
            // startActivity(new Intent(this, ChatActivity.class));
        });
        rvChats.setAdapter(adapter);

        findViewById(R.id.fabNewChat).setOnClickListener(v -> {
            // Open device discovery
            // startActivity(new Intent(this, MainActivity.class));
        });
    }
}
