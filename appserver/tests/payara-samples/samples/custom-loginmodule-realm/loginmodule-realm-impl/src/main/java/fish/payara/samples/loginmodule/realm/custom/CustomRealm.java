package fish.payara.samples.loginmodule.realm.custom;

import java.util.*;

/**
 * Realm wrapper for supporting Custom authentication.
 */
public final class CustomRealm {

    public static final String AUTH_TYPE = "custom";

    private Properties properties = new Properties();

    /**
     * Initializes realm configuration (JAAS context or other settings).
     */
    public synchronized void init(Properties props) {
        this.properties.clear();
        if (props != null) {
            this.properties.putAll(props);
        }

        String jaasCtx = props != null ? props.getProperty("jaas-context") : null;
        if (jaasCtx == null) {
            throw new IllegalArgumentException("No jaas-context specified");
        }
    }

    /**
     * Returns the authentication type.
     */
    public String getAuthType() {
        return AUTH_TYPE;
    }

    /**
     * Performs authentication. Returns group names on success, or null on failure.
     */
    public String[] authenticate(String username, char[] password) {
        if ("realmUser".equals(username) && Arrays.equals(password, "realmPassword".toCharArray())) {
            return new String[]{"realmGroup"};
        }
        return null;
    }

    /**
     * Returns all groups for a given user.
     */
    public Enumeration<String> getGroupNames(String username) {
        if ("realmUser".equals(username)) {
            return Collections.enumeration(Collections.singletonList("realmGroup"));
        }
        return Collections.enumeration(Collections.emptyList());
    }
}
