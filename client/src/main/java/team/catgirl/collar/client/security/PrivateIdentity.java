package team.catgirl.collar.client.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.client.HomeDirectory;
import team.catgirl.collar.security.TokenGenerator;
import team.catgirl.collar.utils.Utils;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Identity used for storing and reading personal encrypted data on the server
 */
public final class PrivateIdentity {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";

    private final SecretKey secretKey;
    public final byte[] token;

    private PrivateIdentity(SecretKey secretKey, byte[] token) {
        this.secretKey = secretKey;
        this.token = token;
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
            State state;
            try {
                state = Utils.messagePackMapper().readValue(file, State.class);
            } catch (IOException e) {
                throw new IllegalStateException("could not read identity.cif", e);
            }
            SecretKey key = new SecretKeySpec(state.secretKey, 0, state.secretKey.length, ALGORITHM);
            return new PrivateIdentity(key, state.token);
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
            State state = new State(privateIdentity.secretKey.getEncoded(), privateIdentity.encrypt(token));
            try {
                Utils.messagePackMapper().writeValue(file, state);
            } catch (IOException e) {
                throw new IllegalStateException("could not write identity.cif", e);
            }
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

    private static class State {
        @JsonProperty("secretKey")
        public final byte[] secretKey;
        @JsonProperty("token")
        public final byte[] token;

        public State(@JsonProperty("secretKey") byte[] secretKey,
                     @JsonProperty("token") byte[] token) {
            this.secretKey = secretKey;
            this.token = token;
        }
    }
}
