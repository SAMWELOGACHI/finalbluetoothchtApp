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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMS = 2001;
    private static final int REQ_ENABLE_BT = 1001;
    private static final int REQ_LOCATION_ENABLE = 1002;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private final List<BluetoothDevice> devices = new ArrayList<>();
    private DeviceAdapter deviceAdapter;
    private RecyclerView rvDevices;
    private boolean isRestartingDiscovery = false;

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                } else {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                }
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);
                if (device != null && !devices.contains(device)) {
                    devices.add(device);
                    deviceAdapter.addSignalStrength(device, rssi);
                    deviceAdapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (isRestartingDiscovery) return;
                runOnUiThread(() -> {
                    String msg = devices.isEmpty()
                            ? "No devices found. Make sure the other device is discoverable."
                            : "Scan complete. Found " + devices.size() + " device(s).";
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                    View cv = findViewById(R.id.cvScanning);
                    if (cv != null) cv.setVisibility(View.GONE);
                    MaterialButton btnScan = findViewById(R.id.btnScan);
                    if (btnScan != null) btnScan.setText("Scan Again");
                });
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

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device.", Toast.LENGTH_LONG).show();
            return;
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnEnableBluetooth).setOnClickListener(v -> enableBluetooth());
        findViewById(R.id.btnScan).setOnClickListener(v -> startDiscovery());
        findViewById(R.id.btnDisconnect).setOnClickListener(v -> disconnect());
        findViewById(R.id.btnServer).setOnClickListener(v -> startServer());

        if (!ensurePermissions()) {
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            enableBluetooth();
        } else {
            showPairedDevices();
        }
    }

    private void showPairedDevices() {
        if (bluetoothAdapter == null || !hasConnectPermission()) return;
        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            devices.clear();
            if (pairedDevices != null && !pairedDevices.isEmpty()) {
                devices.addAll(pairedDevices);
                Toast.makeText(this,
                        "Showing " + pairedDevices.size() + " paired device(s). Tap Scan to find new ones.",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this,
                        "No paired devices. Tap Scan to discover nearby devices.",
                        Toast.LENGTH_LONG).show();
            }
            deviceAdapter.notifyDataSetChanged();
        } catch (SecurityException e) {
            Toast.makeText(this, "Bluetooth permission required.", Toast.LENGTH_SHORT).show();
        }
    }

    private BluetoothServer bluetoothServer;

    private void startServer() {
        if (!hasConnectPermission()) { ensurePermissions(); return; }
        if (bluetoothServer != null) {
            bluetoothServer.stopServer();
            bluetoothServer = null;
            Toast.makeText(this, "Server stopped", Toast.LENGTH_SHORT).show();
            ((MaterialButton) findViewById(R.id.btnServer)).setText("Start Server");
            return;
        }
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        try { startActivity(discoverableIntent); } catch (SecurityException ignored) {}
        try {
            bluetoothServer = new BluetoothServer(this);
            bluetoothServer.start();
            Toast.makeText(this, "Server started. Waiting for connection...", Toast.LENGTH_LONG).show();
            ((MaterialButton) findViewById(R.id.btnServer)).setText("Stop Server");
        } catch (IOException | SecurityException e) {
            Toast.makeText(this, "Error starting server: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        if (!hasConnectPermission()) { ensurePermissions(); return; }
        try { bluetoothAdapter.cancelDiscovery(); } catch (SecurityException ignored) {}
        new Thread(() -> {
            try {
                BluetoothClient client = new BluetoothClient(device, bluetoothAdapter);
                BluetoothSocket socket = client.connect();
                runOnUiThread(() -> {
                    if (socket != null) {
                        bluetoothSocket = socket;
                        try {
                            ChatHolder.connection = new ChatConnection(socket);
                            Intent serviceIntent = new Intent(MainActivity.this, BluetoothService.class);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(serviceIntent);
                            } else {
                                startService(serviceIntent);
                            }
                            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                            intent.putExtra("device_name", device.getName());
                            intent.putExtra("device_address", device.getAddress());
                            startActivity(intent);
                        } catch (IOException e) {
                            Toast.makeText(this, "Connection error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this,
                                "Failed to connect. Make sure the other device has Server mode active.",
                                Toast.LENGTH_LONG).show();
                    }
                });
            } catch (SecurityException e) {
                runOnUiThread(() -> Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void enableBluetooth() {
        if (bluetoothAdapter == null) return;
        if (!hasConnectPermission()) { ensurePermissions(); return; }
        try {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQ_ENABLE_BT);
            } else {
                showPairedDevices();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission denied to enable Bluetooth.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled!", Toast.LENGTH_SHORT).show();
                showPairedDevices();
            } else {
                Toast.makeText(this, "Bluetooth must be enabled to use BT Chat.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQ_LOCATION_ENABLE) {
            startDiscovery();
        }
    }

    private void startDiscovery() {
        if (bluetoothAdapter == null) return;
        if (!hasScanPermission()) { ensurePermissions(); return; }
        if (bluetoothAdapter.isDiscovering()) {
            try { bluetoothAdapter.cancelDiscovery(); } catch (SecurityException ignored) {}
            View cv = findViewById(R.id.cvScanning);
            if (cv != null) cv.setVisibility(View.GONE);
            ((MaterialButton) findViewById(R.id.btnScan)).setText("Scan Again");
            return;
        }
        if (!isLocationEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Location Required")
                    .setMessage("Android requires Location to be ON to scan for Bluetooth devices. Please enable it in Settings.")
                    .setPositiveButton("Open Settings", (d, w) ->
                            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQ_LOCATION_ENABLE))
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }
        try {
            devices.clear();
            deviceAdapter.notifyDataSetChanged();
            View cv = findViewById(R.id.cvScanning);
            if (cv != null) cv.setVisibility(View.VISIBLE);
            ((MaterialButton) findViewById(R.id.btnScan)).setText("Stop Scan");
            try { unregisterReceiver(discoveryReceiver); } catch (Exception ignored) {}
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(discoveryReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(discoveryReceiver, filter);
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    if (hasScanPermission()) {
                        boolean started = bluetoothAdapter.startDiscovery();
                        if (!started) {
                            Toast.makeText(this, "Could not start discovery. Is Bluetooth on?", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Scanning for nearby devices...", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (SecurityException e) {
                    Toast.makeText(this, "Scan permission denied.", Toast.LENGTH_SHORT).show();
                }
            }, 300);
        } catch (SecurityException ignored) {}
    }

    private void disconnect() {
        try {
            if (bluetoothSocket != null) { bluetoothSocket.close(); bluetoothSocket = null; }
            ChatHolder.connection = null;
            stopService(new Intent(this, BluetoothService.class));
            Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
        } catch (IOException | SecurityException e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean hasScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isLocationEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return true;
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return false;
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private boolean ensurePermissions() {
        List<String> perms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.BLUETOOTH_SCAN);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.BLUETOOTH_CONNECT);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!perms.isEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), REQ_PERMS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) { allGranted = false; break; }
            }
            if (allGranted) {
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
                if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
                    enableBluetooth();
                } else {
                    showPairedDevices();
                }
            } else {
                Toast.makeText(this,
                        "Permissions denied. Go to Phone Settings > Apps > BT Chat > Permissions and grant all permissions.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled() && hasConnectPermission()) {
            showPairedDevices();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(discoveryReceiver); } catch (Exception ignored) {}
        if (bluetoothServer != null) bluetoothServer.stopServer();
    }
}
