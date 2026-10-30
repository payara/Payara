/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
import com.sun.enterprise.security.auth.login.ClientCertificateLoginModule;
import com.sun.enterprise.security.auth.login.common.LoginException;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.auth.realm.certificate.CertificateRealm;
import fish.payara.security.annotations.CertificateIdentityStoreDefinition;
import fish.payara.security.api.CertificateCredential;
import fish.payara.security.realm.config.CertificateRealmIdentityStoreConfiguration;
import fish.payara.security.realm.RealmUtil;
import static fish.payara.security.realm.RealmUtil.ASSIGN_GROUPS;
import java.security.cert.X509Certificate;
import static java.util.Arrays.asList;
import java.util.Properties;
import java.util.Set;
import static java.util.stream.Collectors.toSet;
import jakarta.enterprise.inject.Typed;
import javax.security.auth.Subject;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import static jakarta.security.enterprise.identitystore.CredentialValidationResult.INVALID_RESULT;
import static jakarta.security.enterprise.identitystore.CredentialValidationResult.NOT_VALIDATED_RESULT;
import jakarta.security.enterprise.identitystore.IdentityStore;
import org.glassfish.security.common.Group;

/**
 * {@link CertificateRealmIdentityStore} Identity store validates client
 * certificate using dynamically created certificate realm instance and returns
 * the validation result with the caller name and groups.
 *
 * @author Gaurav Gupta
 */
@Typed(CertificateRealmIdentityStore.class)
public class CertificateRealmIdentityStore implements IdentityStore {

    private CertificateRealmIdentityStoreConfiguration configuration;

    public final static Class<CertificateRealm> REALM_CLASS = CertificateRealm.class;
    public final static Class<ClientCertificateLoginModule> REALM_LOGIN_MODULE_CLASS = ClientCertificateLoginModule.class;

    public void init(CertificateRealmIdentityStoreConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public CredentialValidationResult validate(Credential credential) {
        if (credential instanceof CertificateCredential) {
            return validate((CertificateCredential) credential, configuration.getName());
        }
        return NOT_VALIDATED_RESULT;
    }

    public static CredentialValidationResult validate(CertificateCredential credential, String realmName) {
        try {
            Subject subject = login(credential, realmName);
            Set<String> groups = subject.getPrincipals(Group.class)
                    .stream()
                    .map(g -> g.getName())
                    .collect(toSet());
            return new CredentialValidationResult(credential.getPrincipal(), groups);
        } catch (LoginException ex) {
            return INVALID_RESULT;
        }
    }

    private static Subject login(CertificateCredential credential, String realmName) {
        Subject subject = createSubjectWithCerts(credential.getCertificates());
        WebAndEjbToJaasBridge.doX500Login(subject, realmName, null);
        return subject;
    }

    private static Subject createSubjectWithCerts(X509Certificate[] certificates) {
        Subject subject = new Subject();
        subject.getPublicCredentials().add(certificates[0].getSubjectX500Principal());
        subject.getPublicCredentials().add(asList(certificates));
        return subject;
    }

}
