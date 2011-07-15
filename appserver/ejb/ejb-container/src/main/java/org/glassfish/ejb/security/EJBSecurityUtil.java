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

package org.glassfish.ejb.security;

import com.sun.ejb.EjbInvocation;
import java.lang.reflect.InvocationTargetException;
import org.glassfish.api.container.Container;
import org.glassfish.ejb.security.application.EJBSecurityManager;

import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * @author Mahesh Kannan
 *         Date: Jul 6, 2008
 */
public class EJBSecurityUtil {
    /**
     * This method is similiar to the runMethod, except it keeps the
     * semantics same as the one in reflection. On failure, if the
     * exception is caused due to reflection, it returns the
     * InvocationTargetException.  This method is called from the
     * containers for ejbTimeout, WebService and MDBs.
     *
     * @param beanClassMethod, the bean class method to be invoked
     * @param inv,             the current invocation
     * @param o,               the object on which this method is to be
     *                         invoked in this case the ejb,
     * @param oa,              the parameters for the method,
     * @param ejbSecMgr,             security manager for this container,
     *                         can be a null value, where in the container will be queried to
     *                         find its security manager.
     * @return Object, the result of the execution of the method.
     */

    public static Object invoke(Method beanClassMethod, EjbInvocation inv, Object o, Object[] oa,
                                EJBSecurityManager ejbSecMgr) throws Throwable {

        final Method meth = beanClassMethod;
        final Object obj = o;
        final Object[] objArr = oa;
        Object ret = null;

        // Optimization.  Skip doAsPrivileged call if this is a local
        // invocation and the target ejb uses caller identity or the
        // System Security Manager is disabled.
        // Still need to execute it within the target bean's policy context.
        // see CR 6331550
        if ((inv.isLocal && ejbSecMgr.getUsesCallerIdentity()) ||
                System.getSecurityManager() == null) {
            ret = ejbSecMgr.runMethod(meth, obj, objArr);
        } else {

            PrivilegedExceptionAction pea =
                    new PrivilegedExceptionAction() {
                        public java.lang.Object run() throws Exception {
                            return meth.invoke(obj, objArr);
                        }
                    };

            try {
                ret = ejbSecMgr.doAsPrivileged(pea);
            } catch (PrivilegedActionException pae) {
                Throwable cause = pae.getCause();
                throw cause;
            }
        }
        return ret;
    }
    
    /** This method is called from the generated code to execute the
     * method.  This is a translation of method.invoke that the
     * generated code needs to do, to invoke a particular ejb
     * method. The method is invoked under a security Subject. This
     * method is called from the generated code.
     * @param Method beanClassMethod, the bean class method to be invoked
     * @param Invocation inv, the current invocation object
     * @param Object o, the object on which this method needs to be invoked,
     * @param Object[] oa, the parameters to the methods,
     * @param Container c, the container from which the appropriate subject is 
     * queried from.
     */
    
    public static Object runMethod(Method beanClassMethod, EjbInvocation inv, Object o, Object[] oa,
           EJBSecurityManager mgr)
    throws Throwable {

	    final Method meth = beanClassMethod;
	    final Object obj = o;
	    final Object[] objArr = oa;
	    Object ret;
	    //EJBSecurityManager mgr = (EJBSecurityManager) c.getSecurityManager();
 	    if (mgr == null) {
 		throw new SecurityException("SecurityManager not set");
	    }

            // Optimization.  Skip doAsPrivileged call if this is a local
            // invocation and the target ejb uses caller identity or the
	    // System Security Manager is disabled.
            // Still need to execute it within the target bean's policy context.
            // see CR 6331550
            if((inv.isLocal && mgr.getUsesCallerIdentity()) || 
	       System.getSecurityManager() == null) {
                ret = mgr.runMethod(meth, obj, objArr);
            } else {
                try {
                    PrivilegedExceptionAction pea =
                        new PrivilegedExceptionAction(){
                            public java.lang.Object run() throws Exception {
                                return meth.invoke(obj, objArr);
                            }
                        };

                    ret = mgr.doAsPrivileged(pea);
                } catch(PrivilegedActionException pae) {
                    Throwable cause = pae.getCause();
                    if( cause instanceof InvocationTargetException ) {
                        cause = ((InvocationTargetException) cause).getCause();
                    } 
                    throw cause;
                } 
            }
	    return ret;
    } 
}
