package team.catgirl.collar.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class Utils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    public static final SecureRandom SECURERANDOM;

    static {
        try {
            SECURERANDOM = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ObjectMapper createObjectMapper() {
        return MAPPER;
    }

    public static SecureRandom createSecureRandom() {
        return SECURERANDOM;
    }

    private Utils() {}
}
