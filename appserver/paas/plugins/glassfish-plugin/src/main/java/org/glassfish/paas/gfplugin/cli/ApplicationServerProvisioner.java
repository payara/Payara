/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.gfplugin.cli;


import org.glassfish.embeddable.CommandResult;
import org.jvnet.hk2.annotations.Contract;

import java.util.List;
import java.util.Properties;

/**
 * @author Jagadish Ramu
 */
@Contract
public interface ApplicationServerProvisioner extends Provisioner {

    void provisionCluster(int instancesCount, List<String> instanceIPs, String masterInstanceIP);

    void unProvisionCluster(int instancesCount, List<String> remoteInstanceIPs, String masterInstanceIP);

    int scaleUp(int count, String clusterName);

    int scaleDown(int count, String clusterName);

    void startCluster(String masterInstanceIP, String cluster);

    void stopCluster(String masterInstanceIP, String cluster);

    void createDomain(String domainName, String ipAddress, String... options);

    void deleteDomain(String domainName, String ipAddress);

    void startDomain(String ipAddress, String domainName);

    void stopDomain(String ipAddress, String domainName);

    void enableSecureAdmin(String ipAddress);

    public String provisionNode(String dasIP, String instanceIP, String clusterName, String nodeName, String instanceName);

    public void unProvisionNode(String dasIP, String instanceIP, String nodeName, String instanceName);

    void createCluster(String masterInstanceIP, String cluster);

    void deleteCluster(String masterInstanceIP, String cluster, boolean cascade);

    void createInstance(String masterInstanceIP, String cluster, String node, String instance);

    void deleteInstance(String masterInstanceIP, String instanceName);

    void startInstance(String masterInstanceIP, String instanceName);

    void stopInstance(String masterInstanceIP, String instanceName);

    CommandResult executeRemoteCommand(String command, String... options);

    boolean handles(Properties metaData);

    void initialize(Properties properties);

    String deploy(String masterInstanceIP, String appLocation, String... options);

    void createJdbcConnectionPool(String masterInstanceIP, String target, Properties props, String poolName);

    void createJdbcResource(String masterInstanceIP, String target, String poolName, String resourceName);

    void associateLBWithApplicationServer(String masterInstanceIP, String targetName, String lbIPAddress,
                                          String lbServiceName);

    void refreshLBConfiguration(String masterInstanceIP, String lbServiceName);

    void deleteJdbcResource(String masterInstanceIP, String target, String resourceName);

    void deleteJdbcConnectionPool(String masterInstanceIP, String poolName);
}
