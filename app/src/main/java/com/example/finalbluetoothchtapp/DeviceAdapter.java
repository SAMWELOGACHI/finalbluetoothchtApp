package com.example.finalbluetoothchtapp;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    // Callback interface for clicks
    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDevice device);
    }

    private final List<BluetoothDevice> devices;
    private final OnDeviceClickListener listener;
    private final Map<String, Integer> signalStrengths = new HashMap<>();

    public DeviceAdapter(List<BluetoothDevice> devices, OnDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    // ✅ Store RSSI values for each device
    public void addSignalStrength(BluetoothDevice device, int rssi) {
        signalStrengths.put(device.getAddress(), rssi);
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        BluetoothDevice device = devices.get(position);

        // Device name + address
        String name = device.getName();
        holder.tvDeviceName.setText(name != null ? name : "Unknown Device");
        holder.tvDeviceAddress.setText(device.getAddress());

        // ✅ Map RSSI to signal icon
        Integer rssi = signalStrengths.get(device.getAddress());
        if (rssi != null) {
            if (rssi > -50) {
                holder.ivSignal.setImageResource(R.drawable.ic_signal_strong);
            } else if (rssi > -70) {
                holder.ivSignal.setImageResource(R.drawable.ic_signal_medium);
            } else {
                holder.ivSignal.setImageResource(R.drawable.ic_signal_weak);
            }
            holder.ivSignal.setContentDescription("Signal strength: " + rssi + " dBm");
        } else {
            holder.ivSignal.setImageResource(R.drawable.ic_bluetooth);
            holder.ivSignal.setContentDescription("Signal strength unknown");
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDeviceClick(device);
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    // ✅ ViewHolder for item_device.xml
    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName, tvDeviceAddress;
        ImageView ivSignal, ivDeviceIcon;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceAddress = itemView.findViewById(R.id.tvDeviceAddress);
            ivSignal = itemView.findViewById(R.id.ivSignal);
            ivDeviceIcon = itemView.findViewById(R.id.ivDeviceIcon);
        }
    }
}