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
import androidx.annotation.RequiresPermission;
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
                    deviceAdapter.notifyDataSetChanged(); // Reliability
                                                          // focus
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (isRestartingDiscovery)
                    return;

                Toast.makeText(context, "Discovery finished", Toast.LENGTH_SHORT).show();
                View cv = findViewById(R.id.cvScanning);
                if (cv != null)
                    cv.setVisibility(View.GONE);
                ((MaterialButton) findViewById(R.id.btnScan)).setText("Scan Again");
            }
        }
    };

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
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
        findViewById(R.id.btnScan).setOnClickListener(v -> startDiscovery());
        findViewById(R.id.btnDisconnect).setOnClickListener(v -> disconnect());
        findViewById(R.id.btnServer).setOnClickListener(v -> startServer());

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            enableBluetooth();
        } else {
            showPairedDevices();
        }
    }

    private void showPairedDevices() {
        if (bluetoothAdapter == null || !hasConnectPermission())
            return;
        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices != null && !pairedDevices.isEmpty()) {
                devices.clear();
                devices.addAll(pairedDevices);
                deviceAdapter.notifyDataSetChanged();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private BluetoothServer bluetoothServer;

    private void startServer() {
        if (!hasConnectPermission()) {
            ensurePermissions();
            return;
        }
        if (bluetoothServer != null) {
            bluetoothServer.stopServer();
            bluetoothServer = null;
            Toast.makeText(this, "Server stopped", Toast.LENGTH_SHORT).show();
            ((MaterialButton) findViewById(R.id.btnServer)).setText("Start Server");
            return;
        }
        try {
            bluetoothServer = new BluetoothServer(this);
            bluetoothServer.start();
            Toast.makeText(this, "Server started...", Toast.LENGTH_SHORT).show();
            ((MaterialButton) findViewById(R.id.btnServer)).setText("Stop Server");
        } catch (IOException | SecurityException e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        if (!hasConnectPermission()) {
            ensurePermissions();
            return;
        }
        try {
            bluetoothAdapter.cancelDiscovery();
        } catch (SecurityException ignored) {
        }

        new Thread(() -> {
            try {
                BluetoothClient client = new BluetoothClient(device, bluetoothAdapter);
                BluetoothSocket socket = client.connect();
                runOnUiThread(() -> {
                    if (socket != null) {
                        bluetoothSocket = socket;
                        try {
                            ChatHolder.connection = new ChatConnection(socket);

                            // Start background service
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
                            Toast.makeText(this, "Connection error", Toast.LENGTH_SHORT).show();
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
        if (bluetoothAdapter == null)
            return;
        if (!hasConnectPermission()) {
            ensurePermissions();
            return;
        }
        try {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQ_ENABLE_BT);
            } else {
                showPairedDevices();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startDiscovery() {
        if (bluetoothAdapter == null)
            return;
        if (bluetoothAdapter.isDiscovering()) {
            try {
                bluetoothAdapter.cancelDiscovery();
            } catch (SecurityException ignored) {
            }
            return;
        }
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Enable Location for scanning", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }

        try {
            devices.clear();
            deviceAdapter.notifyDataSetChanged();
            View cv = findViewById(R.id.cvScanning);
            if (cv != null)
                cv.setVisibility(View.VISIBLE);

            try {
                unregisterReceiver(discoveryReceiver);
            } catch (Exception ignored) {
            }
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
                    if (ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                            || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        bluetoothAdapter.startDiscovery();
                        ((MaterialButton) findViewById(R.id.btnScan)).setText("Stop Scan");
                    }
                } catch (SecurityException ignored) {
                }
            }, 500);
        } catch (SecurityException ignored) {
        }
    }

    private void disconnect() {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }
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

    private boolean isLocationEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            return true;
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
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
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.POST_NOTIFICATIONS);
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
        if (bluetoothServer != null)
            bluetoothServer.stopServer();
    }
}
