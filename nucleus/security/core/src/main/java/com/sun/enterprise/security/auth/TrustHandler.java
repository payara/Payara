/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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

package com.sun.enterprise.security.auth;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 *
 * Enables developers to provide custom implementation to enable sip containers
 * to determine if a network entity can be trusted.
 */
public interface TrustHandler {

    
    public void initialize(Properties props);
    /**
     * determines if the container can trust the network entity from which we received the message with P-Asserted-Identity
     * header. This method also validates if the identity that was used to secure(eg: SSL) the message is trusted.
     *
     * @param pAssertedValues P-Asserted-Identity header values
     * @param messageDirection "Incoming" if this method is invoked for a incoming request, "Outgoing" if the message is being sent out.
     * @param asserterAddress ipaddress/hostname of the network entity from which we received the SIP message
     * with P-Asserted-Identity header. Inorder to accept/use the values in P-Asserted-Identity
     * header the network entity should be a trusted.
     * @param securityid is the asserting security identity, if a secure connection is used then this
     * would be the java.security.cert.X509Certificate, else null.
     * @return true if we trust the networtid and the securityid.
     */
    public boolean isTrusted(String asserterAddress, String messageDirection,X509Certificate securityid, Principal[] pAssertedValues);

   
}
