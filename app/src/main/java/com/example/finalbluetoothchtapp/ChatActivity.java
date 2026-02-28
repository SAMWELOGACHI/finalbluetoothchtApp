package com.example.finalbluetoothchtapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalbluetoothchtapp.db.AppDatabase;
import com.example.finalbluetoothchtapp.db.MessageEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private ChatAdapter chatAdapter;
    private final List<Message> messages = new ArrayList<>();
    private ChatConnection connection;
    private Thread readThread;
    private String deviceAddress;
    private String deviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        RecyclerView recyclerView = findViewById(R.id.rvMessages);
        EditText input = findViewById(R.id.etMessage);
        View sendBtn = findViewById(R.id.btnSend);
        View backBtn = findViewById(R.id.btnBack);
        View attachBtn = findViewById(R.id.btnAttach);
        TextView tvTitle = findViewById(R.id.tvChatTitle);

        deviceName = getIntent().getStringExtra("device_name");
        deviceAddress = getIntent().getStringExtra("device_address");

        if (deviceName != null) {
            tvTitle.setText(deviceName);
        }

        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        chatAdapter = new ChatAdapter(messages);
        recyclerView.setAdapter(chatAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load History from Room
        if (deviceAddress != null) {
            new Thread(() -> {
                List<MessageEntity> history = AppDatabase.getInstance(this).messageDao()
                        .getMessagesForDevice(deviceAddress);
                runOnUiThread(() -> {
                    for (MessageEntity entity : history) {
                        messages.add(
                                new Message(entity.content, entity.imageUri, entity.isMe, entity.timestamp, "sent"));
                    }
                    chatAdapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) {
                        recyclerView.scrollToPosition(messages.size() - 1);
                    }
                });
            }).start();
        }

        connection = ChatHolder.connection;

        // Listen for incoming messages
        if (connection != null) {
            readThread = new Thread(() -> {
                try {
                    String msg;
                    while (!Thread.currentThread().isInterrupted() &&
                            (msg = connection.readLine()) != null) {
                        String finalMsg = msg;

                        String imageUri = null;
                        String textContent = finalMsg;
                        if (finalMsg.startsWith("[IMG]")) {
                            imageUri = finalMsg.substring(5);
                            textContent = null;
                        }

                        // Save to Room
                        String finalImageUri = imageUri;
                        String finalTextContent = textContent;
                        new Thread(() -> {
                            if (deviceAddress != null) {
                                MessageEntity entity = new MessageEntity(deviceAddress, deviceName, finalTextContent,
                                        finalImageUri, System.currentTimeMillis(), false);
                                AppDatabase.getInstance(this).messageDao().insert(entity);
                            }
                        }).start();

                        Message message = new Message(textContent, imageUri, false);
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

        // Attach button logic
        if (attachBtn != null) {
            attachBtn.setOnClickListener(v -> {
                Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                picker.addCategory(Intent.CATEGORY_OPENABLE);
                picker.setType("image/*");
                startActivityForResult(picker, 100);
            });
        }

        // Send button
        if (sendBtn != null) {
            sendBtn.setOnClickListener(v -> {
                String text = input.getText().toString().trim();
                if (!text.isEmpty() && connection != null) {
                    try {
                        connection.writeLine(text);

                        // Save to Room
                        String finalText = text;
                        new Thread(() -> {
                            if (deviceAddress != null) {
                                MessageEntity entity = new MessageEntity(deviceAddress, "Me", finalText, null,
                                        System.currentTimeMillis(), true);
                                AppDatabase.getInstance(this).messageDao().insert(entity);
                            }
                        }).start();

                        Message message = new Message(text, true);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null && connection != null) {
                // Persistent permission for URI if possible
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException e) {
                    // Ignore
                }

                try {
                    connection.writeLine("[IMG]" + uri.toString());

                    new Thread(() -> {
                        if (deviceAddress != null) {
                            MessageEntity entity = new MessageEntity(deviceAddress, "Me", null, uri.toString(),
                                    System.currentTimeMillis(), true);
                            AppDatabase.getInstance(this).messageDao().insert(entity);
                        }
                    }).start();

                    Message message = new Message(null, uri.toString(), true);
                    messages.add(message);
                    chatAdapter.notifyItemInserted(messages.size() - 1);
                } catch (IOException e) {
                    Toast.makeText(this, "Failed to send image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (readThread != null)
            readThread.interrupt();
        if (isFinishing() && connection != null) {
            try {
                connection.close();
            } catch (IOException ignored) {
            }
            ChatHolder.connection = null;
        }
    }
}