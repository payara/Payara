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

package com.sun.ejb;

import java.lang.reflect.Method;

import com.sun.ejb.containers.interceptors.InterceptorManager;
import com.sun.enterprise.security.ee.CachedPermission;
import org.glassfish.ejb.deployment.descriptor.EjbRemovalInfo;

/**
 * InvocationInfo caches various attributes of the method that
 * is currently held in the invocation object (that is currently executed)
 * This avoids some of the expensive operations like (for example)
 *      method.getName().startsWith("findByPrimaryKey")
 *
 * Every container maintains a HashMap of method VS invocationInfo that
 *  is populated during container initialization
 *
 * @author Mahesh Kannan
 */

public class InvocationInfo {

    public String     ejbName;
    public Method     method;
    public String     methodIntf;


    public int        txAttr;
    public CachedPermission cachedPermission;

    public boolean    isBusinessMethod;
    public boolean    isHomeFinder;
    public boolean    isCreateHomeFinder;
    public boolean    startsWithCreate;
    public boolean    startsWithFind;
    public boolean    startsWithRemove; 
    public boolean    startsWithFindByPrimaryKey;
    
    // Used by InvocationHandlers to cache bean class methods that 
    // correspond to ejb interface methods.
    public Method     targetMethod1;
    public Method     targetMethod2;
    public boolean    ejbIntfOverride;

    public boolean    flushEnabled;
    public boolean    checkpointEnabled;

    public InterceptorManager.InterceptorChain interceptorChain;

    // method associated with @AroundInvoke or @AroundTimeout
    public Method aroundMethod;
    public boolean isEjbTimeout;


    // Only applies to EJB 3.0 SFSBs
    public EjbRemovalInfo     removalInfo;

    public boolean isTxRequiredLocalCMPField = false;

    public MethodLockInfo methodLockInfo;

    private boolean asyncMethodFlag;

    // Stringified method signature to be used for monitoring
    public String str_method_sig;

    public InvocationInfo() {}
    
    public InvocationInfo(Method method) {
        this.method = method;
    }

    public void setIsAsynchronous(boolean val) {
        this.asyncMethodFlag = val;
    }

    public boolean isAsynchronous() {
        return asyncMethodFlag;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Invocation Info for ejb " + ejbName + "\t");
        sb.append("method=" + method + "\t");
        sb.append("methodIntf = " + methodIntf + "\t");
        if (txAttr != -1) {
            sb.append("tx attr = " + Container.txAttrStrings[txAttr] + "\t");
        } else {
            sb.append("tx attr = -1\t");
        }
        sb.append("Cached permission = " + cachedPermission + "\t");
        sb.append("target method 1 = " + targetMethod1 + "\t");
        sb.append("target method 2 = " + targetMethod2 + "\t");
        sb.append("ejbIntfOverride = " + ejbIntfOverride + "\t");
        sb.append("flushenabled = " + flushEnabled + "\t");
        sb.append("checkpointenabled = " + checkpointEnabled + "\t");
        sb.append("removalInfo = " + removalInfo + "\t");
        sb.append("lockInfo = " + methodLockInfo + "\t");
        sb.append("async = " + asyncMethodFlag + "\t");
        sb.append("\n");
        return sb.toString();
    }
}
