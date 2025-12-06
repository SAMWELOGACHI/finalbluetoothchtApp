package com.example.finalbluetoothchtapp;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
    private Thread readThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        RecyclerView recyclerView = findViewById(R.id.rvMessages);
        EditText input = findViewById(R.id.etMessage);
        View sendBtn = findViewById(R.id.btnSend);
        View backBtn = findViewById(R.id.btnBack);
        TextView tvTitle = findViewById(R.id.tvChatTitle);

        String deviceName = getIntent().getStringExtra("device_name");
        if (deviceName != null) {
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
            readThread = new Thread(() -> {
                try {
                    String msg;
                    while (!Thread.currentThread().isInterrupted() &&
                            (msg = connection.readLine()) != null) {
                        Message message = new Message(msg, false); // Peer message
                        runOnUiThread(() -> {
                            messages.add(message);
                            chatAdapter.notifyItemInserted(messages.size() - 1);
                            recyclerView.smoothScrollToPosition(messages.size() - 1);
                        });
                    }
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "Connection lost", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            });
            readThread.start();
        }

        // Send button
        if (sendBtn != null) {
            sendBtn.setOnClickListener(v -> {
                String text = input.getText().toString().trim();
                if (!text.isEmpty() && connection != null) {
                    try {
                        connection.writeLine(text);
                        Message message = new Message(text, true); // Me message
                        messages.add(message);
                        chatAdapter.notifyItemInserted(messages.size() - 1);
                        recyclerView.smoothScrollToPosition(messages.size() - 1);
                        input.setText("");
                    } catch (IOException e) {
                        Toast.makeText(ChatActivity.this, "Failed to send", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (readThread != null) readThread.interrupt();
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException ignored) {}
            ChatHolder.connection = null;
        }
    }
}