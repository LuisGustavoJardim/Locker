package com.example.locker.ble.exceptions;


public class MissingBlePermissionException extends Exception {

    final private static String MSG = Messages.BLE_NOT_AVAILABLE;
    @Override
    public String getMessage() {
        return MSG;
    }
}
