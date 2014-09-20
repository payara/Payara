/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.resources.naming;


import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.glassfish.resourcebase.resources.util.ResourceUtil;

import javax.naming.*;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * A proxy object factory for an external JNDI factory
 */
public class JndiProxyObjectFactory implements ObjectFactory {

    // for every external-jndi-resource there is an InitialContext
    // created from the factory and environment properties
    private static Hashtable<ResourceInfo, Context> contextMap = new Hashtable<ResourceInfo, Context>();

    public static Context removeInitialContext(ResourceInfo resourceInfo) {
        return contextMap.remove(resourceInfo);
    }

    /**
     * load the context factory
     */
    private Context loadInitialContext(String factoryClass, Hashtable env) {
	Object factory = ResourceUtil.loadObject(factoryClass);
        if (factory == null) {
        	System.err.println("Cannot load external-jndi-resource " +
                                   "factory-class '" + factoryClass + "'");
                return null;
        } else if (! (factory instanceof
                            InitialContextFactory)) {

                System.err.println("external-jndi-resource factory-class '"
                                  + factoryClass + "' must be of type "
                                  + "javax.naming.spi.InitialContextFactory");
                return null;
        }

        Context context = null;
        try {
        	context = ((InitialContextFactory)factory).getInitialContext(env);
        } catch (NamingException ne) {
          	System.err.println("Exception thrown creating initial context " +
                                   "for external JNDI factory '" +
                                   factoryClass + "' " + ne.getMessage());
        }

	return context;
    }

    /**
    * create the object instance from the factory
    */
    public Object getObjectInstance(Object obj,
                    Name name, Context nameCtx, Hashtable environment)
                    throws NamingException {

        // name to lookup in the external factory
        String jndiLookupName = "";
        String jndiFactoryClass = null;
 	    ResourceInfo resourceInfo = null;

        // get the target initial naming context and the lookup name
        Reference ref = (Reference) obj;
        Enumeration addrs = ref.getAll();
        while (addrs.hasMoreElements()) {
            RefAddr addr = (RefAddr) addrs.nextElement();

            String prop = addr.getType();
            if (prop.equals("resourceInfo")) {
                resourceInfo = (ResourceInfo)addr.getContent();
            }
            else if (prop.equals("jndiLookupName")) {
                jndiLookupName = (String) addr.getContent();
            }
            else if (prop.equals("jndiFactoryClass")) {
                jndiFactoryClass = (String) addr.getContent();
            }
        }

        if (resourceInfo == null) {
		    throw new NamingException("JndiProxyObjectFactory: no resourceInfo context info");
	    }

	    ProxyRefAddr contextAddr =
                (ProxyRefAddr)ref.get(resourceInfo.getName());
	    Hashtable env = null;
	    if (contextAddr == null ||
            jndiFactoryClass == null ||
	        (env = (Hashtable)(contextAddr.getContent())) == null) {
		    throw new NamingException("JndiProxyObjectFactory: no info in the " +
                    "reference about the target context; contextAddr = " + contextAddr + " " +
                    "env = " + env + " factoryClass = " + jndiFactoryClass);
	    }

        // Context of the external naming factory
        Context context = contextMap.get(resourceInfo);
        if (context == null) {
            synchronized (contextMap) {
                context = contextMap.get(resourceInfo);
                if (context == null) {
                    context = loadInitialContext(jndiFactoryClass, env);
                    contextMap.put(resourceInfo, context);
                }
            }
        }

        // use the name to lookup in the external JNDI naming context
        Object retObj = null;
        try {
            retObj = context.lookup(jndiLookupName);
        } catch (NameNotFoundException e) {
            //Fixing issue: http://java.net/jira/browse/GLASSFISH-15447
            throw new ExternalNameNotFoundException(e);
        }  catch (javax.naming.NamingException ne) {
            //Fixing issue: http://java.net/jira/browse/GLASSFISH-15447
            context = loadInitialContext(jndiFactoryClass, env);
            if (context == null) {
                throw new NamingException ("JndiProxyObjectFactory no InitialContext" + jndiFactoryClass);
            } else {
                contextMap.put(resourceInfo, context);
                try {
                    retObj = context.lookup(jndiLookupName);
                } catch (NameNotFoundException e) {
                    throw new ExternalNameNotFoundException(e);
                }
            }
        }
        return retObj;
    }
}
