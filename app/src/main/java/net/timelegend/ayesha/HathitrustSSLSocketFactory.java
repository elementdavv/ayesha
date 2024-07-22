package net.timelegend.ayesha;

import android.content.Context;

import java.io.InputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/*
 * reference:
 * https://stackoverflow.com/questions/64844311/certpathvalidatorexception-connecting-to-a-lets-encrypt-host-on-android-m-or-ea
 */
public class HathitrustSSLSocketFactory {
    protected static SSLSocketFactory hathitrustSSLSocketFactory = null;

    public static SSLSocketFactory get(Context context) {
        if (hathitrustSSLSocketFactory == null) {
            try {
                KeyStore keyStore = getKeyStore(context);
                String algorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
                tmf.init(keyStore);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
                hathitrustSSLSocketFactory = sslContext.getSocketFactory();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return hathitrustSSLSocketFactory;
    }

    public static KeyStore getKeyStore(Context context)
        throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        InputStream is1 = context.getResources().openRawResource(R.raw.isrg_root_x1);
        Certificate cert1 = cf.generateCertificate(is1);

        InputStream is2 = context.getResources().openRawResource(R.raw.isrg_root_x2);
        Certificate cert2 = cf.generateCertificate(is2);

        String type = KeyStore.getDefaultType();
        KeyStore ks = KeyStore.getInstance(type);
        ks.load(null, null);
        ks.setCertificateEntry("isrg_root_x1", cert1);
        ks.setCertificateEntry("isrg_root_x2", cert2);

        return ks;
    }
}
