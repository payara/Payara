package fish.payara.samples.jaxws.security;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.*;

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
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        return sc.getSocketFactory();
    }

    // A trust manager that does not validate certificate chains
    private static class TrustingX509TrustManager implements X509TrustManager {

        public TrustingX509TrustManager() {
        }

        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {
        }
    }

}
