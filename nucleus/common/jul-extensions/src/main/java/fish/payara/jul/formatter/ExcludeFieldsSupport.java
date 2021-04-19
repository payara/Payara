/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright [2020-2021] [Payara Foundation and/or affiliates]

package fish.payara.jul.formatter;

import fish.payara.jul.tracing.PayaraLoggingTracer;

import java.util.BitSet;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static fish.payara.jul.formatter.ExcludeFieldsSupport.SupplementalAttribute.values;
import static java.util.Arrays.stream;

/**
 * @author sanshriv
 * @author David Matejcek
 */
public class ExcludeFieldsSupport {

    /**
     * Additional log record attributes.
     */
    public enum SupplementalAttribute {
        /** Thread id and name */
        TID("tid"),
        /** Milliseconds since 1970 */
        TIME_MILLIS("timeMillis"),
        /** Integer value of the log level. See {@link Level#intValue()} */
        LEVEL_VALUE("levelValue"),
        ;

        private final String id;

        SupplementalAttribute(final String id) {
            this.id = id;
        }


        /**
         * @return name of the attribute in logging.properties
         */
        public String getId() {
            return this.id;
        }
    }

    private final BitSet excludedAttributes = new BitSet(values().length);

    /**
     * @param id
     * @return {@link SupplementalAttribute} if such exists with the same id.
     */
    public static SupplementalAttribute getById(final String id) {
        for (SupplementalAttribute value : values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }


    /**
     * @param excludeFields comma-separated list of {@link SupplementalAttribute} names.
     */
    public void setExcludedFields(final String excludeFields) {
        excludedAttributes.clear();
        if (excludeFields == null || excludeFields.isEmpty()) {
            return;
        }
        final String[] fields = excludeFields.split(",");
        for (final String field : fields) {
            final SupplementalAttribute found = getById(field);
            if (found == null) {
                PayaraLoggingTracer.error(getClass(), "Ignoring excluded field because no such exists: " + field);
            } else {
                excludedAttributes.set(found.ordinal());
            }
        }
    }


    /**
     * @param attribute
     * @return true if the attribute should be excluded.
     */
    public boolean isSet(final SupplementalAttribute attribute) {
        return excludedAttributes.get(attribute.ordinal());
    }


    /**
     * Returns excluded field identificators, separated by comma.
     */
    @Override
    public String toString() {
        return stream(values()).filter(this::isSet).map(SupplementalAttribute::getId).collect(Collectors.joining(","));
    }
}
