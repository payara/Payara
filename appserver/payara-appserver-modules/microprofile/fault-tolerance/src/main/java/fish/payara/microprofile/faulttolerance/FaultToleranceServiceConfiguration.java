/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017-2020] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.microprofile.faulttolerance;

import javax.validation.constraints.Min;

import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;

/**
 * Configuration for the Fault Tolerance Service.
 *
 * @author Andrew Pielage (initial)
 * @author Jan Bernitt (change to pool size)
 */
@Configured(name = "microprofile-fault-tolerance-configuration")
public interface FaultToleranceServiceConfiguration extends ConfigExtension {

    /**
     * @return The maximum number of threads used to run asynchronous methods concurrently. This is the upper limit. The
     *         executor will vary the actual pool size depending on demand up to this upper limit. If no demand exist
     *         the actual pool size is zero.
     */
    @Attribute(defaultValue = "1000", dataType = Integer.class)
    @Min(value = 20)
    String getAsyncMaxPoolSize();
    void setAsyncMaxPoolSize(String asyncMaxPoolSize);

    /**
     * @return The maximum number of threads used to schedule delayed execution and detect timeouts processing FT
     *         semantics. This should be understood as upper limit. The implementation might choose to keep up to this
     *         number of threads alive or vary the actual pool size according to demands.
     */
    @Attribute(defaultValue = "20", dataType = Integer.class)
    @Min(value = 1)
    public String getDelayMaxPoolSize();
    public void setDelayMaxPoolSize(String delayMaxPoolSize);
}
