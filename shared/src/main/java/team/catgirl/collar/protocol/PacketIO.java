package team.catgirl.collar.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.catgirl.collar.io.ByteBufferInputStream;
import team.catgirl.collar.security.Identity;
import team.catgirl.collar.security.cipher.Cipher;
import team.catgirl.collar.io.IO;

import java.io.*;
import java.nio.ByteBuffer;

/**
 * Encodes and decodes packets for/from the wire, handling encryption and different types of signal messages
 * Packet format is int(0x22)+int(version)+int(ENCRYPTEDMODE)+int(SIGNAL_MESSAGE_TYPE)+CiphertextMessage()
 */
public final class PacketIO {
    /** UwU **/
    private static final int PACKET_MARKER = 0x22;
    private static final int VERSION = 1;
    private static final int MODE_PLAIN = 0xc001;
    private static final int MODE_ENCRYPTED = 0xba5ed;

    private final ObjectMapper mapper;
    private final Cipher cipher;

    public PacketIO(ObjectMapper mapper, Cipher cipher) {
        this.mapper = mapper;
        this.cipher = cipher;
    }

    public <T> T decode(Identity sender, InputStream is, Class<T> type) throws IOException {
        return decode(sender, IO.toByteBuffer(is), type);
    }

    public <T> T decode(Identity sender, ByteBuffer buffer, Class<T> type) throws IOException {
        T decoded;
        int packetType;
        try (DataInputStream objectStream = new DataInputStream(new ByteBufferInputStream(buffer))) {
            int packetMarker = objectStream.readInt();
            if (packetMarker != PACKET_MARKER) {
                throw new IllegalStateException("not a collar packet " + Integer.toHexString(packetMarker));
            }
            int version = objectStream.readInt();
            if (version != VERSION) {
                throw new IllegalStateException("unknown packet version " + version);
            }
            packetType = objectStream.readInt();
            byte[] remainingBytes = IO.toByteArray(objectStream);
            if (packetType == MODE_PLAIN) {
                checkPacketSize(remainingBytes);
                decoded = mapper.readValue(remainingBytes, type);
            } else if (packetType == MODE_ENCRYPTED) {
                if (sender == null) {
                    throw new IllegalStateException("Cannot read encrypted packets with no sender");
                }
                remainingBytes = cipher.decrypt(sender, remainingBytes);
                checkPacketSize(remainingBytes);
                decoded = mapper.readValue(remainingBytes, type);
            } else {
                throw new IllegalStateException("unknown packet type " + packetType);
            }
        }
        return decoded;
    }

    public byte[] encodePlain(Object object) throws IOException {
        byte[] rawBytes = mapper.writeValueAsBytes(object);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (DataOutputStream objectStream = new DataOutputStream(outputStream)) {
                objectStream.writeInt(PACKET_MARKER);
                objectStream.writeInt(VERSION);
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
                objectStream.writeInt(PACKET_MARKER);
                objectStream.writeInt(VERSION);
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

    private void checkPacketSize(byte[] bytes) {
        if (bytes.length > Short.MAX_VALUE) {
            throw new IllegalStateException("Packet is too large. Size " + bytes.length + " bytes");
        }
    }
}
