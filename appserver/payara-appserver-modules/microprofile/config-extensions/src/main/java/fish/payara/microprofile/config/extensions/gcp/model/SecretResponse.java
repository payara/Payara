package fish.payara.microprofile.config.extensions.gcp.model;

public class SecretResponse {

    private SecretVersion payload;

    public SecretVersion getPayload() {
        return payload;
    }

    public void setPayload(SecretVersion payload) {
        this.payload = payload;
    }
}
