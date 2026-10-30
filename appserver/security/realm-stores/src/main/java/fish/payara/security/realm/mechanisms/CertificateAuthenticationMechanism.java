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
package fish.payara.security.realm.mechanisms;

import fish.payara.security.api.CertificateCredential;
import fish.payara.security.realm.CertificateCredentialImpl;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import static jakarta.security.enterprise.AuthenticationStatus.SEND_FAILURE;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import static jakarta.security.enterprise.identitystore.CredentialValidationResult.Status.VALID;
import jakarta.security.enterprise.identitystore.IdentityStoreHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import org.apache.catalina.LogFacade;
import static org.apache.catalina.LogFacade.NO_CLIENT_CERTIFICATE_CHAIN;
import static org.glassfish.grizzly.http.server.util.Globals.CERTIFICATES_ATTR;
import static org.glassfish.grizzly.http.server.util.Globals.SSL_CERTIFICATE_ATTR;
import static org.glassfish.soteria.Utils.isEmpty;

/**
 * Authentication mechanism that authenticates using client certificate
 * authentication
 *
 * @author Gaurav Gupta
 *
 */
@Typed(CertificateAuthenticationMechanism.class)
public class CertificateAuthenticationMechanism implements HttpAuthenticationMechanism {

    @Inject
    private IdentityStoreHandler identityStoreHandler;

    private static final Logger LOGGER = Logger.getLogger(CertificateAuthenticationMechanism.class.getName());

    protected static final ResourceBundle SERVLET_CONTAINER_BUNDLE = LogFacade.getLogger().getResourceBundle();

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response, HttpMessageContext httpMsgContext) throws AuthenticationException {

        X509Certificate[] certificates = getCertificates(request);

        if (!isEmpty(certificates) && certificates.length != 0) {

            CredentialValidationResult result = identityStoreHandler.validate(
                    new CertificateCredentialImpl(certificates));

            if (result.getStatus() == VALID) {
                return httpMsgContext.notifyContainerAboutLogin(
                        result.getCallerPrincipal(), result.getCallerGroups());
            }
        }

        if (httpMsgContext.isProtected()) {
            if (isEmpty(certificates) || certificates.length == 0) {
                try {
                    response.sendError(SC_BAD_REQUEST, SERVLET_CONTAINER_BUNDLE.getString(NO_CLIENT_CERTIFICATE_CHAIN));
                    return SEND_FAILURE;
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            } else {
                return httpMsgContext.responseUnauthorized();
            }
        }

        return httpMsgContext.doNothing();
    }

    private X509Certificate[] getCertificates(HttpServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute(CERTIFICATES_ATTR);
        if (certs == null || certs.length < 1) {
            certs = (X509Certificate[]) request.getAttribute(SSL_CERTIFICATE_ATTR);
        }
        return certs;
    }

}
