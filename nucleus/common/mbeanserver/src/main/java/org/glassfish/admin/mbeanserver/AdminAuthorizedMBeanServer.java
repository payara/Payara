/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.mbeanserver;

import com.sun.logging.LogDomains;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.server.RemoteServer;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.*;
import javax.management.remote.MBeanServerForwarder;
import javax.security.auth.Subject;
import org.glassfish.internal.api.AdminAccessController;

/**
 * Allows per-access security checks on MBean attribute set/get and other
 * invoked operations.
 * <p>
 * This class wraps the normal GlassFish MBeanServer with a security checker.
 * If a current Subject exists then the request arrived via an external JMX
 * request.  The Subject will have a Principal that indicates whether read-only
 * or read-write access should be permitted.  This class uses that information
 * to decide, depending on exactly what the request wants to do, whether to allow
 * the current request or not.  If so, it delegates to the real MBeanServer; if
 * not it throws an exception.
 * 
 * @author tjquinn
 */
public class AdminAuthorizedMBeanServer {
    
    private final static Logger mLogger = LogDomains.getLogger(AdminAuthorizedMBeanServer.class, LogDomains.JMX_LOGGER);

    private static final Set<String> RESTRICTED_METHOD_NAMES = new HashSet<String>(Arrays.asList(
            "setAttribute",
            "setAttributes"
        ));
    
    private static class Handler implements InvocationHandler {
        
        private final MBeanServer mBeanServer;
        private final boolean isInstance;
        
        private Handler(final MBeanServer mbs, final boolean isInstance) {
            this.mBeanServer = mbs;
            this.isInstance = isInstance;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final String methodName = method.getName();
            
            if (isAllowed(method, args)) {
                            return method.invoke(mBeanServer, args);
            } else {
                String objectNameString = "??";
                if ((args[0] != null) && (args[0] instanceof ObjectName)) {
                    objectNameString  = ((ObjectName) args[0]).toString();
                }
                final String msg = MessageFormat.format(
                        mLogger.getResourceBundle().getString("jmx.noaccess"),
                        methodName, objectNameString, AdminAccessController.Access.READONLY);
                throw new AccessControlException(msg);
            }
            
        }
        
        private boolean isAllowed(
                final Method method,
                final Object[] args) throws InstanceNotFoundException, IntrospectionException, ReflectionException, NoSuchMethodException {

            return (! isInstance)
                || ( isReadonlyRequest(method, args) )
                ;
        }

        private boolean isReadonlyRequest(final Method method, final Object[] args) throws InstanceNotFoundException, IntrospectionException, ReflectionException, NoSuchMethodException {
            if (RESTRICTED_METHOD_NAMES.contains(method.getName())) {
                return false;
            }
            
            if (method.getName().equals("invoke")) {
                final ObjectName objectName = (ObjectName) args[0];
                final String operationName = (String) args[1];
                final String[] signature = (String[]) args[3];
                final MBeanInfo info = mBeanServer.getMBeanInfo(objectName);
                if (info != null) {
                
                    /*
                    * Find the matching operation.  
                    */
                    for (MBeanOperationInfo opInfo : info.getOperations()) {
                        if (opInfo.getName().equals(operationName) && isSignatureEqual(opInfo.getSignature(), signature)) {
                            if (opInfo.getImpact() != MBeanOperationInfo.INFO) {
                                mLogger.log(Level.FINE, 
                                        "Disallowing access to {0} operation {1} because the impact is declared as {2}", 
                                        new Object[]{
                                            objectName.toString(), 
                                            operationName, 
                                            impactToString(opInfo.getImpact())}
                                        );
                            }
                            /*
                             * This is a read-only request iff the operation 
                             * describes itself as an INFO operation.
                             */
                            return (opInfo.getImpact() == MBeanOperationInfo.INFO);
                        }
                    }
                    /*
                    * No matching operation.
                    */
                    throw new NoSuchMethodException(method.getName());
                }
                
            }
            /*
             * Any other method we'll interpret as read-only for the purposes
             * of off-node access.
             */
            return true;
        }
        
        private static String impactToString(final int impact) {
            String result = null;
            switch (impact) {
                case MBeanOperationInfo.ACTION: result = "action"; break;
                case MBeanOperationInfo.ACTION_INFO : result = "action_info"; break;
                case MBeanOperationInfo.INFO : result = "info" ; break;
                case MBeanOperationInfo.UNKNOWN : result = "unknown"; break;
                default: result = "?";
            }
            return result;
                
        }
        
        private boolean isSignatureEqual(final MBeanParameterInfo[] declaredMBeanParams, final String[] calledSig) {
            if (declaredMBeanParams.length != calledSig.length) {
                return false;
            }
            
            for (int i = 0; i < declaredMBeanParams.length; i++) {
                if (! declaredMBeanParams[i].getType().equals(calledSig[i])) {
                    return false;
                }
            }
            return true;
        }
    }
        
    private static AdminAccessController.Access getAccess() {
        /*
         * Temp workaround.  Still working on this.
         */
        
        return AdminAccessController.Access.FULL;
        
//        AdminAccessController.Access result = JMXAccessInfo.getAccess();
//        if (result == null) {
//            result = AdminAccessController.Access.READONLY;
//        }
//        return result;
    }
    
    /**
     * Returns an MBeanServer that will check security and then forward requests
     * to the real MBeanServer.
     * 
     * @param mbs the real MBeanServer to which to delegate
     * @return the security-checking wrapper around the MBeanServer
     */
    public static MBeanServerForwarder newInstance(final MBeanServer mbs, final boolean isInstance) {
        final Handler handler = new Handler(mbs, isInstance);
        
        return (MBeanServerForwarder) Proxy.newProxyInstance(
                MBeanServerForwarder.class.getClassLoader(),
                new Class[] {MBeanServerForwarder.class},
                handler);
    }
    
}
