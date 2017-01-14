/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.micro.data;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.URL;
import java.util.Collection;
import java.util.List;

/**
 * Class describing an instance of Payara
 * @author Steve Millidge
 */
public interface InstanceDescriptor extends Serializable {

    /**
     * Overrides equals purely based on the UUID value
     *
     * @param obj
     * @return
     */
    public boolean equals(Object obj);

    /**
     * Gets the admin port number for this instance descriptor
     *
     * @return the admin port number in use by this instance
     */
    public int getAdminPort();

    public List<URL> getApplicationURLS();

    /**
     * @return the deployedApplications
     */
    public Collection<ApplicationDescriptor> getDeployedApplications();

    /**
     * Gets the Hazelcast port number of this instance descriptor
     *
     * @return the port number in use by Hazelcast
     */
    public int getHazelcastPort();

    /**
     * @return the hostName
     */
    public InetAddress getHostName();

    /**
     * @return the httpPorts
     */
    public List<Integer> getHttpPorts();

    /**
     * @return the httpsPorts
     */
    public List<Integer> getHttpsPorts();

    public String getInstanceName();

    /**
     * Gets the instance type that this descriptor describes
     *
     * @return the instance type that this descriptor describes
     */
    public String getInstanceType();

    /**
     * @return the memberUUID
     */
    public String getMemberUUID();

    /**
     * Checks whether or not this instance is described as a Lite Hazelcast
     * member
     *
     * @return true if this instance describes a Hazelcast Lite member
     */
    public boolean isLiteMember();

    /**
     * Checks whether or not this descriptor describes a Payara Micro instance
     *
     * @return true if this descriptor describes a Payara Micro instances
     */
    public boolean isMicroInstance();

    /**
     * Checks whether or not this descriptor describes a Payara Server instance
     * or the DAS
     *
     * @return true if this descriptor describes a Payara Server instance or the
     * DAS
     */
    public boolean isPayaraInstance();


    /**
     * Returns the instance group name of the instance
     * @return 
     */
    public String getInstanceGroup();

}
