package net.timelegend.ayesha;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import android.content.Context;

public class HathitrustSSLSocketFactory extends AdditionalSSLSocketFactory {
    protected static HathitrustSSLSocketFactory hathitrustSSLSocketFactory = null;

    public static HathitrustSSLSocketFactory get(Context context) {
        if (hathitrustSSLSocketFactory == null) {
            try {
                hathitrustSSLSocketFactory = new HathitrustSSLSocketFactory(context);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return hathitrustSSLSocketFactory;
    }

    protected HathitrustSSLSocketFactory(Context context)
        throws IOException, NoSuchAlgorithmException, KeyStoreException, CertificateException, KeyManagementException {

        super(context);
    }

    @Override
    protected void setStore(Context context) {
        storeUri = context.getString(R.string.hathitrust_keystore);
        password = context.getString(R.string.hathitrust_keystore_password);
    }
}
