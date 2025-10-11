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
// Portions Copyright [2016-2019] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.web.integration;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import com.sun.enterprise.security.auth.realm.certificate.OID;

import org.glassfish.security.common.PrincipalImpl;

import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.SecurityContextProxy;

import javax.security.auth.x500.X500Principal;

public class WebPrincipal extends PrincipalImpl implements SecurityContextProxy {

    private static final long serialVersionUID = 1L;

    private char[] password;
    private X509Certificate[] certificates;
    private final boolean useCertificate;
    private final SecurityContext securityContext;
    private Principal customPrincipal;

    public WebPrincipal(Principal principal, SecurityContext context) {
        super(principal.getName());
        if (!(principal instanceof PrincipalImpl)) {
            customPrincipal = principal;
        }
        this.useCertificate = false;
        this.securityContext = context;
    }

    public WebPrincipal(String user, String password, SecurityContext context) {
        this(user, password == null ? null : password.toCharArray(), context);
    }

    public WebPrincipal(String user, char[] pwd, SecurityContext context) {
        super(user);

        // Copy the password to another reference before storing it to the
        // instance field.
        this.password = pwd == null ? null : Arrays.copyOf(pwd, pwd.length);

        this.useCertificate = false;
        this.securityContext = context;
    }

    public WebPrincipal(X509Certificate[] certs, SecurityContext context) {
        this(certs, context, false);
    }

    public WebPrincipal(X509Certificate[] certificates, SecurityContext context, boolean nameFromContext) {
        super(getPrincipalName(certificates, context, nameFromContext));
        this.certificates = certificates;
        this.useCertificate = true;
        this.securityContext = context;
    }

    public char[] getPassword() {
        // Copy the password to another reference and return the reference
        return password == null ? null : Arrays.copyOf(password, password.length);
    }

    public X509Certificate[] getCertificates() {
        return certificates;
    }

    public boolean isUsingCertificate() {
        return useCertificate;
    }

    @Override
    public SecurityContext getSecurityContext() {
        return securityContext;
    }

    @Override
    public String getName() {
        if (customPrincipal == null) {
            return super.getName();
        }

        return customPrincipal.getName();
    }

    @Override
    public boolean equals(Object another) {
        if (customPrincipal == null) {
            return super.equals(another);
        }

        return customPrincipal.equals(another);
    }

    @Override
    public int hashCode() {
        if (customPrincipal == null) {
            return super.hashCode();
        }

        return customPrincipal.hashCode();
    }

    @Override
    public String toString() {
        if (customPrincipal == null) {
            return super.toString();
        }

        return customPrincipal.toString();
    }

    public Principal getCustomPrincipal() {
        return customPrincipal;
    }

    private static String getPrincipalName(X509Certificate[] certificates, SecurityContext context, boolean nameFromContext) {
        if (nameFromContext) {
            // Use the principal name from the security context, ensuring the context caller principal and
            // the web principal have the same name.
            //
            // This will typically be an org.glassfish.security.common.PrincipalImpl which as its name has
            // the name obtained from javax.security.auth.x500.X500Principal, which is obtained from
            // certificates[0].getSubjectX500Principal().
            //
            // I.e. the internal principal in the security context is effectively created via:
            //
            // new PrincipalImpl(certificates[0].getSubjectX500Principal());
            //
            // The format of the X.500 distinguished name (DN) returned here will then be RFC 2253, e.g.
            // C=UK,ST=lak,L=zak,O=kaz,OU=bar,CN=lfoo
            return context.getCallerPrincipal().getName();
        }

        // Use the full DN name from the certificates. This should normally be the same as
        // context.getCallerPrincipal(), but a realm could have decided to map the name in which
        // case they will be different.
        return certificates[0].getSubjectX500Principal().getName(X500Principal.RFC2253, OID.getOIDMap());
    }
}
