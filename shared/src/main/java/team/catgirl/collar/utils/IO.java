package team.catgirl.collar.utils;

import java.io.*;

public final class IO {
    /**
     * Write byte structure to {@link DataOutputStream}
     * @param os to write to
     * @param bytes to write
     * @throws IOException on error
     */
    public static void writeBytes(DataOutputStream os, byte[] bytes) throws IOException {
        os.write(bytes.length);
        for (byte b : bytes) {
            os.write(b);
        }
    }

    /**
     * Read byte structure from {@link DataInputStream}
     * @param is to read from
     * @return bytes
     * @throws IOException on error
     */
    public static byte[] readBytes(DataInputStream is) throws IOException {
        int length = is.read();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = is.readByte();
        }
        return bytes;
    }

    public IO() {}
}
