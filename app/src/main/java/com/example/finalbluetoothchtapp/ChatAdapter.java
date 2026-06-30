package com.example.finalbluetoothchtapp;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<Message> messages;
    private MediaPlayer mediaPlayer;
    private int playingPosition = -1;
    private final Handler seekHandler = new Handler(Looper.getMainLooper());
    private Runnable seekRunnable;

    // Interface to handle message long clicks (for reactions/replies)
    public interface OnMessageLongClickListener {
        void onMessageLongClick(Message message, int position);
    }

    private OnMessageLongClickListener longClickListener;

    public ChatAdapter(List<Message> messages) {
        this.messages = messages;
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isMe() ? 0 : 1;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_me, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_peer, parent, false);
        }
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Message message = messages.get(position);

        // Long Click for replies / reactions
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onMessageLongClick(message, holder.getAdapterPosition());
                return true;
            }
            return false;
        });

        // 1. Text content
        if (message.getText() != null && !message.getText().isEmpty()) {
            holder.txtMessage.setVisibility(View.VISIBLE);
            holder.txtMessage.setText(message.getText());
        } else {
            holder.txtMessage.setVisibility(View.GONE);
        }

        // 2. Image content
        if (message.isImage()) {
            holder.ivImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(message.getImageUri())
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setVisibility(View.GONE);
        }

        // 3. Audio Content
        if (message.isAudio()) {
            holder.layoutAudio.setVisibility(View.VISIBLE);
            holder.tvAudioDuration.setText("0:00");
            holder.sbAudioProgress.setProgress(0);

            // Handle current playing state visual updates
            if (playingPosition == position) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    holder.btnPlayPause.setImageResource(R.drawable.ic_pause);
                    holder.sbAudioProgress.setMax(mediaPlayer.getDuration());
                    startSeekUpdates(holder.sbAudioProgress, holder.tvAudioDuration, position);
                } else {
                    holder.btnPlayPause.setImageResource(R.drawable.ic_play);
                }
            } else {
                holder.btnPlayPause.setImageResource(R.drawable.ic_play);
            }

            holder.btnPlayPause.setOnClickListener(v -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION) return;

                if (playingPosition == currentPos) {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        holder.btnPlayPause.setImageResource(R.drawable.ic_play);
                        stopSeekUpdates();
                    } else if (mediaPlayer != null) {
                        mediaPlayer.start();
                        holder.btnPlayPause.setImageResource(R.drawable.ic_pause);
                        startSeekUpdates(holder.sbAudioProgress, holder.tvAudioDuration, currentPos);
                    }
                } else {
                    stopCurrentAudio();
                    playingPosition = currentPos;
                    mediaPlayer = new MediaPlayer();
                    try {
                        mediaPlayer.setDataSource(holder.itemView.getContext(), Uri.parse(message.getAudioUri()));
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                        holder.btnPlayPause.setImageResource(R.drawable.ic_pause);
                        holder.sbAudioProgress.setMax(mediaPlayer.getDuration());
                        startSeekUpdates(holder.sbAudioProgress, holder.tvAudioDuration, currentPos);

                        mediaPlayer.setOnCompletionListener(mp -> {
                            stopCurrentAudio();
                            notifyItemChanged(currentPos);
                        });
                    } catch (IOException e) {
                        Toast.makeText(holder.itemView.getContext(), "Audio file not found or corrupted", Toast.LENGTH_SHORT).show();
                        playingPosition = -1;
                    }
                }
            });

            holder.sbAudioProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && playingPosition == holder.getAdapterPosition() && mediaPlayer != null) {
                        mediaPlayer.seekTo(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

        } else {
            holder.layoutAudio.setVisibility(View.GONE);
        }

        // 4. Quote / Reply Content
        if (message.isReply()) {
            holder.layoutQuote.setVisibility(View.VISIBLE);
            holder.tvQuoteSender.setText(message.getReplyToSender());
            holder.tvQuoteText.setText(message.getReplyToText());
        } else {
            holder.layoutQuote.setVisibility(View.GONE);
        }

        // 5. Reaction Content
        if (message.getReaction() != null && !message.getReaction().isEmpty()) {
            holder.cvReaction.setVisibility(View.VISIBLE);
            holder.tvReaction.setText(message.getReaction());
        } else {
            holder.cvReaction.setVisibility(View.GONE);
        }

        // 6. Time and Status Checkmarks
        String timeStr = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(message.getTimestamp()));
        holder.txtTimestamp.setText(timeStr);

        if (holder.ivStatus != null) {
            String status = message.getStatus();
            if ("read".equalsIgnoreCase(status)) {
                holder.ivStatus.setImageResource(R.drawable.ic_double_check);
                holder.ivStatus.setImageTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), android.R.color.holo_blue_light));
            } else if ("delivered".equalsIgnoreCase(status)) {
                holder.ivStatus.setImageResource(R.drawable.ic_double_check);
                holder.ivStatus.setImageTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.text_secondary));
            } else {
                holder.ivStatus.setImageResource(R.drawable.ic_check);
                holder.ivStatus.setImageTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.text_secondary));
            }
        }
    }

    private void startSeekUpdates(SeekBar seekBar, TextView durationTv, int position) {
        stopSeekUpdates();
        seekRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && playingPosition == position) {
                    int current = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(current);
                    durationTv.setText(formatDuration(current));
                    seekHandler.postDelayed(this, 250);
                }
            }
        };
        seekHandler.post(seekRunnable);
    }

    private void stopSeekUpdates() {
        if (seekRunnable != null) {
            seekHandler.removeCallbacks(seekRunnable);
            seekRunnable = null;
        }
    }

    private void stopCurrentAudio() {
        stopSeekUpdates();
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        int prevPlaying = playingPosition;
        playingPosition = -1;
        if (prevPlaying != -1) {
            notifyItemChanged(prevPlaying);
        }
    }

    public void releaseMediaPlayer() {
        stopCurrentAudio();
    }

    private String formatDuration(int ms) {
        int seconds = (ms / 1000) % 60;
        int minutes = (ms / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage;
        ImageView ivImage;
        TextView txtTimestamp;

        // Custom features
        View layoutQuote;
        TextView tvQuoteSender;
        TextView tvQuoteText;

        View layoutAudio;
        ImageView btnPlayPause;
        SeekBar sbAudioProgress;
        TextView tvAudioDuration;

        View cvReaction;
        TextView tvReaction;

        ImageView ivStatus;

        ChatViewHolder(View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.tvMessage);
            ivImage = itemView.findViewById(R.id.ivImage);
            txtTimestamp = itemView.findViewById(R.id.tvTimestamp);

            layoutQuote = itemView.findViewById(R.id.layoutQuote);
            tvQuoteSender = itemView.findViewById(R.id.tvQuoteSender);
            tvQuoteText = itemView.findViewById(R.id.tvQuoteText);

            layoutAudio = itemView.findViewById(R.id.layoutAudio);
            btnPlayPause = itemView.findViewById(R.id.btnPlayPause);
            sbAudioProgress = itemView.findViewById(R.id.sbAudioProgress);
            tvAudioDuration = itemView.findViewById(R.id.tvAudioDuration);

            cvReaction = itemView.findViewById(R.id.cvReaction);
            tvReaction = itemView.findViewById(R.id.tvReaction);

            ivStatus = itemView.findViewById(R.id.ivStatus); // Only in item_message_me
        }
    }
}