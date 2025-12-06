package com.example.finalbluetoothchtapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        RecyclerView rvChats = findViewById(R.id.rvChats);
        rvChats.setLayoutManager(new LinearLayoutManager(this));

        // Dummy data for now
        List<ChatListAdapter.ChatSession> chats = new ArrayList<>();
        chats.add(new ChatListAdapter.ChatSession("Alice", "Hey, are you free to test this out?", "10:45 AM", 2, true));
        chats.add(new ChatListAdapter.ChatSession("Bob", "Okay, I'm connected now.", "9:30 AM", 0, true));
        chats.add(new ChatListAdapter.ChatSession("Charlie", "Let's sync up tomorrow.", "Yesterday", 0, false));
        chats.add(new ChatListAdapter.ChatSession("Diana", "Sounds good!", "3/15/24", 0, false));

        ChatListAdapter adapter = new ChatListAdapter(chats, chat -> {
            // Open ChatActivity when a chat is clicked
            Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
            intent.putExtra("device_name", chat.getName());
            startActivity(intent);
        });
        rvChats.setAdapter(adapter);

        // Floating Action Button: start device discovery
        findViewById(R.id.fabNewChat).setOnClickListener(v -> {
            Intent intent = new Intent(ChatListActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }
}