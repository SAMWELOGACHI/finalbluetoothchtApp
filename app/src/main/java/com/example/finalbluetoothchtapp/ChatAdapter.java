package com.example.finalbluetoothchtapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<Message> messages;

    public ChatAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        // Decide which layout to use based on sender
        return messages.get(position).isMe() ? 0 : 1;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            // Me bubble
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_me, parent, false);
        } else {
            // Peer bubble
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_peer, parent, false);
        }
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.txtMessage.setText(messages.get(position).getText());
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage;

        ChatViewHolder(View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.txtMessage);
        }
    }
}