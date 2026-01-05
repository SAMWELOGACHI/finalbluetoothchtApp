package com.example.finalbluetoothchtapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
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
    private BluetoothSocket bluetoothSocket;
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
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);

                if (device != null && !devices.contains(device)) {
                    devices.add(device);
                    deviceAdapter.addSignalStrength(device, rssi);
                    deviceAdapter.notifyItemInserted(devices.size() - 1);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(context, "Discovery finished", Toast.LENGTH_SHORT).show();
                findViewById(R.id.cvScanning).setVisibility(View.GONE);
                try { unregisterReceiver(this); } catch (Exception ignored) {}
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvDevices = findViewById(R.id.rvDevices);
        rvDevices.setLayoutManager(new LinearLayoutManager(this));

        // ✅ Pass click listener to adapter
        deviceAdapter = new DeviceAdapter(devices, this::connectToDevice);
        rvDevices.setAdapter(deviceAdapter);

        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = manager != null ? manager.getAdapter() : null;

        ensurePermissions();

        // ✅ Button listeners
        findViewById(R.id.btnEnableBluetooth).setOnClickListener(v -> {
            Toast.makeText(this, "Enable Bluetooth clicked", Toast.LENGTH_SHORT).show();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            enableBluetooth();
        });

        findViewById(R.id.btnScan).setOnClickListener(v -> {
            Toast.makeText(this, "Scan clicked", Toast.LENGTH_SHORT).show();
            startDiscovery();
        });

        findViewById(R.id.btnDisconnect).setOnClickListener(v -> {
            Toast.makeText(this, "Disconnect clicked", Toast.LENGTH_SHORT).show();
            disconnect();
        });

        findViewById(R.id.btnServer).setOnClickListener(v -> {
            Toast.makeText(this, "Server clicked", Toast.LENGTH_SHORT).show();
            startServer();
        });

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            enableBluetooth();
        }
    }

    // ✅ Start Server
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void startServer() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            ensurePermissions();
            return;
        }

        try {
            BluetoothServer server = new BluetoothServer(MainActivity.this);
            server.start();
            Toast.makeText(this, "Server started, waiting for connections...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Server error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, "Bluetooth server permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Connect to Device
    private void connectToDevice(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            ensurePermissions();
            return;
        }

        bluetoothAdapter.cancelDiscovery();

        new Thread(() -> {
            BluetoothClient client = new BluetoothClient(device, bluetoothAdapter);
            BluetoothSocket socket = client.connect();

            runOnUiThread(() -> {
                if (socket != null) {
                    bluetoothSocket = socket;
                    Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                    intent.putExtra("device_name", device.getName());
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Failed to connect", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // ✅ Enable Bluetooth
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void enableBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            ensurePermissions();
            return;
        }

        try {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQ_ENABLE_BT);
            } else {
                Toast.makeText(this, "Bluetooth already enabled", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Start Discovery
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startDiscovery() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            ensurePermissions();
            return;
        }

        try {
            devices.clear();
            deviceAdapter.notifyDataSetChanged();

            View cvScanning = findViewById(R.id.cvScanning);
            if (cvScanning != null) cvScanning.setVisibility(View.VISIBLE);

            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(discoveryReceiver, filter);

            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }

            boolean ok = bluetoothAdapter.startDiscovery();
            if (!ok) {
                Toast.makeText(this, "Failed to start discovery", Toast.LENGTH_SHORT).show();
                if (cvScanning != null) cvScanning.setVisibility(View.GONE);
            } else {
                Toast.makeText(this, "Scanning...", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Bluetooth scan permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Disconnect
    private void disconnect() {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
                Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No active connection", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error disconnecting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, "Bluetooth disconnect permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Permission Helpers
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
        }
    }
}