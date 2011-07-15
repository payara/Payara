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

package org.glassfish.admin.amx.intf.config;

import org.glassfish.admin.amx.base.Singleton;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;

import java.util.List;
import java.util.Map;

@AMXMBeanMetadata
@Deprecated
public interface Domain
        extends Singleton, ConfigElement, PropertiesAccess, SystemPropertiesAccess {


    public Resources getResources();

    public Applications getApplications();

    public SystemApplications getSystemApplications();

    public Servers getServers();

    public Configs getConfigs();

    public Clusters getClusters();

    public NodeAgents getNodeAgents();

    public AmxPref getAmxPref();

    public String getVersion();

    public LbConfigs getLbConfigs();

    public LoadBalancers getLoadBalancers();

    public String getApplicationRoot();

    public void setApplicationRoot(String param1);

    public String getLocale();

    public void setLocale(String param1);

    public String getLogRoot();

    public void setLogRoot(String param1);

    public void setResources(Resources param1);

    public boolean isServer(String param1);

    public Map<String, ApplicationRef> getSystemApplicationsReferencedFrom(String param1);

    public Application getSystemApplicationReferencedFrom(String param1, String param2);

    public boolean isNamedSystemApplicationReferencedFrom(String param1, String param2);

    public Server getServerNamed(String param1);

    public List getAllDefinedSystemApplications();

    public Map<String, ApplicationRef> getApplicationRefsInServer(String param1);

    public ApplicationRef getApplicationRefInServer(String param1, String param2);

    public SecureAdmin getSecureAdmin();

    public void setSecureAdmin(SecureAdmin param1);

    public void setApplications(Applications param1);

    public void setSystemApplications(SystemApplications param1);

    public void setConfigs(Configs param1);

    public void setServers(Servers param1);

    public void setClusters(Clusters param1);

    public Nodes getNodes();

    public void setNodes(Nodes param1);

    public void setNodeAgents(NodeAgents param1);

    public void setLbConfigs(LbConfigs param1);

    public void setLoadBalancers(LoadBalancers param1);

    public void setAmxPref(AmxPref param1);

    public Config getConfigNamed(String param1);

    public Cluster getClusterNamed(String param1);

    public Node getNodeNamed(String param1);

    public boolean isCurrentInstanceMatchingTarget(String param1, String param2, String param3, List param4);

    public Map<String, Server> getServersInTarget(String param1);

    public Map<String, ApplicationRef> getApplicationRefsInTarget(String param1);

    public ApplicationRef getApplicationRefInTarget(String param1, String param2);

    public ApplicationRef getApplicationRefInTarget(String param1, String param2, boolean param3);

    public boolean isAppRefEnabledInTarget(String param1, String param2);

    public boolean isAppEnabledInTarget(String param1, String param2);

    public List getAllReferencedTargetsForApplication(String param1);

    public List getAllTargets();

    public Map<String, Applications> getApplicationsInTarget(String param1);

    public String getVirtualServersForApplication(String param1, String param2);

    public String getEnabledForApplication(String param1, String param2);

    public Cluster getClusterForInstance(String param1);

    public List getAllReferenceContainers();

    public Map<String, RefContainer> getReferenceContainersOf(Config param1);

    public List getInstancesOnNode(String param1);

    public Map<String, Cluster> getClustersOnNode(String param1);

}
