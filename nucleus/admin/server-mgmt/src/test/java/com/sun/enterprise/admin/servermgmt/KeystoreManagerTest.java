package com.sun.enterprise.admin.servermgmt;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import org.junit.Before;
import org.junit.Test;

public class KeystoreManagerTest {

    private KeystoreManager manager;
    private File keyStoreFile;

    @Before
    public void configureKeystoreManager() throws Exception {
        manager = new KeystoreManager();
        keyStoreFile = File.createTempFile("cacerts-temp", ".jks.tmp");
        KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
        store.load(null);
        store.store(new FileOutputStream(keyStoreFile), "password".toCharArray());
        keyStoreFile.deleteOnExit();
    }

    @Test
    public void testCopyCertificatesFromJdk() throws Exception {
        manager.copyCertificatesFromJdk(keyStoreFile, "password");

        KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
        store.load(new FileInputStream(keyStoreFile), "password".toCharArray());

        // Loop through all certificates and check they're all valid
        Enumeration<String> aliases = store.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            X509Certificate cert = (X509Certificate) store.getCertificate(alias);
            cert.checkValidity();
        }

        // Check that some certificates were loaded
        assertTrue("No certificates were loaded from the JDK trust store", store.size() > 0);
    }

}