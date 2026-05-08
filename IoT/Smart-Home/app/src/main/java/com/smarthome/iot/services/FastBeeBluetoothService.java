package com.smarthome.iot.services;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FastBee Bluetooth Low Energy (BLE) provisioning service.
 * Handles scanning for IoT devices and sending MQTT credentials via BLE.
 */
public class FastBeeBluetoothService {
    private static final String TAG = "FastBeeBLE";

    // FastBee BLE UUIDs (standard UUIDs - replace with actual FastBee UUIDs from device firmware)
    private static final UUID SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
    private static final UUID CHARACTERISTIC_WRITE_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");
    private static final UUID CHARACTERISTIC_NOTIFY_UUID = UUID.fromString("0000FFE2-0000-1000-8000-00805F9B34FB");

    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private Handler handler;

    private ProvisioningCallback provisioningCallback;
    private ScanCallback scanCallback;
    private String targetDeviceAddress;
    private boolean isScanning = false;

    /**
     * Callback interface for provisioning events
     */
    public interface ProvisioningCallback {
        void onScanStarted();
        void onDeviceFound(BluetoothDevice device, int rssi);
        void onConnecting();
        void onConnected();
        void onCredentialsSent();
        void onProvisioningComplete();
        void onError(String error);
    }

    public FastBeeBluetoothService(Context context) {
        this.context = context;
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
        this.handler = new Handler(Looper.getMainLooper());
    }

    /**
     * Check if Bluetooth is enabled and supported
     */
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * Check if Bluetooth permissions are granted
     */
    public boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                   ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                   ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Scan for FastBee devices by device name pattern
     * @param deviceNamePrefix Device name prefix (e.g., "FastBee_1001" for productId 1001)
     * @param callback Provisioning callback
     */
    public void scanForDevice(String deviceNamePrefix, ProvisioningCallback callback) {
        this.provisioningCallback = callback;

        if (!isBluetoothEnabled()) {
            callback.onError("Bluetooth is not enabled");
            return;
        }

        if (!hasBluetoothPermissions()) {
            callback.onError("Bluetooth permissions not granted");
            return;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            callback.onError("BLE scanner not available");
            return;
        }

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                BluetoothDevice device = result.getDevice();
                String deviceName = device.getName();

                if (deviceName != null && deviceName.startsWith(deviceNamePrefix)) {
                    Log.d(TAG, "Found FastBee device: " + deviceName + " (" + device.getAddress() + ")");
                    targetDeviceAddress = device.getAddress();
                    stopScan();
                    callback.onDeviceFound(device, result.getRssi());
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "BLE scan failed: " + errorCode);
                callback.onError("Scan failed with error code: " + errorCode);
            }
        };

        // Start scanning
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        List<ScanFilter> filters = new ArrayList<>();
        // Optional: Add filter by device name prefix if supported
        // ScanFilter filter = new ScanFilter.Builder().setDeviceName(deviceNamePrefix).build();
        // filters.add(filter);

        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothLeScanner.startScan(filters, scanSettings, scanCallback);
                isScanning = true;
                callback.onScanStarted();
                Log.d(TAG, "Started BLE scan for device: " + deviceNamePrefix);

                // Auto-stop scan after 10 seconds
                handler.postDelayed(() -> {
                    if (isScanning) {
                        stopScan();
                        callback.onError("Device not found. Please ensure the device is in pairing mode.");
                    }
                }, 10000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start scan", e);
            callback.onError("Failed to start scan: " + e.getMessage());
        }
    }

    /**
     * Stop BLE scan
     */
    public void stopScan() {
        if (isScanning && bluetoothLeScanner != null && scanCallback != null) {
            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothLeScanner.stopScan(scanCallback);
                    isScanning = false;
                    Log.d(TAG, "Stopped BLE scan");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error stopping scan", e);
            }
        }
    }

    /**
     * Connect to FastBee device and send MQTT credentials
     * @param deviceAddress Bluetooth device address
     * @param brokerUrl MQTT broker URL (e.g., "mqtt://172.86.88.76:1883")
     * @param username MQTT username
     * @param password MQTT password
     * @param clientId MQTT client ID
     */
    public void provisionDevice(String deviceAddress, String brokerUrl, String username, String password, String clientId) {
        if (!hasBluetoothPermissions()) {
            if (provisioningCallback != null) {
                provisioningCallback.onError("Bluetooth permissions not granted");
            }
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        if (device == null) {
            if (provisioningCallback != null) {
                provisioningCallback.onError("Device not found");
            }
            return;
        }

        if (provisioningCallback != null) {
            provisioningCallback.onConnecting();
        }

        // Connect to GATT server
        BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to GATT server");
                    if (provisioningCallback != null) {
                        handler.post(() -> provisioningCallback.onConnected());
                    }

                    // Discover services
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.discoverServices();
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from GATT server");
                    gatt.close();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Services discovered");

                    // Find FastBee service and characteristic
                    BluetoothGattService service = gatt.getService(SERVICE_UUID);
                    if (service == null) {
                        Log.e(TAG, "FastBee service not found");
                        if (provisioningCallback != null) {
                            handler.post(() -> provisioningCallback.onError("FastBee service not found on device"));
                        }
                        return;
                    }

                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_WRITE_UUID);
                    if (characteristic == null) {
                        Log.e(TAG, "Write characteristic not found");
                        if (provisioningCallback != null) {
                            handler.post(() -> provisioningCallback.onError("Write characteristic not found"));
                        }
                        return;
                    }

                    // Build JSON payload with MQTT credentials
                    JSONObject credentials = new JSONObject();
                    try {
                        credentials.put("broker", brokerUrl);
                        credentials.put("username", username);
                        credentials.put("password", password);
                        credentials.put("clientId", clientId);

                        String payload = credentials.toString();
                        Log.d(TAG, "Sending credentials: " + payload);

                        // Write credentials to characteristic
                        characteristic.setValue(payload.getBytes(StandardCharsets.UTF_8));
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            boolean success = gatt.writeCharacteristic(characteristic);
                            if (!success) {
                                Log.e(TAG, "Failed to write characteristic");
                                if (provisioningCallback != null) {
                                    handler.post(() -> provisioningCallback.onError("Failed to send credentials"));
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to build credentials JSON", e);
                        if (provisioningCallback != null) {
                            handler.post(() -> provisioningCallback.onError("Failed to build credentials: " + e.getMessage()));
                        }
                    }
                } else {
                    Log.e(TAG, "Service discovery failed: " + status);
                    if (provisioningCallback != null) {
                        handler.post(() -> provisioningCallback.onError("Service discovery failed"));
                    }
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Credentials sent successfully");
                    if (provisioningCallback != null) {
                        handler.post(() -> {
                            provisioningCallback.onCredentialsSent();
                            provisioningCallback.onProvisioningComplete();
                        });
                    }

                    // Disconnect after successful write
                    handler.postDelayed(() -> {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            gatt.disconnect();
                        }
                    }, 1000);
                } else {
                    Log.e(TAG, "Failed to write characteristic: " + status);
                    if (provisioningCallback != null) {
                        handler.post(() -> provisioningCallback.onError("Failed to send credentials (write error)"));
                    }
                }
            }
        };

        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                bluetoothGatt = device.connectGatt(context, false, gattCallback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to device", e);
            if (provisioningCallback != null) {
                provisioningCallback.onError("Failed to connect: " + e.getMessage());
            }
        }
    }

    /**
     * Disconnect and cleanup
     */
    public void disconnect() {
        stopScan();
        if (bluetoothGatt != null) {
            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothGatt.disconnect();
                    bluetoothGatt.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error disconnecting", e);
            }
            bluetoothGatt = null;
        }
    }
}
