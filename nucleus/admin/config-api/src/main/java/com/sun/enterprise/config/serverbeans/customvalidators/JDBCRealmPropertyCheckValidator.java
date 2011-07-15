/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.serverbeans.customvalidators;

import com.sun.enterprise.config.serverbeans.AuthRealm;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.jvnet.hk2.config.types.Property;

/**
 *
 * @author Nandini Ektare
 */
public class JDBCRealmPropertyCheckValidator
    implements ConstraintValidator<JDBCRealmPropertyCheck, AuthRealm> {

    private static final String JDBC_REALM =
        "com.sun.enterprise.security.auth.realm.jdbc.JDBCRealm";
    private static final String DEFAULT_DIGEST_ALGORITHM = "MD5";

    public void initialize(final JDBCRealmPropertyCheck fqcn) {
    }

    public boolean isValid(final AuthRealm realm,
        final ConstraintValidatorContext constraintValidatorContext) {

        if (realm.getClassname().equals(JDBC_REALM)) {
            Property jaas_context = realm.getProperty("jaas-context");
            Property ds_jndi = realm.getProperty("datasource-jndi");
            Property user_table = realm.getProperty("user-table");
            Property group_table = realm.getProperty("group-table");
            Property user_name_col = realm.getProperty("user-name-column");
            Property passwd_col = realm.getProperty("password-column");
            Property grp_name_col = realm.getProperty("group-name-column");
            Property digest_algo = realm.getProperty("digest-algorithm");

            if ((jaas_context == null) || (ds_jndi == null) ||
                (user_table == null) || (group_table == null) ||
                (user_name_col == null) || (passwd_col == null) ||
                (grp_name_col == null)) {
                
                return false;
            }
            
            if (digest_algo != null) {
                String algoName = digest_algo.getValue();

                if (!("none".equalsIgnoreCase(algoName))) {
                    try {
                        MessageDigest.getInstance(algoName);
                    } catch(NoSuchAlgorithmException e) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}





