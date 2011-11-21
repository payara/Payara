/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import org.glassfish.api.Startup;
import org.glassfish.config.support.PropertyResolver;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.Singleton;

import javax.management.InstanceNotFoundException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

import org.glassfish.api.admin.ServerEnvironment;

@Service
@Scoped(Singleton.class)
public class MbeanService implements Startup {

    @Inject
    private Domain domain;

    @Inject
    private Target tgt;

    @Inject
    private static Habitat habitat;

    @Inject
    private static ServerEnvironment env;

    @Override
    public Lifecycle getLifecycle() {
        return Startup.Lifecycle.SERVER;
    }

    public static MbeanService getInstance() {
        if (habitat == null)
            return null;
        return habitat.getComponent(MbeanService.class);
    }

    public String getHost(String instance) throws InstanceNotFoundException {
        Server s = domain.getServerNamed(instance);
        if (s == null)
            throw new InstanceNotFoundException();
        return s.getAdminHost();
    }

    public String getJMXPort(String instance) throws InstanceNotFoundException {
        Server s = domain.getServerNamed(instance);
        if (s == null)
            throw new InstanceNotFoundException();
        return new PropertyResolver(domain, instance).getPropertyValue("JMX_SYSTEM_CONNECTOR_PORT");
    }

    public boolean isDas() {
        return tgt.isThisDAS();
    }

    public boolean isValidServer(String name) {
        Server s = null;
        try {
            s = domain.getServerNamed(name);
        } catch (Throwable t) {
            return false;
        }
        return (s == null) ? false : true;
    }

    public List<String> getAllInstances() {
        return convertList(tgt.getAllInstances());
    }

    public List<String> getInstances(String name) {
        return convertList(tgt.getInstances(name));
    }

    private List<String> convertList(List<Server> servers) {
        List<String> serverStrings = new ArrayList<String>();
        for (Server svr : servers)
            serverStrings.add(svr.getName());
        return serverStrings;
    }

    public boolean isInstance(String name) {
        return env.getInstanceName().equals(name);
    }

    /**
     * Returns if the SystemJMXConnector is secure or not
     *
     * @param instance
     * @return
     */
    public boolean isSecureJMX(String instance) {
        String isSecure = "false";
        if (domain.getServerNamed(instance) != null) {
            if (domain.getServerNamed(instance).getConfig().getAdminService().getSystemJmxConnector() != null) {
                isSecure = domain.getServerNamed(instance).getConfig().getAdminService().getSystemJmxConnector().getSecurityEnabled();
            }
        }
        return Boolean.parseBoolean(isSecure);

    }
}
