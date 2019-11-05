/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.security.realm.identitystores;

import com.sun.enterprise.security.auth.login.common.LoginException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.auth.realm.pam.PamRealm;
import com.sun.enterprise.security.ee.auth.login.PamLoginModule;
import fish.payara.security.annotations.PamIdentityStoreDefinition;
import fish.payara.security.realm.PamRealmIdentityStoreConfiguration;
import fish.payara.security.realm.RealmUtil;
import static fish.payara.security.realm.RealmUtil.ASSIGN_GROUPS;
import static fish.payara.security.realm.RealmUtil.JAAS_CONTEXT;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import javax.enterprise.inject.Typed;
import javax.security.enterprise.CallerPrincipal;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import static javax.security.enterprise.identitystore.CredentialValidationResult.INVALID_RESULT;
import static javax.security.enterprise.identitystore.CredentialValidationResult.NOT_VALIDATED_RESULT;
import javax.security.enterprise.identitystore.IdentityStore;
import org.glassfish.soteria.identitystores.IdentityStoreException;
import org.jvnet.libpam.UnixUser;

/**
 * {@link PamRealmIdentityStore} Identity store validates the user using
 * dynamically created pam realm instance and returns the validation result with
 * the caller name and groups.
 *
 * @author Gaurav Gupta
 */
@Typed(PamRealmIdentityStore.class)
public class PamRealmIdentityStore implements IdentityStore {

    private PamRealmIdentityStoreConfiguration configuration;

    private PamRealm pamRealm;

    public static final Class<PamRealm> REALM_CLASS = PamRealm.class;

    public static final Class<PamLoginModule> REALM_LOGIN_MODULE_CLASS = PamLoginModule.class;

    public void init(PamIdentityStoreDefinition definition) {
        configuration = PamRealmIdentityStoreConfiguration.from(definition);

        try {
            if (!Realm.isValidRealm(configuration.getName())) {
                Properties props = new Properties();
                props.put(JAAS_CONTEXT, configuration.getName());
                if (!configuration.getAssignGroups().isEmpty()) {
                    props.put(ASSIGN_GROUPS, String.join(",", configuration.getAssignGroups()));
                }
                RealmUtil.createAuthRealm(configuration.getName(), REALM_CLASS.getName(), REALM_LOGIN_MODULE_CLASS.getName(), props);
            }
            pamRealm = REALM_CLASS.cast(Realm.getInstance(configuration.getName()));
        } catch (NoSuchRealmException ex) {
            throw new IdentityStoreException(configuration.getName(), ex);
        }
    }

    @Override
    public CredentialValidationResult validate(Credential credential) {
        if (credential instanceof UsernamePasswordCredential) {
            return validate((UsernamePasswordCredential) credential);
        }
        return NOT_VALIDATED_RESULT;
    }

    public CredentialValidationResult validate(UsernamePasswordCredential credential) {
        try {
            String[] groups = login(credential);
            if (groups != null) {
                return new CredentialValidationResult(new CallerPrincipal(credential.getCaller()), new HashSet<>(Arrays.asList(groups)));
            }
        } catch (LoginException ex) {
            return INVALID_RESULT;
        }
        return INVALID_RESULT;
    }

    public String[] login(UsernamePasswordCredential credential) {
        String username = credential.getCaller();
        char[] password = credential.getPassword().getValue();
        UnixUser user = pamRealm.authenticate(username, password);
        return user.getGroups().toArray(new String[0]);
    }

}
