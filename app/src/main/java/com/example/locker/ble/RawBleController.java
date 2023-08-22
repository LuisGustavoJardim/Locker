package com.example.locker.ble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.example.locker.ble.exceptions.BleNotConnectedException;
import com.example.locker.ble.exceptions.Messages;
import com.example.locker.ble.exceptions.MissingActivityException;
import com.example.locker.ble.interfaces.EventListenerBleInterface;

public class RawBleController extends BluetoothGattCallback {

    private static RawBleController fc;

    private Activity activity;
    private BluetoothAdapter bleAdapter;
    private BluetoothManager bleManager;
    private BluetoothGatt bleGatt;
    private BluetoothGattService gattService;
    private BluetoothGattCharacteristic characteristicRX;
    private BluetoothGattCharacteristic characteristicTX;
    private List<EventListenerBleInterface> listeners;
    //    public static UUID BLE_SERVICE = null;
    public static LockerHard locker = null;
    public static boolean isConnecting = false;
    private boolean isConnected;
    private BluetoothDevice device;
    private String wantedBleUID;


    /**
     * Send command to ble as JSONObject
     * @param command
     * @return
     * @throws BleNotConnectedException
     */
    public static boolean sendCommand(JSONObject command) throws BleNotConnectedException {
        return sendRawData(command.toString());
    }

    /**
     * Send commands do ble as String data.
     * @param command
     * @return
     * @throws BleNotConnectedException
     */
    public static boolean sendRawData(String command) throws BleNotConnectedException {
        if (! fc.isConnected || fc.characteristicRX == null) {
            throw new BleNotConnectedException();
        }
        final int properties = fc.characteristicRX.getProperties();
        fc.dispatchEvent(listenerTypes.log, "Characteristic " + fc.characteristicRX.toString() + " properties: " + properties);
        if ((properties & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
            fc.dispatchEvent(listenerTypes.log, "Characteristic " + fc.characteristicRX.toString() + " does not have WRITE permission.");
            return false;
        }
        fc.dispatchEvent(listenerTypes.log, "sending command: " + command + " to characteristic " + fc.characteristicRX.toString());
        fc.characteristicRX.setValue(command.getBytes());
        @SuppressLint("MissingPermission") boolean status = fc.bleGatt.writeCharacteristic(fc.characteristicRX);
        fc.dispatchEvent(listenerTypes.log, "status from write: " + status);
        return true;
    }
//
//
//    public static void setService(String serviceUUID){
//        BLE_SERVICE = UUID.fromString(serviceUUID);
//    }

    private enum listenerTypes {
        connected, disconnected, bleFailure, discoveryFinished, discoveryStarted, deviceDiscovered,
        deviceNotFound, deviceConnectionFail, failGetServices, connectedAndReady, receivedData, log
    }

    private RawBleController(Activity activity) {
        this.activity = activity;
        this.listeners = new ArrayList<>();
    }

    private void dispatchEvent(listenerTypes type, String msg) {
        for (EventListenerBleInterface listener : listeners) {
            try {
                switch (type) {
                    case connected:
                        listener.onConnected();
                        return;
                    case disconnected:
                        listener.onDisconnected();
                        return;
                    case bleFailure:
                        listener.onConnectFail(msg);
                        return;
                    case discoveryFinished:
                        listener.onDiscoveryFinished();
                        return;
                    case discoveryStarted:
                        listener.onDiscoveryStarted();
                        return;
                    case deviceDiscovered:
                        listener.onDeviceDiscovered();
                        return;
                    case deviceNotFound:
                        listener.onDiscoveryDeviceFail();
                        return;
                    case deviceConnectionFail:
                        listener.onDeviceConnectionFail();
                        return;
                    case failGetServices:
                        listener.onGetServicesFail(msg);
                        return;
                    case connectedAndReady:
                        listener.onConnectedAndReady();
                    case receivedData:
                        listener.onReceivedData(msg);
                    case log:
                        listener.onLog(msg);
                    default:
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }

        }
    }

    public static void addEventListener(EventListenerBleInterface listener) {
        fc.listeners.add(listener);
    }

    public static void removeEventListener(EventListenerBleInterface listener) {
        fc.listeners.remove(listener);
    }

    private boolean checkPermissions() {
        if (activity == null) {
            return false;
        }
        if (bleManager == null) {
            bleManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        }
        if (bleAdapter == null) {
            bleAdapter = bleManager.getAdapter();
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    1);
            return false;
        }

        if (bleAdapter == null || !bleAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, 1);
            return false;
        } else {
            return true;
        }
    }

    @SuppressLint("MissingPermission")
    private boolean isConnected(String bleUID) {
        if (device != null) {
            if (bleUID != null && bleUID.toLowerCase().equals(device.getName().toLowerCase())) {
                int state = bleManager.getConnectionState(device, BluetoothProfile.GATT);
                return BluetoothProfile.STATE_CONNECTED == state;
            } else {
                if (bleGatt != null) {
                    int state = bleManager.getConnectionState(device, BluetoothProfile.GATT);
                    if (BluetoothProfile.STATE_CONNECTED == state) {
                        bleGatt.disconnect();
                        bleGatt = null;
                        device = null;
                        isConnected = false;
                        characteristicRX = null;
                        characteristicTX = null;
                    }
                }
                return false;
            }
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    private void connectOnDevice() {
        if (bleAdapter.isDiscovering()) {
            bleAdapter.cancelDiscovery();
        }
        if (device == null) {
            dispatchEvent(listenerTypes.deviceConnectionFail, "");
            return;
        }

        device.connectGatt(activity.getBaseContext(), true, this);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        dispatchEvent(listenerTypes.log, "device connection state change: " + newState);
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            dispatchEvent(listenerTypes.connected, "");
            bleGatt = gatt;
            isConnected = true;
            gatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            dispatchEvent(listenerTypes.disconnected, "");
            isConnected = false;
            bleGatt = null;
            gattService = null;
            characteristicRX = null;
            characteristicTX = null;
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        dispatchEvent(listenerTypes.log, "Write on Characteristic status: " + status);
    }

    @Override
    public void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            dispatchEvent(listenerTypes.log, "MTU Changed for " + mtu);
        } else {
            dispatchEvent(listenerTypes.log, "MTU of " + mtu + " changing status " + status );
        }
    }

    public UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

        Log.d("BLETEST", "onServicesDiscovered() - status=" + status);

        if (status == BluetoothGatt.GATT_SUCCESS) {

            for (BluetoothGattService svc : gatt.getServices()) {
                Log.d("BLETEST", "" + svc.getUuid().toString());
            }

            gattService = gatt.getService(UUID.fromString(locker.getUuidMac1().toUpperCase()));

            if(gattService == null) {
                gattService = gatt.getService(UUID.fromString(locker.getUuidMac2()));
            }

            Log.d("BLETEST", "gattService " + gattService + " uuid1" + locker.getUuidMac1() + " uuid2=" + locker.getUuidMac2());

            if (gattService != null) {

                characteristicRX = gattService.getCharacteristic( gattService.getCharacteristics().get(1).getUuid());
                characteristicTX = gattService.getCharacteristic( gattService.getCharacteristics().get(0).getUuid());
                //                Update to be notified about data change
                try {
                    gatt.setCharacteristicNotification(characteristicTX, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    BluetoothGattDescriptor descriptor = characteristicTX.getDescriptor(convertFromInteger(0x2902));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                bleAdapter.cancelDiscovery();

                dispatchEvent(listenerTypes.connectedAndReady, "" + status);

            } else {
                isConnecting = false;
                bleGatt.disconnect();
            }
        } else {
            dispatchEvent(listenerTypes.failGetServices, "" + status);
            bleGatt.disconnect();
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        String bleData = characteristic.getStringValue(0);
        dispatchEvent(listenerTypes.receivedData, bleData);
    }

    @SuppressLint("MissingPermission")
    private void discoverAndConnect(String bleUID, final int timerMS) {
        if (bleAdapter.isDiscovering()) {
            bleAdapter.cancelDiscovery();
        }
        wantedBleUID = bleUID;
        isConnected = false;
        device = null;
        bleGatt = null;

        IntentFilter discoveryDeviceFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter discoveryFinished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        activity.registerReceiver(bleReceiver, discoveryDeviceFound);
        activity.registerReceiver(bleReceiver, discoveryFinished);
        bleAdapter.startDiscovery();
        this.dispatchEvent(listenerTypes.discoveryStarted, "");
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(timerMS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                if ( device == null && bleAdapter.isDiscovering()) {
//                    dispatchEvent(listenerTypes.discoveryFinished, "");
//                    dispatchEvent(listenerTypes.deviceNotFound, Messages.DEVICE_NOT_FOUND.replaceAll("\\$\\{device\\}", wantedBleUID));
//                    try {
//                        activity.unregisterReceiver(bleReceiver);
//                    } catch (Exception e) {
//                        Log.e("ERRO ", Objects.requireNonNull(e.getMessage()));
//                    }
//                    //bleAdapter.cancelDiscovery();
//                }
            }
        }.start();
    }

    public static boolean isConnected() {
        return fc != null && fc.isConnected;
    }

    public static boolean prepareInstance(Activity activity) throws MissingActivityException {
        if (fc == null) {
            if (activity == null) throw new MissingActivityException();
            fc = new RawBleController(activity);
        } else {
            if (activity != null) {
                fc.activity = activity;
            }
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public static boolean connect(Activity activity, String bleUID, String serialVersion, int timerMS) throws MissingActivityException {
        if (activity != null && (!RawBleController.prepareInstance(activity) || !fc.checkPermissions())) {
            fc.dispatchEvent(listenerTypes.bleFailure, Messages.BLE_NOT_AVAILABLE);
            return false;
        }

        if (fc != null && fc.isConnected(bleUID) && fc.bleGatt != null) {
            if (fc.gattService == null) {
                fc.bleGatt.discoverServices();
            }
            fc.dispatchEvent(listenerTypes.connected, "");
            return true;
        }
        fc.discoverAndConnect(bleUID, timerMS);
        return true;
    }

    @SuppressLint("MissingPermission")
    public static boolean disconnect() {
        if (fc != null && fc.isConnected(fc.wantedBleUID) && fc.bleGatt != null) {
            fc.bleGatt.disconnect();
            fc.bleGatt.close();
            fc.bleGatt = null;
            fc.device = null;
            fc.isConnected = false;
            fc.characteristicRX = null;
            fc.characteristicTX = null;
        }
        return true;
    }

    private final BroadcastReceiver bleReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @RequiresApi(api = Build.VERSION_CODES.R)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice deviceFound = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String address = deviceFound.getAddress();
                if (!isConnecting && (address.equals(locker.getAddressMac1()) || address.equals(locker.getAddressMac2())) &&
                        deviceFound != null && deviceFound.getName() != null &&
                        deviceFound.getName().toLowerCase().equals(wantedBleUID.toLowerCase())) {
                    device = deviceFound;
                    isConnecting = true;
                    connectOnDevice();
                }
                return;
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//                dispatchEvent(listenerTypes.discoveryFinished, "");
                dispatchEvent(listenerTypes.deviceConnectionFail, "");
                //DoorActivity.dialog.dismissLoadingDialogAlert();
                activity.unregisterReceiver(bleReceiver);
                //return;
            }
        }
    };


}