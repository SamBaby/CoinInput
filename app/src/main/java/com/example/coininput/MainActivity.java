package com.example.coininput;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.Arrays;
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
    TextView money;
    TextView connectText;
    int total = 0;
    int pay = 0;
    Map<Integer, UsbSerialPort> ports = new HashMap<>();
    public static final String USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private D2xxManager coinInputManager;

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
        Button btnEnable = findViewById(R.id.enable_button);
        Button btnDisable = findViewById(R.id.disable_button);
        connectText = findViewById(R.id.connectText);
        money = findViewById(R.id.money_textView);
        money.setText("0");

        try {
            coinInputManager = D2xxManager.getInstance(this);
            IntentFilter filter = new IntentFilter();
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            this.getApplicationContext().registerReceiver(mCoinInputPlugEvents, filter);

            handleFT2232H(coinInputManager);
        } catch (Exception e) {
            e.printStackTrace();
        }

        btnEnable.setOnClickListener(v -> {
            boolean coinEnable = setCoinInputEnable();
            pay = 0;
            if (coinEnable) {
                readCoinInput();
            }
        });
        btnDisable.setOnClickListener(v -> {
            setCoinInputDisable();
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


    private FT_Device coinInputDevice;
    private FT_Device paperInputDevice;
    private FT_Device coin5Device;
    private FT_Device coin10Device;
    private FT_Device coin50Device;
    private boolean readCoinInput = false;
    private boolean readPaperInput = false;
    private static final byte[] enableCoinInput = new byte[]{(byte) 0x90, 0x05, 0x01, 0x03, (byte) 0x99};
    private static final byte[] disableCoinInput = new byte[]{(byte) 0x90, 0x05, 0x02, 0x03, (byte) 0x9a};
    private static final byte[] enableCoinInputSuccess = new byte[]{(byte) 0x90, 0x05, 0x50, 0x03, (byte) 0xe8};
    private static final byte[] getCoin5 = new byte[]{(byte) 0x90, 0x06, 0x12, 0x02, 0x03, (byte) 0xAD};
    private static final byte[] getCoin10 = new byte[]{(byte) 0x90, 0x06, 0x12, 0x02, 0x03, (byte) 0xAE};
    private static final byte[] getCoin50 = new byte[]{(byte) 0x90, 0x06, 0x12, 0x02, 0x03, (byte) 0xAF};
    private static final byte[] paperPower = new byte[]{(byte) 0x80, (byte) 0x8F};
    private static final byte[] paperPowerReply = new byte[]{0x02};
    private static final byte[] enablePaperInput = new byte[]{0x3e};
    private static final byte[] disablePaperInput = new byte[]{0x5e};
    private static final byte[] getPaper = new byte[]{(byte) 0x81, 0x40};
    private static final byte[] getPaperConfirm = new byte[]{0x02};
    private static final byte[] getPaperReject = new byte[]{0x0F};

    private void handleFT2232H(D2xxManager manager) {
        int devCount = 0;
        devCount = manager.createDeviceInfoList(this);
        if (devCount > 0) {
            connectText.setBackgroundColor(Color.GREEN);
            connectText.setText("Connected");
            manager.setVIDPID(1027, 24592);
            coinInputDevice = manager.openByIndex(this, 0);
            setMachineConfig(coinInputDevice);
            paperInputDevice = manager.openByIndex(this, 1);
            setMachineConfig(paperInputDevice);
        }
    }

    private void handleFT4232H(D2xxManager manager) {
        int devCount = 0;
        devCount = manager.createDeviceInfoList(this);
        if (devCount > 0) {
            manager.setVIDPID(1027, 24593);
            coin5Device = manager.openByIndex(this, 0);
            setMachineConfig(coin5Device);
            coin10Device = manager.openByIndex(this, 1);
            setMachineConfig(coin10Device);
            coin50Device = manager.openByIndex(this, 2);
            setMachineConfig(coin50Device);
        }
    }


    private void setMachineConfig(FT_Device ftDev) {
        ftDev.setBaudRate(9600);
        ftDev.setDataCharacteristics(D2xxManager.FT_DATA_BITS_8, D2xxManager.FT_STOP_BITS_1, D2xxManager.FT_PARITY_EVEN);
    }

    private boolean setCoinInputEnable() {
        boolean read = true;
        byte[] data = new byte[5];
        coinInputDevice.write(enableCoinInput);
        while (read) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // readData = new byte[readLength];
            int len = coinInputDevice.getQueueStatus();

            if (len >= 5) {
                coinInputDevice.read(data, 5);
                read = false;
            }
        }
        return Arrays.equals(data, enableCoinInputSuccess);
    }

    private boolean setCoinInputDisable() {
        readCoinInput = false;
        coinInputDevice.write(disableCoinInput);
        boolean read = true;
        byte[] data = new byte[5];
        while (read) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // readData = new byte[readLength];
            int len = coinInputDevice.getQueueStatus();

            if (len >= 5) {
                coinInputDevice.read(data, 5);
                read = false;
            }
        }
        return Arrays.equals(data, enableCoinInputSuccess);
    }

    private void readCoinInput() {
        readCoinInput = true;
        new Thread(() -> {
            while (readCoinInput) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // readData = new byte[readLength];
                int len = coinInputDevice.getQueueStatus();
                byte[] data = new byte[5];
                if (len >= 5) {
                    coinInputDevice.read(data, 6);
                    if (Arrays.equals(data, getCoin5)) {
                        pay += 5;
                    } else if (Arrays.equals(data, getCoin10)) {
                        pay += 10;
                    } else if (Arrays.equals(data, getCoin50)) {
                        pay += 50;
                    }
                    runOnUiThread(() -> {
                        money.setText(String.valueOf(pay));
                    });
                }
            }
        }).start();
    }

    private void readPaperInput() {
        new Thread(() -> {
            while (readPaperInput) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                int len = paperInputDevice.getQueueStatus();
                byte[] data = new byte[1];
                if (len >= 1) {
                    paperInputDevice.read(data, 1);
                    if (Arrays.equals(data, new byte[]{(byte) 0x80})) {
                        len = 0;
                        while (len <= 0) {
                            len = paperInputDevice.getQueueStatus();
                        }
                        paperInputDevice.read(data, 1);
                        if (Arrays.equals(data, new byte[]{(byte) 0x8F})) {
                            paperInputDevice.write(paperPowerReply);
                        }
                    } else if (Arrays.equals(data, new byte[]{(byte) 0x81})) {
                        len = 0;
                        while (len <= 0) {
                            len = paperInputDevice.getQueueStatus();
                        }
                        paperInputDevice.read(data, 1);
                        if (Arrays.equals(data, new byte[]{(byte) 0x40})) {
                            paperInputDevice.write(getPaperConfirm);
                        } else {
                            paperInputDevice.write(getPaperReject);
                        }
                    }
                }
            }
        }).start();
    }

    private final BroadcastReceiver mCoinInputPlugEvents = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                try {
                    handleFT2232H(coinInputManager);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = getIntent();
        String action = intent.getAction();

        String hotplug = "android.intent.action.MAIN";
        if (hotplug.equals(action)) {
            try {
                handleFT2232H(coinInputManager);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}