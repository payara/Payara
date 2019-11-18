package fish.payara.nucleus.hazelcast.encryption;

import com.hazelcast.core.HazelcastException;
import com.hazelcast.nio.IOUtil;
import com.sun.enterprise.security.ssl.impl.MasterPasswordImpl;
import com.sun.enterprise.util.StringUtils;
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
import java.io.ObjectInputStream;
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
import java.util.Base64;
import java.util.Random;

/**
 * Class that encodes and decodes symmetric keys.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 * @author Rudy De Busscher <rudy.de.busscher@payara.fish>
 */
public class SymmetricEncryptor {

    private static final String DATAGRID_KEY_FILE = "datagrid-key";
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String PBKDF_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final String AES = "AES";
    private static final int ITERATION_COUNT = 65556;
    private static final int KEYSIZE = 256;
    private static Random random = new SecureRandom();
    private static SecretKey secretKey;

    static {
        secretKey = readAndDecryptSecretKey();
    }

    public static String encode(byte[] value) {
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
            return Base64.getEncoder().encodeToString(buffer);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException
                | InvalidParameterSpecException | BadPaddingException exception) {
            throw new HazelcastException(exception);
        }
    }

    public static byte[] decode(String encryptedText) {
        byte[] decryptedTextBytes;
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            // Strip off the salt and IV
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode((encryptedText)));
            byte[] saltBytes = new byte[20];
            buffer.get(saltBytes, 0, saltBytes.length);
            byte[] ivBytes = new byte[cipher.getBlockSize()];
            buffer.get(ivBytes, 0, ivBytes.length);
            byte[] encryptedTextBytes = new byte[buffer.capacity() - saltBytes.length - ivBytes.length];

            buffer.get(encryptedTextBytes);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));
            decryptedTextBytes = cipher.doFinal(encryptedTextBytes);
        } catch (BadPaddingException exception) {
            // BadPaddingException -> Wrong key
            throw new HazelcastException("BadPaddingException caught decoding data, " +
                    "this can be caused by an incorrect key: ", exception);
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
            throw new HazelcastException("Error reading encrypted key", ioe);
        }

        if (encryptedBytes == null) {
            throw new HazelcastException("Encrypted key appears to be null");
        }

        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);

            // Strip off the salt and IV
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(encryptedBytes));
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
            throw new HazelcastException("BadPaddingException caught decrypting data from Datagrid " +
                    "- likely caused by an incorrect master password", bpe);
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
            // Blah
        }
        if (bytes == null) {
            throw new HazelcastException("Error converting Object to Byte Array");
        }
        return bytes;
    }

    public static Object byteArrayToObject(byte[] bytes) {
        Object object = null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            object = IOUtil.newObjectInputStream(Thread.currentThread().getContextClassLoader(), null, bis).readObject();
        } catch (IOException | ClassNotFoundException exception) {
            throw new HazelcastException("Error converting Byte Array to Object", exception);
        }
        if (object == null) {
            throw new HazelcastException("Object appears to be null, something probably went wrong converting Byte Array to Object");
        }
        return object;
    }
}
