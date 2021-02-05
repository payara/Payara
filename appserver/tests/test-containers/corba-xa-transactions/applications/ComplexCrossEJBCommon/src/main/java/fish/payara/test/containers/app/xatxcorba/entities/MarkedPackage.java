package fish.payara.test.containers.app.xatxcorba.entities;

import java.io.Serializable;
import java.util.Objects;

public class MarkedPackage implements Serializable{

    private String signature;

    public MarkedPackage() {
    }

    public MarkedPackage(String signature) {
        this.signature = signature;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.signature);
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
        final MarkedPackage other = (MarkedPackage) obj;
        return Objects.equals(this.signature, other.signature);
    }
}
