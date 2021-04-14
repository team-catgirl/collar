package team.catgirl.collar.client.utils;

import com.google.common.io.Resources;
import io.netty.handler.ssl.SslContextBuilder;
import team.catgirl.collar.http.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

public final class Http {

    private static final HttpClient client;

    static {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(Resources.getResource("cacerts").openStream(), "changeit".toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, "changeit".toCharArray());
            client = new HttpClient(SslContextBuilder.forClient().keyManager(kmf).build());
        } catch (NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpClient client() {
        return client;
    }

    private Http() {}
}
