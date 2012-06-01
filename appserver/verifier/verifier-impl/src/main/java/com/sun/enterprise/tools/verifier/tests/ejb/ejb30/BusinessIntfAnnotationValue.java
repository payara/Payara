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

package com.sun.enterprise.tools.verifier.tests.ejb.ejb30;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

import javax.ejb.Local;
import javax.ejb.Remote;
import java.util.Set;

/**
 * It is an error if @Local or @Remote is specified both on the bean class and 
 * on the referenced interface and the values differ. (Expert Group discussions)
 * 
 * @author Vikas Awasthi
 */
public class BusinessIntfAnnotationValue extends EjbTest {

    private Result result;
    private ComponentNameConstructor compName;
    private Class<Remote> remoteAnn = Remote.class;
    private Class<Local> localAnn = Local.class;
    
    public Result check(EjbDescriptor descriptor) {
        result = getInitializedResult();
        compName = getVerifierContext().getComponentNameConstructor();

        testInterfaces(descriptor.getLocalBusinessClassNames(), remoteAnn);
        testInterfaces(descriptor.getRemoteBusinessClassNames(), localAnn);
    
        if(result.getStatus() != Result.FAILED) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                            (getClass().getName() + ".passed",
                            "Valid annotations used in business interface(s)."));
        }
        return result;
    }
    
    private void testInterfaces(Set<String> interfaces, Class annot) {
        // used in failure message
        Class cls = (annot.equals(localAnn))? remoteAnn : localAnn;

        for (String intf : interfaces) {
            try {
                Class intfCls = Class.forName(intf,
                                             false,
                                             getVerifierContext().getClassLoader());
                if(intfCls.getAnnotation(annot)!=null) {
                    addErrorDetails(result, compName);
                    result.failed(smh.getLocalString
                                 (getClass().getName() + ".failed",
                                 "{0} annotation is used in {1} interface [ {2} ].", 
                                 new Object[]{annot.getSimpleName(), 
                                             cls.getSimpleName(), 
                                             intfCls.getName()}));
                }
            } catch (ClassNotFoundException e) {
             //ignore as it will be caught in other tests
            }
        }
    }
}
