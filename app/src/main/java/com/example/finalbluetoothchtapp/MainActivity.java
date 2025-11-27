package com.example.finalbluetoothchtapp;

import static com.example.finalbluetoothchtapp.R.*;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMS = 2001;
    private static final int REQ_ENABLE_BT = 1001;

    private BluetoothAdapter bluetoothAdapter;
    private final List<BluetoothDevice> devices = new ArrayList<>();
    private DeviceAdapter deviceAdapter;
    private RecyclerView rvDevices;

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !devices.contains(device)) {
                    devices.add(device);
                    deviceAdapter.notifyItemInserted(devices.size() - 1);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(context, "Discovery finished", Toast.LENGTH_SHORT).show();
                findViewById(id.cvScanning).setVisibility(View.GONE);
            }
        }
    };

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);

        // Setup RecyclerView
        rvDevices = findViewById(id.rvDevices);
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        deviceAdapter = new DeviceAdapter(devices, this::connectToDevice);
        rvDevices.setAdapter(deviceAdapter);

        // Bluetooth setup
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = manager != null ? manager.getAdapter() : null;

        // Button actions
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // Permission handling is done in ensurePermissions
        }

        findViewById(id.btnScan).setOnClickListener(v -> startDiscovery());

        // Request permissions
        ensurePermissions();

        // Auto-enable bluetooth if possible
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            enableBluetooth();
        }
    }

    private void startServer() {
        try {
            BluetoothServer server = new BluetoothServer(MainActivity.this);
            server.start();
            Toast.makeText(this, "Server started, waiting for connections...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Server error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothAdapter.cancelDiscovery(); // stop scanning before connecting
        BluetoothClient client = new BluetoothClient(device, bluetoothAdapter);
        client.connect(socket -> {
            try {
                ChatConnection conn = new ChatConnection(socket);
                ChatHolder.connection = conn;
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                intent.putExtra("device_name", device.getName());
                startActivity(intent);
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast
                        .makeText(MainActivity.this, "Connection error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void enableBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQ_ENABLE_BT);
        } else {
            // Toast.makeText(this, "Bluetooth already enabled", Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startDiscovery() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasScanPermissions()) {
            ensurePermissions();
            return;
        }

        devices.clear();
        deviceAdapter.notifyDataSetChanged();

        // Show scanning indicator
        View cvScanning = findViewById(id.cvScanning);
        if (cvScanning != null)
            cvScanning.setVisibility(View.VISIBLE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryReceiver, filter);

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }

        boolean ok = bluetoothAdapter.startDiscovery();
        if (!ok) {
            Toast.makeText(this, "Failed to start discovery", Toast.LENGTH_SHORT).show();
            if (cvScanning != null)
                cvScanning.setVisibility(View.GONE);
        } else {
            Toast.makeText(this, "Scanning...", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasScanPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this,
                            Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void ensurePermissions() {
        List<String> perms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.BLUETOOTH_SCAN);
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!perms.isEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), REQ_PERMS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(discoveryReceiver);
        } catch (Exception ignored) {
        }
    }
}