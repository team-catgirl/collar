package team.catgirl.collar.security;

import com.google.common.io.BaseEncoding;

import java.security.SecureRandom;
import java.util.Arrays;

public final class TokenGenerator {
    public static final SecureRandom secureRandom = new SecureRandom();

    public static String verificationCode() {
        int[] code = new int[6];
        for (int i = 0; i < code.length; i++) {
            code[i] = secureRandom.nextInt(10);
        }
        return Arrays.toString(code);
    }

    public static byte[] byteToken(int size) {
        byte[] bytes = new byte[size];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    public static byte[] byteToken() {
        return byteToken(128);
    }

    public static String stringToken() {
        return BaseEncoding.base64Url().encode(byteToken());
    }

    public static String urlToken() {
        return BaseEncoding.base64Url().encode(byteToken(16)).replace("==", "");
    }

    private TokenGenerator() {}
}
