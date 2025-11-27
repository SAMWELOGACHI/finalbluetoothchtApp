package com.example.finalbluetoothchtapp;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private ChatAdapter chatAdapter;
    private final List<Message> messages = new ArrayList<>();
    private ChatConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        RecyclerView recyclerView = findViewById(R.id.rvMessages);
        EditText input = findViewById(R.id.etMessage);
        View sendBtn = findViewById(R.id.btnSend); // Changed to View or ImageView
        View backBtn = findViewById(R.id.btnBack);
        android.widget.TextView tvTitle = findViewById(R.id.tvChatTitle);

        String deviceName = getIntent().getStringExtra("device_name");
        if (deviceName != null && tvTitle != null) {
            tvTitle.setText("Connected to " + deviceName);
        }

        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        chatAdapter = new ChatAdapter(messages);
        recyclerView.setAdapter(chatAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        connection = ChatHolder.connection;

        // Listen for incoming messages
        if (connection != null) {
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = connection.readLine()) != null) {
                        Message message = new Message(msg, false); // Peer message
                        runOnUiThread(() -> {
                            messages.add(message);
                            chatAdapter.notifyItemInserted(messages.size() - 1);
                            recyclerView.scrollToPosition(messages.size() - 1);
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        android.widget.Toast
                                .makeText(ChatActivity.this, "Connection lost", android.widget.Toast.LENGTH_SHORT)
                                .show();
                        finish();
                    });
                }
            }).start();
        }

        // Send button
        sendBtn.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (!text.isEmpty()) {
                if (connection != null) {
                    try {
                        connection.writeLine(text);
                        Message message = new Message(text, true); // Me message
                        messages.add(message);
                        chatAdapter.notifyItemInserted(messages.size() - 1);
                        recyclerView.scrollToPosition(messages.size() - 1);
                        input.setText("");
                    } catch (IOException e) {
                        e.printStackTrace();
                        android.widget.Toast
                                .makeText(ChatActivity.this, "Failed to send", android.widget.Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}