package com.example.locker.ble.exceptions;


public class BleNotConnectedException extends Exception {

    final private static String MSG = "Bluetooth is not connected on device.";
    @Override
    public String getMessage() {
        return MSG;
    }
}
