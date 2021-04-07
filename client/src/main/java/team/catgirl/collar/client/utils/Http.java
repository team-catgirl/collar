package team.catgirl.collar.client.utils;

import okhttp3.OkHttpClient;
import team.catgirl.collar.http.HttpClient;
import team.catgirl.collar.utils.Utils;

import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public final class Http {

    private static final HttpClient http;
    private static final HttpClient external;

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
        http = new HttpClient(Utils.jsonMapper(), null);
        external = new HttpClient(Utils.jsonMapper(), null);
    }

    public static HttpClient collar() {
        return http;
    }

    public static HttpClient external() {
        return external;
    }

    private Http() {}
}
