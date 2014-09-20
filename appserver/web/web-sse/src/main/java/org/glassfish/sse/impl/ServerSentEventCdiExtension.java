/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.sse.impl;

import org.glassfish.sse.api.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A CDI extension that creates ServerSentEventHandlerContext beans so that
 * they can be injected into other EE components
 *
 * @author Jitendra Kotamraju
 */
public class ServerSentEventCdiExtension implements Extension {
    
    private final Logger LOGGER = Logger.getLogger(ServerSentEventCdiExtension.class.getName());

    // path --> application
    private final Map<String, ServerSentEventApplication> applicationMap
        = new HashMap<String, ServerSentEventApplication>();

    public Map<String, ServerSentEventApplication> getApplicationMap() {
        return applicationMap;
    }

    @SuppressWarnings("UnusedDeclaration")
    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        bbd.addQualifier(ServerSentEventContext.class);
    }

    @SuppressWarnings("ClassExplicitlyAnnotation")
    static class WebHandlerContextAnnotationLiteral extends AnnotationLiteral<ServerSentEventContext> implements ServerSentEventContext {
        private final String path;

        WebHandlerContextAnnotationLiteral(String path) {
            this.path = path;
        }

        @Override
        public String value() {
            return path;
        }
    }

    static class ServerSentEventHandlerContextBean implements Bean<ServerSentEventHandlerContext> {

        private final String path;
        private final ServerSentEventHandlerContext instance;
        private final Class<?> handlerClass;

        ServerSentEventHandlerContextBean(String path, ServerSentEventHandlerContext instance, Class<?> handlerClass) {
            this.path = path;
            this.instance = instance;
            this.handlerClass = handlerClass;
        }

        @Override
        public Set<Type> getTypes() {
            Set<Type> types = new HashSet<Type>();
            types.add(new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments() {
                    return new Type[] {handlerClass};
                }

                @Override
                public Type getRawType() {
                    return ServerSentEventHandlerContext.class;
                }

                @Override
                public Type getOwnerType() {
                    return null;
                }
            });
            return types;
        }

        @Override
        public Set<Annotation> getQualifiers() {
            Set<Annotation> qualifiers = new HashSet<Annotation>();
            qualifiers.add(new WebHandlerContextAnnotationLiteral(path));
            return qualifiers;
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return ApplicationScoped.class;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return Collections.emptySet();
        }

        @Override
        public Class<?> getBeanClass() {
            return ServerSentEventHandlerContext.class;
        }

        @Override
        public boolean isAlternative() {
            return false;
        }

        @Override
        public boolean isNullable() {
            return false;
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return Collections.emptySet();
        }

        @Override
        public ServerSentEventHandlerContext create(CreationalContext<ServerSentEventHandlerContext> context) {
            return instance;
        }

        @Override
        public void destroy(ServerSentEventHandlerContext instance, CreationalContext<ServerSentEventHandlerContext> context) {
        }

    }

    // For each ServerSentEvent hanlder, it creates a corresponding ServerSentHandlerContext
    // This context can be got anywhere from BeanManager
    @SuppressWarnings("UnusedDeclaration")
    void afterBeanDiscovery(@Observes AfterBeanDiscovery bbd) {
        for(Map.Entry<String, ServerSentEventApplication> entry : applicationMap.entrySet()) {
            bbd.addBean(new ServerSentEventHandlerContextBean(entry.getKey(), entry.getValue().getHandlerContext(),
                    entry.getValue().getHandlerClass()));
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> pat,
            BeanManager beanManager) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("scanning type: " + pat.getAnnotatedType().getJavaClass().getName());
        }

        for (Annotation an : pat.getAnnotatedType().getAnnotations()) {
            Class clazz = pat.getAnnotatedType().getJavaClass();

            if (an instanceof ServerSentEvent) {
                if (!ServerSentEventHandler.class.isAssignableFrom(clazz)) {
                    throw new RuntimeException("Invalid base class '"
                            + clazz.getName() + "' for handler.");
                }
                ServerSentEvent wh = (ServerSentEvent) an;
                String path = normalizePath(wh.value());
                ServerSentEventApplication app = applicationMap.get(path);
                if (app != null) {
                    throw new RuntimeException("Two ServerSentEvent handlers are mapped to same path="+path);
                }
                app = new ServerSentEventApplication(clazz, path);
                applicationMap.put(path, app);
            }
        }
    }

    private String normalizePath(String path) {
        path = path.trim();
        return path.startsWith("/") ? path : "/" + path;
    }

}
