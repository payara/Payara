package fish.payara.microprofile.config.extensions.gcp.model;

public class Secret {

    private String name;
    private Replication replication = new Replication();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.split("/secrets/")[1];
    }

    public Replication getReplication() {
        return replication;
    }
}
