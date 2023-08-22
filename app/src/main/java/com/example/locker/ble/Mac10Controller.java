package com.example.locker.ble;

import android.app.Activity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.example.locker.ble.exceptions.BleNotConnectedException;
import com.example.locker.ble.exceptions.MissingActivityException;
import com.example.locker.ble.exceptions.MissingBlePermissionException;
import com.example.locker.ble.interfaces.EventListenerBleInterface;

public class Mac10Controller {
    private Activity activity;
    private String bleUID;
    private String authContent;
    private List<String> nordicContent;
    private List<String> atmelContent;
    private EventListener listener;

    public enum CommandType {
        unlock_door, lock_door, start_engine, stop_engine, cut_fuel, uncut_fuel, reset_device, init, update_atmel, update_nodic
    };


    /**
     * On instance needs to pass activity to ble able to request ble and locations permissions.
     * The steps are: Instance it, addListener, connectBle and when receive and event of ready start sending/reading commands
     * @param activity
     * @throws MissingActivityException - Thrown when missing activity
     * @throws MissingBlePermissionException - Thrown when initialing the bleController gone wrong.
     */
    public Mac10Controller(Activity activity) throws MissingActivityException, MissingBlePermissionException {
        this.activity = activity;
        if (!RawBleController.prepareInstance(activity)) {
            throw new MissingBlePermissionException();
        }
        listener = new EventListener();
        RawBleController.addEventListener(listener);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        RawBleController.removeEventListener(listener);
    }

    public void destroy() {
        RawBleController.removeEventListener(listener);
    }

    /**
     * Event listener to listen to rawBleController notifications
     * Needs to be a class that implements EventListenerBleInterface
     * @param listener
     */
    public void addListener(EventListenerBleInterface listener) {
        RawBleController.addEventListener(listener);
    }

    public void setUUID(String uuid){
//        RawBleController.setService(uuid);
    }

    /**
     * Remove the listener from rawBleController.
     * @param listener
     */
    public void removeListener(EventListenerBleInterface listener) {
        RawBleController.removeEventListener(listener);
    }

    /**
     * Connect to BLE UID given the timeout in milliseconds
     * @param bleUID
     * @param timeoutMS
     * @return
     * @throws MissingActivityException - Thrown when the activity is missing to check permissions.
     * @throws MissingBlePermissionException - Thrown when the app does not have all permissions to use BLE
     */
    public boolean connectBle(String bleUID, String serialVersion, String authContent, int timeoutMS) throws MissingActivityException, MissingBlePermissionException {
        if (!RawBleController.connect(this.activity, bleUID, serialVersion, timeoutMS)) {
            throw new MissingBlePermissionException();
        }
        this.authContent = authContent;
        this.bleUID = bleUID;
        return true;
    }

    /**
     * Disconnects from BLE
     * @return
     */
    public boolean disconnectBle() {
        return RawBleController.disconnect();
    }

    /**
     * Execute an async command on device.
     * And proper response will be get from events dispatched from RawBleController when received data.
     * @param command
     * @return
     * @throws BleNotConnectedException - thrown when not connected
     */
    public boolean asyncExecCommand(String data, int command) throws BleNotConnectedException {

        return basicCommandControl(data, command);
    }

    /**
     *
     * @param deviceID
     * @param userUID
     * @param command
     * @param data - read bin file transformed on string based on chunk parts. Each line is on chunk
     * @param version
     * @return
     * @throws BleNotConnectedException
     */
    public boolean execCommandOta(String deviceID, String userUID, CommandType command, String data, String version) throws BleNotConnectedException {
        List<String> content = null;
        if (CommandType.update_atmel == command) {
            atmelContent = new ArrayList<>();
            content = atmelContent;
        } else if (CommandType.update_nodic == command) {
            nordicContent = new ArrayList<>();
            content = nordicContent;
        }
        if (content != null) {
            Collections.addAll(content, data.split("\n"));
            otaCommandControl(deviceID, userUID, command, content.size(), version);
            return true;
        } else {
            return false;
        }
    }

    /*public void setMTU(int mtu) throws BleNotConnectedException {
        RawBleController.setMTU(mtu);
    }*/

    private boolean sendOtaData(CommandType command, int chunk, String data) throws BleNotConnectedException {
        JSONObject cmdCtrl = new JSONObject();
        JSONObject commandObj = new JSONObject();
        try {
            commandObj.put("cmd", command.ordinal() + 1);
            commandObj.put("chunk", chunk);
            commandObj.put("data", data);

            cmdCtrl.put("CmdCtrl", commandObj);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        if (this.bleUID != null && !RawBleController.isConnected()) {
            throw new BleNotConnectedException();
        } else {

            return RawBleController.sendCommand(cmdCtrl);
        }
    }

    public boolean basicCommandRelay(int target) throws BleNotConnectedException {
        JSONObject commandObj = new JSONObject();
        JSONObject cmdMem = new JSONObject();
        try {
            commandObj.put("target", target);
            commandObj.put("cmd", "1");
            cmdMem.put("CmdCtrl", commandObj);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        if (this.bleUID != null && !RawBleController.isConnected()) {
            throw new BleNotConnectedException();
        } else {
            return RawBleController.sendCommand(cmdMem);
        }
    }

    public boolean resetRelay() throws BleNotConnectedException {
        JSONObject commandObj = new JSONObject();
        JSONObject cmdMem = new JSONObject();
        try {
            commandObj.put("cmd", "2");
            cmdMem.put("CmdCtrl", commandObj);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
//        if (this.bleUID != null && !RawBleController.isConnected()) {
//            throw new BleNotConnectedException();
//        } else {
//            return RawBleController.sendCommand(cmdMem);
//        }
        return RawBleController.sendCommand(cmdMem);
    }

    public boolean getTemperature(int min, int max) throws BleNotConnectedException {
        JSONObject commandObj = new JSONObject();
        JSONObject cmdMem = new JSONObject();
        try {
            commandObj.put("cmd", "6");
            commandObj.put("min", min);
            commandObj.put("max", max);
            commandObj.put("timeout", 1000);
            cmdMem.put("CmdCtrl", commandObj);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        if (this.bleUID != null && !RawBleController.isConnected()) {
            throw new BleNotConnectedException();
        } else {
            return RawBleController.sendCommand(cmdMem);
        }
    }

    private boolean basicCommandControl(String data, int command) throws BleNotConnectedException {
        JSONObject commandObj = new JSONObject();
        JSONObject cmdMem = new JSONObject();
        try {
            commandObj.put("cmd", command);
            commandObj.put("data", data);
            cmdMem.put("CmdCtrl", commandObj);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        if (this.bleUID != null && !RawBleController.isConnected()) {
            throw new BleNotConnectedException();
        } else {
            return RawBleController.sendCommand(cmdMem);
        }
    }

    private boolean otaCommandControl(String deviceID, String user, CommandType command, int chunks, String version) throws BleNotConnectedException {
        long startTime = System.currentTimeMillis();
        JSONObject commandObj = new JSONObject();
        JSONObject cmdCtrl = new JSONObject();
        try {
            commandObj.put("id", deviceID);
            commandObj.put("user", user);
            commandObj.put("cmd", command.ordinal() + 1);
            commandObj.put("version", version);
            commandObj.put("chunks", chunks);
            cmdCtrl.put("CmdCtrl", commandObj);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        if (this.bleUID != null && !RawBleController.isConnected()) {
            throw new BleNotConnectedException();
        } else {
            return RawBleController.sendCommand(cmdCtrl);
        }
    }

    /**
     * Send a write memory command to BLE.
     * @param Address
     * @param data
     * @return
     * @throws BleNotConnectedException
     */
    public boolean writeMemoryCommand(int Address, String data) throws BleNotConnectedException {
        JSONObject commandObj = new JSONObject();
        JSONObject cmdMem = new JSONObject();
        try {
            commandObj.put("flag", "SET");
            commandObj.put("address", Address);
            commandObj.put("data", data);
            cmdMem.put("CmdMem", commandObj);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        if (this.bleUID != null && !RawBleController.isConnected()) {
            throw new BleNotConnectedException();
        } else {
            return RawBleController.sendCommand(cmdMem);
        }
    }

    /**
     * Send a read memory command to ble.
     * Its responses needs to be read from dispatched event from RawBleController.
     * @param Address
     * @return
     * @throws BleNotConnectedException
     */
    public boolean readMemoryCommand(int Address) throws BleNotConnectedException {
        JSONObject commandObj = new JSONObject();
        JSONObject cmdMem = new JSONObject();
        try {
            commandObj.put("flag", "GET");
            commandObj.put("address", Address);
            cmdMem.put("CmdMem", commandObj);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        if (this.bleUID != null && !RawBleController.isConnected()) {
            throw new BleNotConnectedException();
        } else {
            return RawBleController.sendCommand(cmdMem);
        }
    }

    private class EventListener implements EventListenerBleInterface {
        @Override
        public void onConnected() {

        }

        @Override
        public void onDisconnected() {

        }

        @Override
        public void onConnectFail(String msg) {

        }

        @Override
        public void onDiscoveryStarted() {

        }

        @Override
        public void onDiscoveryFinished() {

        }

        @Override
        public void onDeviceDiscovered() {

        }

        @Override
        public void onDiscoveryDeviceFail() {

        }

        @Override
        public void onDeviceConnectionFail() {

        }

        @Override
        public void onGetServicesFail(String status) {

        }

        @Override
        public void onConnectedAndReady() {

        }

        @Override
        public void onReceivedData(String data) {

            // check if is not and ATMEL or NORDIC update communication

            if (data == null || data.isEmpty()) {
                return;
            }
            data = data.trim().toLowerCase();
            try {
                JSONObject jsonObj = new JSONObject(data);
                if(jsonObj != null) {
                    JSONObject cmdCtrl = jsonObj.optJSONObject("cmdctrl");
                    if (cmdCtrl != null) {
                        int cmd = cmdCtrl.optInt("cmd", 0);
                        int chunk = cmdCtrl.optInt("chunk", -1);
                        if ((cmd == CommandType.update_atmel.ordinal()+1 || cmd == CommandType.update_nodic.ordinal()+1) && chunk != -1) {

                            List<String> content = null;
                            if (cmd == CommandType.update_atmel.ordinal()+1 ) content = atmelContent;
                            if (cmd == CommandType.update_nodic.ordinal()+1 ) content = nordicContent;
                            if (content != null && content.size() > (chunk-1)) {
                                sendOtaData(CommandType.values()[cmd-1], chunk, content.get(chunk-1));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onLog(String log) {

        }
    }
}
