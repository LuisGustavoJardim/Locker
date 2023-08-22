package com.example.locker.ble.exceptions;


public class MissingActivityException extends Exception {

    final private static String MSG = "Missing Activity on method prepareInstance. Must call prepareInstance with an Activity before use it.";
    @Override
    public String getMessage() {
        return MSG;
    }
}
