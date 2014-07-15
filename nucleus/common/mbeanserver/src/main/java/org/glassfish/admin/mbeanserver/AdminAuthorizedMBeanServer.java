/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessControlException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.*;
import javax.management.remote.MBeanServerForwarder;
import org.glassfish.internal.api.AdminAccessController;
import org.glassfish.external.amx.AMXGlassfish;
import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * Allows per-access security checks on MBean attribute set/get and other
 * invoked operations.
 * <p>
 * This class wraps the normal GlassFish MBeanServer with a security checker.
 * If control reaches this class then the incoming connection has already
 * authenticated successfully.  This class decides, depending on exactly what 
 * the request wants to do and what MBean is involved, whether to allow
 * the current request or not.  If so, it delegates to the real MBeanServer; if
 * not it throws an exception.
 * <p>
 * Currently we allow all access to non-AMX MBeans.  This permits, for example,
 * the normal operations to view JVM performance characteristics.  If the
 * attempted access concerns an AMX MBean and we're running in the DAS then
 * we allow it - it's OK to adjust configuration via JMX to the DAS.  But if
 * this is a non-DAS instance we make sure the operation on the AMX MBean is 
 * read-only before  allowing it.
 * 
 * @author tjquinn
 */
public class AdminAuthorizedMBeanServer {
    
    private final static Logger mLogger = Util.JMX_LOGGER;
    
    @LogMessageInfo(message = "Attempted access to method {0} on object {1} rejected; user was granted {2} but the operation reports its impact as \"{3}\"", level="FINE")
    private final static String JMX_NOACCESS="NCLS-JMX-00010";
    
    private static final Set<String> RESTRICTED_METHOD_NAMES = new HashSet<String>(Arrays.asList(
            "setAttribute",
            "setAttributes"
        ));
    
    private static final Set<String> METHOD_NAMES_SUBJECT_TO_ACCESS_CONTROL = new HashSet<String> (Arrays.asList(
            "invoke","setAttribute","setAttributes","getAttribute","getAttributes"));
    
    
    private static class Handler implements InvocationHandler {
        
        private final MBeanServer mBeanServer;
        private final boolean isInstance;
        
        private Handler(final MBeanServer mbs, final boolean isInstance) {
            this.mBeanServer = mbs;
            this.isInstance = isInstance;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (isAllowed(method, args)) {
                return method.invoke(mBeanServer, args);
            } else {
                final String format = mLogger.getResourceBundle().getString(JMX_NOACCESS);
                final String objNameString = objectNameString(args);
                final String operationImpact = impactToString(operationImpact(method, args));
                final String msg = MessageFormat.format(format, operationName(method, args),
                        objNameString, AdminAccessController.Access.READONLY, operationImpact);
                mLogger.log(Level.FINE, 
                        "Disallowing access to {0} operation {1} because the impact is declared as {2}", 
                        new Object[]{
                            objNameString, 
                            operationName(method, args), 
                            operationImpact}
                        );
                throw new AccessControlException(msg);
            }
        }
        
        private String operationName(final Method method, final Object[] args) {
            if (method.getName().equals("invoke")) {
                return ((objectNameString(args) == null) || (args.length < 2) || (args[1] == null) ? "null" : (String) args[1]);
            } else {
                return method.getName();
            }
        }
        
        private String objectNameString(Object[] args) {
            return (args == null || args.length == 0 || ( ! (args[0] instanceof ObjectName))) ? null : ((ObjectName) args[0]).toString();
        }
        
        private boolean isAllowed(
                final Method method,
                final Object[] args) throws InstanceNotFoundException, IntrospectionException, ReflectionException, NoSuchMethodException {
            /*
             * Allow access if this is the DAS (not an instance) or if the
             * request does not affect an AMX MBean or if the request is
             * read-only.
             */
            return ( ! isInstance) 
                    ||  ! isSubjectToAccessControl(method, args) // do this before invoking isAMX to avoid intermittent
                                                                 // problems during instance shutdown
                    || isAMX(args)
                    || isReadonlyRequest(method, args);
        }
        
        private boolean isAMX(final Object[] args) {
            return (args == null) 
                    || (args[0] == null)
                    || ( ! (args[0] instanceof ObjectName))
                    || ( ! isAMX((ObjectName) args[0]));
        }
        
        private boolean isAMX(final ObjectName objectName) {
            final String amxDomain = amxDomain();
            return (objectName == null || amxDomain == null) ? false : amxDomain.equals(objectName.getDomain());
        }
        
        private String amxDomain() {
            return AMXGlassfish.DEFAULT.domainRoot().getDomain();
        }

        private boolean isSubjectToAccessControl(final Method method, final Object[] args) {
            return (METHOD_NAMES_SUBJECT_TO_ACCESS_CONTROL.contains(method.getName()));
        }
        
        private boolean isReadonlyRequest(final Method method, final Object[] args) throws InstanceNotFoundException, IntrospectionException, ReflectionException, NoSuchMethodException {
            if (RESTRICTED_METHOD_NAMES.contains(method.getName())) {
                return false;
            }
            
            return ( ! method.getName().equals("invoke") 
                       || (operationImpact(method, args) == MBeanOperationInfo.INFO));
        }
        
        private int operationImpact(final Method method, final Object[] args) throws InstanceNotFoundException, IntrospectionException, ReflectionException, NoSuchMethodException {
            if (RESTRICTED_METHOD_NAMES.contains(method.getName())) {
                return MBeanOperationInfo.ACTION;
            }
            if (method.getName().equals("invoke")) {
                return operationImpactOfInvoke(args);
            } else {
                /*
                 * We've checked for setAttribute(s) and invoke already.  We
                 * are OK with any other operation.
                 */
                return MBeanOperationInfo.INFO;
            }
        }
        
        private int operationImpactOfInvoke(final Object[] args) throws InstanceNotFoundException, IntrospectionException, ReflectionException, NoSuchMethodException {
            
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
                        return opInfo.getImpact();
                    }
                }
                /*
                * No matching operation.
                */
                throw new NoSuchMethodException(operationName);
            }
            return MBeanOperationInfo.UNKNOWN;
        }
        
        private static String impactToString(final int impact) {
            String result;
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
    
    /**
     * Returns an MBeanServer that will check security and then forward requests
     * to the real MBeanServer.
     * 
     * @param mbs the real MBeanServer to which to delegate
     * @return the security-checking wrapper around the MBeanServer
     */
    public static MBeanServerForwarder newInstance(final MBeanServer mbs, final boolean isInstance,
            final BootAMX bootAMX) {
        final AdminAuthorizedMBeanServer.Handler handler = new AdminAuthorizedMBeanServer.Handler(mbs, isInstance);
       
        return (MBeanServerForwarder) Proxy.newProxyInstance(
                MBeanServerForwarder.class.getClassLoader(),
                new Class[] {MBeanServerForwarder.class},
                handler);
    }
    
}
