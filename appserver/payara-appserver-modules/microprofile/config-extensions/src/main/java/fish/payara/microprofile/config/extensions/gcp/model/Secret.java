package fish.payara.microprofile.config.extensions.gcp.model;

public class Secret {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.split("/secrets/")[1];
    }
}
