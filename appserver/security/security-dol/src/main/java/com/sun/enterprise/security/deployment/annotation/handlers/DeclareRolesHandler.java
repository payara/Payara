/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.deployment.annotation.handlers;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.annotation.context.EjbContext;
import com.sun.enterprise.deployment.annotation.context.WebBundleContext;
import com.sun.enterprise.deployment.annotation.context.WebComponentContext;
import com.sun.enterprise.deployment.annotation.handlers.AbstractCommonAttributeHandler;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.security.common.Role;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.security.DeclareRoles;
import java.lang.annotation.Annotation;

/**
 * This handler is responsible for handling the
 * javax.annotation.security.DeclareRoles.
 *
 * @author Shing Wai Chan
 */
@Service
@AnnotationHandlerFor(DeclareRoles.class)
public class DeclareRolesHandler extends AbstractCommonAttributeHandler {
    
    public DeclareRolesHandler() {
    }

    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
            EjbContext[] ejbContexts) throws AnnotationProcessorException {
        
        DeclareRoles rolesRefAn = (DeclareRoles)ainfo.getAnnotation();

        for (EjbContext ejbContext : ejbContexts) {
            EjbDescriptor ejbDescriptor = ejbContext.getDescriptor();
            for (String roleName : rolesRefAn.value()) {
                if (ejbDescriptor.getRoleReferenceByName(roleName) == null) {
                    RoleReference roleRef = new RoleReference(roleName, "");
                    roleRef.setRolename(roleName);
                    roleRef.setSecurityRoleLink(
                           new SecurityRoleDescriptor(roleName, ""));
                    ejbDescriptor.addRoleReference(roleRef);
                }

                Role role = new Role(roleName);
                ejbDescriptor.getEjbBundleDescriptor().addRole(role);
            }
        }
        return getDefaultProcessedResult();
    }   

    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
            WebComponentContext[] webCompContexts)
            throws AnnotationProcessorException { 
        WebBundleDescriptor webBundleDesc =
            webCompContexts[0].getDescriptor().getWebBundleDescriptor();
        return processAnnotation(ainfo, webBundleDesc);
    }

    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
             WebBundleContext webBundleContext)
             throws AnnotationProcessorException {
        WebBundleDescriptor webBundleDesc = webBundleContext.getDescriptor();
        return processAnnotation(ainfo, webBundleDesc);
    }

    private HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
             WebBundleDescriptor webBundleDesc) {
        DeclareRoles rolesRefAn = (DeclareRoles)ainfo.getAnnotation();
        for (String roleName : rolesRefAn.value()) {
            Role role = new Role(roleName);
            webBundleDesc.addRole(role);
        }
        return getDefaultProcessedResult();
    }

    /**
     * @return an array of annotation types this annotation handler would 
     * require to be processed (if present) before it processes it's own 
     * annotation type.
     */
    public Class<? extends Annotation>[] getTypeDependencies() {
        return getEjbAnnotationTypes();
    }

    protected boolean supportTypeInheritance() {
        return true;
    }
}
