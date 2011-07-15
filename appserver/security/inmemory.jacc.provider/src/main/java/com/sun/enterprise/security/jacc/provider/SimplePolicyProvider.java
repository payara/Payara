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

package com.sun.enterprise.security.jacc.provider;

import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;

/**
 *
 * @author monzillo
 */
public class SimplePolicyProvider extends Policy {

    private static final String REUSE = "java.security.Policy.supportsReuse";
    Policy basePolicy;
    /**
     * ThreadLocal object to keep track of the reentrancy status of each thread.
     * It contains a byte[] object whose single element is either 0 (initial
     * value or no reentrancy), or 1 (current thread is reentrant). When a
     * thread exists the implies method, byte[0] is alwasy reset to 0.
     */
    private static ThreadLocal reentrancyStatus;

    static {
        reentrancyStatus = new ThreadLocal() {

            @Override
            protected synchronized Object initialValue() {
                return new byte[]{0};
            }
        };
    }

    /** Create a new instance of SimplePolicyProvider 
     * delegates to existing policy provider unless one
     * is not defined, in which case it trys to load 
     * default sun provider
     */
    public SimplePolicyProvider() {
        basePolicy = Policy.getPolicy();
        if (basePolicy == null) {
            basePolicy = new sun.security.provider.PolicyFile();
        }
    }

    /**
     * Evaluates the global policy and returns a
     * PermissionCollection object specifying the set of
     * permissions allowed for code from the specified
     * code source.
     *
     * @param codesource the CodeSource associated with the caller.
     * This encapsulates the original location of the code (where the code
     * came from) and the public key(s) of its signer.
     *
     * @return the set of permissions allowed for code from <i>codesource</i>
     * according to the policy.The returned set of permissions must be
     * a new mutable instance and it must support heterogeneous
     * Permission types.
     *
     */
    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
        PermissionCollection pC = basePolicy.getPermissions(codesource);
        try {
            pC = SimplePolicyConfiguration.getPermissions(pC, codesource);
        } catch (PolicyContextException pce) {
            SimplePolicyConfiguration.logGetPermissionsFailure(codesource,pce);
        }
        return pC;
    }

    /**
     * Evaluates the global policy and returns a
     * PermissionCollection object specifying the set of
     * permissions allowed given the characteristics of the
     * protection domain.
     *
     * @param domain the ProtectionDomain associated with the caller.
     *
     * @return the set of permissions allowed for the <i>domain</i>
     * according to the policy.The returned set of permissions must be
     * a new mutable instance and it must support heterogeneous
     * Permission types.
     *
     * @see java.security.ProtectionDomain
     * @see java.security.SecureClassLoader
     * @since 1.4
     */
    @Override
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        PermissionCollection pC = basePolicy.getPermissions(domain);
        try {
            pC = SimplePolicyConfiguration.getPermissions(pC, domain);
        } catch (PolicyContextException pce) {
            SimplePolicyConfiguration.logGetPermissionsFailure(domain,pce);
        }
        return pC;
    }

    /**
     * Evaluates the global policy for the permissions granted to
     * the ProtectionDomain and tests whether the permission is
     * granted.
     *
     * @param domain the ProtectionDomain to test
     * @param permission the Permission object to be tested for implication.
     *
     * @return true if "permission" is a proper subset of a permission
     * granted to this ProtectionDomain.
     *
     * @see java.security.ProtectionDomain
     * @since 1.4
     */
    @Override
    public boolean implies(ProtectionDomain domain, Permission permission) {
        byte[] alreadyCalled = (byte[]) reentrancyStatus.get();
        if (alreadyCalled[0] == 1) {
            return true;
        } else {
            alreadyCalled[0] = 1;
            try {
                return doImplies(domain, permission);
            } finally {
                alreadyCalled[0] = 0;
            }
        }
    }

    private boolean doImplies(final ProtectionDomain domain,
            final Permission permission) {
        int result = -1;
        try {
            result = SimplePolicyConfiguration.implies(domain, permission);
            if (result > 0) {
                return true;
            }
        } catch (PolicyContextException pce) {
            // the following block is included as a debugging convenience
            if (result != 0) {
                result = 1;
            }
        }
        boolean rvalue = false;
        if (result == 0) {
            rvalue = basePolicy.implies(domain, permission);
        }
        if (!rvalue) {
            SimplePolicyConfiguration.logAccessFailure(domain, permission);
        }
        return rvalue;
    }

    /**
     * Refreshes/reloads the policy configuration. The behavior of this method
     * depends on the implementation. For example, calling <code>refresh</code>
     * on a file-based policy will cause the file to be re-read.
     *
     */
    @Override
    public void refresh() {
        basePolicy.refresh();
        try {
            // will enable permission caching of container, unless REUSE 
            // property is set, and its value is not "true".
            String propValue = System.getProperty(REUSE);
            boolean supportsReuse =
                    (propValue == null ? true : Boolean.valueOf(propValue));
            if (supportsReuse) {
                if (PolicyContext.getHandlerKeys().contains(REUSE)) {
                    PolicyContext.getContext(REUSE);
                }
            }
            SimplePolicyConfiguration.refresh();
        } catch (PolicyContextException pce) {
            SimplePolicyConfiguration.logException(Level.SEVERE, "refresh.failure", pce);
            throw new IllegalStateException(pce);
        }
    }
    /*
     * NB: Excluded perms should be removed from the collections returned by 
     * getPermissions. Permissions that imply excluded permissions should
     * also be excluded. There is a potential semantic integrity issue if
     * the exluded perms have been assigned to the protection domain. The calls
     * to getPermissions and implies of SimplePolicyConfiguration remove excluded
     * permissions from the returned results.
     */
}
