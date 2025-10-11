/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

package fish.payara.security.identitystores;

import static jakarta.security.enterprise.identitystore.IdentityStore.ValidationType.VALIDATE;
import static fish.payara.security.identitystores.ConfigRetriever.resolveConfigAttribute;

import fish.payara.security.annotations.YubikeyIdentityStoreDefinition;
import com.yubico.client.v2.ResponseStatus;
import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.exceptions.YubicoValidationFailure;
import com.yubico.client.v2.exceptions.YubicoVerificationException;
import fish.payara.notification.requesttracing.EventType;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import org.glassfish.internal.api.Globals;

/**
 * A Yubikey identity store. Supports connecting to the Yubico's cloud validation service. You must provide an API 
 * client ID and key for this service in the {@link YubikeyIdentityStoreDefinition}
 * You can obtain one directly from Yubico at https://upgrade.yubico.com/getapikey/
 * 
 * @author Mark Wareham
 */
@Typed(YubikeyIdentityStore.class)
public class YubikeyIdentityStore implements IdentityStore {
    
    
    private static final Logger LOG = Logger.getLogger(YubikeyIdentityStore.class.getName());
    
    private YubicoAPI yubicoAPI;
    
    private RequestTracingService requestTracing;
    private int priority; //priority is handled as immediate, not deferred
        
    public YubikeyIdentityStore init(YubikeyIdentityStoreDefinition definition) {
        try {
            this.requestTracing = Globals.get(RequestTracingService.class);
        } catch (NullPointerException e) {
            LOG.log(Level.INFO, "Error retrieving Request Tracing service "
                    + "during initialisation of Yubikey Identity Store - NullPointerException");
        }
        priority = definition.priority();
        yubicoAPI = new YubicoAPI(definition.yubikeyAPIClientID(), definition.yubikeyAPIKey());
        return this;
    }

    @Override
    public CredentialValidationResult validate(Credential credential) {
        
        if (!(credential instanceof YubikeyCredential)) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }
        YubikeyCredential yubikeyCredential = ((YubikeyCredential) credential);
        try {
            String oneTimePassword = yubikeyCredential.getOneTimePasswordString();
            
            if (!YubicoClient.isValidOTPFormat(oneTimePassword)) {
                return CredentialValidationResult.INVALID_RESULT;
            }
            RequestTraceSpan span = beginTrace(yubikeyCredential);
            ResponseStatus responseStatus = yubicoAPI.verify(oneTimePassword).getStatus();
            doTrace(span, responseStatus);
            LOG.log(Level.FINE, "Yubico server reported {0}", responseStatus.name());

            switch (responseStatus) {
                case BAD_OTP:
                case REPLAYED_OTP:
                case BAD_SIGNATURE:
                case NO_SUCH_CLIENT:
                    return CredentialValidationResult.INVALID_RESULT;
                case MISSING_PARAMETER:
                case OPERATION_NOT_ALLOWED:
                case BACKEND_ERROR:
                case NOT_ENOUGH_ANSWERS:
                case REPLAYED_REQUEST:
                    LOG.log(Level.WARNING, "Yubico reported {0}",
                            responseStatus.name());
                    return CredentialValidationResult.NOT_VALIDATED_RESULT;
                case OK:
                    break;//carry on.
                default:
                    LOG.log(Level.SEVERE, "Unknown/new yubico return status");
            }

            return new CredentialValidationResult(yubikeyCredential.getPublicID());
        
        } catch (YubicoVerificationException | YubicoValidationFailure ex) {
            LOG.log(Level.SEVERE, null, ex);
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }
    }
    
    @Override
    public Set<ValidationType> validationTypes() {
        return EnumSet.of(VALIDATE);
        //This IdentityStore does not provide groups.
    }

    @Override
    public int priority() {
       return priority;
    }

    private RequestTraceSpan beginTrace(YubikeyCredential yubikeyCredential) {
        if (requestTracing==null || !requestTracing.isRequestTracingEnabled()) {
            return null;
        }
        
        RequestTraceSpan span = new RequestTraceSpan(EventType.REQUEST_EVENT, "verifyYubikeyCloudServiceRequest");
        span.addSpanTag("API Client ID", ""+yubicoAPI.getClientId());
        span.addSpanTag("Yubikey public ID", yubikeyCredential.getPublicID());
        span.addSpanTag("Yubico validation URLs", Arrays.toString(yubicoAPI.getWsapiUrls()));
        return span;
    }

    private void doTrace(RequestTraceSpan span, ResponseStatus responseStatus) {
        if(span !=null){
            span.addSpanTag("Yubico response status", responseStatus.name());
            requestTracing.traceSpan(span);
        }
    }
}
