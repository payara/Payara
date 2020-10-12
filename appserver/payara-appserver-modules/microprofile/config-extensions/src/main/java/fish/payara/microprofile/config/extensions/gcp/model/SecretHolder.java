package fish.payara.microprofile.config.extensions.gcp.model;

public class SecretHolder {

    private SecretVersion payload;

    public SecretHolder() {

    }

    public SecretHolder(String data) {
        this.payload = new SecretVersion(data);
    }

    public SecretVersion getPayload() {
        return payload;
    }

    public void setPayload(SecretVersion payload) {
        this.payload = payload;
    }
}
