/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.sun.enterprise.security.ssl;

import com.sun.enterprise.server.pluggable.SecuritySupport;
import com.sun.enterprise.universal.GFBase64Encoder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import sun.security.pkcs.ContentInfo;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X500Name;

/**
 * A utility class to sign jar files.
 *
 * @author Sudarsan Sridhar
 */
public class JarSigner {

    private static final GFBase64Encoder b64encoder = new GFBase64Encoder();
    private final MessageDigest md;
    private final String digestAlgorithm;
    private final String keyAlgorithm;
    private static final SecuritySupport secSupp = SecuritySupport.getDefaultInstance();

    public JarSigner(String digestAlgorithm, String keyAlgorithm)
            throws NoSuchAlgorithmException {
        this.digestAlgorithm = digestAlgorithm;
        this.keyAlgorithm = keyAlgorithm;
        this.md = MessageDigest.getInstance(digestAlgorithm);
    }

    public static void main(String[] args) throws Exception {
        File in = new File(args[0]);
        File out = new File(args[1]);
        new JarSigner("SHA1", "RSA").signJar(in, out, "s1as");
    }

    /**
     * Hash the string completely.
     *
     * @param content String to be hashed.
     * @return the hash.
     */
    private String hash(String content) {
        return hash(content.getBytes());
    }

    /**
     * Hash the data.
     * 
     * @param data
     * @return 
     */
    private String hash(final byte[] data) {
        return b64encoder.encodeBuffer(md.digest(data)).trim();
    }

    /**
     * Signs a jar.
     * 
     * @param input input jar file
     * @param output output jar file
     * @param alias signing alias in the keystore
     */
    public void signJar(File input, File output, String alias)
            throws IOException, KeyStoreException, NoSuchAlgorithmException,
            InvalidKeyException, UnrecoverableKeyException, SignatureException {
        
        signJar(input, output, alias, null);
    }
    
    /**
     * Signs a JAR, adding caller-specified attributes to the manifest's main attrs.
     * 
     * @param input input JAR file
     * @param output output JAR file
     * @param alias signing alias in the keystore
     * @param additionalAttrs additional attributes to add to the manifest's main attrs (null if none)
     */
    public void signJar(File input, File output, String alias, final Attributes additionalAttrs) 
            throws IOException, KeyStoreException, NoSuchAlgorithmException,
            InvalidKeyException, UnrecoverableKeyException, SignatureException {
        final ZipOutputStream zout = new ZipOutputStream(
            new FileOutputStream(output));
        try {
            signJar(input, zout, alias, additionalAttrs, Collections.EMPTY_MAP);
        } finally {
            zout.close();
        }
    }
    
    /**
     * Signs a JAR, adding caller-specified attributes to the manifest's main attrs and also
     * inserting (and signing) additional caller-supplied content as new entries in the
     * zip output stream.
     * @param input input JAR file
     * @param zout Zip output stream created
     * @param alias signing alias in the keystore
     * @param additionalAttrs additional attributes to add to the manifest's main attrs (null if none)
     * @param additionalEntries entry-name/byte[] pairs of additional content to add to the signed output
     * @throws IOException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws UnrecoverableKeyException
     * @throws SignatureException 
     */
    public void signJar(File input, ZipOutputStream zout, String alias, final Attributes additionalAttrs,
            Map<String,byte[]> additionalEntries)
            throws IOException, KeyStoreException, NoSuchAlgorithmException,
            InvalidKeyException, UnrecoverableKeyException, SignatureException {

        JarFile jf = new JarFile(input);
        try {
            Enumeration<JarEntry> jes;
            // manifestEntries is content of META-INF/MANIFEST.MF
            StringBuilder manifestEntries = new StringBuilder();

            byte[] manifestContent;
            byte[] sigFileContent = getExistingSignatureFile(jf);
            boolean signed = (sigFileContent != null);

            if (!signed || ! additionalEntries.isEmpty()) {
                jes = jf.entries();// manifestHeader is header of META-INF/MANIFEST.MF, initialized to default
                Manifest manifest = retrieveManifest(jf);
                StringBuilder manifestHeader = new StringBuilder();
                Attributes mfAttrs = manifest.getMainAttributes();
                if (additionalAttrs != null) {
                    mfAttrs.putAll(additionalAttrs);
                }
                appendAttributes(manifestHeader, mfAttrs);
                
                // sigFileEntries is content of META-INF/ME.SF
                StringBuilder sigFileEntries = new StringBuilder();
                while (jes.hasMoreElements()) {
                    JarEntry je = jes.nextElement();
                    String name = je.getName();
                    if ((je.isDirectory() && manifest.getAttributes(name) == null)
                            || name.equals(JarFile.MANIFEST_NAME)) {
                        continue;
                    }
                    processMetadataForEntry(manifest, manifestEntries, sigFileEntries, name, readJarEntry(jf, je));
                }

                if (additionalEntries != null) {
                    for (Map.Entry<String,byte[]> entry : additionalEntries.entrySet()) {
                        processMetadataForEntry(manifest, manifestEntries, sigFileEntries, entry.getKey(), entry.getValue());
                    }
                }
                
                // META-INF/ME.SF
                StringBuilder sigFile = new StringBuilder("Signature-Version: 1.0\r\n").append(digestAlgorithm).append("-Digest-Manifest-Main-Attributes: ").append(hash(manifestHeader.toString())).append("\r\n").append("Created-By: ").append(System.getProperty("java.version")).append(" (").append(System.getProperty("java.vendor")).append(")\r\n");
                // Combine header and content of MANIFEST.MF, and rehash
                manifestHeader.append(manifestEntries);
                sigFile.append(digestAlgorithm).append("-Digest-Manifest: ").append(hash(manifestHeader.toString())).append("\r\n\r\n");

                // Combine header and content of ME.SF
                sigFile.append(sigFileEntries);
                manifestContent = manifestHeader.toString().getBytes();
                sigFileContent = sigFile.toString().getBytes();
            } else {
                manifestContent = readJarEntry(jf,
                        jf.getJarEntry(JarFile.MANIFEST_NAME));
            }
            X509Certificate[] certChain = null;
            PrivateKey privKey = null;
            KeyStore[] ks = secSupp.getKeyStores();
            for (int i = 0; i < ks.length; i++) {
                privKey = secSupp.getPrivateKeyForAlias(alias, i);
                if (privKey != null) {
                    Certificate[] cs = ks[i].getCertificateChain(alias);
                    certChain = new X509Certificate[cs.length];
                    for (int j = 0; j < cs.length; j++) {
                        certChain[j] = (X509Certificate) cs[j];
                    }
                }
            }

            // Sign ME.SF
            Signature sig = Signature.getInstance(digestAlgorithm + "with" + keyAlgorithm);
            sig.initSign(privKey);
            sig.update(sigFileContent);

            // Create PKCS7 block
            PKCS7 pkcs7 = new PKCS7(
                    new AlgorithmId[]{AlgorithmId.get(digestAlgorithm)},
                    new ContentInfo(sigFileContent),
                    certChain,
                    new SignerInfo[]{new SignerInfo(
                        (X500Name) certChain[0].getIssuerDN(),
                        certChain[0].getSerialNumber(),
                        AlgorithmId.get(digestAlgorithm),
                        AlgorithmId.get(keyAlgorithm),
                        sig.sign())
                    });
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            pkcs7.encodeSignedData(bout);

            // Write output
            
            zout.putNextEntry((signed)
                    ? getZipEntry(jf.getJarEntry(JarFile.MANIFEST_NAME))
                    : new ZipEntry(JarFile.MANIFEST_NAME));
            zout.write(manifestContent);

            zout.putNextEntry(new ZipEntry("META-INF/"
                    + alias.toUpperCase(Locale.US) + ".SF"));
            zout.write(sigFileContent);

            zout.putNextEntry(new ZipEntry("META-INF/"
                    + alias.toUpperCase(Locale.US) + "." + keyAlgorithm));
            zout.write(bout.toByteArray());

            jes = jf.entries();
            while (jes.hasMoreElements()) {
                JarEntry je = jes.nextElement();
                String name = je.getName();
                if (!name.equals(JarFile.MANIFEST_NAME)) {
                    zout.putNextEntry(getZipEntry(je));
                    byte[] data = readJarEntry(jf, je);
                    zout.write(data);
                }
            }
            if (additionalEntries != null) {
                for (Map.Entry<String, byte[]> entry : additionalEntries.entrySet()) {
                    final ZipEntry newZipEntry = new ZipEntry(entry.getKey());
                    zout.putNextEntry(newZipEntry);
                    zout.write(entry.getValue());
                }
            }

        } finally {
            jf.close();
        }
    }

    private void processMetadataForEntry(final Manifest manifest, 
            final StringBuilder manifestEntries,
            final StringBuilder sigFileEntries,
            final String name, 
            final byte[] content) {
        StringBuilder me = new StringBuilder();
        StringBuilder currentLine = new StringBuilder();
        // Create digest lines in MANIFEST.MF
        currentLine.append("Name: ").append(name);
        appendLine(me, currentLine);
        currentLine.setLength(0);
        me.append(digestAlgorithm).append("-Digest: ").append(hash(content)).append("\r\n");
        appendAttributes(me, manifest, name);
        // Create digest lines in ME.SF
        currentLine.append("Name: ").append(name);
        appendLine(sigFileEntries, currentLine);
        currentLine.setLength(0);
        sigFileEntries.append(digestAlgorithm).append("-Digest: ").append(hash(me.toString())).append("\r\n\r\n");
        manifestEntries.append(me);
    }
    
    /**
     * Retrieve manifest from jar, create a default template if none exists.
     *
     * @param jf The jar file
     * @return The Manifest
     * @throws IOException
     */
    private Manifest retrieveManifest(JarFile jf) throws IOException {
        Manifest manifest = jf.getManifest();
        if (manifest == null) {
            manifest = new Manifest();
            Attributes mainAttributes = manifest.getMainAttributes();
            mainAttributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
            mainAttributes.putValue("Created-By", System.getProperty("java.version")
                    + " (" + System.getProperty("java.vendor") + ")");
        }
        Map<String, Attributes> entriesMap = manifest.getEntries();
        for (Iterator<String> entries = entriesMap.keySet().iterator(); entries.hasNext();) {
            if (jf.getJarEntry(entries.next()) == null) {
                entries.remove();
            }
        }
        return manifest;
    }

    /**
     * Add attributes for the current entry.
     *
     * @param manifestEntry - The StringBuilder to which the attributes are to be added.
     * @param manifest - The Jar Manifest Entry
     * @param entry - The named entry in the manifest. null means the Main Attribute section.
     * @return manifestEntry with attributes added.
     */
    private static StringBuilder appendAttributes(StringBuilder manifestEntry,
            Manifest manifest, String entry) {
        return appendAttributes(manifestEntry, (entry == null)
                ? manifest.getMainAttributes() : manifest.getAttributes(entry));
    }
    
    private static StringBuilder appendAttributes(StringBuilder manifestEntry,
            Attributes attributes) {
        StringBuilder line = new StringBuilder();
        if (attributes != null) {
            for (Map.Entry attr : attributes.entrySet()) {
                line.append(attr.getKey().toString()).append(": ").append((String) attr.getValue());
                appendLine(manifestEntry, line);
                line.setLength(0);
            }
        }
        return manifestEntry.append("\r\n");
    }

    /**
     * Process a long manifest line and add continuation if required
     *
     * @param sb - The output string
     * @param line - The line to be processed.
     * @return sb with the line added.
     */
    private static StringBuilder appendLine(StringBuilder sb, StringBuilder line) {
        int begin = 0;
        for (int end = 70; line.length() - begin > 70; end += 69) {
            sb.append(line.subSequence(begin, end)).append("\r\n ");
            begin = end;
        }
        return sb.append(line.subSequence(begin, line.length())).append("\r\n");
    }

    /**
     * If jar is signed, return existing Signature file, else return null.
     *
     * @param jf The jar file
     * @return Signature file
     * @throws IOException
     */
    private static byte[] getExistingSignatureFile(JarFile jf) throws IOException {
        Enumeration<JarEntry> entries = jf.entries();
        JarEntry je = null;
        while (entries.hasMoreElements()) {
            JarEntry cje = entries.nextElement();
            if (cje.getName().startsWith("META-INF/") && cje.getName().endsWith(".SF")) {
                je = cje;
                break;
            }
        }
        return readJarEntry(jf, je);
    }

    /**
     * Read completely the bytes from Entry je of jarfile jf.
     * 
     * @param jf the jar file
     * @param je the jar entry
     * @return bytes from je.
     * @throws IOException
     */
    private static byte[] readJarEntry(JarFile jf, JarEntry je) throws IOException {
        if (je == null) {
            return null;
        }
        byte[] data = new byte[(int) je.getSize()];
        InputStream jis = jf.getInputStream(je);
        int current;
        int idx = 0;
        while ((current = jis.read()) > -1) {
            data[idx++] = (byte) current;
        }
        return data;
    }

    /**
     * Get the ZipEntry for the given JarEntry. Added in order to suppress the
     * compressedSize field as it was causing errors
     *
     * @param je The jar entry.
     * @return ZipEntry with fields populated from the JarEntry.
     */
    private static ZipEntry getZipEntry(JarEntry je) {
        ZipEntry ze = new ZipEntry(je.getName());

        ze.setComment(je.getComment());
        ze.setCrc(je.getCrc());
        ze.setExtra(je.getExtra());
        ze.setMethod(je.getMethod());
        ze.setSize(je.getSize());
        ze.setTime(je.getTime());

        return ze;
    }
}
