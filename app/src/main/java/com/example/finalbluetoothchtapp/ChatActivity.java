package com.example.finalbluetoothchtapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalbluetoothchtapp.db.AppDatabase;
import com.example.finalbluetoothchtapp.db.MessageEntity;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private static final int REQ_AUDIO_PERM = 3001;

    private ChatAdapter chatAdapter;
    private final List<Message> messages = new ArrayList<>();
    private ChatConnection connection;
    private Thread readThread;
    private String deviceAddress;
    private String deviceName;

    // Voice Recording
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String currentRecordingPath;
    private long recordingStartTime;
    private Handler recordingTimerHandler;
    private Runnable recordingTimerRunnable;

    // Reply state
    private Message replyingToMessage = null;

    // Search
    private boolean isSearchMode = false;
    private String currentSearchQuery = "";

    // UI References
    private RecyclerView recyclerView;
    private EditText input;
    private View btnSend;
    private View btnMic;
    private View layoutReplyPreview;
    private TextView tvReplySender;
    private TextView tvReplyText;
    private View replyDivider;
    private TextView tvRecordStatus;
    private EditText etSearch;
    private View btnSearch;
    private View btnCloseSearch;
    private TextView tvChatTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.rvMessages);
        input = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        View backBtn = findViewById(R.id.btnBack);
        View attachBtn = findViewById(R.id.btnAttach);
        tvChatTitle = findViewById(R.id.tvChatTitle);
        tvRecordStatus = findViewById(R.id.tvRecordStatus);

        // Reply preview
        layoutReplyPreview = findViewById(R.id.layoutReplyPreview);
        tvReplySender = findViewById(R.id.tvReplySender);
        tvReplyText = findViewById(R.id.tvReplyText);
        replyDivider = findViewById(R.id.replyDivider);
        View btnCancelReply = findViewById(R.id.btnCancelReply);

        // Search
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnCloseSearch = findViewById(R.id.btnCloseSearch);

        deviceName = getIntent().getStringExtra("device_name");
        deviceAddress = getIntent().getStringExtra("device_address");

        if (deviceName != null) {
            tvChatTitle.setText(deviceName);
        } else if (deviceAddress != null) {
            tvChatTitle.setText(deviceAddress);
        }

        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        chatAdapter = new ChatAdapter(messages);
        recyclerView.setAdapter(chatAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        // Long click handler for reactions & replies
        chatAdapter.setOnMessageLongClickListener((message, position) -> showMessageOptionsDialog(message, position));

        // Load History from Room
        if (deviceAddress != null) {
            new Thread(() -> {
                List<MessageEntity> history = AppDatabase.getInstance(this).messageDao()
                        .getMessagesForDevice(deviceAddress);
                runOnUiThread(() -> {
                    for (MessageEntity entity : history) {
                        messages.add(new Message(
                                entity.content, entity.imageUri, entity.audioUri,
                                entity.isMe, entity.timestamp,
                                entity.status != null ? entity.status : "sent",
                                entity.reaction, entity.replyToText, entity.replyToSender));
                    }
                    chatAdapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) {
                        recyclerView.scrollToPosition(messages.size() - 1);
                    }
                });
            }).start();
        }

        connection = ChatHolder.connection;

        // Listen for incoming messages (JSON protocol)
        if (connection != null) {
            sendProfilePacket(); // Send our profile to the peer
            
            readThread = new Thread(() -> {
                try {
                    String line;
                    while (!Thread.currentThread().isInterrupted() &&
                            (line = connection.readLine()) != null) {
                        handleIncomingLine(line);
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

        // Toggle mic/send visibility based on input text
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    btnSend.setVisibility(View.GONE);
                    btnMic.setVisibility(View.VISIBLE);
                } else {
                    btnSend.setVisibility(View.VISIBLE);
                    btnMic.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Send button
        if (btnSend != null) {
            btnSend.setOnClickListener(v -> sendTextMessage());
        }

        // Mic button (tap to toggle recording)
        if (btnMic != null) {
            btnMic.setOnClickListener(v -> {
                if (isRecording) {
                    stopRecordingAndSend();
                } else {
                    startRecording();
                }
            });
        }

        // Cancel reply
        if (btnCancelReply != null) {
            btnCancelReply.setOnClickListener(v -> cancelReply());
        }

        // Search
        setupSearch();
    }

    // ========================
    //  JSON Protocol Handling
    // ========================

    private void handleIncomingLine(String line) {
        try {
            JSONObject json = new JSONObject(line);
            String type = json.optString("type", "text");

            switch (type) {
                case "profile":
                    handleProfile(json);
                    break;
                case "ack":
                    handleAck(json);
                    break;
                case "reaction":
                    handleReaction(json);
                    break;
                case "text":
                case "image":
                case "audio":
                case "reply":
                    handleIncomingMessage(json, type);
                    break;
                default:
                    handleIncomingMessage(json, "text");
                    break;
            }
        } catch (Exception e) {
            // Fallback: legacy plain text message (non-JSON)
            handleLegacyMessage(line);
        }
    }

    private void handleProfile(JSONObject json) {
        String peerName = json.optString("name", "");
        String peerAvatarBase64 = json.optString("avatar_base64", "");

        if (deviceAddress != null) {
            SharedPreferences prefs = getSharedPreferences("PeerProfiles", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(deviceAddress + "_name", peerName);
            editor.putString(deviceAddress + "_avatar", peerAvatarBase64);
            editor.apply();
            
            if (!peerName.isEmpty()) {
                deviceName = peerName;
                runOnUiThread(() -> tvChatTitle.setText(deviceName));
            }
        }
    }

    private void handleAck(JSONObject json) {
        long targetTimestamp = json.optLong("targetTimestamp", 0);
        String status = json.optString("status", "delivered");

        // Update in DB
        new Thread(() -> {
            if (deviceAddress != null) {
                AppDatabase.getInstance(this).messageDao()
                        .updateMessageStatus(targetTimestamp, true, status);
            }
        }).start();

        // Update in UI
        runOnUiThread(() -> {
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message msg = messages.get(i);
                if (msg.isMe() && msg.getTimestamp() == targetTimestamp) {
                    msg.setStatus(status);
                    chatAdapter.notifyItemChanged(i);
                    break;
                }
            }
        });
    }

    private void handleReaction(JSONObject json) {
        long targetTimestamp = json.optLong("targetTimestamp", 0);
        String emoji = json.optString("emoji", "");

        // Update in DB
        new Thread(() -> {
            if (deviceAddress != null) {
                AppDatabase.getInstance(this).messageDao()
                        .updateMessageReaction(targetTimestamp, emoji);
            }
        }).start();

        // Update in UI
        runOnUiThread(() -> {
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message msg = messages.get(i);
                if (msg.getTimestamp() == targetTimestamp) {
                    msg.setReaction(emoji);
                    chatAdapter.notifyItemChanged(i);
                    break;
                }
            }
        });
    }

    private void handleIncomingMessage(JSONObject json, String type) {
        String textContent = json.optString("content", null);
        if ("null".equals(textContent)) textContent = null;
        String imageUri = null;
        String audioUri = null;
        String replyToText = json.optString("quotedText", null);
        if ("null".equals(replyToText)) replyToText = null;
        String replyToSender = json.optString("quotedSender", null);
        if ("null".equals(replyToSender)) replyToSender = null;
        long timestamp = json.optLong("timestamp", System.currentTimeMillis());

        if ("image".equals(type)) {
            String base64Data = json.optString("base64", null);
            if (base64Data != null) {
                imageUri = saveBase64ImageToCache(base64Data);
            }
            textContent = null;
        } else if ("audio".equals(type)) {
            String base64Data = json.optString("base64", null);
            if (base64Data != null) {
                audioUri = saveBase64AudioToCache(base64Data);
            }
            textContent = null;
        } else if ("reply".equals(type)) {
            textContent = json.optString("content", null);
            if ("null".equals(textContent)) textContent = null;
        }

        // Save to Room
        String finalImageUri = imageUri;
        String finalAudioUri = audioUri;
        String finalTextContent = textContent;
        String finalReplyToText = replyToText;
        String finalReplyToSender = replyToSender;
        new Thread(() -> {
            if (deviceAddress != null) {
                MessageEntity entity = new MessageEntity(deviceAddress, deviceName, finalTextContent,
                        finalImageUri, finalAudioUri, "sent", null,
                        finalReplyToText, finalReplyToSender,
                        timestamp, false);
                AppDatabase.getInstance(this).messageDao().insert(entity);
            }
        }).start();

        Message message = new Message(textContent, imageUri, audioUri, false,
                timestamp, "sent", null, replyToText, replyToSender);

        runOnUiThread(() -> {
            messages.add(message);
            chatAdapter.notifyItemInserted(messages.size() - 1);
            recyclerView.smoothScrollToPosition(messages.size() - 1);
        });

        // Send delivery acknowledgment
        sendAck(timestamp, "delivered");

        // If activity is in foreground, send read ack too
        runOnUiThread(() -> sendAck(timestamp, "read"));
    }

    private void handleLegacyMessage(String line) {
        String imageUri = null;
        String textContent = line;

        if (line.startsWith("[IMG_BASE64]")) {
            String base64Data = line.substring(12);
            imageUri = saveBase64ImageToCache(base64Data);
            textContent = null;
        } else if (line.startsWith("[IMG]")) {
            imageUri = line.substring(5);
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

    // ========================
    //  Sending Messages & Profile
    // ========================

    private void sendProfilePacket() {
        if (connection == null) return;
        new Thread(() -> {
            SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
            String name = prefs.getString("display_name", "");
            String avatarBase64 = prefs.getString("avatar_base64", "");

            if (!name.isEmpty() || !avatarBase64.isEmpty()) {
                try {
                    JSONObject json = new JSONObject();
                    json.put("type", "profile");
                    json.put("name", name);
                    json.put("avatar_base64", avatarBase64);
                    connection.writeLine(json.toString());
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void sendTextMessage() {
        String text = input.getText().toString().trim();
        if (text.isEmpty() || connection == null) return;

        long timestamp = System.currentTimeMillis();
        try {
            JSONObject json = new JSONObject();
            if (replyingToMessage != null) {
                json.put("type", "reply");
                json.put("quotedText", replyingToMessage.getText() != null ? replyingToMessage.getText() : "");
                json.put("quotedSender", replyingToMessage.isMe() ? "Me" : (deviceName != null ? deviceName : ""));
            } else {
                json.put("type", "text");
            }
            json.put("content", text);
            json.put("timestamp", timestamp);

            connection.writeLine(json.toString());

            String replyToText = replyingToMessage != null ? replyingToMessage.getText() : null;
            String replyToSender = replyingToMessage != null ?
                    (replyingToMessage.isMe() ? "Me" : (deviceName != null ? deviceName : "")) : null;

            // Save to Room
            new Thread(() -> {
                if (deviceAddress != null) {
                    MessageEntity entity = new MessageEntity(deviceAddress, deviceName, text,
                            null, null, "sent", null, replyToText, replyToSender,
                            timestamp, true);
                    AppDatabase.getInstance(this).messageDao().insert(entity);
                }
            }).start();

            Message message = new Message(text, null, null, true, timestamp, "sent",
                    null, replyToText, replyToSender);
            messages.add(message);
            chatAdapter.notifyItemInserted(messages.size() - 1);
            recyclerView.smoothScrollToPosition(messages.size() - 1);
            input.setText("");
            cancelReply();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendAck(long targetTimestamp, String status) {
        if (connection == null) return;
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("type", "ack");
                json.put("targetTimestamp", targetTimestamp);
                json.put("status", status);
                connection.writeLine(json.toString());
            } catch (Exception ignored) {}
        }).start();
    }

    private void sendReactionPacket(long targetTimestamp, String emoji) {
        if (connection == null) return;
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("type", "reaction");
                json.put("targetTimestamp", targetTimestamp);
                json.put("emoji", emoji);
                connection.writeLine(json.toString());
            } catch (Exception ignored) {}
        }).start();
    }

    // ========================
    //  Image Handling
    // ========================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null && connection != null) {
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {}

                Toast.makeText(this, "Compressing & sending photo...", Toast.LENGTH_SHORT).show();

                new Thread(() -> {
                    String base64Image = getBase64FromUri(uri);
                    if (base64Image == null) {
                        runOnUiThread(() -> Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    long timestamp = System.currentTimeMillis();
                    try {
                        JSONObject json = new JSONObject();
                        json.put("type", "image");
                        json.put("base64", base64Image);
                        json.put("timestamp", timestamp);
                        connection.writeLine(json.toString());

                        if (deviceAddress != null) {
                            MessageEntity entity = new MessageEntity(deviceAddress, deviceName, null,
                                    uri.toString(), null, "sent", null, null, null,
                                    timestamp, true);
                            AppDatabase.getInstance(this).messageDao().insert(entity);
                        }

                        runOnUiThread(() -> {
                            Message message = new Message(null, uri.toString(), null, true,
                                    timestamp, "sent", null, null, null);
                            messages.add(message);
                            chatAdapter.notifyItemInserted(messages.size() - 1);
                            recyclerView.smoothScrollToPosition(messages.size() - 1);
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(this, "Failed to send image", Toast.LENGTH_SHORT).show());
                    }
                }).start();
            }
        }
    }

    // ========================
    //  Voice Recording
    // ========================

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO_PERM);
            return;
        }

        File cacheDir = new File(getCacheDir(), "bt_voice_notes");
        if (!cacheDir.exists()) cacheDir.mkdirs();
        currentRecordingPath = new File(cacheDir, "vn_" + System.currentTimeMillis() + ".m4a").getAbsolutePath();

        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(64000);
            mediaRecorder.setAudioSamplingRate(22050);
            mediaRecorder.setOutputFile(currentRecordingPath);
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            recordingStartTime = System.currentTimeMillis();

            // Show recording UI
            input.setVisibility(View.GONE);
            tvRecordStatus.setVisibility(View.VISIBLE);

            // Start timer
            recordingTimerHandler = new Handler(Looper.getMainLooper());
            recordingTimerRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isRecording) {
                        long elapsed = System.currentTimeMillis() - recordingStartTime;
                        int seconds = (int) (elapsed / 1000) % 60;
                        int minutes = (int) (elapsed / (1000 * 60)) % 60;
                        tvRecordStatus.setText(String.format(Locale.getDefault(),
                                "🎙️ Recording %d:%02d", minutes, seconds));
                        recordingTimerHandler.postDelayed(this, 500);
                    }
                }
            };
            recordingTimerHandler.post(recordingTimerRunnable);

        } catch (Exception e) {
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
            isRecording = false;
        }
    }

    private void stopRecordingAndSend() {
        if (!isRecording || mediaRecorder == null) return;

        try {
            mediaRecorder.stop();
            mediaRecorder.release();
        } catch (Exception ignored) {}
        mediaRecorder = null;
        isRecording = false;

        // Stop timer
        if (recordingTimerHandler != null && recordingTimerRunnable != null) {
            recordingTimerHandler.removeCallbacks(recordingTimerRunnable);
        }

        // Restore UI
        input.setVisibility(View.VISIBLE);
        tvRecordStatus.setVisibility(View.GONE);

        // Send audio file
        if (currentRecordingPath != null && connection != null) {
            Toast.makeText(this, "Sending voice note...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                String base64Audio = getBase64FromFile(currentRecordingPath);
                if (base64Audio == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to encode audio", Toast.LENGTH_SHORT).show());
                    return;
                }

                long timestamp = System.currentTimeMillis();
                try {
                    JSONObject json = new JSONObject();
                    json.put("type", "audio");
                    json.put("base64", base64Audio);
                    json.put("timestamp", timestamp);
                    connection.writeLine(json.toString());

                    String audioUri = Uri.fromFile(new File(currentRecordingPath)).toString();
                    if (deviceAddress != null) {
                        MessageEntity entity = new MessageEntity(deviceAddress, deviceName, null,
                                null, audioUri, "sent", null, null, null,
                                timestamp, true);
                        AppDatabase.getInstance(this).messageDao().insert(entity);
                    }

                    runOnUiThread(() -> {
                        Message message = new Message(null, null,
                                Uri.fromFile(new File(currentRecordingPath)).toString(),
                                true, timestamp, "sent", null, null, null);
                        messages.add(message);
                        chatAdapter.notifyItemInserted(messages.size() - 1);
                        recyclerView.smoothScrollToPosition(messages.size() - 1);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to send voice note", Toast.LENGTH_SHORT).show());
                }
            }).start();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(this, "Microphone permission required for voice notes", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ========================
    //  Reactions & Replies
    // ========================

    private void showMessageOptionsDialog(Message message, int position) {
        String[] emojis = {"👍", "❤️", "😂", "😮", "😢", "🙏"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog);
        builder.setTitle("Message Options");

        String[] options = new String[emojis.length + 1];
        options[0] = "↩️  Reply";
        for (int i = 0; i < emojis.length; i++) {
            options[i + 1] = emojis[i] + "  React";
        }

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Reply
                setReplyTo(message);
            } else {
                // Reaction
                String emoji = emojis[which - 1];
                message.setReaction(emoji);
                chatAdapter.notifyItemChanged(position);

                // Persist in DB
                new Thread(() -> AppDatabase.getInstance(this).messageDao()
                        .updateMessageReaction(message.getTimestamp(), emoji)).start();

                // Send reaction to peer
                sendReactionPacket(message.getTimestamp(), emoji);
            }
        });

        builder.show();
    }

    private void setReplyTo(Message message) {
        replyingToMessage = message;
        layoutReplyPreview.setVisibility(View.VISIBLE);
        replyDivider.setVisibility(View.VISIBLE);

        String sender = message.isMe() ? "You" : (deviceName != null ? deviceName : "Peer");
        tvReplySender.setText(sender);

        if (message.getText() != null) {
            tvReplyText.setText(message.getText());
        } else if (message.isImage()) {
            tvReplyText.setText("📷 Photo");
        } else if (message.isAudio()) {
            tvReplyText.setText("🎙️ Voice Note");
        }

        input.requestFocus();
    }

    private void cancelReply() {
        replyingToMessage = null;
        layoutReplyPreview.setVisibility(View.GONE);
        replyDivider.setVisibility(View.GONE);
    }

    // ========================
    //  Search
    // ========================

    private void setupSearch() {
        btnSearch.setOnClickListener(v -> enterSearchMode());
        btnCloseSearch.setOnClickListener(v -> exitSearchMode());

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearch.getText().toString().trim());
                return true;
            }
            return false;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void enterSearchMode() {
        isSearchMode = true;
        tvChatTitle.setVisibility(View.GONE);
        btnSearch.setVisibility(View.GONE);
        etSearch.setVisibility(View.VISIBLE);
        btnCloseSearch.setVisibility(View.VISIBLE);
        etSearch.requestFocus();
    }

    private void exitSearchMode() {
        isSearchMode = false;
        tvChatTitle.setVisibility(View.VISIBLE);
        btnSearch.setVisibility(View.VISIBLE);
        etSearch.setVisibility(View.GONE);
        btnCloseSearch.setVisibility(View.GONE);
        etSearch.setText("");
        currentSearchQuery = "";
    }

    private void performSearch(String query) {
        currentSearchQuery = query;
        if (query.isEmpty()) return;

        // Find and scroll to the first matching message (from bottom)
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg.getText() != null && msg.getText().toLowerCase(Locale.getDefault())
                    .contains(query.toLowerCase(Locale.getDefault()))) {
                recyclerView.smoothScrollToPosition(i);
                break;
            }
        }
    }

    // ========================
    //  Utility Methods
    // ========================

    private String getBase64FromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) return null;

            int maxDimension = 800;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxDimension || height > maxDimension) {
                float aspectRatio = (float) width / (float) height;
                if (aspectRatio > 1) {
                    width = maxDimension;
                    height = (int) (maxDimension / aspectRatio);
                } else {
                    height = maxDimension;
                    width = (int) (maxDimension * aspectRatio);
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
            byte[] bytes = outputStream.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getBase64FromFile(String filePath) {
        try {
            File file = new File(filePath);
            byte[] bytes = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(bytes);
            fis.close();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String saveBase64ImageToCache(String base64Data) {
        try {
            byte[] bytes = Base64.decode(base64Data, Base64.NO_WRAP);
            File cacheDir = new File(getCacheDir(), "bt_chat_images");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File imageFile = new File(cacheDir, "img_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(imageFile);
            fos.write(bytes);
            fos.flush();
            fos.close();
            return Uri.fromFile(imageFile).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String saveBase64AudioToCache(String base64Data) {
        try {
            byte[] bytes = Base64.decode(base64Data, Base64.NO_WRAP);
            File cacheDir = new File(getCacheDir(), "bt_voice_notes");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File audioFile = new File(cacheDir, "vn_" + System.currentTimeMillis() + ".m4a");
            FileOutputStream fos = new FileOutputStream(audioFile);
            fos.write(bytes);
            fos.flush();
            fos.close();
            return Uri.fromFile(audioFile).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (readThread != null) readThread.interrupt();

        // Release audio
        chatAdapter.releaseMediaPlayer();

        // Stop recording if active
        if (isRecording && mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception ignored) {}
            mediaRecorder = null;
        }

        if (isFinishing() && connection != null) {
            try {
                connection.close();
            } catch (IOException ignored) {}
            ChatHolder.connection = null;
        }
    }
}