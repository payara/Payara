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

package org.glassfish.api.naming;

import org.jvnet.hk2.annotations.Contract;

import javax.naming.Context;

import javax.naming.Name;
import javax.naming.NamingException;
import java.util.Collection;
import org.omg.CORBA.ORB;

import java.rmi.Remote;
import java.util.Hashtable;
import java.util.Map;

/**
 * The NamingManager provides an interface for various components to use naming
 * functionality. It provides methods for binding and unbinding environment
 * properties, resource and ejb references.
 */

@Contract
public interface GlassfishNamingManager {

    public static final String LOGICAL_NAME ="com.sun.enterprise.naming.logicalName";
    
    public static final String NAMESPACE_METADATA_KEY = "NamespacePrefixes";

    /**
     * Get the initial context.
     */

    public Context getInitialContext();

    /**
     *
     * Lookup a naming entry for a particular componentId
     */
    public Object lookup(String componentId, String name) throws NamingException;

    /**
     *
     * Lookup a naming entry in a particular application's namespace
     * @param appName application-name
     * @param name name of the object
     * @param env Environment
     * @return Object found by the name
     * @throws javax.naming.NamingException when unable to find the object
     */
    public Object lookupFromAppNamespace(String appName, String name, Hashtable env) throws NamingException ;

    /**
     *
     * Lookup a naming entry in a particular application's module's namespace
     * @param appName application-name
     * @param moduleName module-name
     * @param name name of the object
     * @param env Environment
     * @return Object found by the name
     * @throws javax.naming.NamingException when unable to find the object
     */
    public Object lookupFromModuleNamespace(String appName, String moduleName, String name, Hashtable env)
            throws NamingException ;

    /**
     * Publish an object in the naming service.
     *
     * @param name   Object that needs to be bound.
     * @param obj    Name that the object is bound as.
     * @param rebind operation is a bind or a rebind.
     * @throws Exception
     */

    public void publishObject(String name, Object obj, boolean rebind)
            throws NamingException;

    /**
     * Publish an object in the naming service.
     *
     * @param name   Object that needs to be bound.
     * @param obj    Name that the object is bound as.
     * @param rebind operation is a bind or a rebind.
     * @throws Exception
     */

    public void publishObject(Name name, Object obj, boolean rebind)
            throws NamingException;

    /**
     * Publish a CosNaming object.  The object is published to both
     * the server's CosNaming service and the global naming service.
     * Objects published with this method must be unpublished via
     * unpublishCosNamingObject.
     *
     * @param name   Object that needs to be bound.
     * @param obj    Name that the object is bound as.
     * @param rebind operation is a bind or a rebind.
     * @throws Exception
     */

    public void publishCosNamingObject(String name, Object obj, boolean rebind)
            throws NamingException;

    /**
     * This method enumerates the env properties, ejb and resource references
     * etc for a J2EE component and binds them in the applicable java:
     * namespace.
     *
     * @param treatComponentAsModule true if java:comp and java:module refer to the same
     *         namespace
     *
     */
    public void bindToComponentNamespace(String appName, String moduleName,
                                         String componentId, boolean treatComponentAsModule, 
                                         Collection<? extends JNDIBinding> bindings)
            throws NamingException;


    /**
     * Binds the bindings to module namespace of an application<br>
     * Typically, to get access to application's namespace, invocation context
     * must be set to appropriate application's context.<br>
     * This API is useful in cases where containers within GlassFish
     * need to bind the objects in application's name-space and do not have
     * application's invocation context<br>
     * @param appName application-name
     * @param bindings list of bindings
     * @throws NamingException when unable to bind the bindings
     */
    public void bindToAppNamespace(String appName, Collection<? extends JNDIBinding> bindings)
            throws NamingException;

    /**
     * Binds the bindings to module namespace of an application<br>
     * Typically, to get access to application's module namespace, invocation context
     * must be set to appropriate application's context.<br>
     * This API is useful in cases where containers within GlassFish
     * need to bind the objects in application's module name-space and do not have
     * application's invocation context<br>
     * @param appName application-name
     * @param moduleName module-name
     * @param bindings list of bindings
     * @throws NamingException when unable to bind the bindings
     */
    public void bindToModuleNamespace(String appName, String moduleName, Collection<? extends JNDIBinding> bindings)
            throws NamingException;

    /**
     * Remove an object from the naming service.
     *
     * @param name Name that the object is bound as.
     * @throws Exception
     */
    public void unpublishObject(String name) throws NamingException;

    /**
     * Remove an object from the CosNaming service and global naming service.
     *
     * @param name Name that the object is bound as.
     * @throws Exception
     */
    public void unpublishCosNamingObject(String name) throws NamingException;

    /**
     * Remove an object from the application's namespace.<br>
     * Typically, to get access to application's namespace, invocation context
     * must be set to appropriate application's context.<br>
     * This API is useful in cases where containers within GlassFish
     * need to unbind the objects in application's name-space and do not have
     * application's invocation context<br>
     * @param name Name that the object is bound as.
     * @param appName application-name
     * @throws NamingException when unable to unbind the object
     */
    public void unbindAppObject(String appName, String name) throws NamingException;

    /**
     * Remove an object from the module name-space of an application<br>
     * Typically, to get access to application's module namespace, invocation context
     * must be set to appropriate application's context.<br>
     * This API is useful in cases where containers within GlassFish
     * need to unbind the objects in application's module name-space and do not have
     * application's invocation context<br>
     * @param name Name that the object is bound as.
     * @param appName application-name
     * @param moduleName module-name
     * @throws NamingException when unable to unbind the object
     */
    public void unbindModuleObject(String appName, String moduleName, String name) throws NamingException;

    /**
     * Remove an object from the naming service.
     *
     * @param name Name that the object is bound as.
     * @throws Exception
     */
    public void unpublishObject(Name name) throws NamingException;


    /**
     *
     * Unbind component-level bindings
     */
    public void unbindComponentObjects(String componentId) throws NamingException;


    /**
     * Unbind app and module level bindings for the given app name.
     */
    public void unbindAppObjects(String appName) throws NamingException;

    /**
     * Recreate a context for java:comp/env or one of its sub-contexts given the
     * context name.
     */
    public Context restoreJavaCompEnvContext(String contextName)
            throws NamingException;

    /**
     * Initialize RMI-IIOP naming services 
     * @param orb
     * @return RemoteSerialProvider object instance
     */
    public Remote initializeRemoteNamingSupport(ORB orb) throws NamingException;

}
