package team.catgirl.collar.io;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Cute IO utility
 */
public final class IO {
    /**
     * Write byte structure to {@link DataOutputStream}
     * @param os to write to
     * @param bytes to write
     * @throws IOException on error
     */
    public static void writeBytes(DataOutputStream os, byte[] bytes) throws IOException {
        os.writeInt(bytes.length);
        for (byte b : bytes) {
            os.writeByte(b);
        }
    }

    /**
     * Read byte structure from {@link DataInputStream}
     * @param is to read from
     * @return bytes
     * @throws IOException on error
     */
    public static byte[] readBytes(DataInputStream is) throws IOException {
        int length = is.readInt();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = is.readByte();
        }
        return bytes;
    }

    /**
     * Read byte structure from {@link DataInputStream}
     * @param is to read from
     * @throws IOException on error
     */
    public static void skipBytes(DataInputStream is) throws IOException {
        int size = is.readInt();
        if (is.skipBytes(size) != size) {
            throw new IOException("could not skip bytes because end of stream was reached");
        }
    }

    /**
     * Write UUID to stream
     * @param os to write to
     * @param uuid to write
     * @throws IOException on error
     */
    public static void writeUUID(DataOutputStream os, UUID uuid) throws IOException {
        os.writeLong(uuid.getMostSignificantBits());
        os.writeLong(uuid.getLeastSignificantBits());
    }

    /**
     * Read UUID from stream
     * @param is to read
     * @return uuid
     * @throws IOException on error
     */
    public static UUID readUUID(DataInputStream is) throws IOException {
        return new UUID(is.readLong(), is.readLong());
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

    /**
     * Copy stream to byte array
     * @param input to copy
     * @return contents
     * @throws IOException if stream failed to be read
     */
    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        for (int n = input.read(buf); n != -1; n = input.read(buf)) {
            os.write(buf, 0, n);
        }
        return os.toByteArray();
    }

    /**
     * Copy stream to byte array
     * @param input to copy
     * @return contents
     * @throws IOException if stream failed to be read
     */
    public static ByteBuffer toByteBuffer(InputStream input) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(Short.MAX_VALUE);
        byte[] buf = new byte[1024];
        for (int n = input.read(buf); n != -1; n = input.read(buf)) {
            buffer.put(buf, 0, n);
        }
        buffer.flip();
        return buffer;
    }

    public IO() {}
}
