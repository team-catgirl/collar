package team.catgirl.collar.utils;

public final class Hex {
    /**
     * Convert bytes to hex string
     * @param bytes to convert
     * @return hex representation
     */
    public static String hexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            int decimal = (int) aByte & 0xff;
            // get last 8 bits
            String hex = Integer.toHexString(decimal);
            if (hex.length() % 2 == 1) {
                hex = "0" + hex;
            }
            result.append(hex);
        }
        return result.toString();
    }

    private Hex() {}
}
