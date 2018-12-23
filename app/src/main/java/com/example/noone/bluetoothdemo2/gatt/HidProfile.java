package com.example.noone.bluetoothdemo2.gatt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

public class HidProfile {

    private static final String TAG = HidProfile.class.getSimpleName();

    /* hid Service UUID */
    public static final UUID HID_SERVICE = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb");
    /* Report Characteristic */
    public static final UUID REPORT = UUID.fromString("00002a4d-0000-1000-8000-00805f9b34fb");
    /* Report Reference Descriptor */
    public static final UUID REPORT_REFERENCE_DESCRIPTOR = UUID.fromString("00002908-0000-1000-8000-00805f9b34fb");
    /* Report Map Characteristic */
    public static final UUID REPORT_MAP = UUID.fromString("00002a4b-0000-1000-8000-00805f9b34fb");
    /* HID Information Characteristic */
    public static final UUID HID_INFORMATION = UUID.fromString("00002a4a-0000-1000-8000-00805f9b34fb");
    /* HID Control Point Characteristic */
    public static final UUID HID_CONTROL_POINT = UUID.fromString("00002a4c-0000-1000-8000-00805f9b34fb");

    private static final byte[] HID_INFORMATION_VALUE = {
            (byte) 0x0112,  // bcd hid规范版本号
            0x00,           // 国家码
            0x00            // flags
    };

    private static final byte[] REPORT_REFERENCE_VALUE = {
            0x00,           // report id
            0x01,           // Input Report
    };

    private static final byte[] REPORT_MAP_DATA = {
            0x05, 1,      // Usage Page (1: Generic Desktop)
            0x09, 6,      // Usage (6: Keyboard) 表示报表定义的是HID键盘
            (byte) 0xA1, 1,      // Collection (1: Application) ====================集合开始

            (byte) 0x85, 0x00, // Report Id (2)
            //   以下定义了键盘的修饰键输入报表，共有8个键，组成一个字节
            //   用法见HID Usage Table中的第10节中的键盘用法定义
            0x05, 7,      //   Usage page (7: Key Codes)
            0x19, (byte) 224,   //   Usage Minimum (224)
            0x29, (byte) 231,    //   Usage Maximum (231)
            0x15, 0,      //   Logical Minimum (0)
            0x25, 1,     //   Logical Maximum (1)
            0x75, 1,      //   Report Size (1)
            (byte) 0x95, 8,      //   Report Count (8)
            (byte) 0x81, 2,      //   Input (Data,Variable,Absolute)

            //   以下定义了一个保留字节的输入报表
            (byte) 0x95, 1,      //   Report Count (1)
            0x75, 8,      //   Report Size (8),
            (byte) 0x81, 1,     //   Input (Constant) = Reserved Byte

            //   以下定义了键盘的LED指示灯输出报表项目，共有5个指示灯
            //   用法见HID Usage Table中的第11节中的LED用法定义
            (byte) 0x95, 5,      //   Report Count (5)
            0x75, 1,     //   Report Size (1)
            0x05, 8,      //   Usage Page (Page# for LEDs)
            0x19, 1,      //   Usage Minimum (1)
            0x29, 5,      //   Usage Maximum (5)
            (byte) 0x91, 2,      //   Output (Data, Variable, Absolute)

            //   以下定义了3个填充位，与前面的5个LED指示灯数据组成一个完整的字节
            (byte) 0x95, 1,      //   Report Count (1)
            0x75, 3,      //   Report Size (3)
            (byte) 0x91, 1,      //   Output (Constant)

            //   以下定义了键盘的按键值输入报表项目，共6个字节，存放键编号（0~101）
            //   用法见HID Usage Table中的第10节中的键盘用法定义
            //   这样的设计可以允许一次输入6个按键的键值
            (byte) 0x95, 6,      //   Report Count (6)
            0x75, 8,      //   Report Size (8)
            0x15, 0,      //   Logical Minimum (0)
            0x25, 101,    //   Logical Maximum (101)
            0x05, 7,      //   Usage Page (7: Key Codes)
            0x19, 0,      //   Usage Minimum (0)
            0x29, 101,    //   Usage Maximum (101)
            (byte) 0x81, 0,     //   Input (Data, Array)

            (byte) 0xC0         // End_Collection ================================ 集合结束

    };

    private Context context;
    /* Bluetooth API */
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothDevice device;

    public HidProfile(Context context) {
        this.context = context;
        this.mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    public void startService() {
        startAdvertising();
        startServer();
    }

    public void stopService() {
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        if (bluetoothAdapter.isEnabled()) {
            stopServer();
            stopAdvertising();
        }
    }

    private BluetoothGattService createHidService() {
        BluetoothGattService service = new BluetoothGattService(HID_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // report
        BluetoothGattCharacteristic reportCharacteristic = new BluetoothGattCharacteristic(HidProfile.REPORT,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        // report reference
        BluetoothGattDescriptor reportReferenceDescriptor = new BluetoothGattDescriptor(REPORT_REFERENCE_DESCRIPTOR,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        reportReferenceDescriptor.setValue(REPORT_REFERENCE_VALUE);
        reportCharacteristic.addDescriptor(reportReferenceDescriptor);

        // report map
        BluetoothGattCharacteristic reportMapCharacteristic = new BluetoothGattCharacteristic(REPORT_MAP,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        reportMapCharacteristic.setValue(REPORT_MAP_DATA);

        // hid information
        BluetoothGattCharacteristic hidInformationCharacteristic = new BluetoothGattCharacteristic(HID_INFORMATION,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        hidInformationCharacteristic.setValue(HID_INFORMATION_VALUE);

        BluetoothGattCharacteristic hidControlPoint = new BluetoothGattCharacteristic(HID_CONTROL_POINT,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        service.addCharacteristic(reportCharacteristic);
        service.addCharacteristic(reportMapCharacteristic);
        service.addCharacteristic(hidInformationCharacteristic);
        service.addCharacteristic(hidControlPoint);

        return service;
    }

    private void startAdvertising() {
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Log.w(TAG, "Failed to create advertiser");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(HID_SERVICE))
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) {
            return;
        }

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    private void startServer() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(context, mGattServerCallback);
        if (mBluetoothGattServer == null) {
            Log.w(TAG, "Unable to create GATT server");
            return;
        }

        mBluetoothGattServer.addService(createHidService());

    }

    private void stopServer() {
        if (mBluetoothGattServer == null) {
            return;
        }

        mBluetoothGattServer.close();
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: "+errorCode);
        }
    };

    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BluetoothDevice CONNECTED: " + device);
                HidProfile.this.device = device;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "BluetoothDevice DISCONNECTED: " + device);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            if (REPORT_MAP.equals(characteristic.getUuid())) {
                Log.i(TAG, "Read report map");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        REPORT_MAP_DATA);
            } else if (HID_INFORMATION.equals(characteristic.getUuid())) {
                Log.i(TAG, "Read hid information");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        HID_INFORMATION_VALUE);
            } else {
                // Invalid characteristic
                Log.w(TAG, "Invalid Characteristic Read: " + characteristic.getUuid());
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattDescriptor descriptor) {
            if (REPORT_REFERENCE_DESCRIPTOR.equals(descriptor.getUuid())) {
                Log.d(TAG, "Report reference descriptor read");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        REPORT_REFERENCE_VALUE);
            } else {
                Log.w(TAG, "Unknown descriptor read request");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
            if (HID_CONTROL_POINT.equals(descriptor.getUuid())) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Subscribe device to notifications: " + device);
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Unsubscribe device from notifications: " + device);
                }
            } else {
                Log.w(TAG, "Unknown descriptor write request");
            }
        }

    };

    public void sendKeyCode(byte keyCode) {
        byte[] buf = new byte[]{
                0,
                0,
                keyCode,
                0,
                0,
                0,
                0,
                0
        };
        mBluetoothGattServer.sendResponse(device,
                1,
                BluetoothGatt.GATT_FAILURE,
                0,
                buf);
        Log.v(TAG, "send keycode:" + keyCode);
    }

}
