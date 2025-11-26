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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMS = 2001;
    private static final int REQ_ENABLE_BT = 1001;

    private BluetoothAdapter bluetoothAdapter;
    private final List<BluetoothDevice> devices = new ArrayList<>();
    private final List<String> deviceNames = new ArrayList<>();
    private ArrayAdapter<String> listAdapter;
    private ListView listView;

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !devices.contains(device)) {
                    devices.add(device);
                    String name = device.getName() != null ? device.getName() : "Unknown";
                    deviceNames.add(name + " (" + device.getAddress() + ")");
                    listAdapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(context, "Discovery finished", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);

        // Setup ListView
        listView = findViewById(id.listDevices);
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);
        listView.setAdapter(listAdapter);

        // Bluetooth setup
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = manager != null ? manager.getAdapter() : null;

        // Button actions
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
        findViewById(id.btnEnableBt).setOnClickListener(v -> enableBluetooth());
        findViewById(id.btnScan).setOnClickListener(v -> startDiscovery());

        // NEW: Start server button
        findViewById(id.btnStartServer).setOnClickListener(v -> {
            try {
                BluetoothServer server = new BluetoothServer(MainActivity.this);
                server.start();
                Toast.makeText(this, "Server started, waiting for connections...", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Server error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // List item click: connect to device (client mode)
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= devices.size()) return;
            bluetoothAdapter.cancelDiscovery(); // stop scanning before connecting
            BluetoothDevice device = devices.get(position);
            BluetoothClient client = new BluetoothClient(device, bluetoothAdapter);
            client.connect(socket -> {
                try {
                    ChatConnection conn = new ChatConnection(socket);
                    ChatHolder.connection = conn;
                    startActivity(new Intent(MainActivity.this, ChatActivity.class));
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Connection error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        });

        // Request permissions
        ensurePermissions();
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
            Toast.makeText(this, "Bluetooth already enabled", Toast.LENGTH_SHORT).show();
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
        deviceNames.clear();
        listAdapter.notifyDataSetChanged();

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
        } else {
            Toast.makeText(this, "Scanning...", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasScanPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void ensurePermissions() {
        List<String> perms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.BLUETOOTH_SCAN);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!perms.isEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), REQ_PERMS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(discoveryReceiver); } catch (Exception ignored) {}
    }
}