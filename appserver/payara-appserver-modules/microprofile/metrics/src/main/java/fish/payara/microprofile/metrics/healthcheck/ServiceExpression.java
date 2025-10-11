/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2022] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.microprofile.metrics.healthcheck;

import static fish.payara.microprofile.metrics.jmx.MetricsMetadataHelper.ATTRIBUTE_SEPARATOR;
import static fish.payara.microprofile.metrics.jmx.MetricsMetadataHelper.SUB_ATTRIBUTE_SEPARATOR;

public class ServiceExpression {

    private String service;

    private String attributeName;
    private String subAttributeName;


    public ServiceExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Service Expression is null");
        }
        int slashIndex = expression.lastIndexOf(ATTRIBUTE_SEPARATOR);
        if (slashIndex >= 0) {
            service = expression.substring(0, slashIndex);
            attributeName = expression.substring(slashIndex + 1);
            if (attributeName.contains(SUB_ATTRIBUTE_SEPARATOR)) {
                int hashIndex = attributeName.indexOf(SUB_ATTRIBUTE_SEPARATOR);
                subAttributeName = attributeName.substring(hashIndex + 1);
                attributeName = attributeName.substring(0, hashIndex);
            }
        } else {
            slashIndex = expression.lastIndexOf(SUB_ATTRIBUTE_SEPARATOR);
            if (slashIndex < 0) {
                throw new IllegalArgumentException("Service Expression is invalid : " + expression);
            }
            service = expression.substring(0, slashIndex);
            subAttributeName = expression.substring(slashIndex + 1);
        }
    }

    public String getServiceId() {
        return service;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getSubAttributeName() {
        return subAttributeName;
    }

}
