/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.ejb.rest.client;

/**
 * This is the context factory that creates the context used for looking up and invoking
 * remote EJBs.
 * 
 * <p><strong>This class is deprecated</strong> use {@code fish.payara.ejb.http.client.RemoteEJBContextFactory}
 *
 * @deprecated in favor of package {@code fish.payara.ejb.http.client}
 * @author Arjan Tijms
 * @since Payara 5.191
 */
@Deprecated
public class RemoteEJBContextFactory extends fish.payara.ejb.http.client.RemoteEJBContextFactory {
    
    public static final String FISH_PAYARA_WITH_CONFIG = "fish.payara.withConfig";
    public static final String FISH_PAYARA_TRUST_STORE = "fish.payara.trustStore";
    public static final String FISH_PAYARA_SSL_CONTEXT = "fish.payara.sslContext";
    public static final String FISH_PAYARA_SCHEDULED_EXECUTOR_SERVICE = "fish.payara.scheduledExecutorService";
    public static final String FISH_PAYARA_READ_TIMEOUT = "fish.payara.readTimeout";
    public static final String FISH_PAYARA_KEY_STORE = "fish.payara.keyStore";
    public static final String FISH_PAYARA_HOSTNAME_VERIFIER = "fish.payara.hostnameVerifier";
    public static final String FISH_PAYARA_EXECUTOR_SERVICE = "fish.payara.executorService";
    public static final String FISH_PAYARA_CONNECT_TIMEOUT = "fish.payara.connectTimeout";

}
