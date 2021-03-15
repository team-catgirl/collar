package team.catgirl.collar.utils;

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

class Certificates {

    public static SSLContext load() throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, KeyManagementException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);

        X509Certificate dev = load("dev.api.collarmc.com.cer");
        ks.setCertificateEntry(Integer.toString(1), dev);

        X509Certificate prod = load("api.collarmc.com.cer");
        ks.setCertificateEntry(Integer.toString(2), prod);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tmf.getTrustManagers(), null);
        return context;
    }

    private static X509Certificate load(String name) throws CertificateException, IOException {
        try (InputStream inputStream = Resources.getResource("certs/" + name).openStream()) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new BufferedInputStream(inputStream));
        }
    }

    public Certificates() {}
}
