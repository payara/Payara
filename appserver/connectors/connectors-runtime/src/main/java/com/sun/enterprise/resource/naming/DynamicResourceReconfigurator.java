/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.resource.naming;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.spi.BadConnectionEventListener;
import com.sun.enterprise.connectors.ConnectorRegistry;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.resource.DynamicallyReconfigurableResource;
import com.sun.logging.LogDomains;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.glassfish.resourcebase.resources.naming.ResourceNamingService;

import javax.resource.ResourceException;
import javax.resource.spi.RetryableUnavailableException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Invocation Handler used by proxy to connection-factory objects<br>
 *
 * @author Jagadish Ramu
 */
public class DynamicResourceReconfigurator implements InvocationHandler, DynamicallyReconfigurableResource {

    private Object actualObject;
    private ResourceInfo resourceInfo;
    private boolean invalid = false;
    private long resourceInfoVersion = 0;

    protected final static Logger _logger = LogDomains.getLogger(DynamicResourceReconfigurator.class,LogDomains.RSR_LOGGER);

    public DynamicResourceReconfigurator(Object actualObject, ResourceInfo resourceInfo){
        this.actualObject = actualObject;
        this.resourceInfo = resourceInfo;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if (invalid) {
            throw new ResourceException("Resource ["+ resourceInfo +"] instance is not valid any more");
        }

        if (method.getName().equals(DynamicallyReconfigurableResource.SET_DELEGATE_METHOD_NAME) && args.length == 1) {
            setDelegate(args[0]);
        } else if (method.getName().equals(DynamicallyReconfigurableResource.SET_INVALID_METHOD_NAME)
                && args.length == 0) {
            setInvalid();
        } else {
        	long version = ConnectorRegistry.getInstance().getResourceInfoVersion(resourceInfo);
        	if (version == -1L) {
        		// since we're not keeping the list of proxies, we need to invalidate as soon we are aware of the fact
        		setInvalid();
        		invoke(proxy,method,args); // just to trigger the exception
        	}
        	            
        	boolean status = resourceInfoVersion >= version;
        	resourceInfoVersion = version;
        	if (!status) {
        		debug("status is outdated: " + this);                
                Hashtable env = new Hashtable();
                env.put(ConnectorConstants.DYNAMIC_RECONFIGURATION_PROXY_CALL, "TRUE");
                //TODO ASR : resource-naming-service need to support "env" for module/app scope also
                ResourceNamingService namingService = ConnectorRuntime.getRuntime().getResourceNamingService();
                actualObject = namingService.lookup(resourceInfo, resourceInfo.getName(), env);
                debug("actualObject : " + actualObject);
            }else{
                debug("status is true: " + this);
            }

            debug("DynamicResourceReconfigurator : method : " + method.getName());
            try {
                return method.invoke(actualObject, args);
            } catch (InvocationTargetException ite) {
                debug("exception [ " + ite + " ] in method : " + method.getName());
                if (ite.getCause() != null && ite.getCause().getCause() != null){
                    return retryIfNeeded(proxy, method, args, ite.getCause().getCause());
                }else if(ite.getCause() != null){
                    return retryIfNeeded(proxy, method, args, ite.getCause());
                }else{
                    throw ite;
                }
            }
        }
        return null;
    }

    /**
     * retry the operation if it is of expected exception type.
     * @param proxy Proxy object
     * @param method Method to be invoked
     * @param args arguments to method
     * @param actualException ActualException thrown by the method
     * @return Result of invoking the method
     * @throws Throwable when calling the method fails.
     */
    private Object retryIfNeeded(Object proxy, Method method, Object[] args, Throwable actualException)
            throws Throwable {
        if ((actualException instanceof RetryableUnavailableException)) {
            RetryableUnavailableException rue =
                    (RetryableUnavailableException) actualException;
            if (BadConnectionEventListener.POOL_RECONFIGURED_ERROR_CODE.equals(rue.getErrorCode())) {
                debug(" DynamicResourceReconfigurator : retryable-exception in method, retrying : " +
                        method.getName());
                return invoke(proxy, method, args);
            }
        }
            throw actualException;
    }

    public void setDelegate(Object o) {
        actualObject = o;
    }

    public void setInvalid() {
        invalid = true;
    }

    private void debug(String message){
        if(_logger.isLoggable(Level.FINEST)) {
            _logger.finest("[DRC] : " + message);
        }
    }
}
