package team.catgirl.collar.client.utils;

import okhttp3.OkHttpClient;

import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public final class Http {

    private static final OkHttpClient http;
    private static final OkHttpClient external = new OkHttpClient();

    static {
        SSLContext sslContext;
        try {
            sslContext = Certificates.load();
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        TrustManagerFactory tmf;
        try {
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new IllegalStateException("could not load TrustManagerFactory", e);
        }
        TrustManager trustManager = Arrays.stream(tmf.getTrustManagers())
                .filter(candidate -> candidate instanceof X509TrustManager)
                .findFirst().orElseThrow(() -> new IllegalStateException("could not find X509TrustManager"));
        http = new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustManager)
                .build();
    }

    public static OkHttpClient collar() {
        return http;
    }

    public static OkHttpClient external() {
        return external;
    }

    private Http() {}
}
