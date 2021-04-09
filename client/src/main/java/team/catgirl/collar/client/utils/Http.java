package team.catgirl.collar.client.utils;

import io.netty.handler.ssl.SslContextBuilder;
import team.catgirl.collar.http.HttpClient;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public final class Http {

    private static final HttpClient collar;
    private static final HttpClient external;

    static {
        try {
            collar = new HttpClient(SslContextBuilder.forClient().trustManager(Certificates.load()).build());
        } catch (NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
        external = new HttpClient();
    }

    public static HttpClient collar() {
        return collar;
    }

    public static HttpClient external() {
        return external;
    }

    private Http() {}
}
