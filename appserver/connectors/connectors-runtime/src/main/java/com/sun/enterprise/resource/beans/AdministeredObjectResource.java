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

package com.sun.enterprise.resource.beans;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.enterprise.connectors.ConnectorRegistry;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.connectors.util.SetMethodAction;
import com.sun.enterprise.deployment.AdminObject;
import com.sun.enterprise.deployment.ConnectorConfigProperty;
import com.sun.logging.LogDomains;
import org.glassfish.resources.api.JavaEEResource;
import org.glassfish.resources.api.JavaEEResourceBase;
import org.glassfish.resources.naming.SerializableObjectRefAddr;
import org.glassfish.resourcebase.resources.api.ResourceInfo;

import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resource infor for Connector administered objects
 *
 * @author Qingqing Ouyang
 */
public class AdministeredObjectResource extends JavaEEResourceBase {

    private final static Logger _logger = LogDomains.getLogger(AdministeredObjectResource.class, LogDomains.RSR_LOGGER);

    private String resadapter_;
    private String adminObjectClass_;
    private String adminObjectType_;
    private Set configProperties_;


    public AdministeredObjectResource(ResourceInfo resourceInfo) {
        super(resourceInfo);
    }

    protected JavaEEResource doClone(ResourceInfo resourceInfo) {
        AdministeredObjectResource clone =
                new AdministeredObjectResource(resourceInfo);
        clone.setResourceAdapter(getResourceAdapter());
        clone.setAdminObjectType(getAdminObjectType());
        return clone;
    }


    public int getType() {
        // FIX ME
        return 0;
        //return J2EEResource.ADMINISTERED_OBJECT;
    }

    public void initialize(AdminObject desc) {
        configProperties_ = new HashSet();
        adminObjectClass_ = desc.getAdminObjectClass();
        adminObjectType_ = desc.getAdminObjectInterface();
    }

    public String getResourceAdapter() {
        return resadapter_;
    }

    public void setResourceAdapter(String resadapter) {
        resadapter_ = resadapter;
    }

    public String getAdminObjectType() {
        return adminObjectType_;
    }

    public void setAdminObjectType(String adminObjectType) {
        this.adminObjectType_ = adminObjectType;
    }

    public void setAdminObjectClass(String name) {
        this.adminObjectClass_ = name;
    }

    public String getAdminObjectClass() {
        return this.adminObjectClass_;
    }

    /*
     * Add a configProperty to the set
     */
    public void addConfigProperty(ConnectorConfigProperty  configProperty) {
        this.configProperties_.add(configProperty);
    }

    /**
     * Add a configProperty to the set
     */
    public void removeConfigProperty(ConnectorConfigProperty  configProperty) {
        this.configProperties_.remove(configProperty);
    }

    public Reference createAdminObjectReference() {
        Reference ref =
                new Reference(getAdminObjectType(),
                        new SerializableObjectRefAddr("jndiName", this),
                        ConnectorConstants.ADMINISTERED_OBJECT_FACTORY, null);

        return ref;
    }


    // called by com.sun.enterprise.naming.factory.AdministeredObjectFactory
    // FIXME.  embedded??
    public Object createAdministeredObject(ClassLoader jcl)
            throws PoolingException {

        try {
            if (jcl == null) {
                // use context class loader
                jcl = (ClassLoader) AccessController.doPrivileged
                        (new PrivilegedAction() {
                            public Object run() {
                                return
                                        Thread.currentThread().getContextClassLoader();
                            }
                        });
            }


            Object adminObject =
                    jcl.loadClass(adminObjectClass_).newInstance();

            AccessController.doPrivileged
                    (new SetMethodAction(adminObject, configProperties_));

        // associate ResourceAdapter if the admin-object is RAA    
        if(adminObject instanceof ResourceAdapterAssociation){
            try {
                ResourceAdapter ra = ConnectorRegistry.getInstance().
                        getActiveResourceAdapter(resadapter_).getResourceAdapter();
                ((ResourceAdapterAssociation) adminObject).setResourceAdapter(ra);
            } catch (ResourceException ex) {
                _logger.log(Level.SEVERE, "rardeployment.assoc_failed", ex);
            }
        }

            // At this stage, administered object is instantiated, config properties applied
            // validate administered object

            //ConnectorRuntime should be available in CLIENT mode now as admin-object-factory would have bootstapped
            //connector-runtime.
            ConnectorRuntime.getRuntime().getConnectorBeanValidator().validateJavaBean(adminObject, resadapter_);

            return adminObject;
        } catch (PrivilegedActionException ex) {
            throw(PoolingException) (new PoolingException().initCause(ex));
        } catch (Exception ex) {
            throw(PoolingException) (new PoolingException().initCause(ex));
        }
    }

    public String toString() {
        return "< Administered Object : " + getResourceInfo() +
                " , " + getResourceAdapter() +
                " , " + getAdminObjectType() + " >";
    }
}
