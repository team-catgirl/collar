package team.catgirl.collar.security;

import com.google.common.io.BaseEncoding;
import team.catgirl.collar.utils.Utils;

import java.security.SecureRandom;

public final class TokenGenerator {
    private static final SecureRandom RANDOM = Utils.createSecureRandom();

    public static byte[] byteToken(int size) {
        byte[] bytes = new byte[size];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    public static byte[] byteToken() {
        return byteToken(128);
    }

    public static String stringToken() {
        return BaseEncoding.base64Url().encode(byteToken());
    }

    /**
     * @return new url token
     */
    public static String urlToken() {
        return BaseEncoding.base64Url().encode(byteToken(16)).replace("==", "");
    }

    private TokenGenerator() {}
}
