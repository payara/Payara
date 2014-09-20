/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.security.services.common;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.Filter;

public class SecurityAccessFilter implements Filter {

    private static final String SYS_PROP_JAVA_SEC_POLICY = "java.security.policy";
    private static final Logger LOG = SecurityAccessValidationService._theLog;

    private static boolean javaPolicySet =
        AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

            @Override
            public Boolean run() {
                Boolean rtn = Boolean.FALSE;
                
                String wlsName = System.getProperty(SYS_PROP_JAVA_SEC_POLICY);
                
                if ( wlsName != null && !wlsName.isEmpty() )
                        rtn = Boolean.TRUE;
                
                return rtn;
            }
        });
    
    
    @Override
    public boolean matches(Descriptor d) {

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Descripter: " + d );
        }

        if (!javaPolicySet) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("java security policy is not set, so no validation for security servies.");
            }

            return false;
        }
        
        if (d == null)
            return false;

        Set<String> qualifiers = d.getQualifiers();
        if (qualifiers != null && qualifiers.size() != 0) {
            for (String s : qualifiers) {
                if (Secure.class.getCanonicalName().equals(s)) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("The instance is annotated with \'Secure\': " + s);                                
                    }
                    return true;
                }
            }
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("The instance has no \'Secure\' annotated ");
        }

        return false;
    }

}
