/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package org.glassfish.admin.rest.adapter;

import static java.util.logging.Level.INFO;
import static org.glassfish.admin.rest.RestLogging.INIT_FAILED;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.security.auth.Subject;

import org.glassfish.admin.rest.RestLogging;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.jersey.inject.hk2.Hk2ReferencingFactory;
import org.glassfish.api.container.EndpointRegistrationException;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.internal.api.InternalSystemAdministrator;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.jersey.internal.ServiceFinder;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.admin.report.PropsFileActionReporter;

/**
 *
 * @author martinmares
 */
@Service
@RunLevel(value= StartupRunLevel.VAL)
public class JerseyContainerCommandService implements PostConstruct {

    @Inject
    protected ServiceLocator habitat;

    @Inject
    private InternalSystemAdministrator kernelIdentity;
    
    private Future<JerseyContainer> future = null;

    @Override
    public void postConstruct() {
        if (Boolean.valueOf(System.getenv("AS_INIT_REST_EAGER"))) {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            this.future = executor.submit(new Callable<JerseyContainer>() {
                                                     @Override
                                                     public JerseyContainer call() throws Exception {
                                                         return exposeContext();
                                                     }
                                                 });
            executor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        final CommandRunner.CommandInvocation invocation =
                                        		habitat.getService(CommandRunner.class)
                                        			   .getCommandInvocation(
                                    					   "uptime", new PropsFileActionReporter(), kernelIdentity.getSubject());
                                        
                                        invocation.parameters(new ParameterMap());
                                        invocation.execute();
                                    }
                                });
            executor.shutdown();
        }
    }


    public JerseyContainer getJerseyContainer() throws EndpointRegistrationException {
        try {
            if (future == null) {
                return exposeContext();
            }
                
            return future.get();
        } catch (InterruptedException ex) {
            return exposeContext();
        } catch (ExecutionException ex) {
            Throwable orig = ex.getCause();
            if (orig instanceof EndpointRegistrationException) {
                throw (EndpointRegistrationException) orig;
            } 
                
            RestLogging.restLogger.log(INFO, INIT_FAILED, orig);
            
            return null;
        }
    }

    private ServerContext getServerContext() {
        return habitat.getService(ServerContext.class);
    }

    private JerseyContainer exposeContext() throws EndpointRegistrationException {
        Set<Class<?>> classes = RestCommandResourceProvider.getResourceClasses();
        // Use common classloader. Jersey artifacts are not visible through
        // module classloader. Actually there is a more important reason to use CommonClassLoader.
        // jax-rs API called RuntimeDelegate makes stupid class loading assumption and throws LinkageError
        // when it finds an implementation of RuntimeDelegate that's part of WLS system class loader.
        // So, we force it to restrict its search space using common class loader.
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader apiClassLoader = getServerContext().getCommonClassLoader();
            Thread.currentThread().setContextClassLoader(apiClassLoader);
            ResourceConfig rc = (new RestCommandResourceProvider()).getResourceConfig(classes, getServerContext(), habitat, getAdditionalBinders());
            return getJerseyContainer(rc);
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    private JerseyContainer getJerseyContainer(ResourceConfig rc) {
        AdminJerseyServiceIteratorProvider iteratorProvider = new AdminJerseyServiceIteratorProvider();
        try {
            ServiceFinder.setIteratorProvider(iteratorProvider);
            final HttpHandler httpHandler = ContainerFactory.createContainer(HttpHandler.class, rc);
            return new JerseyContainer() {
                @Override
                public void service(Request request, Response response) throws Exception {
                    httpHandler.service(request, response);
                }
            };
        } finally {
            iteratorProvider.disable();
        }
    }

    private Set<? extends Binder> getAdditionalBinders() {
        return Collections.singleton(new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(RestAdapter.SubjectReferenceFactory.class).to(new TypeLiteral<Ref<Subject>>() {
                }).in(PerLookup.class);
                
                bindFactory(Hk2ReferencingFactory.<Subject>referenceFactory()).to(new TypeLiteral<Ref<Subject>>() {
                }).in(RequestScoped.class);
            }
        });
    }

}
