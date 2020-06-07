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

import com.sun.enterprise.security.auth.WebAndEjbToJaasBridge;
import static com.sun.enterprise.security.auth.login.LoginContextDriver.getValidRealm;
import com.sun.enterprise.security.auth.login.common.LoginException;
import com.sun.enterprise.security.auth.login.common.PasswordCredential;
import static com.sun.enterprise.security.common.AppservAccessController.privileged;
import fish.payara.security.annotations.RealmIdentityStoreDefinition;
import fish.payara.security.api.CertificateCredential;
import fish.payara.security.realm.config.RealmIdentityStoreConfiguration;
import java.util.Set;
import static java.util.stream.Collectors.toSet;
import javax.enterprise.inject.Typed;
import javax.security.auth.Subject;
import javax.security.enterprise.CallerPrincipal;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import static javax.security.enterprise.identitystore.CredentialValidationResult.INVALID_RESULT;
import static javax.security.enterprise.identitystore.CredentialValidationResult.NOT_VALIDATED_RESULT;
import javax.security.enterprise.identitystore.IdentityStore;
import org.glassfish.security.common.Group;

/**
 * {@link RealmIdentityStore} Identity store validates the credential using
 * existing realm instance and returns the validation result with the caller
 * name and groups.
 *
 * @author Gaurav Gupta
 */
@Typed(RealmIdentityStore.class)
public class RealmIdentityStore implements IdentityStore {

    private RealmIdentityStoreConfiguration configuration;

    public void setConfiguration(RealmIdentityStoreDefinition definition) {
        this.configuration = RealmIdentityStoreConfiguration.from(definition);
    }

    @Override
    public CredentialValidationResult validate(Credential credential) {
        if (credential instanceof UsernamePasswordCredential) {
            return validate((UsernamePasswordCredential) credential, configuration.getName());
        } else if (credential instanceof CertificateCredential) {
            return CertificateRealmIdentityStore.validate((CertificateCredential) credential, configuration.getName());
        }
        return NOT_VALIDATED_RESULT;
    }

    protected CredentialValidationResult validate(UsernamePasswordCredential credential, String realmName) {
        try {
            Subject subject = login(credential, realmName);
            Set<String> groups = subject.getPrincipals(Group.class)
                    .stream()
                    .map(g -> g.getName())
                    .collect(toSet());
            if (!groups.isEmpty()) {
                return new CredentialValidationResult(new CallerPrincipal(credential.getCaller()), groups);
            }
        } catch (LoginException ex) {
            return INVALID_RESULT;
        }
        return INVALID_RESULT;
    }

    protected Subject login(UsernamePasswordCredential credential, String realmName) {
        String username = credential.getCaller();
        char[] password = credential.getPassword().getValue();
        Subject subject = new Subject();
        privileged(() -> subject.getPrivateCredentials().add(new PasswordCredential(username, password, getValidRealm(realmName))));

        WebAndEjbToJaasBridge.login(subject, PasswordCredential.class);
        return subject;
    }

}
