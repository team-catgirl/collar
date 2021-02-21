package team.catgirl.collar.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.catgirl.collar.security.cipher.Cipher;
import team.catgirl.collar.security.Identity;

import java.io.*;

/**
 * Encodes and decodes packets for/from the wire, handling encryption and different types of signal messages
 * Packet format is int(ENCRYPTEDMODE)+int(SIGNAL_MESSAGE_TYPE)+CiphertextMessage()
 * TODO: stabilize packet format before 1.0
 * TODO: limit the size of packets so people don't do scuffed things
 */
public final class PacketIO {

    private static final int MODE_PLAIN = 0xc001;
    private static final int MODE_ENCRYPTED = 0xba5ed;

    private final ObjectMapper mapper;
    private final Cipher cipher;

    public PacketIO(ObjectMapper mapper, Cipher cipher) {
        this.mapper = mapper;
        this.cipher = cipher;
    }

    public <T> T decode(Identity sender, InputStream is, Class<T> type) throws IOException {
        return decode(sender, toByteArray(is), type);
    }

    public <T> T decode(Identity sender, byte[] bytes, Class<T> type) throws IOException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
            int packetType;
            try (DataInputStream objectStream = new DataInputStream(is)) {
                packetType = objectStream.readInt();
                byte[] remainingBytes = toByteArray(objectStream);
                if (packetType == MODE_PLAIN) {
                    checkPacketSize(remainingBytes);
                    return mapper.readValue(remainingBytes, type);
                } else if (packetType == MODE_ENCRYPTED) {
                    if (sender == null) {
                        throw new IllegalStateException("Cannot read encrypted packets with no sender");
                    }
                    remainingBytes = cipher.decrypt(sender, remainingBytes);
                    checkPacketSize(remainingBytes);
                    return mapper.readValue(remainingBytes, type);
                } else {
                    throw new IllegalStateException("unknown packet type " + packetType);
                }
            }
        }
    }

    public byte[] encodePlain(Object object) throws IOException {
        byte[] rawBytes = mapper.writeValueAsBytes(object);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (DataOutputStream objectStream = new DataOutputStream(outputStream)) {
                objectStream.writeInt(MODE_PLAIN);
                objectStream.write(rawBytes);
            }
            byte[] bytes = outputStream.toByteArray();
            checkPacketSize(bytes);
            return bytes;
        }
    }

    public byte[] encodeEncrypted(Identity recipient, Object object) throws IOException {
        byte[] rawBytes = mapper.writeValueAsBytes(object);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (DataOutputStream objectStream = new DataOutputStream(outputStream)) {
                objectStream.writeInt(MODE_ENCRYPTED);
                if (recipient == null) {
                    throw new IllegalArgumentException("recipient cannot be null when sending MODE_ENCRYPTED packets");
                }
                objectStream.write(cipher.crypt(recipient, rawBytes));
            }
            byte[] bytes = outputStream.toByteArray();
            checkPacketSize(bytes);
            return bytes;
        }
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        for (int n = input.read(buf); n != -1; n = input.read(buf)) {
            os.write(buf, 0, n);
        }
        return os.toByteArray();
    }

    private void checkPacketSize(byte[] bytes) {
        if (bytes.length > Short.MAX_VALUE) {
            throw new IllegalStateException("Packet is too large. Size " + bytes.length + " bytes");
        }
    }
}
