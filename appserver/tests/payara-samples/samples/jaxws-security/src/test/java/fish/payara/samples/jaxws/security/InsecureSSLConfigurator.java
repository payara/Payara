package fish.payara.samples.jaxws.security;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class InsecureSSLConfigurator {

    private SSLSocketFactory previousSSLFactory;
    private HostnameVerifier previousHostnameVerifier;

    public void enableInsecureSSL() throws KeyManagementException, NoSuchAlgorithmException {
        trustAllCertificates();
        allowAllHosts();
    }

    public void revertSSLConfiguration() {
        switchBackToPreviousSSLFactory();
        switchBackToPreviousHostsVerifier();
    }

    private void trustAllCertificates() throws KeyManagementException, NoSuchAlgorithmException {
        SSLSocketFactory trustingFactory = this.createTrustingSocketFactory();
        previousSSLFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
        HttpsURLConnection.setDefaultSSLSocketFactory(trustingFactory);
    }

    private void switchBackToPreviousSSLFactory() {
        HttpsURLConnection.setDefaultSSLSocketFactory(previousSSLFactory);
    }

    private void allowAllHosts() {
        previousHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        HostnameVerifier allowAllHostNames = (String hostname, SSLSession session) -> true;
        HttpsURLConnection.setDefaultHostnameVerifier(allowAllHostNames);
    }

    private void switchBackToPreviousHostsVerifier() {
        HttpsURLConnection.setDefaultHostnameVerifier(previousHostnameVerifier);
    }

    private SSLSocketFactory createTrustingSocketFactory() throws KeyManagementException, NoSuchAlgorithmException {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new TrustingX509TrustManager()
        };
        SSLContext sc = SSLContext.getInstance("TLSv1.2");
        sc.init(null, trustAllCerts, new SecureRandom());
        return sc.getSocketFactory();
    }

    // A trust manager that does not validate certificate chains
    private static class TrustingX509TrustManager implements X509TrustManager {

        public TrustingX509TrustManager() {
        }


        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }


        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }


        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }
    }

}
