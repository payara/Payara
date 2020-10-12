package fish.payara.microprofile.config.extensions.gcp.model;

public class Replication {

    private ReplicationType automatic = new AutomaticReplicationType();

    public ReplicationType getAutomatic() {
        return automatic;
    }

    class ReplicationType {
    }

    class AutomaticReplicationType extends ReplicationType {
    }
}
