package team.catgirl.collar.client.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

class Certificates {

    public static X509Certificate[] load() throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, KeyManagementException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);

        X509Certificate dev = load("dev.api.collarmc.com.cer");
        ks.setCertificateEntry("dev.api.collarmc.com", dev);

        X509Certificate prod = load("api.collarmc.com.cer");
        ks.setCertificateEntry("api.collarmc.com", prod);

        return new X509Certificate[] { dev, prod };
    }

    private static X509Certificate load(String name) throws CertificateException, IOException {
        try (InputStream inputStream = Resources.getResource("certs/" + name).openStream()) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new BufferedInputStream(inputStream));
        }
    }

    public Certificates() {}
}
