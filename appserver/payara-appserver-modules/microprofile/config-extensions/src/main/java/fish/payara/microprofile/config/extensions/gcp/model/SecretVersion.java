package fish.payara.microprofile.config.extensions.gcp.model;

import java.util.Base64;

public class SecretVersion {

    private String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = new String(Base64.getDecoder().decode(data));
    }
}
