package com.example.locker.ble.interfaces;

public interface EventListenerBleInterface {
    void onConnected();
    void onDisconnected();
    void onConnectFail(String msg);
    void onDiscoveryStarted();
    void onDiscoveryFinished();
    void onDeviceDiscovered();
    void onDiscoveryDeviceFail();
    void onDeviceConnectionFail();
    void onGetServicesFail(String status);
    void onConnectedAndReady();
    void onReceivedData(String data);
    void onLog(String log);
}
