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

import fish.payara.nucleus.healthcheck.HealthCheckStatsProvider;
import java.util.Set;
import org.eclipse.microprofile.metrics.Counter;

/**
 * Implementation of a counter based off an HealthCheck. As this is just a proxy
 * for the HealthCheck calling the {@link #inc() } method will throw an
 * {@link UnsupportedOperationException}. Just use the {@link #getCount()}
 * method to get the value of the HealthCheck backing this.
 */
public class HealthCheckCounter implements Counter, HealthCheckStatsProvider {

    private final HealthCheckStatsProvider healthCheck;
    
    private final ServiceExpression expression;

    public HealthCheckCounter(HealthCheckStatsProvider healthCheck, ServiceExpression expression) {
        this.healthCheck = healthCheck;
        this.expression = expression;
    }

    /**
     * Throws {@link UnsupportedOperationException} - this is all dealt with by
     * the backing HealthCheck
     */
    @Override
    public void inc() {
        throw new UnsupportedOperationException("Not supported - use getCount() to get value from HealthCheck directly.");
    }

    /**
     * Throws {@link UnsupportedOperationException} - this is all dealt with by
     * the backing HealthCheck
     * @param n the increment value
     */
    @Override
    public void inc(long n) {
        throw new UnsupportedOperationException("Not supported - use getCount() to get value from HealthCheck directly.");
    }

    @Override
    public long getCount() {
        return getValue(Long.class, expression.getAttributeName(), expression.getSubAttributeName());
    }

    @Override
    public boolean isEnabled() {
       return healthCheck.isEnabled();
    }

    @Override
    public <T> T getValue(Class<T> type, String attribute, String subAttribute) {
        return healthCheck.getValue(type, attribute, subAttribute);
    }

    @Override
    public Set<String> getAttributes() {
        return healthCheck.getAttributes();
    }

    @Override
    public Set<String> getSubAttributes() {
        return healthCheck.getSubAttributes();
    }
}
