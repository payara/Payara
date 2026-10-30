/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.nucleus.hazelcast.encryption;

import com.hazelcast.core.HazelcastException;
import com.hazelcast.internal.nio.IOUtil;
import com.sun.enterprise.security.ssl.impl.MasterPasswordImpl;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.internal.api.Globals;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that encodes and decodes symmetric keys.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 * @author Rudy De Busscher <rudy.de.busscher@payara.fish>
 */
public class HazelcastSymmetricEncryptor {

    private static final String DATAGRID_KEY_FILE = "datagrid-key";
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String PBKDF_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final String AES = "AES";
    private static final int ITERATION_COUNT = 65556;
    private static final int KEYSIZE = 256;
    private static Random random = new SecureRandom();
    private static SecretKey secretKey;

    static {
        try {
            secretKey = readAndDecryptSecretKey();
            Logger.getLogger(HazelcastSymmetricEncryptor.class.getName()).log(Level.FINE,
                    "Data grid encryption key successfully read and decrypted");
        } catch (Exception exception) {
            // Shutting down Payara from the thread we're running in can only be done in fairly brutal ways
            Logger.getLogger(HazelcastSymmetricEncryptor.class.getName()).log(Level.SEVERE,
                    "Error starting Hazelcast due to exception reading encryption key", exception);
            Globals.get(HazelcastCore.class).getInstance().shutdown();
        }
    }

    public static byte[] encode(byte[] value) {
        // Generate IV.
        byte[] saltBytes = new byte[20];
        random.nextBytes(saltBytes);

        try {
            // Encrypting the data
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            AlgorithmParameters params = cipher.getParameters();
            byte[] ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();
            byte[] encryptedTextBytes = cipher.doFinal(value);

            // Prepend salt and IV
            byte[] buffer = new byte[saltBytes.length + ivBytes.length + encryptedTextBytes.length];
            System.arraycopy(saltBytes, 0, buffer, 0, saltBytes.length);
            System.arraycopy(ivBytes, 0, buffer, saltBytes.length, ivBytes.length);
            System.arraycopy(encryptedTextBytes, 0, buffer, saltBytes.length + ivBytes.length,
                    encryptedTextBytes.length);
            return buffer;
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException
                | InvalidParameterSpecException | BadPaddingException exception) {
            throw new HazelcastException(exception);
        }
    }

    public static byte[] decode(byte[] encryptedTextBytes) {
        byte[] decryptedTextBytes;
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            // Strip off the salt and IV
            ByteBuffer buffer = ByteBuffer.wrap(encryptedTextBytes);
            byte[] saltBytes = new byte[20];
            buffer.get(saltBytes, 0, saltBytes.length);
            byte[] ivBytes = new byte[cipher.getBlockSize()];
            buffer.get(ivBytes, 0, ivBytes.length);
            encryptedTextBytes = new byte[buffer.capacity() - saltBytes.length - ivBytes.length];

            buffer.get(encryptedTextBytes);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));
            decryptedTextBytes = cipher.doFinal(encryptedTextBytes);
        } catch (BadPaddingException exception) {
            // BadPaddingException -> Wrong key
            throw new HazelcastException("BadPaddingException caught decoding data, " +
                    "this can be caused by an incorrect or changed key: ", exception);
        } catch (IllegalBlockSizeException | NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | InvalidKeyException | NoSuchPaddingException exception) {
            throw new HazelcastException(exception);
        }

        return decryptedTextBytes;
    }

    private static SecretKey readAndDecryptSecretKey() {
        ServerEnvironment serverEnvironment = Globals.getDefaultBaseServiceLocator().getService(ServerEnvironment.class);
        char[] masterPassword = Globals.getDefaultBaseServiceLocator().getService(MasterPasswordImpl.class).getMasterPassword();

        byte[] encryptedBytes = null;
        try {
            encryptedBytes = Files.readAllBytes(
                    new File(serverEnvironment.getConfigDirPath() + File.separator + DATAGRID_KEY_FILE).toPath());
        } catch (IOException ioe) {
            Logger.getLogger(HazelcastSymmetricEncryptor.class.getName()).log(Level.SEVERE,
                    "Error reading datagrid key, please check if it's accessible at expected location: "
                            + serverEnvironment.getConfigDirPath() + File.separator + DATAGRID_KEY_FILE);
            throw new HazelcastException("Error reading encrypted key", ioe);
        }

        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);

            // Strip off the salt and IV
            ByteBuffer buffer = ByteBuffer.wrap(encryptedBytes);
            byte[] saltBytes = new byte[20];
            buffer.get(saltBytes, 0, saltBytes.length);
            byte[] ivBytes = new byte[cipher.getBlockSize()];
            buffer.get(ivBytes, 0, ivBytes.length);
            byte[] encryptedTextBytes = new byte[buffer.capacity() - saltBytes.length - ivBytes.length];
            buffer.get(encryptedTextBytes);

            // Deriving the key
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF_ALGORITHM);
            PBEKeySpec spec = new PBEKeySpec(masterPassword, saltBytes, ITERATION_COUNT, KEYSIZE);
            SecretKey secretKeySpec = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), AES);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(ivBytes));
            return new SecretKeySpec(cipher.doFinal(encryptedTextBytes), "AES");
        } catch (BadPaddingException bpe) {
            throw new HazelcastException("BadPaddingException caught decrypting data grid key " +
                    "- likely caused by an incorrect or changed master password", bpe);
        } catch (IllegalBlockSizeException | NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | InvalidKeyException | InvalidKeySpecException | NoSuchPaddingException exception) {
            throw new HazelcastException(exception);
        }
    }

    public static byte[] objectToByteArray(Object object) {
        byte[] bytes = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            bytes = bos.toByteArray();
        } catch (IOException ioe) {
            // See "if (bytes == null)"
        }
        if (bytes == null) {
            throw new HazelcastException("Error converting Object to Byte Array");
        }
        return bytes;
    }

    public static Object byteArrayToObject(byte[] bytes) {
        Object object;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            object = IOUtil.newObjectInputStream(Thread.currentThread().getContextClassLoader(), null, bis).readObject();
        } catch (IOException | ClassNotFoundException exception) {
            throw new HazelcastException("Error converting Byte Array to Object", exception);
        }
        return object;
    }
}
