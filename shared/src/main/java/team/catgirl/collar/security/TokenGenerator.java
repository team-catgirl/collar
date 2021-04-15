package team.catgirl.collar.security;

import com.google.common.io.BaseEncoding;
import team.catgirl.collar.utils.Utils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public final class TokenGenerator {
    private static final SecureRandom RANDOM = Utils.secureRandom();

    public static byte[] byteToken(int size) {
        byte[] bytes = new byte[size];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    public static long longToken() {
        return RANDOM.nextLong();
    }

    /**
     * @return new url token
     */
    public static String urlToken() {
        return BaseEncoding.base64Url().encode(byteToken(16)).replace("==", "");
    }

    private TokenGenerator() {}
}
