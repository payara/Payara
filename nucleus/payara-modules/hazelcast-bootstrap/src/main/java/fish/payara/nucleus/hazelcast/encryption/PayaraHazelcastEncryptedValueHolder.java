package fish.payara.nucleus.hazelcast.encryption;

import java.io.Serializable;

public class PayaraHazelcastEncryptedValueHolder implements Serializable {

    private String encryptedObjectString;

    public PayaraHazelcastEncryptedValueHolder(String encryptedObjectString) {
        this.encryptedObjectString = encryptedObjectString;
    }

    public String getEncryptedObjectString() {
        return encryptedObjectString;
    }
}
