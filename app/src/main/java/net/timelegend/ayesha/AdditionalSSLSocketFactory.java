package net.timelegend.ayesha;

import android.content.Context;

import java.io.InputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/*
 * reference:
 * https://stackoverflow.com/questions/2642777/trusting-all-certificates-using-httpclient-over-https
 */
public abstract class AdditionalSSLSocketFactory extends SSLSocketFactory {
    protected SSLSocketFactory internalSSLSocketFactory;;
    protected String storeUri;
    protected String password;

    protected AdditionalSSLSocketFactory(Context context) 
        throws IOException, NoSuchAlgorithmException, KeyStoreException, CertificateException, KeyManagementException {

        setStore(context);
        KeyStore keyStore = getKeyStore(context);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new AdditionalTrustManager(keyStore)}, null);
        internalSSLSocketFactory = sslContext.getSocketFactory();
        Log.i("AdditionalSSLSocketFactory ready");
    }

    protected abstract void setStore(Context context);

    @Override
    public String[] getDefaultCipherSuites() {
        return internalSSLSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return internalSSLSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        return internalSSLSocketFactory.createSocket();
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return internalSSLSocketFactory.createSocket(socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return internalSSLSocketFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
        return internalSSLSocketFactory.createSocket(host, port, localAddress, localPort);
    }

    @Override
    public Socket createSocket(InetAddress address, int port) throws IOException {
        return internalSSLSocketFactory.createSocket(address, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return internalSSLSocketFactory.createSocket(address, port, localAddress, localPort);
    }

    public KeyStore getKeyStore(Context context)
        throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {

        Log.i(storeUri);

        final KeyStore ks = KeyStore.getInstance("BKS");
        URL url = new URL(storeUri);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.connect();
        InputStream is = conn.getInputStream();

        try {
            ks.load(is, password.toCharArray());
        }
        finally {
            is.close();
        }

        return ks;
    }

    public static class AdditionalTrustManager implements X509TrustManager {
        protected ArrayList<X509TrustManager> x509TrustManagers = new ArrayList<X509TrustManager>();

        protected AdditionalTrustManager(KeyStore... additionalkeyStores)
            throws NoSuchAlgorithmException, KeyStoreException {

            final ArrayList<TrustManagerFactory> factories = new ArrayList<TrustManagerFactory>();

            final TrustManagerFactory original = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            original.init((KeyStore) null);
            factories.add(original);

            for(KeyStore keyStore : additionalkeyStores) {
                final TrustManagerFactory additionalCerts = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                additionalCerts.init(keyStore);
                factories.add(additionalCerts);
            }

            for (TrustManagerFactory tmf : factories)
                for(TrustManager tm : tmf.getTrustManagers())
                    if (tm instanceof X509TrustManager)
                        x509TrustManagers.add((X509TrustManager)tm);

            if(x509TrustManagers.size() == 0)
                throw new RuntimeException("Couldn't find any X509TrustManagers");
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            final X509TrustManager defaultX509TrustManager = x509TrustManagers.get(0);
            defaultX509TrustManager.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            for(X509TrustManager tm : x509TrustManagers) {
                try {
                    tm.checkServerTrusted(chain,authType);
                    return;
                } catch(CertificateException e) {
                }
            }

            throw new CertificateException();
        }

        public X509Certificate[] getAcceptedIssuers() {
            final ArrayList<X509Certificate> list = new ArrayList<X509Certificate>();

            for(X509TrustManager tm : x509TrustManagers)
                list.addAll(Arrays.asList(tm.getAcceptedIssuers()));

            return list.toArray(new X509Certificate[list.size()]);
        }
    }
}
