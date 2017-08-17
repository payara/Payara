/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.weld.jsf;

import javax.el.ELContextListener;
import javax.el.ExpressionFactory;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.application.Application;
import javax.faces.application.ApplicationWrapper;
import javax.faces.context.FacesContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;


import org.glassfish.weld.util.Util;

import org.apache.jasper.runtime.JspApplicationContextImpl;

public class WeldApplication extends ApplicationWrapper {
   
    private final Application application;
    private ExpressionFactory expressionFactory;
   
    public WeldApplication(Application application) {
        this.application = application;
        BeanManager beanManager = getBeanManager();
        if (beanManager != null) {
            application.addELContextListener(Util.<ELContextListener>newInstance(
                "org.jboss.weld.module.web.el.WeldELContextListener"));
            application.addELResolver(beanManager.getELResolver());
            JspApplicationContext jspAppContext = JspFactory.getDefaultFactory().
                getJspApplicationContext((ServletContext)FacesContext.getCurrentInstance().getExternalContext().getContext());
            this.expressionFactory = beanManager.wrapExpressionFactory(jspAppContext.getExpressionFactory());
            ((JspApplicationContextImpl)jspAppContext).setExpressionFactory(this.expressionFactory);
        }
    }

    public Application getWrapped() {
        return this.application;
    }

    @Override
    public ExpressionFactory getExpressionFactory() {
        if (this.expressionFactory == null) {
            BeanManager beanManager = getBeanManager();
            if (beanManager != null) {
                this.expressionFactory = beanManager.wrapExpressionFactory(getWrapped().getExpressionFactory());
          } else {
              this.expressionFactory = getWrapped().getExpressionFactory(); 
          }
        }
        return expressionFactory;
    }

    private BeanManager getBeanManager() {
        try {
            InitialContext context = new InitialContext();
            return (BeanManager) context.lookup("java:comp/BeanManager");
        } catch (NamingException e) {
            return null;
        }

    }
}
