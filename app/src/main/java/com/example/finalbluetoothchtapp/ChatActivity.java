package com.example.finalbluetoothchtapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

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
        Button sendBtn = findViewById(R.id.btnSend);

        chatAdapter = new ChatAdapter(messages);
        recyclerView.setAdapter(chatAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        connection = ChatHolder.connection;

        // Listen for incoming messages
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
            }
        }).start();

        // Send button
        sendBtn.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (!text.isEmpty()) {
                try {
                    connection.writeLine(text);
                    Message message = new Message(text, true); // Me message
                    messages.add(message);
                    chatAdapter.notifyItemInserted(messages.size() - 1);
                    recyclerView.scrollToPosition(messages.size() - 1);
                    input.setText("");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}