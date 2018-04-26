package com.sun.enterprise.admin.servermgmt;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.Enumeration;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

public class KeystoreManagerTest {

    private KeystoreManager manager;
    private File keyStoreFile;

	char[] storePw = "changeit".toCharArray();

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

	@Test
	public void testLoadingJKS() throws Exception {
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(this.getClass().getResourceAsStream("sample-store.jks"), storePw);
		assertTrue(ks.aliases().hasMoreElements());
	}

	@Test
	public void testLoadingPKCS12() throws Exception {
		KeyStore ks = KeyStore.getInstance("PKCS12");
		ks.load(this.getClass().getResourceAsStream("sample-store.p12"), storePw);
		assertTrue(ks.aliases().hasMoreElements());
	}

	@Test
	public void testFallbackLoadingPKCS12() throws Exception {
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(this.getClass().getResourceAsStream("sample-store.p12"), storePw);
		assertTrue(ks.aliases().hasMoreElements());
	}

	@Test
	public void testReadPemPrivateKeyAsInputStream() throws Exception {
		PrivateKey readPemPrivateKey = manager.readPlainPKCS8PrivateKey(
				this.getClass().getResourceAsStream("privatekey.key"), "RSA");
		assertNotNull(readPemPrivateKey);
	}

	@Test
	public void testReadPemPrivateKeyAsFile() throws Exception {
		PrivateKey readPemPrivateKey = manager.readPlainPKCS8PrivateKey(
				new File(this.getClass().getResource("privatekey.key").getFile()));
		assertNotNull(readPemPrivateKey);
	}

	@Test
	public void testLoadingCertificates() throws KeyStoreException {
		File file = new File(this.getClass().getResource("certificate.pem").getFile());
		Collection<? extends Certificate> certChain = manager.readPemCertificateChain(file);
		assertNotNull(certChain);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRefuseShortPassword() {
		manager.enforcePasswordComplexity("short".toCharArray(), "invalidPassword");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRefuseNullPassword() {
		manager.enforcePasswordComplexity(null, "invalidPassword");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSaveKeyStoreBadPw() throws KeyStoreException {
		manager.saveKeyStore(null, null, "short".toCharArray());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddKeyPairBadStorePw() throws KeyStoreException {
		manager.addKeyPair(null, "JKS", null, null, null, "nullTest");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddKeyPairBadKeyPw() throws KeyStoreException {
		manager.addKeyPair(null, "JKS", "min6chars".toCharArray(), null, null, null, "nullTest");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testChangeKeyPasswordBadKeyPw() throws KeyStoreException {
		manager.changeKeyPassword(null, "JKS", "min6chars".toCharArray(), "nullTest", null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testChangeKeyStorePasswordBadStorePw() throws KeyStoreException {
		manager.changeKeyStorePassword(null, "JKS", "min6chars".toCharArray(), null, true);
	}

	@Test
	public void testWriteAndReadEmptyKeystore() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {

		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(null, null);
		File tempFile = File.createTempFile("temp-", ".tmp");
		assertTrue(tempFile.length() == 0);
		tempFile.deleteOnExit();
		manager.saveKeyStore(ks, tempFile, storePw);
		assertTrue(tempFile.canRead());
		assertTrue(tempFile.length() > 0);

		KeyStore ks2 = manager.openKeyStore(tempFile, "JKS", storePw);
		assertTrue(ks2.size() == 0);
		tempFile.delete();
	}

	@Test
	public void testAddKeyPair()
			throws CertificateException, IOException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
		String alias = "test2";

		File f = copySampleKeyStore(storePw);
		PrivateKey privKey = manager.readPlainPKCS8PrivateKey(
				this.getClass().getResourceAsStream("privatekey.key"), "RSA");
		Collection<? extends Certificate> certificates = CertificateFactory.getInstance("X.509")
				.generateCertificates(this.getClass().getResourceAsStream("certificate.pem"));
		manager.addKeyPair(f, "JKS", storePw, privKey, certificates.toArray(new Certificate[1]), alias);

		KeyStore ks = manager.openKeyStore(f, "JKS", storePw);
		assertTrue(ks.aliases().hasMoreElements());
		assertTrue(ks.containsAlias(alias));
		assertNotNull(ks.getKey(alias, storePw));
		f.delete();
	}

	@Test
	public void testAddKeyPairDifferentPw()
			throws CertificateException, IOException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
		String alias = "test-non-gf";
		char[] keypw = "nongfpw".toCharArray();

		File f = copySampleKeyStore(storePw);
		PrivateKey privKey = manager.readPlainPKCS8PrivateKey(
				this.getClass().getResourceAsStream("privatekey.key"), "RSA");
		Collection<? extends Certificate> certificates = CertificateFactory.getInstance("X.509")
				.generateCertificates(this.getClass().getResourceAsStream("certificate.pem"));
		manager.addKeyPair(f, "JKS", storePw, privKey, keypw, certificates.toArray(new Certificate[1]), alias);

		KeyStore ks = manager.openKeyStore(f, "JKS", storePw);
		assertTrue(ks.aliases().hasMoreElements());
		assertTrue(ks.containsAlias(alias));
		assertNotNull(ks.getKey(alias, keypw));
		f.delete();
	}

	@Test
	public void testChangeStorePwOnly()
			throws CertificateException, IOException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
		String alias = "test1";
		char[] newPw = "newStorePw".toCharArray();

		File f = copySampleKeyStore(storePw);
		manager.changeKeyStorePassword(f, "JKS", storePw, newPw, false);

		KeyStore ks = manager.openKeyStore(f, "JKS", newPw);
		assertTrue(ks.aliases().hasMoreElements());
		assertTrue(ks.containsAlias(alias));

		// note: using the old PW
		assertNotNull(ks.getKey(alias, storePw));
		f.delete();
	}

	@Test
	public void testChangeStoreAndKeyPw()
			throws CertificateException, IOException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
		String alias = "test1";
		char[] newPw = "newStorePw".toCharArray();
		File f = copySampleKeyStore(storePw);
		manager.changeKeyStorePassword(f, "JKS", storePw, newPw);

		KeyStore ks = manager.openKeyStore(f, "JKS", newPw);
		assertTrue(ks.aliases().hasMoreElements());
		assertTrue(ks.containsAlias(alias));

		// using the new PW
		assertNotNull(ks.getKey(alias, newPw));
		f.delete();
	}

	@Test
	public void testchangeKeyPasswordOnly()
			throws CertificateException, IOException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
		String alias = "test1";
		char[] newPw = "newStorePw".toCharArray();
		File f = copySampleKeyStore(storePw);
		manager.changeKeyPassword(f, "JKS", storePw, alias, storePw, newPw);

		// opening the store with the old PW
		KeyStore ks = manager.openKeyStore(f, "JKS", storePw);
		assertTrue(ks.aliases().hasMoreElements());
		assertTrue(ks.containsAlias(alias));

		// using the new PW to open the key
		assertNotNull(ks.getKey(alias, newPw));
		f.delete();
	}

	File copySampleKeyStore(char[] newStorePw)
			throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
		try (InputStream in = this.getClass().getResourceAsStream("sample-store.jks")) {
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(in, storePw);

			File tempFile = File.createTempFile("temp-", ".tmp");
			tempFile.deleteOnExit();
			manager.saveKeyStore(ks, tempFile, newStorePw);
			return tempFile;
		}
	}

}