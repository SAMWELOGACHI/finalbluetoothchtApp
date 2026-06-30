package com.example.finalbluetoothchtapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private final List<ChatSession> chats;
    private final OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(ChatSession chat);
    }

    public ChatListAdapter(List<ChatSession> chats, OnChatClickListener listener) {
        this.chats = chats;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_list, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatSession chat = chats.get(position);
        holder.bind(chat);

        // Handle click once here
        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvLastMessage, tvTime, tvBadge;
        View viewOnlineStatus;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvBadge = itemView.findViewById(R.id.tvBadge);
            viewOnlineStatus = itemView.findViewById(R.id.viewOnlineStatus);
        }

        public void bind(ChatSession chat) {
            tvName.setText(chat.getName() != null ? chat.getName() : "Unknown");
            tvLastMessage.setText(chat.getLastMessage() != null ? chat.getLastMessage() : "");
            tvTime.setText(chat.getTime() != null ? chat.getTime() : "");

            if (chat.getUnreadCount() > 0) {
                tvBadge.setVisibility(View.VISIBLE);
                tvBadge.setText(String.valueOf(chat.getUnreadCount()));
            } else {
                tvBadge.setVisibility(View.GONE);
            }

            viewOnlineStatus.setVisibility(chat.isOnline() ? View.VISIBLE : View.GONE);

            // Load Avatar
            if (chat.getAvatarBase64() != null && !chat.getAvatarBase64().isEmpty()) {
                try {
                    byte[] decodedString = android.util.Base64.decode(chat.getAvatarBase64(), android.util.Base64.DEFAULT);
                    android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    com.bumptech.glide.Glide.with(itemView.getContext())
                            .load(decodedByte)
                            .circleCrop()
                            .into(ivAvatar);
                } catch (Exception e) {
                    ivAvatar.setImageResource(R.drawable.ic_person);
                }
            } else {
                ivAvatar.setImageResource(R.drawable.ic_person);
            }
        }
    }

    // Simple data model for chat session
    public static class ChatSession {
        private final String name;
        private final String address;
        private final String lastMessage;
        private final String time;
        private final int unreadCount;
        private final boolean isOnline;
        private final String avatarBase64;

        public ChatSession(String name, String address, String lastMessage, String time, int unreadCount, boolean isOnline, String avatarBase64) {
            this.name = name;
            this.address = address;
            this.lastMessage = lastMessage;
            this.time = time;
            this.unreadCount = unreadCount;
            this.isOnline = isOnline;
            this.avatarBase64 = avatarBase64;
        }

        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getLastMessage() { return lastMessage; }
        public String getTime() { return time; }
        public int getUnreadCount() { return unreadCount; }
        public boolean isOnline() { return isOnline; }
        public String getAvatarBase64() { return avatarBase64; }
    }
}