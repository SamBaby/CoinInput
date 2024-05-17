package coin_machine;

public interface CoinMachineInterface {
    boolean isMachineReady();

    boolean isPayStart();

    void payStart();

    void payEnd();

    public static String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        StringBuilder hex = new StringBuilder(byteArray.length * 2);
        for (byte aData : byteArray) {
            hex.append(String.format("%02X ", aData));
        }
        return hex.toString();
    }
}
