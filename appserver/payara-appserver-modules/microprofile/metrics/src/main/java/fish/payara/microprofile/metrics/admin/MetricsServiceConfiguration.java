/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018-2019] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.metrics.admin;

import static fish.payara.microprofile.Constants.DEFAULT_GROUP_NAME;
import java.beans.PropertyVetoException;
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

/**
 * Configuration for the Metrics Service.
 *
 * @author Gaurav Gupta
 */
@Configured(name = "microprofile-metrics-configuration")
public interface MetricsServiceConfiguration extends ConfigBeanProxy, ConfigExtension {

    /**
     * @return a Boolean value determining if the service is enabled or
     * disabled.
     */
    @Attribute(defaultValue = "true", dataType = Boolean.class)
    String getEnabled();
    void setEnabled(String value) throws PropertyVetoException;

    /**
     * @return a Boolean value determining if the metrics service is secure or
     * not.
     */
    @Deprecated
    @Attribute(defaultValue = "false", dataType = Boolean.class)
    String getSecureMetrics();
    @Deprecated
    void setSecureMetrics(String value) throws PropertyVetoException;
    
    /**
     * @return a Boolean value determining if the service is dynamic or not.
     */
    @Attribute(defaultValue = "true", dataType = Boolean.class)
    String getDynamic();
    void setDynamic(String value) throws PropertyVetoException;

    /**
     * @return a String value defines the endpoint of metrics service.
     */
    @Attribute(defaultValue = "metrics")
    String getEndpoint();
    void setEndpoint(String value) throws PropertyVetoException;

    /**
     * @return a String value defines the attached virtual servers.
     */
    @Attribute(defaultValue = "", dataType = String.class)
    String getVirtualServers();
    void setVirtualServers(String value) throws PropertyVetoException;

    /**
     * @return a Boolean value determining if the security is enabled or not.
     */
    @Attribute(defaultValue = "false", dataType = Boolean.class)
    String getSecurityEnabled();
    void setSecurityEnabled(String value) throws PropertyVetoException;

    /**
     * @return a String value defines the roles.
     */
    @Attribute(defaultValue = DEFAULT_GROUP_NAME, dataType = String.class)
    String getRoles();
    void setRoles(String value) throws PropertyVetoException;

}
