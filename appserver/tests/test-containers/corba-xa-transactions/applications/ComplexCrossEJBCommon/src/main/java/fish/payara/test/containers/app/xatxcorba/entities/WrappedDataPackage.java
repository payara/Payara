package fish.payara.test.containers.app.xatxcorba.entities;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author fabio
 */
public class WrappedDataPackage implements Serializable{

    private String signature;
    private DataPackage inPackage;

    public WrappedDataPackage() {
    }

    public WrappedDataPackage(String signature, DataPackage inPackage) {
        this.signature = signature;
        this.inPackage = inPackage;
    }

    public String getSignature() {
        return signature;
    }

    public DataPackage getInPackage() {
        return inPackage;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.signature);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final WrappedDataPackage other = (WrappedDataPackage) obj;
        return Objects.equals(this.signature, other.signature);
    }

    @Override
    public String toString() {
        return "WrappedDataPackage{" + "signature=" + signature + ", inPackage=" + inPackage + '}';
    }
}
