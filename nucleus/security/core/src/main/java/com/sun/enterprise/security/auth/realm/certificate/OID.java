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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;

/**
 * Common object identifiers.
 *
 * @see sun.security.x509.AVAKeyword AVAKeyword contains similar list
 * @author David Matejcek
 */
public enum OID {

    /** Common Name */
    CN("2.5.4.3"),
    /** Surname */
    SURNAME("2.5.4.4"),
    /** Serial Number of the certificate */
    SERIALNUMBER("2.5.4.5"),
    /** Country */
    C("2.5.4.6"),
    /** Locality */
    L("2.5.4.7"),
    /** State */
    ST("2.5.4.8"),
    /** Street */
    STREET("2.5.4.9"),
    /** Organisation */
    O("2.5.4.10"),
    /** Organisation Unit */
    OU("2.5.4.11"),
    /** Title */
    T("2.5.4.12"),
    /** Given Name */
    GIVENNAME("2.5.4.42"),
    /** Initials */
    INITIALS("2.5.4.43"),
    /** Generation */
    GENERATION("2.5.4.44"),
    /** DN Qualifier */
    DNQUALIFIER("2.5.4.46"),

    /** User ID */
    UID("0.9.2342.19200300.100.1.1"),
    /** Domain Component */
    DC("0.9.2342.19200300.100.1.25"),
    /** E-Mail address */
    EMAILADDRESS("1.2.840.113549.1.9.1"),
    /** IP Address */
    IP("1.3.6.1.4.1.42.2.11.2.1"), //
    ;

    private static final Map<String, String> OID_TO_NAME;
    static {
        OID_TO_NAME = unmodifiableMap(Arrays.stream(values()).collect(toMap(OID::getObjectId, OID::name)));
    }

    private String objectId;


    private OID(String objectId) {
        this.objectId = objectId;
    }


    /**
     * @return object ID defined in RFC
     */
    public String getObjectId() {
        return this.objectId;
    }


    /**
     * @return name of the OID
     */
    public String getName() {
        return name();
    }


    /**
     * @return unmodifiable map of object identifiers and their names
     */
    public static Map<String, String> getOIDMap() {
        return OID_TO_NAME;
    }


    /**
     * @param objectId can be null
     * @return OID with the same objectId or null
     */
    public static OID getByObjectId(final String objectId) {
        return getByGetter(OID::getObjectId, objectId);
    }


    /**
     * @param name case insensitive name, the parameter can be null
     * @return OID with the same name or null
     */
    public static OID getByName(final String name) {
        return getByGetter(OID::getName, name);
    }


    /**
     * Checks for given names and returns a set of OIDs with same names.
     *
     * @param names case insensitive OID names.
     * @return a set of found names
     */
    public static Set<OID> toOIDS(String[] names) {
        final Predicate<OID> filter = o -> o != null;
        return Arrays.stream(names).map(OID::getByName).filter(filter).collect(Collectors.toSet());
    }


    private static OID getByGetter(final Function<OID, String> getter, final String value) {
        for (OID oid : values()) {
            if (getter.apply(oid).equalsIgnoreCase(value)) {
                return oid;
            }
        }
        return null;
    }
}
