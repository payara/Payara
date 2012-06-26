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

package com.sun.enterprise.connectors;

import com.sun.enterprise.config.serverbeans.BindableResource;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.ResourcePool;
import org.glassfish.connectors.config.*;
import org.glassfish.connectors.config.ConnectorConnectionPool;

public class DeferredResourceConfig {


    private String rarName;
    private AdminObjectResource adminObject;
    private ResourcePool resourcePool;
    private BindableResource bindableResource;
    private ResourceAdapterConfig[] resourceAdapterConfig;
    private Resource[] resourcesToLoad;


    public DeferredResourceConfig() {

    }

    public DeferredResourceConfig(
            String rarName,
            AdminObjectResource adminObject,
            ResourcePool resourcePool,
            BindableResource bindableResource,
            ResourceAdapterConfig[] resAdapterConfig) {

        this.rarName = rarName;
        this.adminObject = adminObject;
        this.resourcePool = resourcePool;
        this.bindableResource = bindableResource;
        this.resourceAdapterConfig = resAdapterConfig;

    }

    public void setRarName(String rarName) {
        this.rarName = rarName;
    }

    public String getRarName() {
        return this.rarName;
    }

    public void setAdminObject(AdminObjectResource adminObject) {
        this.adminObject = adminObject;
    }

    public AdminObjectResource getAdminObject() {
        return this.adminObject;
    }

    public void setResourcePool(
            ResourcePool resourcePool) {
        this.resourcePool = resourcePool;
    }

    public ResourcePool getResourcePool() {
        return this.resourcePool;
    }

    public void setBindableResource(BindableResource bindableResource) {
        this.bindableResource = bindableResource;
    }

    public BindableResource getBindableResource() {
        return this.bindableResource;
    }

    public void setResourceAdapterConfig(
            ResourceAdapterConfig[] resourceAdapterConfig) {
        this.resourceAdapterConfig = resourceAdapterConfig;
    }

    public ResourceAdapterConfig[] getResourceAdapterConfig() {
        return this.resourceAdapterConfig;
    }

    public void setResourcesToLoad(Resource[] resourcesToLoad) {
        this.resourcesToLoad = resourcesToLoad;
    }

    public Resource[] getResourcesToLoad() {
        return this.resourcesToLoad;
    }
}
