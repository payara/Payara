/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
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
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.glassfish.soteria.mechanisms.LoginToContinueHolder;

/**
 * Authentication mechanism that authenticates One Time Password
 *
 * @author Mark Wareham
 */
//TODO remove this class.
@AutoApplySession
@LoginToContinue
@Typed(TwoFactorAuthenticationMechanism.class) //Restricts the type
public class TwoFactorAuthenticationMechanism implements HttpAuthenticationMechanism, LoginToContinueHolder {

    private static final Logger LOG = Logger.getLogger(TwoFactorAuthenticationMechanism.class.getName());
    private LoginToContinue loginToContinue;
    private CredentialValidationResult firstValidationResult;
    
    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
            HttpServletResponse response, HttpMessageContext httpMessageContext) throws AuthenticationException {
        if (hasCredential(httpMessageContext)) {

            IdentityStoreHandler identityStoreHandler = CDI.current().select(IdentityStoreHandler.class).get();
            
            CredentialValidationResult currentRoundValidationResult = identityStoreHandler.validate(
                    httpMessageContext.getAuthParameters().getCredential());
            
            if(firstValidationResult == null){//first factor
                firstValidationResult = currentRoundValidationResult;
                return httpMessageContext.doNothing();
            }else{//second factor
                CredentialValidationResult finalResult = 
                        decideResult(firstValidationResult, currentRoundValidationResult);
                return httpMessageContext.notifyContainerAboutLogin(finalResult);
            }
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

    public TwoFactorAuthenticationMechanism loginToContinue(LoginToContinue loginToContinue) {
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
    
    @Override
    public void cleanSubject(HttpServletRequest request, HttpServletResponse response, HttpMessageContext httpMessageContext) {
        httpMessageContext.cleanClientSubject();
        this.firstValidationResult = null;
    }

    private CredentialValidationResult decideResult(
            CredentialValidationResult firstValidationResult, 
            CredentialValidationResult secondValidationResult) {
        
        CredentialValidationResult rtn;
        LOG.info("first IdentityStoreID = " + firstValidationResult.getIdentityStoreId());//TODO remove me
        LOG.info("second IdentityStoreID = " + secondValidationResult.getIdentityStoreId());//TODO remove me
        
        //TODO could more intelligently return the main IDStore validation result here if priorities are adjusted by the user. 
        //for now assume first is higher.
        
        if(firstValidationResult.getStatus() == CredentialValidationResult.Status.VALID
                && secondValidationResult.getStatus() == CredentialValidationResult.Status.VALID){
            rtn = firstValidationResult;
        }
        else if(secondValidationResult.getStatus()!=CredentialValidationResult.Status.VALID){
            rtn = secondValidationResult;
        }else {
            rtn = firstValidationResult;
        }
        LOG.info("decided on return result of " + rtn.getStatus().name() + " that's from " + firstValidationResult.getIdentityStoreId());//TODO remove me
        return rtn;
    }
}

