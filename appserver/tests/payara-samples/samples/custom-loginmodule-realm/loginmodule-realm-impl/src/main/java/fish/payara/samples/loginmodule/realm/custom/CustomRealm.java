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
