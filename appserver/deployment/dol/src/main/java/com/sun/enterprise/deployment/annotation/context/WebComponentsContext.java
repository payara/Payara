/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.annotation.context;

import com.sun.enterprise.deployment.WebComponentDescriptor;
import org.glassfish.apf.context.AnnotationContext;

import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;

/**
 * This provides a context for a collection of web components with the same
 * impl class name.
 *
 * @author Shing Wai Chan
 */
public class WebComponentsContext extends AnnotationContext
            implements ComponentContext {

    private WebComponentContext[] webCompContexts;
    private String componentClassName;

    public WebComponentsContext(WebComponentDescriptor[] webComps) {
        webCompContexts = new WebComponentContext[webComps.length];
        for (int i = 0; i < webComps.length ; i++) {
            webCompContexts[i] = new WebComponentContext(webComps[i]);
        }
        if (webComps[0].isServlet()) {
            componentClassName = webComps[0].getWebComponentImplementation();
        }
    }
   
    /**
     * Create a new instance of WebComponentContext.
     * Note that, for performance, we don't make a safe copy of array here.
     */
    public WebComponentsContext(WebComponentContext[] webCompContexts) {
        this.webCompContexts = webCompContexts;
        this.componentClassName = webCompContexts[0].getComponentClassName();
    }

    /**
     * Note that, for performance, we don't make a safe copy of array here.
     */
    public WebComponentContext[] getWebComponentContexts() {
        return webCompContexts;
    }
    
    public void endElement(ElementType type, AnnotatedElement element) {
        
        if (ElementType.TYPE.equals(type)) {
            // done with processing this class, let's pop this context
            getProcessingContext().popHandler();
        }
    }

    public String getComponentClassName() {
        return componentClassName;
    }
}
