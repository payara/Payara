/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.amx.intf.config;

import org.glassfish.admin.amx.base.DomainRoot;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;

/**
 * Getters for common high-level config items.
 *
 * @author llc
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
public final class AMXConfigGetters {
    private final DomainRoot mDomainRoot;
    private final Domain mDomainConfig;

    /**
     * Pass any AMXProxy to initialize.
     *
     * @param amx any AMXProxy
     */
    public AMXConfigGetters(final AMXProxy amx) {
        mDomainRoot = amx.extra().proxyFactory().getDomainRootProxy();
        mDomainConfig = mDomainRoot.child(Domain.class);
    }

    public DomainRoot domainRoot() {
        return mDomainRoot;
    }

    public Domain domainConfig() {
        return mDomainConfig;
    }

    public Resources resources() {
        return domainConfig().getResources();
    }

    public Applications applications() {
        return domainConfig().getApplications();
    }

    public SystemApplications systemApplications() {
        return domainConfig().getSystemApplications();
    }


    /**
     * Get any Resource by name.
     *
     * @param name  name of the resource
     * @param clazz interface to be applied to the resource
     */
    public <T extends Resource> T
    getResource(final String name, final Class<T> clazz) {
        for (final AMXProxy child : resources().childrenSet()) {
            if (child.getName().equals(name)) {
                return child.as(clazz);
            }
        }
        return null;
    }

    public Resource getResource(final String name) {
        return getResource(name, Resource.class);
    }

    public Server getServer(final String name) {
        return child(domainConfig().getServers(), Server.class, name);
    }


    /**
     * Get any {@link Application} by name.  Looks under Applications (first) and SystemApplications.
     *
     * @param name  name of the resource
     * @param clazz interface to be applied to the resource
     */
    public Application
    getApplication(final String name) {
        Application appConfig = applications().childrenMap(Application.class).get(name);
        if (appConfig == null) {
            appConfig = systemApplications().childrenMap(Application.class).get(name);
        }
        return appConfig;
    }

    /**
     * Get a named child of the specified interface.
     */
    public <T extends AMXProxy> T child(final AMXProxy parent, final Class<T> intf, final String name) {
        return parent.childrenMap(intf).get(name);
    }

    public Config getConfig(final String name) {
        return child(domainConfig().getConfigs(), Config.class, name);
    }
}












