/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
// Portions Copyright [2018-2024] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.ee.authentication.jakarta;

import java.util.Map;

import jakarta.security.auth.message.MessageInfo;

import org.jvnet.hk2.annotations.Contract;

import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;

/**
 * A Delegate interface for handling WebServices specific security and JASPIC (JSR 196) providers. 
 * 
 * <p>
 * This insulates the Payara Web-Bundle from any WebServices dependencies. This interface is implemented
 * in the web-services security project (webservices.security).
 * 
 * @author kumar.jayanti
 */
@Contract
public interface WebServicesDelegate {
     
    /**
     * 
     * @param serviceReference The ServiceReferenceDescriptor
     * @param properties The Properties Map passed to WebServices Code Via PipeCreator
     * @return The MessageSecurityBindingDescriptor
     */
    MessageSecurityBindingDescriptor getBinding(ServiceReferenceDescriptor serviceReference, Map properties);

    /**
     * This method returns the class name of the default JASPIC (JSR 196) WebServices security provider.
     * 
     * <p>
     * In practice this typically the Metro Security Provider, which is 
     * <code>"com.sun.xml.wss.provider.wsit.WSITAuthConfigProvider"</code>
     * 
     * @return the class name of the default JASPIC (JSR 196) WebServices security provider.
     */
    String getDefaultWebServicesProvider();

    /**
     * @param messageInfo The MessageInfo
     * @return the AuthContextID computed from the argument MessageInfo
     */
    String getAuthContextID(MessageInfo messageInfo);
    
}
