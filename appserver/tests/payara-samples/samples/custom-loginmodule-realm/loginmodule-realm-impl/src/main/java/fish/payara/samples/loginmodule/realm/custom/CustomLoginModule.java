package fish.payara.samples.loginmodule.realm.custom;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Simplified custom LoginModule without Payara internal dependencies.
 * Uses standard JAAS interfaces.
 */
public class CustomLoginModule implements LoginModule {

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;

    private String username;
    private char[] password;
    private boolean authenticated;

    private CustomRealm customRealm = new CustomRealm(); // You can inject or mock this

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;
    }

    @Override
    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("No CallbackHandler available");
        }

        NameCallback nameCb = new NameCallback("Username: ");
        PasswordCallback passCb = new PasswordCallback("Password: ", false);

        try {
            callbackHandler.handle(new Callback[]{nameCb, passCb});
        } catch (Exception e) {
            throw new LoginException("Error during callback handling: " + e.getMessage());
        }

        username = nameCb.getName();
        password = passCb.getPassword();

        if (username == null || username.isEmpty()) {
            throw new LoginException("No username provided");
        }

        String[] groups = customRealm.authenticate(username, password);

        if (groups == null) {
            throw new LoginException("Login failed for " + username);
        }

        authenticated = true;

        // Add principals
        Set<java.security.Principal> principals = subject.getPrincipals();
        principals.add(new UserPrincipal(username));
        for (String g : groups) {
            principals.add(new GroupPrincipal(g));
        }

        return true;
    }

    @Override
    public boolean commit() {
        return authenticated;
    }

    @Override
    public boolean abort() {
        return !authenticated;
    }

    @Override
    public boolean logout() {
        subject.getPrincipals().clear();
        return true;
    }

    // --- helper classes ---
    public static class UserPrincipal implements java.security.Principal {
        private final String name;
        public UserPrincipal(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public static class GroupPrincipal implements java.security.Principal {
        private final String name;
        public GroupPrincipal(String name) { this.name = name; }
        public String getName() { return name; }
    }
}
