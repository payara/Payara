/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.gjc.util;

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.gjc.common.DataSourceObjectBuilder;
import com.sun.gjc.spi.ConnectionRequestInfoImpl;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * SecurityUtils for Generic JDBC Connector.
 *
 * @author Evani Sai Surya Kiran
 * @version 1.0, 02/07/22
 */
public class SecurityUtils {

    static private StringManager sm = StringManager.getManager(
            DataSourceObjectBuilder.class);

    /**
     * This method returns the <code>PasswordCredential</code> object, given
     * the <code>ManagedConnectionFactory</code>, subject and the
     * <code>ConnectionRequestInfo</code>. It first checks if the
     * <code>ConnectionRequestInfo</code> is null or not. If it is not null,
     * it constructs a <code>PasswordCredential</code> object with
     * the user and password fields from the <code>ConnectionRequestInfo</code> and returns this
     * <code>PasswordCredential</code> object. If the <code>ConnectionRequestInfo</code>
     * is null, it retrieves the <code>PasswordCredential</code> objects from
     * the <code>Subject</code> parameter and returns the first
     * <code>PasswordCredential</code> object which contains a
     * <code>ManagedConnectionFactory</code>, instance equivalent
     * to the <code>ManagedConnectionFactory</code>, parameter.
     *
     * @param mcf     <code>ManagedConnectionFactory</code>
     * @param subject <code>Subject</code>
     * @param info    <code>ConnectionRequestInfo</code>
     * @return <code>PasswordCredential</code>
     * @throws <code>ResourceException</code> generic exception if operation fails
     * @throws <code>SecurityException</code> if access to the <code>Subject</code> instance is denied
     */
    public static PasswordCredential getPasswordCredential(final ManagedConnectionFactory mcf,
                                                           final Subject subject, javax.resource.spi.ConnectionRequestInfo info) throws ResourceException {

        if (info == null) {
            if (subject == null) {
                return null;
            } else {
                PasswordCredential pc = (PasswordCredential) AccessController.doPrivileged
                        (new PrivilegedAction() {
                            public Object run() {
                                Set passwdCredentialSet = subject.getPrivateCredentials(PasswordCredential.class);
                                Iterator iter = passwdCredentialSet.iterator();
                                while (iter.hasNext()) {
                                    PasswordCredential temp = (PasswordCredential) iter.next();
                                    if (temp.getManagedConnectionFactory().equals(mcf)) {
                                        return temp;
                                    }
                                }
                                return null;
                            }
                        });
                if (pc == null) {
                    String msg = sm.getString("su.no_passwd_cred");
                    throw new javax.resource.spi.SecurityException(msg);
                } else {
                    return pc;
                }
            }
        } else {
            ConnectionRequestInfoImpl cxReqInfo = (ConnectionRequestInfoImpl) info;
            PasswordCredential pc = new PasswordCredential(cxReqInfo.getUser(), cxReqInfo.getPassword().toCharArray());
            pc.setManagedConnectionFactory(mcf);
            return pc;
        }
    }

    /**
     * Returns true if two strings are equal; false otherwise
     *
     * @param str1 <code>String</code>
     * @param str2 <code>String</code>
     * @return true    if the two strings are equal
     *         false	otherwise
     */
    static private boolean isEqual(String str1, String str2) {
        if (str1 == null) {
            return (str2 == null);
        } else {
            return str1.equals(str2);
        }
    }

    /**
     * Returns true if two <code>PasswordCredential</code> objects are equal; false otherwise
     *
     * @param pC1 <code>PasswordCredential</code>
     * @param pC2 <code>PasswordCredential</code>
     * @return true    if the two PasswordCredentials are equal
     *         false	otherwise
     */
    static public boolean isPasswordCredentialEqual(PasswordCredential pC1, PasswordCredential pC2) {
        if (pC1 == pC2)
            return true;
        if (pC1 == null || pC2 == null)
            return (pC1 == pC2);
        if (!isEqual(pC1.getUserName(), pC2.getUserName())) {
            return false;
        }
        return Arrays.equals(pC1.getPassword(), pC2.getPassword());
    }
}
