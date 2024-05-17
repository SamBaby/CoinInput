package com.example.coininput;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import coin_machine.CoinInputMachine;
import event.Var;

public class MainActivity extends AppCompatActivity {
    Var<Boolean> enable = new Var<>(false);
    Var<Boolean> enableCheck = new Var<>(false);
    Var<Boolean> coinInputReady = new Var<>(false);
    Var<Boolean> paperInputReady = new Var<>(false);
    int total = 0;
    int pay = 0;
    Map<Integer, UsbSerialPort> ports = new HashMap<>();
    public static final String USB_PERMISSION = "com.android.example.USB_PERMISSION";

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
        TextView output = findViewById(R.id.output_textView);
        EditText should_pay = findViewById(R.id.should_pay);
        should_pay.setText("160");
        StringBuilder builder = new StringBuilder();
        for (UsbSerialDriver driver : availableDrivers) {
            if (Objects.equals(driver.getDevice().getManufacturerName(), "FTDI")) {
                builder.append(String.format("Device Name:%s, PID:%d, VID:%d, port:%d\n", driver.getDevice().getProductName(), driver.getDevice().getProductId(), driver.getDevice().getVendorId(), driver.getPorts().size()));
            }
            if (Objects.equals(driver.getDevice().getProductName(), "FT2232H device")) {
                try {
                    if (!manager.hasPermission(driver.getDevice())) {
                        //add UsbManager.requestPermission(driver.getDevice(), ..) handling here
                        final Boolean[] granted = {null};
                        BroadcastReceiver usbReceiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                String action = intent.getAction();
                                if (action != null && action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                                    // A USB device has been attached
                                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                                    if (device != null) {
                                        // Check if the app has permission to access the USB device
                                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                                            // Permission granted, handle the USB device
                                            // You can access the device here
//                                            granted[0] = true;
                                        } else {
                                            // Permission denied, request permission from the user
                                            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                                            usbManager.requestPermission(device, PendingIntent.getBroadcast(context, 0, new Intent("com.example.USB_PERMISSION"), 0));
                                        }
                                    }
                                }
                            }
                        };
                        IntentFilter filter = new IntentFilter();
                        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
                        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
                        registerReceiver(usbReceiver, filter);
                        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.example.USB_PERMISSION"), 0);
                        manager.requestPermission(driver.getDevice(), permissionIntent);
                        for (int i = 0; i < 5000; i++) {
                            if (granted[0] != null) break;
                            Thread.sleep(1000);
                        }
                    }

                    for (int i = 0; i < 2; i++) {
                        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
                        if (connection == null) {
                            continue;
                        }
                        UsbSerialPort port = driver.getPorts().get(i); // Most devices have just one port (port 0)
                        port.open(connection);
                        port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_EVEN);
                        switch (i) {
                            case 0:
                                setCoinInput(port);
                                break;
                            case 1:
                                setPaperInput(port);
                                break;
                            default:
                                break;
                        }
                    }
//                    port.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        output.setText(builder.toString());
        btnEnable.setOnClickListener(v -> {
            if (!should_pay.getText().toString().isEmpty()) {
                total = Integer.parseInt(should_pay.getText().toString());
                pay = 0;
                enable.set(true);
                try {
                    //enable coin input machine
                    ports.get(0).write(new byte[]{(byte) 0x90, 0x05, 0x01, 0x03, (byte) 0x99}, 500);
                    //enable paper input machine
                    ports.get(1).write(new byte[]{(byte) 0x3E}, 500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        btnDisable.setOnClickListener(v -> {
            enable.set(false);
            paperInputReady.set(false);
            coinInputReady.set(false);
            output.setText(String.valueOf(pay - total));
            total = 0;
            pay = 0;
            try {
                //disable coin input machine
                ports.get(0).write(new byte[]{(byte) 0x90, 0x05, 0x02, 0x03, (byte) 0x9a}, 500);
                //disable paper input machine
                ports.get(1).write(new byte[]{(byte) 0x5E}, 500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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

    private void setCoinInput(UsbSerialPort port) {
        SerialInputOutputManager input = new SerialInputOutputManager(port, new SerialInputOutputManager.Listener() {
            @Override
            public void onNewData(byte[] bytes) {
                String ret = byteArrayToHexStr(bytes).toUpperCase();
                if (enable.get()) {
                    if (paperInputReady.get()) {
                        switch (ret) {
                            case "90 06 12 02 03 AD ":
                                pay += 5;
                                break;
                            case "90 06 12 03 03 AE ":
                                pay += 10;
                                break;
                            case "90 06 12 04 03 AF ":
                                pay += 50;
                                break;
                            default:
                                break;
                        }
                        TextView view = findViewById(R.id.money_textView);
                        view.setText(String.valueOf(pay));
                    } else if (ret.equals("90 05 50 03 E8 ")) {
                        coinInputReady.set(true);
                    }
                }
            }

            @Override
            public void onRunError(Exception e) {
                System.out.println(e);
            }
        });
        input.start();
        ports.put(0, port);
    }

    private void setPaperInput(UsbSerialPort port) {
        SerialInputOutputManager input = new SerialInputOutputManager(port, new SerialInputOutputManager.Listener() {
            @Override
            public void onNewData(byte[] bytes) {
                String ret = byteArrayToHexStr(bytes).toUpperCase();
                if (ret.equals("80 8F ")) {
                    try {
                        port.write(new byte[]{0x02}, 500);
                        return;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (enable.get()) {
                    if (coinInputReady.get()) {
                        switch (ret) {
                            case "81 40 ":
                                pay += 100;
                                try {
                                    port.write(new byte[]{0x02}, 500);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                break;
                            default:
                                try {
                                    port.write(new byte[]{0x0F}, 500);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                break;
                        }
                        TextView view = findViewById(R.id.money_textView);
                        view.setText(String.valueOf(pay));
                    } else if (ret.equals("90 05 50 03 E8 ")) {
                        coinInputReady.set(true);
                    }
                }
            }

            @Override
            public void onRunError(Exception e) {
                System.out.println();
            }
        });
        input.start();
        ports.put(1, port);
    }

    private void setCoinOutput(UsbSerialPort port) {
        SerialInputOutputManager input = new SerialInputOutputManager(port, new SerialInputOutputManager.Listener() {
            @Override
            public void onNewData(byte[] bytes) {
                System.out.println(byteArrayToHexStr(bytes));
            }

            @Override
            public void onRunError(Exception e) {
                System.out.println();
            }
        });
        input.start();
    }
}