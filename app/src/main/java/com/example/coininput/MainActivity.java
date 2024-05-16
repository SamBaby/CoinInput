package com.example.coininput;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.util.List;
import java.util.Objects;

import event.Var;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return;
        }
        Button btnEnable = findViewById(R.id.enable_button);
        Button btnDisable = findViewById(R.id.disable_button);
        TextView money = findViewById(R.id.money_textView);
        for (UsbSerialDriver driver : availableDrivers) {
            if (Objects.equals(driver.getDevice().getManufacturerName(), "FTDI")) {
                try {
                    if (!manager.hasPermission(driver.getDevice())) {
                        //add UsbManager.requestPermission(driver.getDevice(), ..) handling here
                        final Boolean[] granted = {null};
                        BroadcastReceiver usbReceiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                granted[0] = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                            }
                        };
                        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.example.USB_PERMISSION"), 0);
                        manager.requestPermission(driver.getDevice(), permissionIntent);
                        for (int i = 0; i < 5000; i++) {
                            if (granted[0] != null) break;
                            Thread.sleep(1000);
                        }
                    }
                    UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
                    if (connection == null) {
                        return;
                    }
                    UsbSerialPort port = driver.getPorts().get(0); // Most devices have just one port (port 0)
                    port.open(connection);
                    port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    Var<Boolean> enable = new Var<>(false);
                    Var<Boolean> enableCheck = new Var<>(false);
                    new SerialInputOutputManager(port, new SerialInputOutputManager.Listener() {
                        @Override
                        public void onNewData(byte[] bytes) {
                            System.out.println(byteArrayToHexStr(bytes));
                        }

                        @Override
                        public void onRunError(Exception e) {

                        }
                    });
                    btnEnable.setOnClickListener(v->{
                        enable.set(true);
                        try {
                            port.write(new byte[]{(byte) 0x90, 0x05 ,0x01 ,0x03 , (byte) 0x99}, 500);
//                            port.read()
                        }catch (Exception e){
                           e.printStackTrace();
                        }
                    });
                    btnDisable.setOnClickListener(v->{
                        enable.set(false);
                        try {
                            port.write(new byte[]{(byte) 0x90, 0x05 ,0x01 ,0x03 , (byte) 0x9a}, 500);
//                            port.read()
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    });
                    port.close();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        StringBuilder hex = new StringBuilder(byteArray.length * 2);
        for (byte aData : byteArray) {
            hex.append(String.format("%02X ", aData));
        }
        String gethex = hex.toString();
        return gethex;
    }
}