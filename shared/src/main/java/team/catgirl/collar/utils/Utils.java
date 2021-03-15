package team.catgirl.collar.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import okhttp3.OkHttpClient;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.StringTokenizer;
import java.util.UUID;

public final class Utils {

    private static final OkHttpClient http;
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

        SSLContext sslContext;
        try {
            sslContext = Certificates.load();
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        http = new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory)
                .build();
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

    public static OkHttpClient http() { return http; }

    private Utils() {}
}
