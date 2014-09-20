/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
 * RepositoryConfig.java
 *
 * Created on August 19, 2003, 1:59 PM
 */
package com.sun.enterprise.admin.servermgmt;

import java.util.HashMap;
import java.io.File;
import java.util.Map;

import com.sun.enterprise.util.SystemPropertyConstants;

import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.universal.glassfish.ASenvPropertyReader;

/**
 * This class represents a repository configuration. A repository can be either
 * a domain, a node agent, or a server instance. Configuration specific to each
 * (DomainConfig, AgentConfig, InstanceConfig) is derived from this class. A
 * repository config consists of the following attributes:
 *
 * 1)repositoryName -- domain or node agent name (e.g. domain1 or agent1)
 *
 * 2)repositoryRoot -- the parent directory of the repository (e.g.
 * $installDir/domains or $installDir/agents)
 *
 * 3)instanceName -- the optional server instance name (e.g. server1)
 *
 * 4)configurationName -- the optional configuration name of the server instance
 * (e.g. default-config).
 *
 * Using (repositoryName, repositoryRoot, instanceName, configurationName)
 * syntax. Here are the following permutations:
 *
 * 1)For a domain: (domainRootDirectory, domainName, null, null) e.g.
 * ("/sun/appserver/domains", "domain1", null, null)
 *
 * 2)For a node agent: (agentRootDirectory, agentName, "agent", null) e.g
 * ("/sun/appserver/agents", "agent1", "agent", null). Note that the instance
 * name of a node agent is always the literal string "agent".
 *
 * 3)For a server instance (agentRootDirectory, agentName, instanceName,
 * configName) e.g. ("/sun/appserver/agents", "agent1", "server1",
 * "default-config")
 *
 * The RepositoryConfig class is an extensible HashMap that can contain any
 * attributes, but also relies on two system properties being set:
 *
 * 1)com.sun.aas.installRoot -- installation root directory stored under the
 * K_INSTALL_ROOT key.
 *
 * 2)com.sun.aas.configRoot -- configuration root (for locating asenv.conf)
 * stored under the K_CONFIG_ROOT key.
 *
 * @author kebbs
 */
public class RepositoryConfig extends HashMap<String, Object> {
    public static final String K_INSTALL_ROOT = "install.root";
    public static final String K_CONFIG_ROOT = "config.root";
    public static final String K_REFRESH_CONFIG_CONTEXT = "refresh.cc";
    //Name of the domain or node agent. Cannot be null.
    private String _repositoryName;
    //Root directory where the domain or node agent resides. Cannot be null
    private String _repositoryRoot;
    //Name of the server instance. May be null
    private String _instanceName;
    //Name of the configuration. May be null
    private String _configurationName;

    /**
     * Creates a new instance of RepositoryConfig The K_INSTALL_ROOT and
     * K_CONFIG_ROOT attributes are implicitly set
     */
    public RepositoryConfig(String repositoryName, String repositoryRoot, String instanceName,
            String configName) {
        _instanceName = instanceName;
        _repositoryName = repositoryName;
        _repositoryRoot = repositoryRoot;
        _configurationName = configName;
        final Map<String, String> envProperties = getEnvProps();
        put(K_INSTALL_ROOT, getFilePath(
                envProperties.get(SystemPropertyConstants.INSTALL_ROOT_PROPERTY)));
        //SystemPropertyConstants.INSTALL_ROOT_PROPERTY));
        put(K_CONFIG_ROOT, getFilePath(
                envProperties.get(SystemPropertyConstants.INSTALL_ROOT_PROPERTY)));
        //SystemPropertyConstants.CONFIG_ROOT_PROPERTY));
        put(K_REFRESH_CONFIG_CONTEXT, true);
        /*
         * Since the changes for the startup, we have the problem of refreshing
         * config context. So, by default, I am making a change to refresh the
         * config context. If some processes (e.g. start-domain) have already
         * created a config context, then they should explicitly say so.
         */
    }

    public RepositoryConfig(String repositoryName, String repositoryRoot, String instanceName) {
        this(repositoryName, repositoryRoot, instanceName, null);
    }

    public RepositoryConfig(String repositoryName, String repositoryRoot) {
        this(repositoryName, repositoryRoot, null);
    }

    public RepositoryConfig() {
        this(System.getProperty(SystemPropertyConstants.INSTANCE_ROOT_PROPERTY));
    }

    /**
     * Creates a new instance of RepositoryConfig defined using the system
     * property com.sun.aas.instanceRoot. It is assumed that this system
     * property is a directory of the form:
     * <repositoryRootDirectory>/<repositoryName>/<instanceName>
     */
    public RepositoryConfig(String instanceRootString) {
        final File instanceRoot = new File(instanceRootString);
        final File repositoryDir = instanceRoot.getParentFile();
        _instanceName = instanceRoot.getName();
        _repositoryName = repositoryDir.getName();
        _repositoryRoot = FileUtils.makeForwardSlashes(repositoryDir.getParentFile().getAbsolutePath());
        _configurationName = null;
        final Map<String, String> envProperties = getEnvProps();
        put(K_INSTALL_ROOT,
                envProperties.get(SystemPropertyConstants.INSTALL_ROOT_PROPERTY));
        put(K_CONFIG_ROOT,
                getFilePath(envProperties.get(SystemPropertyConstants.CONFIG_ROOT_PROPERTY)));
    }

    public String toString() {
        return ("repositoryRoot " + _repositoryRoot + " repositoryName " + _repositoryName
                + " instanceName " + _instanceName + " configurationName " + _configurationName);
    }

    protected String getFilePath(String propertyName) {
        File f = new File(propertyName);
        return FileUtils.makeForwardSlashes(f.getAbsolutePath());
    }

    public void setConfigurationName(String configurationName) {
        _configurationName = configurationName;
    }

    public String getConfigurationName() {
        return _configurationName;
    }

    public String getDisplayName() {
        return getRepositoryName();
    }

    public void setInstanceName(String instanceName) {
        _instanceName = instanceName;
    }

    public String getInstanceName() {
        return _instanceName;
    }

    public String getRepositoryName() {
        return _repositoryName;
    }

    protected void setRepositoryRoot(String repositoryRoot) {
        _repositoryRoot = repositoryRoot;
    }

    public String getRepositoryRoot() {
        return _repositoryRoot;
    }

    public String getInstallRoot() {
        return (String) get(K_INSTALL_ROOT);
    }

    public String getConfigRoot() {
        return (String) get(K_CONFIG_ROOT);
    }

    public Boolean getRefreshConfigContext() {
        return ((Boolean) get(K_REFRESH_CONFIG_CONTEXT));
        //this will never be null, because constructor initializes it to false
    }

    public void setRefreshConfingContext(final boolean refresh) {
        this.put(K_REFRESH_CONFIG_CONTEXT, refresh);
    }

    private Map<String, String> getEnvProps() {
        ASenvPropertyReader pr = new ASenvPropertyReader();
        return pr.getProps();
    }
}
