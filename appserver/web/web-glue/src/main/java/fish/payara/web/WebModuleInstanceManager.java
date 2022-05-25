/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.web;

import com.sun.enterprise.container.common.spi.JCDIService;
import com.sun.enterprise.container.common.spi.util.InjectionException;
import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.web.WebComponentInvocation;
import com.sun.enterprise.web.WebModule;
import jakarta.servlet.jsp.tagext.JspTag;
import org.apache.tomcat.InstanceManager;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.web.LogFacade;

import javax.naming.NamingException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of Tomcat {@link InstanceManager} for use with {@link WebModule}. The
 * {@link org.apache.catalina.core.DefaultInstanceManager} provided by Catalina would not integrate itself with the
 * {@link InvocationManager} or {@link InjectionManager} (so would not use Weld as the CDI implementation).
 */
public class WebModuleInstanceManager implements InstanceManager {

    private static final Logger LOGGER = LogFacade.getLogger();
    private static final ResourceBundle RESOURCE_BUNDLE = LOGGER.getResourceBundle();

    private InjectionManager injectionManager;

    private InvocationManager invocationManager;

    private JCDIService jcdiService;

    private WebModule webModule;

    public WebModuleInstanceManager(WebModule webModule) {
        this.webModule = webModule;
        initialiseServices();
    }

    /**
     * Look up {@link InvocationManager}, {@link com.sun.enterprise.container.common.spi.util.InjectionManager},
     * and {@link JCDIService} using {@link ServiceLocator} obtained from
     * {@link ServerContext} if any of them have not already been injected or looked up.
     *
     * @throws IllegalStateException if a {@link ServerContext} could not be obtained from the {@link WebModule}
     */
    private void initialiseServices() throws IllegalStateException {
        ServiceLocator services;
        services = WebModuleGlueUtil.getServerContext(webModule).getDefaultServices();

        if (invocationManager == null) {
            invocationManager = services.getService(InvocationManager.class);
        }

        if (injectionManager == null) {
            injectionManager = services.getService(InjectionManager.class);
        }

        if (jcdiService == null) {
            jcdiService = services.getService(JCDIService.class);
        }
    }

    @Override
    public Object newInstance(Class<?> clazz) throws IllegalAccessException, InvocationTargetException, NamingException,
            InstantiationException, IllegalArgumentException, NoSuchMethodException, SecurityException {
        return createCdiManagedInstance(clazz);
    }

    @Override
    public Object newInstance(String className) throws IllegalAccessException, InvocationTargetException,
            NamingException, InstantiationException, ClassNotFoundException, IllegalArgumentException,
            NoSuchMethodException, SecurityException {
        Class<?> clazz = webModule.getClassLoader().loadClass(className);
        return newInstance(clazz);
    }

    @Override
    public Object newInstance(String className, ClassLoader classLoader) throws IllegalAccessException,
            InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException,
            IllegalArgumentException, NoSuchMethodException, SecurityException {
        Class<?> clazz = classLoader.loadClass(className);
        return newInstance(clazz);
    }

    /**
     * Creates a CDI managed instance of the given class and performs any necessary injections.
     *
     * This functionality previously existed under {@link com.sun.enterprise.web.WebContainer}, split over numerous
     * methods (e.g. createServletInstance).
     *
     * @param clazz
     * @return
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws InvocationTargetException
     */
    private Object createCdiManagedInstance(Class<?> clazz) throws IllegalArgumentException, SecurityException,
            InvocationTargetException {
        validateJSR299Scope(clazz);
        WebComponentInvocation webComponentInvocation = new WebComponentInvocation(webModule);

        try {
            invocationManager.preInvoke(webComponentInvocation);
            return injectionManager.createManagedObject(clazz);
        } catch (InjectionException injectionException) {
            LOGGER.log(Level.SEVERE, injectionException.getMessage());
            throw new InvocationTargetException(injectionException);
        } finally {
            invocationManager.postInvoke(webComponentInvocation);
        }
    }

    /**
     * According to SRV 15.5.15, Servlets, Filters, Listeners can only be without any scope annotation or are annotated
     * with @Dependent scope. All other scopes are invalid and must be rejected.
     *
     * This method previously existed under {@link com.sun.enterprise.web.WebContainer}.
     */
    private void validateJSR299Scope(Class<?> clazz) {
        // Don't validate if class extends JspTag
        if (clazz.isInstance(JspTag.class)) {
            return;
        }

        if (jcdiService != null && jcdiService.isCDIScoped(clazz)) {
            String msg = RESOURCE_BUNDLE.getString(LogFacade.INVALID_ANNOTATION_SCOPE);
            msg = MessageFormat.format(msg, clazz.getName());
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public void newInstance(Object object) throws IllegalAccessException, InvocationTargetException, NamingException {
        processInstantiatedInstance(object);
    }

    /**
     * Processes and injects an already instantiated object (typically a programmatic dynamic filter registration)
     *
     * @param object The object to process and inject
     * @throws InvocationTargetException If there's an issue injecting the instance
     */
    private void processInstantiatedInstance(Object object) throws InvocationTargetException {
        validateJSR299Scope(object.getClass());
        WebComponentInvocation webComponentInvocation = new WebComponentInvocation(webModule);

        try {
            invocationManager.preInvoke(webComponentInvocation);
            injectionManager.injectInstance(object);
        } catch (InjectionException injectionException) {
            LOGGER.log(Level.SEVERE, injectionException.getMessage());
            throw new InvocationTargetException(injectionException);
        } finally {
            invocationManager.postInvoke(webComponentInvocation);
        }
    }

    @Override
    public void destroyInstance(Object object) throws IllegalAccessException, InvocationTargetException {
        try {
            injectionManager.destroyManagedObject(object);
        } catch (InjectionException injectionException) {
            LOGGER.log(Level.WARNING, "Could not destroy managed object of class {0}", object.getClass().getName());
        }
    }
}
