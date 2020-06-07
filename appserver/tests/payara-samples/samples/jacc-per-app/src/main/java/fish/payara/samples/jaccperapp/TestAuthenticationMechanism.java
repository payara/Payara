/** Copyright Payara Services Limited **/

package fish.payara.samples.jaccperapp;

import static javax.security.enterprise.identitystore.CredentialValidationResult.Status.VALID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Test authentication mechanism for authenticating with <code>name</code> and <code>password</code>
 * request parameters. This only makes sense for testing.
 *
 * @author Arjan Tijms
 *
 */
@ApplicationScoped
public class TestAuthenticationMechanism implements HttpAuthenticationMechanism {

    @Inject
    private IdentityStoreHandler identityStoreHandler;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response, HttpMessageContext httpMessageContext) throws AuthenticationException {

        if (request.getParameter("name") != null && request.getParameter("password") !=  null) {

            CredentialValidationResult result = identityStoreHandler.validate(
                new UsernamePasswordCredential(
                    request.getParameter("name"),
                    request.getParameter("password")));

            if (result.getStatus() == VALID) {
                return httpMessageContext.notifyContainerAboutLogin(
                    result.getCallerPrincipal(), result.getCallerGroups());
            }

            return httpMessageContext.responseUnauthorized();
        }

        return httpMessageContext.doNothing();
    }

}
