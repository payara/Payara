package fish.payara.microprofile.config.extensions.gcp.model;

import com.nimbusds.jose.util.Base64;

public class SecretVersion {

    private String data;

    public SecretVersion() {

    }

    public SecretVersion(String data) {
        this.data = Base64.encode(data).toString();
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = new Base64(data).decodeToString();
    }
}
