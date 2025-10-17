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
 *
 */
package fish.payara.security.identitystores;

import com.yubico.client.v2.VerificationResponse;
import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.exceptions.YubicoValidationFailure;
import com.yubico.client.v2.exceptions.YubicoVerificationException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mark Wareham
 */
class YubicoAPI {
    private static final Logger LOG = Logger.getLogger(YubicoAPI.class.getName());

    private YubicoClient yubicoClient;

    YubicoAPI(int apiClientID, String apiClientKey) {
        if(apiClientID==0){
            LOG.log(Level.WARNING, "A Yubico clientID has not been set");
        }else{
            LOG.log(Level.INFO, "Set up YubicoClient with clientID of {0}", apiClientID);
        }
        yubicoClient = YubicoClient.getClient(apiClientID, apiClientKey);
    }

    public VerificationResponse verify(String otp) throws YubicoVerificationException, YubicoValidationFailure {
        return yubicoClient.verify(otp);
    }

    public Integer getClientId() {
        return yubicoClient.getClientId();
    }

    public String[] getWsapiUrls() {
        return yubicoClient.getWsapiUrls();
    }
}
