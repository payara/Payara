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

package org.glassfish.appclient.client.acc.config.util;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.glassfish.appclient.client.acc.config.Property;

/**
 * Logic used during JAXB conversions between XML and objects.
 *
 * @author tjquinn
 */
public class XML {

    private static final List<String> booleanTrueValues = Arrays.asList("yes", "on", "1", "true");

    private static final List<String> providerTypeValues = Arrays.asList("client", "server", "client-server");

    public static boolean parseBoolean(final String booleanText) {
        return _parseBoolean(booleanText.trim());
    }

    private static boolean isWhiteSpace(char ch) {
        return ch==' ' || ch == '\t';
    }
    /**
     *
     * <code><!ENTITY % boolean "(yes | no | on | off | 1 | 0 | true | false)"></code>
     *
     * @param literal
     * @return
     */
    private static boolean _parseBoolean(final CharSequence literal) {
        int i=0;
        int len = literal.length();
        char ch;
        do {
            ch = literal.charAt(i++);
        } while(isWhiteSpace(ch) && i<len);

        // if we are strict about errors, check i==len. and report an error

        return booleanTrueValues.contains(literal.subSequence(i, len));
    }

    public static String parseProviderType(String providerType) {
        if (providerTypeValues.contains(providerType)) {
            return providerType;
        }
        throw new IllegalArgumentException(providerType);
    }

    /**
     * Converts the XML property elements (with name and value) to a Properties
     * object.
     *
     * @param props List of Property elements from the JAXB-converted
     * client container element
     * @return corresponding Properties object
     */
    public static Properties toProperties(final List<Property> props) {
        Properties result = new Properties();
        for (Property p : props) {
            result.setProperty(p.getName(), p.getValue());
        }
        return result;
    }

    public static class Password {
        private char[] pw;

        private Password(String s) {
            pw = s.toCharArray();
        }

        public Password(char[] pw) {
            this.pw = pw;
        }
        
        public static Password parse(String s) {
            return new Password(s);
        }

        public static String print(Password p) {
            return new String(p.pw);
        }

        public char[] get() {
            return pw;
        }
    }
}
