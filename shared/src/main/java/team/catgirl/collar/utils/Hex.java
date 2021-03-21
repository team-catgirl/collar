package team.catgirl.collar.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public final class Hex {
    /**
     * Convert bytes to hex string
     * @param bytes to convert
     * @return hex representation
     */
    @SuppressFBWarnings("IM_BAD_CHECK_FOR_ODD")
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
