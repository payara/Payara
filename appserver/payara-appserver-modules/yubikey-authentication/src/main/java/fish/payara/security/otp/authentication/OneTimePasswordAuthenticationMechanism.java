/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.security.otp.authentication;

import java.util.logging.Logger;

import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.CDI;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.AutoApplySession;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Authentication mechanism that authenticates One Time Password
 *
 * @author Mark Wareham
 */
@AutoApplySession
@LoginToContinue
@Typed(OneTimePasswordAuthenticationMechanism.class) //Restricts the type
public class OneTimePasswordAuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final Logger LOG = Logger.getLogger(OneTimePasswordAuthenticationMechanism.class.getName());
    private LoginToContinue loginToContinue;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
            HttpServletResponse response, HttpMessageContext httpMessageContext) throws AuthenticationException {
        System.out.println("OneTimePasswordAuthenticationMechanism.validateRequest()");
        if (hasCredential(httpMessageContext)) {

            IdentityStoreHandler identityStoreHandler = CDI.current().select(IdentityStoreHandler.class).get();

            return httpMessageContext.notifyContainerAboutLogin(
                    identityStoreHandler.validate(
                            httpMessageContext.getAuthParameters()
                                    .getCredential()));
        }
        return httpMessageContext.doNothing();
    }

    private static boolean hasCredential(HttpMessageContext httpMessageContext) {
        boolean hasCredential = httpMessageContext.getAuthParameters().getCredential() != null;
        if (!hasCredential) {
            LOG.finest("No credential provided.");
        }
        return hasCredential;
    }

    public LoginToContinue getLoginToContinue() {
        return loginToContinue;
    }

    public void setLoginToContinue(LoginToContinue loginToContinue) {
        this.loginToContinue = loginToContinue;
    }

    public OneTimePasswordAuthenticationMechanism loginToContinue(LoginToContinue loginToContinue) {
        setLoginToContinue(loginToContinue);
        return this;
    }

    private static boolean notNull(Object... objects) {
        for (Object object : objects) {
            if (object == null) {
                return false;
            }
        }
        return true;
    }
}
