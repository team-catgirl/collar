package team.catgirl.collar.security;

import com.google.common.io.BaseEncoding;

import java.security.SecureRandom;

public final class TokenGenerator {
    public static final SecureRandom secureRandom = new SecureRandom();

    /**
     * @return random byte token
     */
    public static byte[] byteToken() {
        byte[] bytes = new byte[128];
        secureRandom.nextBytes(bytes);
        return bytes;
    }
    /**
     * @return random string token
     */
    public static String stringToken() {
        return BaseEncoding.base64().encode(byteToken());
    }

    private TokenGenerator() {}
}
