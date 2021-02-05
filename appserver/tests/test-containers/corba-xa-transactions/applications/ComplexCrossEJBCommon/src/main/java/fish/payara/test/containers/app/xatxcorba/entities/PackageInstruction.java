package fish.payara.test.containers.app.xatxcorba.entities;

import java.io.Serializable;

public class PackageInstruction implements Serializable{

    private String signature;
    private Integer contentDetails;

    public PackageInstruction() {
    }

    public PackageInstruction(String signature, Integer contentDetails) {
        this.signature = signature;
        this.contentDetails = contentDetails;
    }

    public String getSignature() {
        return signature;
    }

    public Integer getContentDetails() {
        return contentDetails;
    }

    @Override
    public String toString() {
        return "PackageInstruction{signature=" + signature + ", contentDetails=" + contentDetails + '}';
    }
}
