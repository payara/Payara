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

package com.sun.enterprise.util.net;

public class URLPattern {
    
    // In Ascii table, New Line (NL) decimal value is 10
    private final static int NL = 10;
    // In Ascii table, Carriage Return (CR) decimal value is 13
    private final static int CR = 13;

    /**
     *  This method is used to check the validity of url pattern 
     *  according to the spec. It is used in the following places:
     *
     *  1. in WebResourceCollection 
     *  2. in ServletMapping
     *  3. in ServletFilterMapping
     *  (above three see Servlet Spec, from version 2.3 on, 
     *  Secion 13.2: "Rules for Processing the Deployment Descriptor")
     *
     *  4. in jsp-property-group 
     *  (see JSP.3.3: "JSP Property Groups")
     *
     *  @param urlPattern the url pattern
     *  @return false for invalid url pattern
     */
    public static boolean isValid(String urlPattern) {
        // URL Pattern should not contain New Line (NL) or
        // Carriage Return (CR)
        if (containsCRorLF(urlPattern)) {
            return false;
        }

        // Check validity for extension mapping
        if (urlPattern.startsWith("*.")) {
            if (urlPattern.indexOf('/') < 0) {
                return true;
            } else {
                return false;
            }
        }

        // check validity for path mapping
        if (urlPattern.isEmpty()) {
            return true;
        } else if (urlPattern.startsWith("/") &&
                urlPattern.indexOf("*.") < 0) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * This method is used to check whether a url pattern contains a CR(#xD) or
     * LF (#xA). According to the Servlet spec the developer must be informed
     * when it does.
     * 
     * @param urlPattern
     *            the url pattern (must not be null)
     * @return true if it contains one or more CRs or LFs
     */
    public static boolean containsCRorLF(String urlPattern) {
        return (urlPattern.indexOf(NL) != -1  || urlPattern.indexOf (CR) != -1);
    }
}
