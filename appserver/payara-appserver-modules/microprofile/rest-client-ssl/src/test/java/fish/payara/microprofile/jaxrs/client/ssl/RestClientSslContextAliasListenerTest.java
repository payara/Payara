package fish.payara.microprofile.jaxrs.client.ssl;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.Configuration;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;

import static fish.payara.microprofile.jaxrs.client.ssl.PayaraConstants.PAYARA_MP_CONFIG_CLIENT_CERTIFICATE_ALIAS;
import static fish.payara.microprofile.jaxrs.client.ssl.PayaraConstants.PAYARA_REST_CLIENT_CERTIFICATE_ALIAS;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RestClientSslContextAliasListenerTest {

    @Mock
    private RestClientBuilder restClientBuilder;

    @Mock
    private Configuration configuration;

    @Mock
    private Config config;
    
    @InjectMocks
    @Spy
    private RestClientSslContextAliasListener restClientSslContextAliasListener =
            new RestClientSslContextAliasListener();

    @Test
    public void restClientAliasPropertySslContextTest() throws Exception {
        KeyManager[] managers = getManagers();
        KeyStore[] keyStores = new KeyStore[]{getKeyStores()};

        when(restClientBuilder.getConfiguration()).thenReturn(configuration);
        when(configuration.getProperty(PAYARA_REST_CLIENT_CERTIFICATE_ALIAS)).thenReturn("myKey");
        doReturn(managers).when(restClientSslContextAliasListener).getKeyManagers();
        doReturn(keyStores).when(restClientSslContextAliasListener).getKeyStores();

        restClientSslContextAliasListener.onNewClient(RestClientBuilder.class, restClientBuilder);

        verify(restClientSslContextAliasListener, times(1)).buildSSlContext("myKey");
        verify(restClientSslContextAliasListener, times(1)).getKeyManagers();
        verify(restClientSslContextAliasListener, times(1)).getKeyStores();
        verify(restClientBuilder, times(1)).sslContext(any(SSLContext.class));
    }

    @Test
    public void restClientAliasPropertyFromMPConfigSslContextTest() throws Exception {
        KeyManager[] managers = getManagers();
        KeyStore[] keyStores = new KeyStore[]{getKeyStores()};

        when(restClientBuilder.getConfiguration()).thenReturn(configuration);
        when(configuration.getProperty(PAYARA_REST_CLIENT_CERTIFICATE_ALIAS)).thenReturn(null);
        doReturn(managers).when(restClientSslContextAliasListener).getKeyManagers();
        doReturn(keyStores).when(restClientSslContextAliasListener).getKeyStores();
        doReturn(config).when(restClientSslContextAliasListener).getConfig();
        when(config.getValue(PAYARA_MP_CONFIG_CLIENT_CERTIFICATE_ALIAS, String.class)).thenReturn("myKey");

        restClientSslContextAliasListener.onNewClient(RestClientBuilder.class, restClientBuilder);

        verify(restClientSslContextAliasListener, times(1)).buildSSlContext("myKey");
        verify(restClientSslContextAliasListener, times(1)).getKeyManagers();
        verify(restClientSslContextAliasListener, times(1)).getKeyStores();
        verify(restClientBuilder, times(1)).sslContext(any(SSLContext.class));
    }

    public KeyStore getKeyStores() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, URISyntaxException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        URL resource = getClass().getClassLoader().getResource("keystore.jks");
        FileInputStream keyStoreFile = new FileInputStream(new File(resource.toURI()));
        keyStore.load(keyStoreFile, "changeit".toCharArray());
        return keyStore;
    }

    public KeyManager[] getManagers() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, URISyntaxException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        URL resource = getClass().getClassLoader().getResource("keystore.jks");
        FileInputStream keyStoreFile = new FileInputStream(new File(resource.toURI()));
        keyStore.load(keyStoreFile, "changeit".toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "changeit".toCharArray());
        return kmf.getKeyManagers();
    }

}
