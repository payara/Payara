/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */

package fish.payara.jul.cfg;


/**
 * Representation of a key in logging.properties file.
 * <p>
 * The representation is basically relative to some bussiness object of the JUL system..
 *
 * @author David Matejcek
 */
@FunctionalInterface
public interface LogProperty {
    // note: usual usage is implementing by an enum, so don't shorten getter names,
    // it would be confusing with enum.name().

    /**
     * @return a name of the property, used as a last part of property name in logging.properties
     */
    String getPropertyName();


    /**
     * Concatenates the {@link Class#getName()} with a dot and {@link #getPropertyName()}
     *
     * @param bussinessObjectClass
     * @return complete name of the property, used to access the value.
     */
    default String getPropertyFullName(final Class<?> bussinessObjectClass) {
        return getPropertyFullName(bussinessObjectClass.getName());
    }


    /**
     * Concatenates the prefix with a dot and {@link #getPropertyName()}.
     * If the prefix is null or an empty String, returns just {@link #getPropertyName()} without
     * the dot.
     *
     * @param prefix
     * @return complete name of the property, used to access the value.
     */
    default String getPropertyFullName(final String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return getPropertyName();
        }
        return prefix + "." + getPropertyName();
    }
}
