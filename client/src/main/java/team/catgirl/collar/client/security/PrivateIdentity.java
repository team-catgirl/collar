package team.catgirl.collar.client.security;

import team.catgirl.collar.client.HomeDirectory;
import team.catgirl.collar.security.TokenGenerator;
import team.catgirl.collar.utils.IO;
import team.catgirl.collar.utils.Utils;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Identity used for storing and reading personal encrypted data on the server
 */
public final class PrivateIdentity {
    private static final int VERSION = 1;
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";

    private final SecretKey secretKey;
    public final byte[] token;

    private PrivateIdentity(SecretKey secretKey, byte[] token) {
        this.secretKey = secretKey;
        this.token = token;
    }

    private PrivateIdentity(byte[] bytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            try (DataInputStream dataStream = new DataInputStream(inputStream)) {
                int version = dataStream.readInt();
                if (VERSION != version) {
                    throw new IllegalStateException("invalid version " + version);
                }
                byte[] keyBytes = IO.readBytes(dataStream);
                this.secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, ALGORITHM);
                this.token = IO.readBytes(dataStream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("could not read identity.cif");
        }
    }

    private byte[] serialize() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (DataOutputStream dataStream = new DataOutputStream(outputStream)) {
                dataStream.writeInt(VERSION);
                IO.writeBytes(dataStream, secretKey.getEncoded());
                IO.writeBytes(dataStream, token);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("could not serialize identity.cif");
        }
    }

    /**
     * Creates or generates a new private identity
     * @param directory home
     * @return private identity
     */
    public static PrivateIdentity getOrCreate(HomeDirectory directory) {
        File file;
        try {
            file = new File(directory.profile(), "identity.cif");
        } catch (IOException e) {
            throw new IllegalStateException("could not get the home directory for profiles", e);
        }
        if (file.exists()) {
            byte[] bytes = IO.readBytesFromFile(file);
            return new PrivateIdentity(bytes);
        } else {
            KeyGenerator generator;
            try {
                generator = KeyGenerator.getInstance(ALGORITHM);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("could not generate key", e);
            }
            generator.init(256, Utils.secureRandom());
            byte[] token = TokenGenerator.byteToken(128);
            PrivateIdentity privateIdentity = new PrivateIdentity(generator.generateKey(), token);
            byte[] serialized = privateIdentity.serialize();
            IO.writeBytesToFile(file, serialized);
            return privateIdentity;
        }
    }

    public byte[] encrypt(byte[] bytes) {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(TRANSFORMATION);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalStateException("could not create cipher", e);
        }
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("bad key", e);
        }
        try {
            return cipher.doFinal(bytes);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new IllegalStateException("failed to crypt", e);
        }
    }

    public byte[] decrypt(byte[] bytes) {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(TRANSFORMATION);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalStateException("could not create cipher", e);
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("bad key", e);
        }
        try {
            return cipher.doFinal(bytes);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new IllegalStateException("failed to decrypt", e);
        }
    }
}
