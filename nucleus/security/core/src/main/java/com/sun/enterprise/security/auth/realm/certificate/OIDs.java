/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

package com.sun.enterprise.security.auth.realm.certificate;

/**
 * Class that contains the OID constants of various DN attributes
 */
public class OIDs {

    public static final String UID = "0.9.2342.19200300.100.1.1";   // User ID
    public static final String DC = "0.9.2342.19200300.100.1.25";   // Domain Component
    public static final String EMAILADDRESS = "1.2.840.113549.1.9.1";
    public static final String IP = "1.3.6.1.4.1.42.2.11.2.1";  // IP Address
    public static final String CN = "2.5.4.3";  // Common Name
    public static final String SURNAME = "2.5.4.4";
    public static final String SERIALNUMBER = "2.5.4.5";
    public static final String C = "2.5.4.6";   // Country
    public static final String L = "2.5.4.7";   // Locality
    public static final String ST = "2.5.4.8";   // State
    public static final String STREET = "2.5.4.9";
    public static final String O = "2.5.4.10";  // Organisation
    public static final String OU = "2.5.4.11"; // Organisation Unit
    public static final String T = "2.5.4.12";  // Title
    public static final String GIVENNAME = "2.5.4.42";
    public static final String INITIALS = "2.5.4.43";
    public static final String GENERATION = "2.5.4.44";
    public static final String DNQUALIFIER = "2.5.4.46";

}
