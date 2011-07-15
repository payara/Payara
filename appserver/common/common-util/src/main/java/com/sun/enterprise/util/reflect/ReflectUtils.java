/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.util.reflect;

import java.lang.reflect.Method;

/**
 * Place to put utility methods that the JVM doesn't supply
 * @author Byron Nevins
 */
public final class ReflectUtils {
    /**
     *
     * @param m1 Method to compare
     * @param m2 Method to compare
     * @return null if they have the same signature.  A String describing the differences
     * if they have different signatures.
     */
    public static String equalSignatures(Method m1, Method m2) {
        StringBuilder sb = new StringBuilder();

        if (!m1.getReturnType().equals(m2.getReturnType())) {
            sb.append(Strings.get("return_type_mismatch", m1.getReturnType(), m2.getReturnType()));
            sb.append("  ");
        }

        Class<?>[] types1 = m1.getParameterTypes();
        Class<?>[] types2 = m2.getParameterTypes();

        if (types1.length != types2.length) {
            sb.append(Strings.get("parameter_number_mismatch", types1.length, types2.length));
        }
        else { // don't want to go in here if the lengths don't match!!
            for (int i = 0; i < types1.length; i++) {
                if (!types1[i].equals(types2[i])) {
                    sb.append(Strings.get("parameter_type_mismatch", i, types1[i], types2[i]));
                    sb.append("  ");
                }
            }
        }

        if (sb.length() == 0)
            return null;

        sb.append('\n').append(m1.toGenericString());
        sb.append('\n').append(m2.toGenericString());
        return sb.toString();
    }
}
