package coin_machine;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class CoinInputMachine extends SerialInputOutputManager implements CoinMachineInterface {
    private boolean machineReady = false;
    private boolean payStart = false;


    public CoinInputMachine(UsbSerialPort serialPort, Listener listener) {
        super(serialPort, listener);
    }

    public boolean isMachineReady() {
        return machineReady;
    }

    public void setMachineReady(boolean machineReady) {
        this.machineReady = machineReady;
    }

    public boolean isPayStart() {
        return payStart;
    }

    @Override
    public void payStart() {
        this.payStart = true;
    }

    @Override
    public void payEnd() {
        this.payStart = false;
    }

    public void setPayStart(boolean payStart) {
        this.payStart = payStart;
    }

}
