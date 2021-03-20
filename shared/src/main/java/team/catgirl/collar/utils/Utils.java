package team.catgirl.collar.utils;

import com.fasterxml.jackson.databind.*;
import okhttp3.OkHttpClient;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import javax.net.ssl.*;
import java.security.*;
import java.util.Arrays;

public final class Utils {

    private static final ObjectMapper JSON_MAPPER;
    private static final ObjectMapper MESSAGE_PACK_MAPPER;

    static {
        JSON_MAPPER = new ObjectMapper()
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        MESSAGE_PACK_MAPPER = new ObjectMapper(new MessagePackFactory())
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    public static final SecureRandom SECURERANDOM;

    static {
        try {
            SECURERANDOM = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ObjectMapper jsonMapper() {
        return JSON_MAPPER;
    }

    public static ObjectMapper messagePackMapper() {
        return MESSAGE_PACK_MAPPER;
    }

    public static SecureRandom secureRandom() {
        return SECURERANDOM;
    }

    private Utils() {}
}
