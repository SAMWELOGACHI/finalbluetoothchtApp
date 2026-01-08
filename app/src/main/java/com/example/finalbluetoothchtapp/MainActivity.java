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
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMS = 2001;
    private static final int REQ_ENABLE_BT = 1001;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private final List<BluetoothDevice> devices = new ArrayList<>();
    private DeviceAdapter deviceAdapter;
    private RecyclerView rvDevices;

    // Receiver to handle found devices
    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
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
                View cv = findViewById(R.id.cvScanning);
                if (cv != null) cv.setVisibility(View.GONE);

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

        deviceAdapter = new DeviceAdapter(devices, this::connectToDevice);
        rvDevices.setAdapter(deviceAdapter);

        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = manager != null ? manager.getAdapter() : null;

        ensurePermissions();

        findViewById(R.id.btnEnableBluetooth).setOnClickListener(v -> enableBluetooth());

        findViewById(R.id.btnScan).setOnClickListener(v -> {
            Toast.makeText(this, "Scan clicked", Toast.LENGTH_SHORT).show();
            startDiscovery();
        });

        findViewById(R.id.btnDisconnect).setOnClickListener(v -> disconnect());

        findViewById(R.id.btnServer).setOnClickListener(v -> startServer());

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            enableBluetooth();
        } else {
            showPairedDevices();
        }
    }

    private void showPairedDevices() {
        if (bluetoothAdapter == null || !hasConnectPermission()) return;

        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices != null && !pairedDevices.isEmpty()) {
                devices.clear();
                devices.addAll(pairedDevices);
                deviceAdapter.notifyDataSetChanged();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission required to show paired devices", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted && grantResults.length > 0) {
                Toast.makeText(this, "Permissions granted.", Toast.LENGTH_SHORT).show();
                showPairedDevices();
            } else {
                Toast.makeText(this, "Permissions denied. Bluetooth will not work.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startServer() {
        if (!hasConnectPermission()) {
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
            Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        if (!hasConnectPermission()) {
            ensurePermissions();
            return;
        }

        try {
            bluetoothAdapter.cancelDiscovery();
        } catch (SecurityException e) {
            // permission issue
        }

        new Thread(() -> {
            try {
                BluetoothClient client = new BluetoothClient(device, bluetoothAdapter);
                BluetoothSocket socket = client.connect();

                runOnUiThread(() -> {
                    if (socket != null) {
                        bluetoothSocket = socket;
                        try {
                            // Save connection globally
                            ChatHolder.connection = new ChatConnection(socket);
                            Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
                            
                            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                            intent.putExtra("device_name", device.getName());
                            startActivity(intent);
                        } catch (IOException e) {
                            Toast.makeText(this, "Error creating connection", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Failed to connect", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (SecurityException e) {
                runOnUiThread(() -> Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void enableBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasConnectPermission()) {
            ensurePermissions();
            return;
        }

        try {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQ_ENABLE_BT);
            } else {
                Toast.makeText(this, "Bluetooth already enabled", Toast.LENGTH_SHORT).show();
                showPairedDevices();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Start Discovery (FIXED: Increased Delay + Advertise check)
    private void startDiscovery() {
        if (bluetoothAdapter == null) return;

        // 1. Permission Check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ensurePermissions();
                return;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ensurePermissions();
                return;
            }
        }

        // 2. SYSTEM LOCATION SWITCH CHECK
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Please enable Location/GPS to scan", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            return;
        }

        try {
            devices.clear();
            deviceAdapter.notifyDataSetChanged();

            View cvScanning = findViewById(R.id.cvScanning);
            if (cvScanning != null) cvScanning.setVisibility(View.VISIBLE);

            try { unregisterReceiver(discoveryReceiver); } catch (Exception ignored) {}
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(discoveryReceiver, filter);

            // 3. STOP & WAIT
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }

            // CRITICAL FIX: Increased delay to 2000ms (2 seconds)
            // Bluetooth hardware needs time to stop before starting again.
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    // Double check permission inside the delay
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {

                        boolean ok = bluetoothAdapter.startDiscovery();

                        if (!ok) {
                            Toast.makeText(this, "Discovery failed. Restart App.", Toast.LENGTH_SHORT).show();
                            if (cvScanning != null) cvScanning.setVisibility(View.GONE);
                        } else {
                            Toast.makeText(this, "Scanning...", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (SecurityException e) {
                    // Ignore
                }
            }, 2000); // 2000ms delay

        } catch (SecurityException e) {
            Toast.makeText(this, "Bluetooth scan permission denied", Toast.LENGTH_SHORT).show();
        }
    }

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

    private boolean hasConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean isLocationEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return true;
        }
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // ✅ Permission Helpers (FIXED: Added BLUETOOTH_ADVERTISE for Android 12+)
    private void ensurePermissions() {
        List<String> perms = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            // CRITICAL FIX: Android 12 often requires this even if you aren't advertising
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }

        if (!perms.isEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), REQ_PERMS);
        }
    }
}
