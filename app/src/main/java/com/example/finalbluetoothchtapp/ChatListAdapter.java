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

            // Avatar placeholder
            ivAvatar.setImageResource(R.drawable.bg_avatar_placeholder);
        }
    }

    // Simple data model for chat session
    public static class ChatSession {
        private final String name;
        private final String lastMessage;
        private final String time;
        private final int unreadCount;
        private final boolean isOnline;

        public ChatSession(String name, String lastMessage, String time, int unreadCount, boolean isOnline) {
            this.name = name;
            this.lastMessage = lastMessage;
            this.time = time;
            this.unreadCount = unreadCount;
            this.isOnline = isOnline;
        }

        public String getName() { return name; }
        public String getLastMessage() { return lastMessage; }
        public String getTime() { return time; }
        public int getUnreadCount() { return unreadCount; }
        public boolean isOnline() { return isOnline; }
    }
}