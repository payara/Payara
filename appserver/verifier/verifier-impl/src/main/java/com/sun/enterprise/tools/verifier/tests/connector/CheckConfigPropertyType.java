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

/*
 * CheckConfigProperyType.java
 *
 * Created on October 2, 2000, 3:25 PM
 */

package com.sun.enterprise.tools.verifier.tests.connector;

import java.util.*;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.tools.verifier.tests.*;
import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.ConnectionDefDescriptor;
import com.sun.enterprise.tools.verifier.Result;
/**
 * Properties names defined in the resource adapter config-propery should
 * be of an acceptable type
 *
 * @author  Jerome Dochez
 * @version
 */
public class CheckConfigPropertyType extends ConnectorTest implements ConnectorCheck {

    /**
     * Property allowed type
     */
    private static Class[] allowedTypes = {
					java.lang.String.class,
					java.lang.Boolean.class,
					java.lang.Integer.class,
					java.lang.Double.class,
					java.lang.Byte.class,
					java.lang.Short.class,
					java.lang.Long.class,
					java.lang.Float.class,
					java.lang.Character.class,
					    };

    /** <p>
     * Properties names defined in the resource adapter config-propery should
     * be of an acceptable type
     * </p>
     *
     * @paramm descriptor deployment descriptor for the rar file
     * @return result object containing the result of the individual test
     * performed
     */
    public Result check(ConnectorDescriptor descriptor) {

        boolean oneFailed = false;

        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        //Set properties = descriptor.getConfigProperties();
        ConnectionDefDescriptor desc = descriptor.getConnectionDefinitionByCFType(null, true);
        Set properties = desc.getConfigProperties();
        if (properties.size()!=0) {
            Iterator iterator = properties.iterator();
            // let's add the propery name
            // HashSet hs = new HashSet();
            while (iterator.hasNext()) {
                EnvironmentProperty ep = (EnvironmentProperty) iterator.next();
                String type = ep.getType();
                if (type == null) {
                    result.addErrorDetails(smh.getLocalString
                            ("tests.componentNameConstructor",
                                    "For [ {0} ]",
                                    new Object[] {compName.toString()}));
                    result.failed(smh.getLocalString(getClass().getName() + ".notdefined",
                            "Error: The configuration property named [ {0} ] has no type ",
                            new Object[] {ep.getName()}));
                    return result;
                }
                Class typeClass = null;
                // is it loadable ?
                try {
                    typeClass = Class.forName(type);
                } catch (Throwable t) {
                    result.addErrorDetails(smh.getLocalString
                            ("tests.componentNameConstructor",
                                    "For [ {0} ]",
                                    new Object[] {compName.toString()}));
                    result.failed(smh.getLocalString(getClass().getName() + ".nonexist",
                            "Error: The type [ {0} ] of the configuration property named [ {1} ] cannot be loaded",
                            new Object[] {ep.getType(), ep.getName()}));
                    return result;
                }
                boolean allowedType = false;
                for (int i = 0; i < allowedTypes.length; i++) {
                    if (allowedTypes[i].equals(typeClass)) {
                        allowedType = true;
                        break;
                    }
                }
                if (!allowedType) {
                    oneFailed = true;
                    result.addErrorDetails(smh.getLocalString
                            ("tests.componentNameConstructor",
                                    "For [ {0} ]",
                                    new Object[] {compName.toString()}));
                    result.failed(smh.getLocalString(getClass().getName() + ".failed",
                            "Error: The type [ {0} ] for the configuration property named [ {1} ] is not allowed",
                            new Object[] {ep.getType(), ep.getName()}));
                    return result;
                }
            }
            // for failure, result has been set before
            if (!oneFailed) {
                result.addGoodDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
                                "For [ {0} ]",
                                new Object[] {compName.toString()}));
                result.passed(smh.getLocalString(getClass().getName() + ".passed",
                        "Success: all properties have an allowed type"));

            }
        } else {
            result.addNaDetails(smh.getLocalString
                    ("tests.componentNameConstructor",
                            "For [ {0} ]",
                            new Object[] {compName.toString()}));
            result.notApplicable(smh.getLocalString(getClass().getName() + ".notApplicable",
                    "Not Applicable: There are no config-property element defined" ));

        }
        return result;
    }
}
