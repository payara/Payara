/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.weld;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.jasper.runtime.JspApplicationContextImpl;
import org.glassfish.cdi.CDILoggerInfo;
import org.jboss.weld.el.WeldELContextListener;

/**
 * ServletContextListener implementation that ensures (for Weld applications)
 * the correct Weld EL Resolver and Weld EL Context Listener is used for JSP(s).
 */  
public class WeldContextListener implements ServletContextListener {

    private Logger logger = Logger.getLogger(WeldContextListener.class.getName());

    @Inject
    private BeanManager beanManager;

    /**
     * Stash the Weld EL Resolver and Weld EL Context Listener so it is recognized by JSP.
     */
    public void contextInitialized(ServletContextEvent servletContextEvent) {

        if (null != beanManager) {
             JspApplicationContext jspAppContext = getJspApplicationContext(servletContextEvent);
             jspAppContext.addELResolver(beanManager.getELResolver());

             try {
                 Class weldClass = Class.forName("org.jboss.weld.el.WeldELContextListener");
                 WeldELContextListener welcl = ( WeldELContextListener ) weldClass.newInstance();
                 jspAppContext.addELContextListener(welcl);
             } catch (Exception e) {
                 logger.log(Level.WARNING,
                            CDILoggerInfo.CDI_COULD_NOT_CREATE_WELDELCONTEXTlISTENER,
                            new Object [] {e});
             }

            ( ( JspApplicationContextImpl ) jspAppContext ).setExpressionFactory(
                beanManager.wrapExpressionFactory(jspAppContext.getExpressionFactory()));
        }
    }

    public void contextDestroyed(ServletContextEvent sce) {
        if (null != beanManager) {
            beanManager = null;
        }
    }

    protected JspApplicationContext getJspApplicationContext(ServletContextEvent servletContextEvent) {
        return JspFactory.getDefaultFactory().getJspApplicationContext(servletContextEvent.getServletContext());
    }
}
