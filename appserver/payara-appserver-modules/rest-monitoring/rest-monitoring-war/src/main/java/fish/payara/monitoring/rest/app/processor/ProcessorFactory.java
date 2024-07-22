/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.monitoring.rest.app.processor;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;

/**
 *
 * @author Fraser Savage
 */
// @PROPOSED - FANG-2: Flesh out flexible converter system.
public final class ProcessorFactory {

    /**
     * Returns a {@link TypeProcessor} that is best able to process an MBean attribute.
     * The processibleObject argument should be an MBean attribute.
     * 
     * @param processibleObject The {@link Object} to get the {@link TypeProcessor} for.
     * @return The appropriate {@link TypeProcessor} for the processibleObject given.
     */
    public static TypeProcessor getTypeProcessor(Object processibleObject) {
        if (isSimpleType(processibleObject)) {
            return new SimpleTypeProcessor();
        } else if (isArrayType(processibleObject)) {
            return new ArrayTypeProcessor();
        } else if (isCompositeType(processibleObject)) {
            return new CompositeTypeProcessor();
        } else if (isTabularType(processibleObject)) {
            return new TabularTypeProcessor();
        } else {
            return new OtherTypeProcessor();
        }
    }

    // Checks if the object passed is a OpenMBean SimpleType 
    private static boolean isSimpleType(Object object) {
        if (SimpleType.BIGDECIMAL.isValue(object)) {
            return true;
        } else if (SimpleType.BIGINTEGER.isValue(object)) {
            return true;
        } else if (SimpleType.BOOLEAN.isValue(object)) {
            return true;
        } else if (SimpleType.BYTE.isValue(object)) {
            return true;
        } else if (SimpleType.CHARACTER.isValue(object)) {
            return true;
        } else if (SimpleType.DATE.isValue(object)) {
            return true;
        } else if (SimpleType.DOUBLE.isValue(object)) {
            return true;
        } else if (SimpleType.FLOAT.isValue(object)) {
            return true;
        } else if (SimpleType.INTEGER.isValue(object)) {
            return true;
        } else if (SimpleType.LONG.isValue(object)) {
            return true;
        } else if (SimpleType.OBJECTNAME.isValue(object)) {
            return true;
        } else if (SimpleType.SHORT.isValue(object)) {
            return true;
        } else if (SimpleType.STRING.isValue(object)) {
            return true;
        } else {
            return SimpleType.VOID.isValue(object);
        }
    }
    
    // Checks if the object is an array type
    private static boolean isArrayType(Object object) {
        return object.getClass().isArray();
    }

    // Checks if the object is a composite type 
    private static boolean isCompositeType(Object object) {
        return (CompositeData.class.isAssignableFrom(object.getClass())
                || CompositeType.class.isAssignableFrom(object.getClass()));    
    }

    // Checks if the object is a tabular type
    private static boolean isTabularType(Object object) {
        return (TabularData.class.isAssignableFrom(object.getClass()) 
                || TabularType.class.isAssignableFrom(object.getClass()));
    }

}
