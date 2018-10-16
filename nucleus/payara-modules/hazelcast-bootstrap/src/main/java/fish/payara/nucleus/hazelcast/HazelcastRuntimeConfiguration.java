/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2014-2018] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.hazelcast;

import com.sun.enterprise.config.serverbeans.DomainExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

/**
 * Class to store configuration of Hazelcast
 * @author Steve Millidge (Payara Foundation)
 */
@Configured
public interface HazelcastRuntimeConfiguration 
    extends ConfigBeanProxy, DomainExtension {
    
    @Attribute(defaultValue = "hazelcast-config.xml")
    String getHazelcastConfigurationFile();
    public void setHazelcastConfigurationFile(String value);
    
    @Attribute(defaultValue = "5900")
    String getStartPort();
    public void setStartPort(String value);

    @Attribute(defaultValue = "")
    String getDASPublicAddress();
    public void setDASPublicAddress(String value);
    
    @Attribute(defaultValue = "")
    String getDASBindAddress();
    public void setDASBindAddress(String value);
    
    @Attribute(defaultValue = "4900")
    String getDasPort();
    public void setDasPort(String value);
    
    @Attribute(defaultValue = "224.2.2.3")
    String getMulticastGroup();
    public void setMulticastGroup(String value);
        
    @Attribute(defaultValue = "54327")
    String getMulticastPort();
    public void setMulticastPort(String value);
    
    @Attribute(defaultValue = "127.0.0.1:5900")
    String getTcpipMembers();
    public void setTcpipMembers(String value);
    
    @Attribute(defaultValue = "localhost:5900")
    String getDnsMembers();
    public void setDnsMembers(String value);
    
    // valid discovery modes
    // domain
    // multicast
    // tcpip
    // dns
    @Attribute(defaultValue = "domain")
    String getDiscoveryMode();
    public void setDiscoveryMode(String value);
    
    @Attribute(defaultValue = "false", dataType = Boolean.class)
    String getGenerateNames();
    public void setGenerateNames(String value);

    // can be commaseparated value
    @Attribute(defaultValue = "")
    String getInterface();
    public void setInterface(String value);
    
    @Attribute(defaultValue = "development")
    String getClusterGroupName();
    public void setClusterGroupName(String value);
    
    @Attribute(defaultValue = "D3v3l0pm3nt")
    String getClusterGroupPassword();
    public void setClusterGroupPassword(String value);

    @Attribute(defaultValue = "true", dataType = Boolean.class)
    String getHostAwarePartitioning();
    public void setHostAwarePartitioning(String value);

    @Attribute(defaultValue = "")
    String getLicenseKey();
    public void setLicenseKey(String value);

}
