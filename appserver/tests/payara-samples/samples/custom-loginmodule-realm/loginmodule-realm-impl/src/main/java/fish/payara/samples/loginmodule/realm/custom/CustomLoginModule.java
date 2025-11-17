/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2022 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
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
