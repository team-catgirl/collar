package team.catgirl.collar.utils;

import java.io.*;
import java.nio.file.Files;

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

    /**
     * Write all bytes to file
     * @param file to write to
     * @param bytes to write
     */
    public static void writeBytesToFile(File file, byte[] bytes) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(bytes);
        } catch (IOException e) {
            throw new IllegalStateException("could not write bytes to file " + file);
        }
    }

    /**
     * Read all the bytes from a file
     * @param file to read from
     * @return bytes
     */
    public static byte[] readBytesFromFile(File file) {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("could not read all bytes from file " + file);
        }
    }

    public IO() {}
}
