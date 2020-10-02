package fish.payara.microprofile.config.extensions.gcp.model;

import java.util.List;

public class SecretsResponse {

    private List<Secret> secrets;

    public List<Secret> getSecrets() {
        return secrets;
    }

    public void setSecrets(List<Secret> secrets) {
        this.secrets = secrets;
    }
}
